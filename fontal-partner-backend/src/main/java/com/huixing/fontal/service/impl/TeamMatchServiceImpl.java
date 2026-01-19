package com.huixing.fontal.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.huixing.fontal.common.ErrorCode;
import com.huixing.fontal.exception.BusinessException;
import com.huixing.fontal.mapper.TeamMapper;
import com.huixing.fontal.model.entity.Team;
import com.huixing.fontal.model.entity.User;
import com.huixing.fontal.model.vo.TeamVo;
import com.huixing.fontal.service.TeamMatchService;
import com.huixing.fontal.service.UserService;
import com.huixing.fontal.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 队伍匹配服务实现类
 * 提供基于标签相似度的队伍推荐功能
 *
 * @author fontal
 */
@Slf4j
@Service
public class TeamMatchServiceImpl extends ServiceImpl<TeamMapper, Team> implements TeamMatchService {

    private static final Gson GSON = new Gson();
    private static final int RECOMMEND_LIMIT = 20;
    
    @Resource
    private UserService userService;
    
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 获取推荐队伍（混合策略）
     * 考虑标签相似度、队伍活跃度、时间等因素
     */
    @Override
    public List<TeamVo> getRecommendTeams(Long userId, int limit) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }
        
        // 1. 尝试从缓存获取
        String cacheKey = String.format("fontal:team:recommend:%s:%d", userId, limit);
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        List<TeamVo> cachedResult = (List<TeamVo>) valueOperations.get(cacheKey);
        if (cachedResult != null) {
            return cachedResult;
        }

        // 2. 获取用户信息
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }

        // 3. 解析用户标签
        Set<String> userTags = StringUtils.stringJsonListToStringSet(user.getTags());
        if (CollectionUtils.isEmpty(userTags)) {
            // 如果用户没有标签，返回热门队伍
            return getHotTeams(limit);
        }

        // 4. 查询所有有效队伍
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("isDelete", 0)
                .gt("expireTime", new Date())
                .select("id", "teamName", "teamAvatarUrl", "teamDesc", "maxNum", 
                        "userId", "usersId", "teamStatus", "createTime", "expireTime");
        List<Team> allTeams = this.list(queryWrapper);

        if (CollectionUtils.isEmpty(allTeams)) {
            return new ArrayList<>();
        }

        // 5. 使用并行流计算推荐分数
        long startTime = System.currentTimeMillis();
        
        // 获取用户已加入的队伍ID
        Set<Long> joinedTeamIds = StringUtils.stringJsonListToLongSet(user.getTeamIds());
        
        List<Team> recommendTeams = allTeams.parallelStream()
                .filter(team -> !joinedTeamIds.contains(team.getId())) // 排除已加入的队伍
                .map(team -> {
                    double score = computeRecommendScore(team, userTags);
                    return new AbstractMap.SimpleEntry<>(team, score);
                })
                .filter(entry -> entry.getValue() > 0) // 只保留有分数的队伍
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue())) // 降序排序
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        log.info("计算队伍推荐分数耗时: {}ms, 用户ID: {}", System.currentTimeMillis() - startTime, userId);

        // 6. 转换为TeamVo并设置用户信息
        List<TeamVo> teamVoList = convertToTeamVoList(recommendTeams);

        // 7. 存入缓存（随机过期时间防止缓存雪崩）
        int expireTime = 10 + RandomUtil.randomInt(1, 5);
        valueOperations.set(cacheKey, teamVoList, expireTime, TimeUnit.MINUTES);

        return teamVoList;
    }

    /**
     * 按标签推荐队伍
     * 根据用户标签查找包含相似标签的队伍
     */
    @Override
    public List<TeamVo> getTeamsByTags(Set<String> tags, int limit) {
        if (CollectionUtils.isEmpty(tags)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签不能为空");
        }

        // 1. 尝试从缓存获取
        String cacheKey = String.format("fontal:team:tags:%s:%d", 
                String.join(",", tags), limit);
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        List<TeamVo> cachedResult = (List<TeamVo>) valueOperations.get(cacheKey);
        if (cachedResult != null) {
            return cachedResult;
        }

        // 2. 查询所有有效队伍
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("isDelete", 0)
                .gt("expireTime", new Date())
                .select("id", "teamName", "teamAvatarUrl", "teamDesc", "maxNum", 
                        "userId", "usersId", "teamStatus", "createTime", "expireTime");
        List<Team> allTeams = this.list(queryWrapper);

        if (CollectionUtils.isEmpty(allTeams)) {
            return new ArrayList<>();
        }

        // 3. 根据标签计算相似度
        List<Team> matchedTeams = allTeams.stream()
                .filter(team -> {
                    // 获取队伍成员的标签集合
                    Set<Long> memberIds = StringUtils.stringJsonListToLongSet(team.getUsersId());
                    if (CollectionUtils.isEmpty(memberIds)) {
                        return false;
                    }
                    
                    // 批量查询成员信息
                    List<User> members = userService.listByIds(memberIds);
                    if (CollectionUtils.isEmpty(members)) {
                        return false;
                    }
                    
                    // 收集所有成员的标签
                    Set<String> teamTags = new HashSet<>();
                    for (User member : members) {
                        Set<String> memberTags = StringUtils.stringJsonListToStringSet(member.getTags());
                        if (memberTags != null) {
                            teamTags.addAll(memberTags);
                        }
                    }
                    
                    // 检查是否有匹配的标签
                    return tags.stream().anyMatch(teamTags::contains);
                })
                .limit(limit)
                .collect(Collectors.toList());

        // 4. 转换为TeamVo
        List<TeamVo> teamVoList = convertToTeamVoList(matchedTeams);

        // 5. 存入缓存
        valueOperations.set(cacheKey, teamVoList, 15, TimeUnit.MINUTES);

        return teamVoList;
    }

    /**
     * 获取热门队伍
     * 根据队伍成员数、创建时间等因素排序
     */
    @Override
    public List<TeamVo> getHotTeams(int limit) {
        // 1. 尝试从缓存获取
        String cacheKey = String.format("fontal:team:hot:%d", limit);
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        List<TeamVo> cachedResult = (List<TeamVo>) valueOperations.get(cacheKey);
        if (cachedResult != null) {
            return cachedResult;
        }

        // 2. 查询所有有效队伍
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("isDelete", 0)
                .gt("expireTime", new Date())
                .select("id", "teamName", "teamAvatarUrl", "teamDesc", "maxNum", 
                        "userId", "usersId", "teamStatus", "createTime", "expireTime")
                .orderByDesc("createTime"); // 先按创建时间排序
        List<Team> allTeams = this.list(queryWrapper);

        if (CollectionUtils.isEmpty(allTeams)) {
            return new ArrayList<>();
        }

        // 3. 计算热度分数（成员数 * 权重 + 时间衰减）
        long now = System.currentTimeMillis();
        List<Team> hotTeams = allTeams.stream()
                .map(team -> {
                    Set<Long> memberIds = StringUtils.stringJsonListToLongSet(team.getUsersId());
                    int memberCount = CollectionUtils.isEmpty(memberIds) ? 0 : memberIds.size();
                    
                    // 计算时间衰减（越新的队伍分数越高）
                    long timeDiff = now - team.getCreateTime().getTime();
                    double timeScore = Math.max(0, 1 - timeDiff / (30L * 24 * 60 * 60 * 1000)); // 30天衰减
                    
                    // 热度分数 = 成员数 * 0.6 + 时间分 * 0.4
                    double hotScore = memberCount * 0.6 + timeScore * 0.4;
                    
                    return new AbstractMap.SimpleEntry<>(team, hotScore);
                })
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue())) // 降序排序
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // 4. 转换为TeamVo
        List<TeamVo> teamVoList = convertToTeamVoList(hotTeams);

        // 5. 存入缓存
        valueOperations.set(cacheKey, teamVoList, 20, TimeUnit.MINUTES);

        return teamVoList;
    }

    /**
     * 获取最新队伍
     * 按创建时间倒序
     */
    @Override
    public List<TeamVo> getNewTeams(int limit) {
        // 1. 尝试从缓存获取
        String cacheKey = String.format("fontal:team:new:%d", limit);
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        List<TeamVo> cachedResult = (List<TeamVo>) valueOperations.get(cacheKey);
        if (cachedResult != null) {
            return cachedResult;
        }

        // 2. 查询最新队伍
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("isDelete", 0)
                .gt("expireTime", new Date())
                .select("id", "teamName", "teamAvatarUrl", "teamDesc", "maxNum", 
                        "userId", "usersId", "teamStatus", "createTime", "expireTime")
                .orderByDesc("createTime")
                .last("LIMIT " + limit);
        List<Team> newTeams = this.list(queryWrapper);

        // 3. 转换为TeamVo
        List<TeamVo> teamVoList = convertToTeamVoList(newTeams);

        // 4. 存入缓存
        valueOperations.set(cacheKey, teamVoList, 10, TimeUnit.MINUTES);

        return teamVoList;
    }

    /**
     * 计算队伍推荐分数（混合策略）
     * 标签匹配: 60%
     * 活跃度(成员数): 20%
     * 时间因素: 20%
     */
    private double computeRecommendScore(Team team, Set<String> userTags) {
        // 1. 获取队伍成员
        Set<Long> memberIds = StringUtils.stringJsonListToLongSet(team.getUsersId());
        if (CollectionUtils.isEmpty(memberIds)) {
            return 0;
        }

        // 2. 批量查询成员信息
        List<User> members = userService.listByIds(memberIds);
        if (CollectionUtils.isEmpty(members)) {
            return 0;
        }

        // 3. 计算标签匹配分数
        double tagScore = 0;
        Set<String> teamTags = new HashSet<>();
        for (User member : members) {
            Set<String> memberTags = StringUtils.stringJsonListToStringSet(member.getTags());
            if (memberTags != null) {
                teamTags.addAll(memberTags);
            }
        }
        
        // 计算标签匹配度（交集数量 / 用户标签数量）
        long matchCount = userTags.stream().filter(teamTags::contains).count();
        double tagMatchRate = userTags.isEmpty() ? 0 : (double) matchCount / userTags.size();
        tagScore = tagMatchRate * 60; // 权重60%

        // 4. 计算活跃度分数（成员数）
        double activityScore = Math.min(members.size() / 6.0, 1.0) * 20; // 权重20%

        // 5. 计算时间分数（越新的队伍分数越高）
        long timeDiff = System.currentTimeMillis() - team.getCreateTime().getTime();
        double timeScore = Math.max(0, 1 - timeDiff / (30L * 24 * 60 * 60 * 1000)) * 20; // 权重20%，30天衰减

        // 6. 综合分数
        return tagScore + activityScore + timeScore;
    }

    /**
     * 将Team列表转换为TeamVo列表
     */
    private List<TeamVo> convertToTeamVoList(List<Team> teams) {
        if (CollectionUtils.isEmpty(teams)) {
            return new ArrayList<>();
        }

        // 收集所有用户ID（创建者和成员）
        Set<Long> allUserIds = new HashSet<>();
        Map<Long, Set<Long>> teamMemberMap = new HashMap<>();

        for (Team team : teams) {
            allUserIds.add(team.getUserId());
            Set<Long> memberIds = StringUtils.stringJsonListToLongSet(team.getUsersId());
            allUserIds.addAll(memberIds);
            teamMemberMap.put(team.getId(), memberIds);
        }

        // 批量查询用户信息
        List<User> users = userService.listByIds(allUserIds);
        Map<Long, User> userMap = users.stream()
                .map(userService::getSafetyUser)
                .collect(Collectors.toMap(User::getId, u -> u));

        // 转换为TeamVo
        return teams.stream().map(team -> {
            TeamVo teamVo = new TeamVo();
            BeanUtils.copyProperties(team, teamVo);
            
            // 设置队长
            teamVo.setUser(userMap.get(team.getUserId()));
            
            // 设置成员
            Set<Long> memberIds = teamMemberMap.get(team.getId());
            Set<User> memberSet = memberIds.stream()
                    .map(userMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            teamVo.setUserSet(memberSet);
            
            return teamVo;
        }).collect(Collectors.toList());
    }
}
