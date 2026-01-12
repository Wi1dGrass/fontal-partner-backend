package com.huixing.fontal.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.reflect.TypeToken;
import com.huixing.fontal.common.ErrorCode;
import com.huixing.fontal.exception.BusinessException;
import com.huixing.fontal.mapper.TeamMapper;
import com.huixing.fontal.model.entity.Team;
import com.huixing.fontal.model.entity.User;
import com.huixing.fontal.model.vo.TeamVo;
import com.huixing.fontal.service.TeamService;
import com.huixing.fontal.service.UserService;
import com.huixing.fontal.utils.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.alibaba.druid.util.FnvHash.Constants.GSON;

public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team> implements TeamService  {

    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate redisTemplate;


    private void setRedis(String redisKey, Object data) {
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        try {
            // 解决缓存雪崩
            int i = RandomUtil.randomInt(1, 2);
            valueOperations.set(redisKey, data, 1 + i / 10, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("redis set key error");
        }
    }

    @Override
    public TeamVo getUsersByTeamId(Long teamId, HttpServletRequest request) {
        //1.获取当前登入用户
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"未登入");
        }
        //2.查询队伍信息
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 3. 权限校验
        // 预解析：当前用户加入的队伍 IDs
        Set<Long> userJoinedTeamIds = StringUtils.stringJsonListToLongSet(loginUser.getTeamIds());
        boolean isAdmin = userService.isAdmin(loginUser);
        boolean isLeader = loginUser.getId()==(team.getUserId());
        boolean isMember = userJoinedTeamIds.contains(teamId);
        if (!isAdmin && !isLeader && !isMember) {
            throw new BusinessException(ErrorCode.NO_AUTH, "暂无权限查看该队伍成员");
        }
        // 4. 尝试从缓存获取
        String teamIdKey = String.format("fontal:team:getUsersByTeamId:%s", teamId);
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        TeamVo cachedVo = (TeamVo) valueOperations.get(teamIdKey);
        if (cachedVo != null) {
            return cachedVo;
        }

        // 5. 【性能优化】批量查询用户信息，拒绝循环查库！
        Set<Long> memberIds = StringUtils.stringJsonListToLongSet(team.getUsersId());
        List<User> userList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(memberIds)) {
            // 使用 MyBatis-Plus 的 listByIds 只需要一条 SQL：WHERE id IN (...)
            userList = userService.listByIds(memberIds);
        }

        // 6. 数据脱敏处理
        Set<User> safetyUserSet = userList.stream()
                .map(userService::getSafetyUser)
                .collect(Collectors.toSet());

        // 7. 封装 TeamVo
        TeamVo teamVo = new TeamVo();
        BeanUtils.copyProperties(team, teamVo); // 自动拷贝相同名称的字段：name, desc, maxNum 等

        // 设置队长信息 (再次利用批量查询的结果，或者单独查一次)
        User leader = userService.getById(team.getUserId());
        teamVo.setUser(userService.getSafetyUser(leader));
        teamVo.setUserSet(safetyUserSet);

        // 8. 存入 Redis 并返回
        setRedis(teamIdKey, teamVo);
        return teamVo;
    }
}
