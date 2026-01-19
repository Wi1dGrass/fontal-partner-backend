package com.huixing.fontal.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.huixing.fontal.common.ErrorCode;
import com.huixing.fontal.contant.TeamConstant;
import com.huixing.fontal.exception.BusinessException;
import com.huixing.fontal.mapper.TeamMapper;
import com.huixing.fontal.model.entity.Team;
import com.huixing.fontal.model.entity.User;
import com.huixing.fontal.model.request.KickOutUserRequest;
import com.huixing.fontal.model.request.TeamCreateRequest;
import com.huixing.fontal.model.request.TeamDeleteRequest;
import com.huixing.fontal.model.request.TeamJoinApplyRequest;
import com.huixing.fontal.model.request.TeamJoinRequest;
import com.huixing.fontal.model.request.TeamUpdateRequest;
import com.huixing.fontal.model.request.TransferTeamRequest;
import com.huixing.fontal.model.vo.TeamBasicVO;
import com.huixing.fontal.model.vo.TeamMembershipVO;
import com.huixing.fontal.model.vo.TeamUserVo;
import com.huixing.fontal.model.vo.TeamVo;
import com.huixing.fontal.service.TeamJoinApplyService;
import com.huixing.fontal.service.TeamService;
import com.huixing.fontal.service.UserService;
import com.huixing.fontal.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team> implements TeamService  {

    private static final String SALT = "fontal_team";
    private static final String TEAMS_KEY = String.format("fontal:team:getTeams:%s", "getTeams");
    private static final Gson GSON = new Gson();
    private static final int MAX_TEAM_MEMBERS = 6;
    private static final int MAX_TEAM_DESC_LENGTH = 1024;
    private static final int MAX_ANNOUNCE_LENGTH = 512;

    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private RedissonClient redissonClient;

    @Lazy
    @Resource
    private TeamJoinApplyService teamJoinApplyService;


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
        // 从队伍的 usersId 字段中提取成员ID列表，判断当前用户是否是成员
        Set<Long> memberIds = StringUtils.stringJsonListToLongSet(team.getUsersId());
        boolean isAdmin = userService.isAdmin(loginUser);
        boolean isLeader = loginUser.getId() == team.getUserId();
        boolean isMember = memberIds.contains(loginUser.getId());
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

    @Override
    public TeamUserVo getTeams() {
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();

        // 1. 尝试从缓存获取
        TeamUserVo teamUserVo = (TeamUserVo) valueOperations.get(TEAMS_KEY);

        if (teamUserVo != null) {
            // 获取 Set 并转为 List
            List<TeamVo> teamList = new ArrayList<>(teamUserVo.getTeamSet());
            // 真正的随机打乱
            Collections.shuffle(teamList);
            //List转为Set
            HashSet<TeamVo> teamVos = new HashSet<>(teamList);
            teamUserVo.setTeamSet(teamVos);
            return teamUserVo;
        }

        // 2. 缓存未命中：精简查询（不查已过期、已删除的队伍）
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.gt("expireTime", new Date()) // 只查未过期的
                .eq("isDelete", 0)
                .select("id", "teamName", "teamAvatarUrl", "teamDesc", "maxNum", "userId", "usersId"); // 精简字段

        List<Team> teams = this.list(queryWrapper);

        // 3. 转换并脱敏 (teamSet 方法内部应包含脱敏逻辑)
        teamUserVo = teamSet(teams);

        // 4. 写入缓存（建议设置较短的过期时间，如 5-10 分钟）
        valueOperations.set(TEAMS_KEY, teamUserVo, 5 + new Random().nextInt(5), TimeUnit.MINUTES);

        return teamUserVo;
    }


    @Override
    public TeamUserVo teamSet(List<Team> teamList) {
        if (CollectionUtils.isEmpty(teamList)) {
            return new TeamUserVo();
        }

        // 1. 预处理：收集所有涉及到的用户 ID（包括创建者和所有成员）
        Set<Long> allNeedUserIds = new HashSet<>();
        Map<Long, Set<Long>> teamMemberMap = new HashMap<>();

        for (Team team : teamList) {
            allNeedUserIds.add(team.getUserId()); // 收集创建者 ID
            Set<Long> memberIds = StringUtils.stringJsonListToLongSet(team.getUsersId());
            allNeedUserIds.addAll(memberIds); // 收集成员 ID
            teamMemberMap.put(team.getId(), memberIds);
        }

        // 2. 一次性批量查询所有用户信息并脱敏
        // 关键：100 个人也只需要 1 条 SQL: SELECT * FROM user WHERE id IN (...)
        List<User> usersFromDb = userService.listByIds(allNeedUserIds);
        Map<Long, User> userMap = usersFromDb.stream()
                .map(userService::getSafetyUser)
                .collect(Collectors.toMap(User::getId, user -> user));

        // 3. 封装 Vo
        List<TeamVo> teamVoList = teamList.stream().map(team -> {
            TeamVo teamVo = new TeamVo();
            BeanUtils.copyProperties(team, teamVo); // 批量拷贝属性

            // 从 Map 中直接获取创建者，无需查库
            teamVo.setUser(userMap.get(team.getUserId()));

            // 从 Map 中直接获取成员列表，无需查库
            Set<Long> memberIds = teamMemberMap.get(team.getId());
            Set<User> memberSet = memberIds.stream()
                    .map(userMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            teamVo.setUserSet(memberSet);

            return teamVo;
        }).collect(Collectors.toList());

        // 4. 组装结果
        Set<TeamVo> teamVoSet = new LinkedHashSet<>(teamVoList);
        TeamUserVo teamUserVo = new TeamUserVo();
        // 注意：建议这里使用 List 保持 Shuffle 后的顺序，Set 会打乱顺序
        teamUserVo.setTeamSet(teamVoSet);
        return teamUserVo;
    }

    @Override
    public TeamUserVo getTeamsByIds(Set<Long> teamIds, HttpServletRequest request) {

        //根据用户加入的队伍id获取队伍信息
        //1.获取登入用户的信息
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        //2.根据Id获取
        // 同样精简字段，提高查询效率
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", teamIds)
                .eq("isDelete", 0); // 排除已删除的
        List<Team> teamList = this.list(queryWrapper);
        //3.调用 teamSet
        TeamUserVo teamUserVo = teamSet(teamList);

        return teamUserVo;
    }

    @Override
    public Boolean createTeam(TeamCreateRequest teamCreateRequest, HttpServletRequest request) {
        // 1. 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "请先登录");
        }

        // 2. 参数校验
        // 2.1 队伍名不能为空
        if (teamCreateRequest.getTeamName() == null || teamCreateRequest.getTeamName().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍名不能为空");
        }
        // 2.2 队伍名长度限制
        if (teamCreateRequest.getTeamName().length() > 256) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍名不能超过256个字符");
        }
        // 2.3 描述长度校验
        String teamDesc = teamCreateRequest.getTeamDesc();
        if (teamDesc != null && teamDesc.length() > MAX_TEAM_DESC_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍描述不能超过" + MAX_TEAM_DESC_LENGTH + "个字符");
        }
        // 2.4 公告长度校验
        String announce = teamCreateRequest.getAnnounce();
        if (announce != null && announce.length() > MAX_ANNOUNCE_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍公告不能超过" + MAX_ANNOUNCE_LENGTH + "个字符");//512
        }
        // 2.5 最大人数校验（最多6人）
        if (teamCreateRequest.getMaxNum() == null || teamCreateRequest.getMaxNum() < 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "最大人数至少为1");
        }
        if (teamCreateRequest.getMaxNum() > MAX_TEAM_MEMBERS) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍最多只能有" + MAX_TEAM_MEMBERS + "人");//6
        }
        // 2.6 队伍状态校验
        Integer teamStatus = teamCreateRequest.getTeamStatus();
        if (teamStatus == null) {
            teamStatus = TeamConstant.PUBLIC_TEAM_STATUS; // 默认为公开
        }
        if (teamStatus != TeamConstant.PUBLIC_TEAM_STATUS 
                && teamStatus != TeamConstant.PRIVATE_TEAM_STATUS 
                && teamStatus != TeamConstant.ENCRYPTION_TEAM_STATUS) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍状态参数错误");
        }
        // 2.7 密码校验
        String teamPassword = teamCreateRequest.getTeamPassword();
        if (teamStatus == TeamConstant.ENCRYPTION_TEAM_STATUS) {
            // 加密类型必须提供密码
            if (teamPassword == null || teamPassword.trim().isEmpty()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "加密队伍必须设置密码");
            }
            // 加密密码
            teamPassword = DigestUtils.md5DigestAsHex((SALT + teamPassword).getBytes(StandardCharsets.UTF_8));
        } else {
            // 公开和私有类型不需要密码
            teamPassword = null;
        }

        // 3. 使用分布式锁保证并发安全
        String lockKey = String.format("fontal:team:create:lock:%s", loginUser.getId());
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试获取锁，等待时间0秒，锁过期时间10秒
            if (lock.tryLock(0, 10, TimeUnit.SECONDS)) {
                log.info("get lock for create team, userId: {}", loginUser.getId());
                
                // 4. 创建队伍
                Team team = new Team();
                team.setTeamName(teamCreateRequest.getTeamName());
                team.setTeamAvatarUrl(teamCreateRequest.getTeamAvatarUrl());
                team.setTeamPassword(teamPassword);
                team.setTeamDesc(teamDesc);
                team.setMaxNum(teamCreateRequest.getMaxNum());
                team.setExpireTime(teamCreateRequest.getExpireTime());
                team.setUserId(loginUser.getId()); // 创建者成为leader
                team.setTeamStatus(teamStatus);
                team.setAnnounce(announce);
                team.setIsDelete(0);
                
                // 创建者自动加入队伍（usersId格式为JSON数组，如"[10001]"）
                Set<Long> userIds = new HashSet<>();
                userIds.add(loginUser.getId());
                team.setUsersId(GSON.toJson(userIds));

                // 5. 保存队伍
                boolean result = this.save(team);
                if (!result) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建队伍失败");
                }

                // 6. 更新创建者的teamIds字段
                // 从数据库重新获取用户对象，确保数据一致性
                User userFromDb = userService.getById(loginUser.getId());
                if (userFromDb == null) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户不存在");
                }
                Set<Long> userTeamIds = StringUtils.stringJsonListToLongSet(userFromDb.getTeamIds());
                userTeamIds.add(team.getId());
                userFromDb.setTeamIds(GSON.toJson(userTeamIds));
                boolean updateResult = userService.updateById(userFromDb);
                if (!updateResult) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新用户队伍信息失败");
                }

                // 7. 清除缓存（清除队伍列表缓存，因为新增了队伍）
                try {
                    redisTemplate.delete(TEAMS_KEY);
                    // 清除最新队伍缓存（常用limit: 10, 20, 50）
                    redisTemplate.delete(String.format("fontal:team:new:%d", 10));
                    redisTemplate.delete(String.format("fontal:team:new:%d", 20));
                    redisTemplate.delete(String.format("fontal:team:new:%d", 50));
                    log.info("清除队伍缓存成功");
                } catch (Exception e) {
                    log.error("清除队伍缓存失败", e);
                }

                return true;
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建队伍过于频繁，请稍后再试");
            }
        } catch (InterruptedException e) {
            log.error("createTeam error", e);
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建队伍失败");
        } finally {
            // 释放锁时，先判断是否是当前线程持有的锁
            if (lock.isHeldByCurrentThread()) {
                log.info("unLock for create team, userId: {}", loginUser.getId());
                lock.unlock();
            }
        }
    }

    @Override
    public Boolean joinTeam(TeamJoinRequest teamJoinRequest, HttpServletRequest request) {
        // 1. 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "请先登录");
        }

        // 2. 参数校验
        Long teamId = teamJoinRequest.getTeamId();
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍ID不能为空");
        }

        // 3. 查询队伍信息
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
        }

        // 4. 校验队伍是否已删除
        if (team.getIsDelete() != null && team.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已删除");
        }

        // 5. 校验队伍是否已过期
        if (team.getExpireTime() != null && team.getExpireTime().before(new Date())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已过期");
        }

        // 6. 校验用户是否已在队伍中
        Set<Long> teamMemberIds = StringUtils.stringJsonListToLongSet(team.getUsersId());
        if (teamMemberIds.contains(loginUser.getId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "您已在该队伍中");
        }

        // 7. 校验队伍人数是否已满
        int currentMemberCount = teamMemberIds.size();
        if (team.getMaxNum() != null && currentMemberCount >= team.getMaxNum()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数已满");
        }

        // 8. 根据队伍状态进行不同的校验
        Integer teamStatus = team.getTeamStatus();
        if (teamStatus == null) {
            teamStatus = TeamConstant.PUBLIC_TEAM_STATUS;
        }

        // 8.1 私有队伍需要申请加入
        if (teamStatus == TeamConstant.PRIVATE_TEAM_STATUS) {
            // 创建加入申请
            TeamJoinApplyRequest applyRequest = new TeamJoinApplyRequest();
            applyRequest.setTeamId(teamId);
            applyRequest.setApplyMessage(teamJoinRequest.getApplyMessage());

            Long applyId = teamJoinApplyService.createJoinApplication(applyRequest, request);
            log.info("用户 {} 申请加入私有队伍 {}，申请ID: {}", loginUser.getId(), teamId, applyId);
            return true; // 返回true表示申请已提交
        }

        // 8.2 加密队伍需要验证密码
        if (teamStatus == TeamConstant.ENCRYPTION_TEAM_STATUS) {
            String inputPassword = teamJoinRequest.getPassword();
            if (inputPassword == null || inputPassword.trim().isEmpty()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "请输入队伍密码");
            }
            // 加密输入的密码
            String encryptedPassword = DigestUtils.md5DigestAsHex((SALT + inputPassword).getBytes(StandardCharsets.UTF_8));
            // 校验密码是否匹配
            if (!encryptedPassword.equals(team.getTeamPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍密码错误");
            }
        }

        // 9. 使用分布式锁保证并发安全
        String lockKey = String.format("fontal:team:join:lock:%s", teamId);
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试获取锁，等待时间0秒，锁过期时间10秒
            if (lock.tryLock(0, 10, TimeUnit.SECONDS)) {
                log.info("get lock for join team, teamId: {}, userId: {}", teamId, loginUser.getId());

                // 10. 再次校验队伍人数（防止并发加入）
                Team teamFromDb = this.getById(teamId);
                Set<Long> currentMemberIds = StringUtils.stringJsonListToLongSet(teamFromDb.getUsersId());
                if (currentMemberIds.size() >= teamFromDb.getMaxNum()) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数已满");
                }

                // 11. 更新队伍的usersId字段
                currentMemberIds.add(loginUser.getId());
                teamFromDb.setUsersId(GSON.toJson(currentMemberIds));
                teamFromDb.setUpdateTime(new Date());  // 手动设置更新时间
                boolean updateTeamResult = this.updateById(teamFromDb);
                if (!updateTeamResult) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "加入队伍失败");
                }

                // 12. 更新用户的teamIds字段
                User userFromDb = userService.getById(loginUser.getId());
                if (userFromDb == null) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户不存在");
                }
                Set<Long> userTeamIds = StringUtils.stringJsonListToLongSet(userFromDb.getTeamIds());
                userTeamIds.add(teamId);
                userFromDb.setTeamIds(GSON.toJson(userTeamIds));
                boolean updateUserResult = userService.updateById(userFromDb);
                if (!updateUserResult) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新用户队伍信息失败");
                }

                // 13. 清除缓存
                try {
                    // 清除队伍列表缓存
                    redisTemplate.delete(TEAMS_KEY);
                    // 清除队伍详情缓存
                    String teamIdKey = String.format("fontal:team:getUsersByTeamId:%s", teamId);
                    redisTemplate.delete(teamIdKey);
                    // 清除热门队伍缓存（常用limit: 10, 20, 50）
                    redisTemplate.delete(String.format("fontal:team:hot:%d", 10));
                    redisTemplate.delete(String.format("fontal:team:hot:%d", 20));
                    redisTemplate.delete(String.format("fontal:team:hot:%d", 50));
                    log.info("清除队伍缓存成功, teamId: {}", teamId);
                } catch (Exception e) {
                    log.error("清除队伍缓存失败", e);
                }

                return true;
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "加入队伍过于频繁，请稍后再试");
            }
        } catch (InterruptedException e) {
            log.error("joinTeam error", e);
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "加入队伍失败");
        } finally {
            // 释放锁时，先判断是否是当前线程持有的锁
            if (lock.isHeldByCurrentThread()) {
                log.info("unLock for join team, teamId: {}, userId: {}", teamId, loginUser.getId());
                lock.unlock();
            }
        }
    }

    @Override
    public Boolean  quitTeam(Long teamId, HttpServletRequest request) {
        // 1. 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "请先登录");
        }

        // 2. 参数校验
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍ID不能为空");
        }

        // 3. 查询队伍信息
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
        }

        // 4. 校验队伍是否已删除
        if (team.getIsDelete() != null && team.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已删除");
        }

        // 5. 校验用户是否在队伍中
        Set<Long> teamMemberIds = StringUtils.stringJsonListToLongSet(team.getUsersId());
        if (!teamMemberIds.contains(loginUser.getId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "您不在该队伍中");
        }

        // 6. 判断用户是否是队长
        boolean isLeader = loginUser.getId() == team.getUserId();

        // 7. 使用分布式锁保证并发安全
        String lockKey = String.format("fontal:team:quit:lock:%s", teamId);
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试获取锁，等待时间0秒，锁过期时间10秒
            if (lock.tryLock(0, 10, TimeUnit.SECONDS)) {
                log.info("get lock for quit team, teamId: {}, userId: {}", teamId, loginUser.getId());

                // 8. 从数据库重新获取队伍和用户信息，确保数据一致性
                Team teamFromDb = this.getById(teamId);
                User userFromDb = userService.getById(loginUser.getId());

                if (teamFromDb == null) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
                }

                Set<Long> currentMemberIds = StringUtils.stringJsonListToLongSet(teamFromDb.getUsersId());
                Set<Long> currentUserTeamIds = StringUtils.stringJsonListToLongSet(userFromDb.getTeamIds());

                // 9. 队长退出队伍的特殊处理
                if (isLeader) {
                    // 如果队长是队伍中唯一的成员，删除队伍
                    if (currentMemberIds.size() == 1) {
                        // 删除队伍
                        teamFromDb.setIsDelete(1);
                        boolean deleteTeamResult = this.updateById(teamFromDb);
                        if (!deleteTeamResult) {
                            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除队伍失败");
                        }
                        // 从用户的队伍列表中移除该队伍
                        currentUserTeamIds.remove(teamId);
                        userFromDb.setTeamIds(GSON.toJson(currentUserTeamIds));
                        boolean updateUserResult = userService.updateById(userFromDb);
                        if (!updateUserResult) {
                            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新用户队伍信息失败");
                        }
                    } else {
                        // 队长退出但队伍还有其他成员，需要转让队长权限给第一个成员
                        currentMemberIds.remove(loginUser.getId());
                        // 获取第一个成员作为新队长
                        Long newLeaderId = currentMemberIds.iterator().next();
                        teamFromDb.setUserId(newLeaderId);
                        teamFromDb.setUsersId(GSON.toJson(currentMemberIds));
                        teamFromDb.setUpdateTime(new Date());  // 手动设置更新时间
                        boolean updateTeamResult = this.updateById(teamFromDb);
                        if (!updateTeamResult) {
                            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "转让队长失败");
                        }
                        // 从原队长的队伍列表中移除该队伍
                        currentUserTeamIds.remove(teamId);
                        userFromDb.setTeamIds(GSON.toJson(currentUserTeamIds));
                        boolean updateUserResult = userService.updateById(userFromDb);
                        if (!updateUserResult) {
                            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新用户队伍信息失败");
                        }
                    }
                } else {
                    // 10. 普通成员退出队伍
                    // 从队伍的成员列表中移除用户
                    currentMemberIds.remove(loginUser.getId());
                    teamFromDb.setUsersId(GSON.toJson(currentMemberIds));
                    teamFromDb.setUpdateTime(new Date());  // 手动设置更新时间
                    boolean updateTeamResult = this.updateById(teamFromDb);
                    if (!updateTeamResult) {
                        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "退出队伍失败");
                    }
                    // 从用户的队伍列表中移除该队伍
                    currentUserTeamIds.remove(teamId);
                    userFromDb.setTeamIds(GSON.toJson(currentUserTeamIds));
                    boolean updateUserResult = userService.updateById(userFromDb);
                    if (!updateUserResult) {
                        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新用户队伍信息失败");
                    }
                }

                // 11. 清除缓存
                try {
                    // 清除队伍列表缓存
                    redisTemplate.delete(TEAMS_KEY);
                    // 清除队伍详情缓存
                    String teamIdKey = String.format("fontal:team:getUsersByTeamId:%s", teamId);
                    redisTemplate.delete(teamIdKey);
                    // 清除热门队伍缓存（常用limit: 10, 20, 50）
                    redisTemplate.delete(String.format("fontal:team:hot:%d", 10));
                    redisTemplate.delete(String.format("fontal:team:hot:%d", 20));
                    redisTemplate.delete(String.format("fontal:team:hot:%d", 50));
                    log.info("清除队伍缓存成功, teamId: {}", teamId);
                } catch (Exception e) {
                    log.error("清除队伍缓存失败", e);
                }

                return true;
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "退出队伍过于频繁，请稍后再试");
            }
        } catch (InterruptedException e) {
            log.error("quitTeam error", e);
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "退出队伍失败");
        } finally {
            // 释放锁时，先判断是否是当前线程持有的锁
            if (lock.isHeldByCurrentThread()) {
                log.info("unLock for quit team, teamId: {}, userId: {}", teamId, loginUser.getId());
                lock.unlock();
            }
        }
    }

    @Override
    public Boolean kickOutUser(KickOutUserRequest kickOutUserRequest, HttpServletRequest request) {
        // 1. 获取当前登录用户（队长）
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "请先登录");
        }

        // 2. 参数校验
        Long teamId = kickOutUserRequest.getTeamId();
        Long userId = kickOutUserRequest.getUserId();
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍ID不能为空");
        }
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }

        // 3. 查询队伍信息
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
        }

        // 4. 校验队伍是否已删除
        if (team.getIsDelete() != null && team.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已删除");
        }

        // 5. 校验队伍是否已过期
        if (team.getExpireTime() != null && team.getExpireTime().before(new Date())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已过期");
        }

        // 6. 校验当前用户是否是队长
        boolean isLeader = loginUser.getId() == team.getUserId();
        if (!isLeader) {
            throw new BusinessException(ErrorCode.NO_AUTH, "只有队长才能踢出成员");
        }

        // 7. 校验不能踢出自己
        if (loginUser.getId() == userId) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能踢出自己");
        }

        // 8. 校验要踢出的用户是否在队伍中
        Set<Long> teamMemberIds = StringUtils.stringJsonListToLongSet(team.getUsersId());
        if (!teamMemberIds.contains(userId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该用户不在队伍中");
        }

        // 9. 使用分布式锁保证并发安全
        String lockKey = String.format("fontal:team:kickOut:lock:%s", teamId);
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试获取锁，等待时间0秒，锁过期时间10秒
            if (lock.tryLock(0, 10, TimeUnit.SECONDS)) {
                log.info("get lock for kick out user, teamId: {}, userId: {}, kickedUserId: {}", 
                    teamId, loginUser.getId(), userId);

                // 10. 从数据库重新获取队伍和用户信息，确保数据一致性
                Team teamFromDb = this.getById(teamId);
                User userToBeKicked = userService.getById(userId);

                if (teamFromDb == null) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
                }
                if (userToBeKicked == null) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "要踢出的用户不存在");
                }

                Set<Long> currentMemberIds = StringUtils.stringJsonListToLongSet(teamFromDb.getUsersId());
                Set<Long> kickedUserTeamIds = StringUtils.stringJsonListToLongSet(userToBeKicked.getTeamIds());

                // 11. 再次校验用户是否在队伍中
                if (!currentMemberIds.contains(userId)) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "该用户不在队伍中");
                }

                // 12. 从队伍的成员列表中移除用户
                currentMemberIds.remove(userId);
                teamFromDb.setUsersId(GSON.toJson(currentMemberIds));
                teamFromDb.setUpdateTime(new Date());  // 手动设置更新时间
                boolean updateTeamResult = this.updateById(teamFromDb);
                if (!updateTeamResult) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "踢出用户失败");
                }

                // 13. 从被踢出用户的队伍列表中移除该队伍
                kickedUserTeamIds.remove(teamId);
                userToBeKicked.setTeamIds(GSON.toJson(kickedUserTeamIds));
                boolean updateUserResult = userService.updateById(userToBeKicked);
                if (!updateUserResult) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新用户队伍信息失败");
                }

                // 14. 清除缓存
                try {
                    // 清除队伍列表缓存
                    redisTemplate.delete(TEAMS_KEY);
                    // 清除队伍详情缓存
                    String teamIdKey = String.format("fontal:team:getUsersByTeamId:%s", teamId);
                    redisTemplate.delete(teamIdKey);
                    // 清除热门队伍缓存（常用limit: 10, 20, 50）
                    redisTemplate.delete(String.format("fontal:team:hot:%d", 10));
                    redisTemplate.delete(String.format("fontal:team:hot:%d", 20));
                    redisTemplate.delete(String.format("fontal:team:hot:%d", 50));
                    log.info("清除队伍缓存成功, teamId: {}", teamId);
                } catch (Exception e) {
                    log.error("清除队伍缓存失败", e);
                }

                return true;
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "踢出操作过于频繁，请稍后再试");
            }
        } catch (InterruptedException e) {
            log.error("kickOutUser error", e);
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "踢出用户失败");
        } finally {
            // 释放锁时，先判断是否是当前线程持有的锁
            if (lock.isHeldByCurrentThread()) {
                log.info("unLock for kick out user, teamId: {}, userId: {}", teamId, loginUser.getId());
                lock.unlock();
            }
        }
    }

    @Override
    public Boolean updateTeam(TeamUpdateRequest teamUpdateRequest, HttpServletRequest request) {
        // 1. 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "请先登录");
        }

        // 2. 参数校验
        Long teamId = teamUpdateRequest.getId();
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍ID不能为空");
        }

        // 3. 查询队伍信息
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
        }

        // 4. 校验队伍是否已删除
        if (team.getIsDelete() != null && team.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已删除");
        }

        // 5. 校验当前用户是否是队长
        boolean isLeader = loginUser.getId() == team.getUserId();
        if (!isLeader) {
            throw new BusinessException(ErrorCode.NO_AUTH, "只有队长才能更新队伍信息");
        }

        // 6. 队伍名校验
        String teamName = teamUpdateRequest.getTeamName();
        if (teamName != null && !teamName.trim().isEmpty()) {
            if (teamName.length() > 256) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍名不能超过256个字符");
            }
        }

        // 7. 队伍描述校验
        String teamDesc = teamUpdateRequest.getTeamDesc();
        if (teamDesc != null && teamDesc.length() > MAX_TEAM_DESC_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍描述不能超过" + MAX_TEAM_DESC_LENGTH + "个字符");
        }

        // 8. 公告校验
        String announce = teamUpdateRequest.getAnnounce();
        if (announce != null && announce.length() > MAX_ANNOUNCE_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍公告不能超过" + MAX_ANNOUNCE_LENGTH + "个字符");
        }

        // 9. 最大人数校验
        Integer maxNum = teamUpdateRequest.getMaxNum();
        if (maxNum != null) {
            if (maxNum < 1) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "最大人数至少为1");
            }
            if (maxNum > MAX_TEAM_MEMBERS) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍最多只能有" + MAX_TEAM_MEMBERS + "人");
            }
            // 校验当前人数是否超过新的最大人数
            Set<Long> currentMemberIds = StringUtils.stringJsonListToLongSet(team.getUsersId());
            if (currentMemberIds.size() > maxNum) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "当前队伍人数已超过设置的最大人数");
            }
        }

        // 10. 队伍状态和密码校验
        Integer teamStatus = teamUpdateRequest.getTeamStatus();
        String teamPassword = teamUpdateRequest.getTeamPassword();

        if (teamStatus != null) {
            // 队伍状态参数校验
            if (teamStatus != TeamConstant.PUBLIC_TEAM_STATUS 
                    && teamStatus != TeamConstant.PRIVATE_TEAM_STATUS 
                    && teamStatus != TeamConstant.ENCRYPTION_TEAM_STATUS) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍状态参数错误");
            }

            // 如果修改为加密类型，必须提供密码
            if (teamStatus == TeamConstant.ENCRYPTION_TEAM_STATUS) {
                if (teamPassword == null || teamPassword.trim().isEmpty()) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "加密队伍必须设置密码");
                }
                // 加密密码
                teamPassword = DigestUtils.md5DigestAsHex((SALT + teamPassword).getBytes(StandardCharsets.UTF_8));
            } else {
                // 公开和私有类型不需要密码
                teamPassword = null;
            }
        } else if (teamPassword != null && !teamPassword.trim().isEmpty()) {
            // 如果没有修改状态，但提供了密码，则认为是修改加密队伍的密码
            if (team.getTeamStatus() != TeamConstant.ENCRYPTION_TEAM_STATUS) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "只有加密队伍才能设置密码");
            }
            // 加密新密码
            teamPassword = DigestUtils.md5DigestAsHex((SALT + teamPassword).getBytes(StandardCharsets.UTF_8));
        } else {
            // 保持原密码
            teamPassword = team.getTeamPassword();
        }

        // 11. 使用分布式锁保证并发安全
        String lockKey = String.format("fontal:team:update:lock:%s", teamId);
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试获取锁，等待时间0秒，锁过期时间10秒
            if (lock.tryLock(0, 10, TimeUnit.SECONDS)) {
                log.info("get lock for update team, teamId: {}, userId: {}", teamId, loginUser.getId());

                // 12. 从数据库重新获取队伍信息，确保数据一致性
                Team teamFromDb = this.getById(teamId);
                if (teamFromDb == null) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
                }

                // 13. 更新队伍信息
                if (teamName != null && !teamName.trim().isEmpty()) {
                    teamFromDb.setTeamName(teamName);
                }
                if (teamUpdateRequest.getTeamAvatarUrl() != null) {
                    teamFromDb.setTeamAvatarUrl(teamUpdateRequest.getTeamAvatarUrl());
                }
                if (teamDesc != null) {
                    teamFromDb.setTeamDesc(teamDesc);
                }
                if (maxNum != null) {
                    teamFromDb.setMaxNum(maxNum);
                }
                if (teamUpdateRequest.getExpireTime() != null) {
                    teamFromDb.setExpireTime(teamUpdateRequest.getExpireTime());
                }
                if (teamStatus != null) {
                    teamFromDb.setTeamStatus(teamStatus);
                }
                if (teamPassword != null) {
                    teamFromDb.setTeamPassword(teamPassword);
                }
                if (announce != null) {
                    teamFromDb.setAnnounce(announce);
                }

                teamFromDb.setUpdateTime(new Date());  // 手动设置更新时间
                boolean updateResult = this.updateById(teamFromDb);
                if (!updateResult) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新队伍失败");
                }

                // 14. 清除缓存
                try {
                    // 清除队伍列表缓存
                    redisTemplate.delete(TEAMS_KEY);
                    // 清除队伍详情缓存
                    String teamIdKey = String.format("fontal:team:getUsersByTeamId:%s", teamId);
                    redisTemplate.delete(teamIdKey);
                    log.info("清除队伍缓存成功, teamId: {}", teamId);
                } catch (Exception e) {
                    log.error("清除队伍缓存失败", e);
                }

                return true;
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新队伍过于频繁，请稍后再试");
            }
        } catch (InterruptedException e) {
            log.error("updateTeam error", e);
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新队伍失败");
        } finally {
            // 释放锁时，先判断是否是当前线程持有的锁
            if (lock.isHeldByCurrentThread()) {
                log.info("unLock for update team, teamId: {}, userId: {}", teamId, loginUser.getId());
                lock.unlock();
            }
        }
    }

    @Override
    public Boolean transferTeam(TransferTeamRequest transferTeamRequest, HttpServletRequest request) {
        // 1. 获取当前登录用户（队长）
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "请先登录");
        }

        // 2. 参数校验
        String userAccount = transferTeamRequest.getUserAccount();
        Long teamId = transferTeamRequest.getTeamId();
        if (userAccount == null || userAccount.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号不能为空");
        }
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍ID不能为空");
        }

        // 3. 查询队伍信息
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
        }

        // 4. 校验队伍是否已删除
        if (team.getIsDelete() != null && team.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已删除");
        }

        // 5. 校验队伍是否已过期
        if (team.getExpireTime() != null && team.getExpireTime().before(new Date())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已过期");
        }

        // 6. 校验当前用户是否是队长
        boolean isLeader = loginUser.getId() == team.getUserId();
        if (!isLeader) {
            throw new BusinessException(ErrorCode.NO_AUTH, "只有队长才能转让队伍");
        }

        // 7. 根据用户账号查询要转让的用户
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("userAccount", userAccount);
        User newLeader = userService.getOne(userQueryWrapper);
        if (newLeader == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "要转让的用户不存在");
        }

        // 8. 校验不能转让给自己
        if (loginUser.getId() == newLeader.getId()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能转让给自己");
        }

        // 9. 校验要转让的用户是否在队伍中
        Set<Long> teamMemberIds = StringUtils.stringJsonListToLongSet(team.getUsersId());
        if (!teamMemberIds.contains(newLeader.getId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该用户不在队伍中");
        }

        // 10. 使用分布式锁保证并发安全
        String lockKey = String.format("fontal:team:transfer:lock:%s", teamId);
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试获取锁，等待时间0秒，锁过期时间10秒
            if (lock.tryLock(0, 10, TimeUnit.SECONDS)) {
                log.info("get lock for transfer team, teamId: {}, userId: {}, newLeaderAccount: {}", 
                    teamId, loginUser.getId(), userAccount);

                // 11. 从数据库重新获取队伍信息，确保数据一致性
                Team teamFromDb = this.getById(teamId);
                if (teamFromDb == null) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
                }

                Set<Long> currentMemberIds = StringUtils.stringJsonListToLongSet(teamFromDb.getUsersId());

                // 12. 再次校验要转让的用户是否在队伍中
                if (!currentMemberIds.contains(newLeader.getId())) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "该用户不在队伍中");
                }

                // 13. 更新队伍的队长为新队长
                teamFromDb.setUserId(newLeader.getId());
                teamFromDb.setUpdateTime(new Date());  // 手动设置更新时间
                boolean updateTeamResult = this.updateById(teamFromDb);
                if (!updateTeamResult) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "转让队长失败");
                }

                // 14. 清除缓存
                try {
                    // 清除队伍列表缓存
                    redisTemplate.delete(TEAMS_KEY);
                    // 清除队伍详情缓存
                    String teamIdKey = String.format("fontal:team:getUsersByTeamId:%s", teamId);
                    redisTemplate.delete(teamIdKey);
                    log.info("清除队伍缓存成功, teamId: {}", teamId);
                } catch (Exception e) {
                    log.error("清除队伍缓存失败", e);
                }

                return true;
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "转让操作过于频繁，请稍后再试");
            }
        } catch (InterruptedException e) {
            log.error("transferTeam error", e);
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "转让队长失败");
        } finally {
            // 释放锁时，先判断是否是当前线程持有的锁
            if (lock.isHeldByCurrentThread()) {
                log.info("unLock for transfer team, teamId: {}, userId: {}", teamId, loginUser.getId());
                lock.unlock();
            }
        }
    }

    @Override
    public Boolean
    deleteTeam(TeamDeleteRequest teamDeleteRequest, HttpServletRequest request) {
        // 1. 获取当前登录用户（队长）
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "请先登录");
        }

        // 2. 参数校验
        Long teamId = teamDeleteRequest.getTeamId();
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍ID不能为空");
        }

        // 3. 查询队伍信息
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
        }

        // 4. 校验队伍是否已删除
        if (team.getIsDelete() != null && team.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已删除");
        }

        // 5. 校验队伍是否已过期
        if (team.getExpireTime() != null && team.getExpireTime().before(new Date())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已过期");
        }

        // 6. 校验当前用户是否是队长
        boolean isLeader = loginUser.getId() == team.getUserId();
        if (!isLeader) {
            throw new BusinessException(ErrorCode.NO_AUTH, "只有队长才能删除队伍");
        }

        // 7. 获取队伍所有成员
        Set<Long> teamMemberIds = StringUtils.stringJsonListToLongSet(team.getUsersId());

        // 8. 使用分布式锁保证并发安全
        String lockKey = String.format("fontal:team:delete:lock:%s", teamId);
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试获取锁，等待时间0秒，锁过期时间10秒
            if (lock.tryLock(0, 10, TimeUnit.SECONDS)) {
                log.info("get lock for delete team, teamId: {}, userId: {}", teamId, loginUser.getId());

                // 9. 从数据库重新获取队伍信息，确保数据一致性
                Team teamFromDb = this.getById(teamId);
                if (teamFromDb == null) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
                }

                Set<Long> currentMemberIds = StringUtils.stringJsonListToLongSet(teamFromDb.getUsersId());

                // 10. 逻辑删除队伍
                teamFromDb.setIsDelete(1);
                boolean deleteTeamResult = this.updateById(teamFromDb);
                if (!deleteTeamResult) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除队伍失败");
                }

                // 11. 批量更新所有成员的teamIds字段
                if (CollectionUtils.isNotEmpty(currentMemberIds)) {
                    // 批量查询所有成员
                    List<User> memberList = userService.listByIds(currentMemberIds);
                    if (CollectionUtils.isNotEmpty(memberList)) {
                        for (User member : memberList) {
                            // 从用户的队伍列表中移除该队伍
                            Set<Long> userTeamIds = StringUtils.stringJsonListToLongSet(member.getTeamIds());
                            userTeamIds.remove(teamId);
                            member.setTeamIds(GSON.toJson(userTeamIds));
                        }
                        // 批量更新用户
                        boolean updateUserResult = userService.updateBatchById(memberList);
                        if (!updateUserResult) {
                            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新用户队伍信息失败");
                        }
                    }
                }

                // 12. 清除缓存
                try {
                    // 清除队伍列表缓存
                    redisTemplate.delete(TEAMS_KEY);
                    // 清除队伍详情缓存
                    String teamIdKey = String.format("fontal:team:getUsersByTeamId:%s", teamId);
                    redisTemplate.delete(teamIdKey);

                    // 清除最新队伍缓存（常用limit: 10, 20, 50）
                    redisTemplate.delete(String.format("fontal:team:new:%d", 10));
                    redisTemplate.delete(String.format("fontal:team:new:%d", 20));
                    redisTemplate.delete(String.format("fontal:team:new:%d", 50));

                    // 清除热门队伍缓存（常用limit: 10, 20, 50）
                    redisTemplate.delete(String.format("fontal:team:hot:%d", 10));
                    redisTemplate.delete(String.format("fontal:team:hot:%d", 20));
                    redisTemplate.delete(String.format("fontal:team:hot:%d", 50));

                    // 清除推荐队伍缓存（由于包含userId，无法精确删除，依赖缓存过期）
                    log.info("清除队伍缓存成功, teamId: {}", teamId);
                } catch (Exception e) {
                    log.error("清除队伍缓存失败", e);
                }

                return true;
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除操作过于频繁，请稍后再试");
            }
        } catch (InterruptedException e) {
            log.error("deleteTeam error", e);
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除队伍失败");
        } finally {
            // 释放锁时，先判断是否是当前线程持有的锁
            if (lock.isHeldByCurrentThread()) {
                log.info("unLock for delete team, teamId: {}, userId: {}", teamId, loginUser.getId());
                lock.unlock();
            }
        }
    }

    @Override
    public TeamUserVo getTeamsByUserId(Long userId, HttpServletRequest request) {
        // 1. 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "请先登录");
        }

        // 2. 参数校验
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }

        // 3. 权限校验：只能查看自己的队伍，管理员可以查看所有用户的队伍
        boolean isAdmin = userService.isAdmin(loginUser);
        if (!isAdmin && loginUser.getId() != userId) {
            throw new BusinessException(ErrorCode.NO_AUTH, "只能查看自己的队伍信息");
        }

        // 4. 一次查询：用户创建的 OR 用户加入的
        // 使用 OR 条件一次性查询所有相关队伍，避免多次查询和数据重复
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper
                .eq("userId", userId)  // 用户创建的队伍
                .or(w -> w.apply("JSON_CONTAINS(usersId, {0})", userId.toString()))  // 用户加入的队伍（usersId存储的是数字数组，不需要JSON_QUOTE）
        )
        .eq("isDelete", 0)  // 排除已删除的队伍
        .gt("expireTime", new Date());  // 排除已过期的队伍

        List<Team> teamList = this.list(queryWrapper);

        // 5. 调用 teamSet 方法转换为 VO 并返回
        return teamSet(teamList);
    }

    @Override
    public TeamBasicVO getTeamBasicInfo(Long teamId) {
        // 1. 参数校验
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍ID不能为空");
        }

        // 2. 查询队伍信息
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
        }

        // 3. 校验队伍是否已删除
        if (team.getIsDelete() != null && team.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已删除");
        }

        // 4. 校验队伍是否已过期
        if (team.getExpireTime() != null && team.getExpireTime().before(new Date())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已过期");
        }

        // 5. 尝试从缓存获取
        String cacheKey = String.format("fontal:team:basic:%s", teamId);
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        TeamBasicVO cachedVo = (TeamBasicVO) valueOperations.get(cacheKey);
        if (cachedVo != null) {
            return cachedVo;
        }

        // 6. 查询队长信息
        User captain = userService.getById(team.getUserId());
        if (captain == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "队长信息不存在");
        }

        // 7. 计算当前人数
        Set<Long> memberIds = StringUtils.stringJsonListToLongSet(team.getUsersId());
        int currentNum = memberIds.size();

        // 8. 封装 TeamBasicVO
        TeamBasicVO vo = new TeamBasicVO();
        vo.setId(team.getId());
        vo.setTeamName(team.getTeamName());
        vo.setTeamAvatarUrl(team.getTeamAvatarUrl());
        vo.setTeamDesc(team.getTeamDesc());
        vo.setMaxNum(team.getMaxNum());
        vo.setCurrentNum(currentNum);
        vo.setExpireTime(team.getExpireTime());
        vo.setTeamStatus(team.getTeamStatus());
        vo.setCreateTime(team.getCreateTime());
        vo.setAnnounce(team.getAnnounce());

        // 设置状态描述
        vo.setStatusDesc(getTeamStatusDesc(team.getTeamStatus()));

        // 设置队长信息
        TeamBasicVO.CaptainInfo captainInfo = new TeamBasicVO.CaptainInfo();
        captainInfo.setId(captain.getId());
        captainInfo.setUsername(captain.getUsername());
        captainInfo.setUserAccount(captain.getUserAccount());
        captainInfo.setUserAvatarUrl(captain.getUserAvatarUrl());
        captainInfo.setUserDesc(captain.getUserDesc());
        vo.setCaptain(captainInfo);

        // TODO: 设置标签和要求（需要新增字段到数据库）
        // vo.setTags(Arrays.asList("Java", "Spring Boot"));
        // vo.setRequirements("熟悉 Java 基础");

        // 9. 存入缓存（5分钟）
        setRedis(cacheKey, vo);

        return vo;
    }

    /**
     * 获取队伍状态描述
     */
    private String getTeamStatusDesc(Integer teamStatus) {
        if (teamStatus == null) {
            return "未知";
        }
        switch (teamStatus) {
            case TeamConstant.PUBLIC_TEAM_STATUS:
                return "公开";
            case TeamConstant.PRIVATE_TEAM_STATUS:
                return "私有";
            case TeamConstant.ENCRYPTION_TEAM_STATUS:
                return "加密";
            default:
                return "未知";
        }
    }

    @Override
    public TeamMembershipVO getTeamMembership(Long teamId, HttpServletRequest request) {
        // 1. 参数校验
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍ID不能为空");
        }

        // 2. 查询队伍信息
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
        }

        // 3. 校验队伍是否已删除
        if (team.getIsDelete() != null && team.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已删除");
        }

        // 4. 获取当前登录用户（允许未登录用户访问，返回非成员状态）
        User loginUser = userService.getLoginUser(request);

        // 5. 封装返回结果
        TeamMembershipVO membershipVO = new TeamMembershipVO();

        if (loginUser == null) {
            // 未登录用户
            membershipVO.setIsMember(false);
            membershipVO.setRole(TeamMembershipVO.Role.NON_MEMBER.getValue());
            return membershipVO;
        }

        // 6. 判断用户身份
        // 6.1 判断是否是队长
        boolean isCaptain = loginUser.getId()==team.getUserId();
        if (isCaptain) {
            membershipVO.setIsMember(true);
            membershipVO.setRole(TeamMembershipVO.Role.CAPTAIN.getValue());
            return membershipVO;
        }

        // 6.2 判断是否是普通成员
        Set<Long> memberIds = StringUtils.stringJsonListToLongSet(team.getUsersId());
        boolean isMember = memberIds.contains(loginUser.getId());

        membershipVO.setIsMember(isMember);
        if (isMember) {
            membershipVO.setRole(TeamMembershipVO.Role.MEMBER.getValue());
        } else {
            membershipVO.setRole(TeamMembershipVO.Role.NON_MEMBER.getValue());
        }

        return membershipVO;
    }

    @Override
    public List<TeamVo> searchTeams(String searchText) {
        // 1. 参数校验
        if (searchText == null || searchText.trim().isEmpty()) {
            // 如果搜索文本为空，返回空列表（或者可以返回所有队伍）
            return new ArrayList<>();
        }

        String text = searchText.trim();

        // 2. 构建查询条件
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();

        // 2.1 判断搜索文本是否为纯数字（可能是队伍ID）
        boolean isNumeric = text.matches("\\d+");

        // 2.2 构建多字段 OR 查询
        queryWrapper.nested(wrapper -> wrapper
                .like("teamName", text)
                .or()
                .eq("id", isNumeric ? Long.parseLong(text) : -1)
                .or()
                .like("teamDesc", text)
                .or()
                .like("announce", text)
        );

        // 2.3 添加过滤条件：未删除且未过期
        queryWrapper.eq("isDelete", 0)
                .gt("expireTime", new Date());

        // 3. 按创建时间倒序排列（最新的在前面）
        queryWrapper.orderByDesc("createTime");

        // 4. 执行查询
        List<Team> teamList = this.list(queryWrapper);

        // 5. 转换为 TeamVo 列表
        if (CollectionUtils.isEmpty(teamList)) {
            return new ArrayList<>();
        }

        // 6. 批量转换为 TeamVo（复用 teamSet 的逻辑）
        TeamUserVo teamUserVo = teamSet(teamList);

        // 7. 提取 TeamVo 列表
        return new ArrayList<>(teamUserVo.getTeamSet());
    }
}
