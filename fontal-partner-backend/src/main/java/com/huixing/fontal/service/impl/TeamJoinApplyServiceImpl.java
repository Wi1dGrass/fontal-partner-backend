package com.huixing.fontal.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.huixing.fontal.common.ErrorCode;
import com.huixing.fontal.contant.TeamApplyConstant;
import com.huixing.fontal.exception.BusinessException;
import com.huixing.fontal.mapper.TeamJoinApplyMapper;
import com.huixing.fontal.model.entity.Team;
import com.huixing.fontal.model.entity.TeamJoinApply;
import com.huixing.fontal.model.entity.User;
import com.huixing.fontal.model.request.TeamApplyApproveRequest;
import com.huixing.fontal.model.request.TeamJoinApplyRequest;
import com.huixing.fontal.model.vo.TeamJoinApplyVO;
import com.huixing.fontal.service.TeamJoinApplyService;
import com.huixing.fontal.service.TeamService;
import com.huixing.fontal.service.UserService;
import com.huixing.fontal.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 队伍加入申请服务实现
 *
 * @author fontal
 */
@Slf4j
@Service
public class TeamJoinApplyServiceImpl extends ServiceImpl<TeamJoinApplyMapper, TeamJoinApply>
        implements TeamJoinApplyService {

    private static final Gson GSON = new Gson();

    @Resource
    private UserService userService;

    @Resource
    private TeamService teamService;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private RedisTemplate redisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createJoinApplication(TeamJoinApplyRequest teamJoinApplyRequest, HttpServletRequest request) {
        // 判断是申请场景还是邀请场景
        boolean isInvite = teamJoinApplyRequest.getInviteeId() != null;

        // 1. 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "请先登录");
        }

        // 2. 参数校验
        Long teamId = teamJoinApplyRequest.getTeamId();
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍ID不能为空");
        }

        // 3. 查询队伍信息
        Team team = teamService.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
        }

        // 4. 校验队伍状态
        if (team.getIsDelete() != null && team.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已删除");
        }
        if (team.getExpireTime() != null && team.getExpireTime().before(new Date())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已过期");
        }

        // 5. 根据场景进行不同的校验和处理
        Long targetUserId;  // 目标用户ID（申请人或被邀请人）
        Long operatorUserId; // 操作者用户ID（申请人或邀请人）

        if (isInvite) {
            // ============ 邀请场景 ============
            // 5.1. 校验邀请人是否是队伍成员
            Set<Long> teamMemberIds = StringUtils.stringJsonListToLongSet(team.getUsersId());
            if (!teamMemberIds.contains(loginUser.getId())) {
                throw new BusinessException(ErrorCode.NO_AUTH, "只有队伍成员才能邀请用户加入");
            }

            // 5.2. 参数校验
            Long inviteeId = teamJoinApplyRequest.getInviteeId();
            if (inviteeId == null || inviteeId <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "被邀请用户ID不能为空");
            }

            // 5.3. 不能邀请自己
            if (inviteeId.equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能邀请自己");
            }

            // 5.4. 校验被邀请人是否存在
            User invitee = userService.getById(inviteeId);
            if (invitee == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "被邀请用户不存在");
            }

            // 5.5. 校验被邀请人是否已在队伍中
            if (teamMemberIds.contains(inviteeId)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "该用户已在该队伍中");
            }

            // 5.6. 校验队伍人数是否已满
            int currentMemberCount = teamMemberIds.size();
            if (team.getMaxNum() != null && currentMemberCount >= team.getMaxNum()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数已满");
            }

            targetUserId = inviteeId;
            operatorUserId = loginUser.getId();

        } else {
            // ============ 申请场景 ============
            // 5.1. 校验队伍类型（只有私有队伍需要申请）
            if (team.getTeamStatus() == null || team.getTeamStatus() != 1) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "只有私有队伍需要申请加入");
            }

            // 5.2. 校验用户是否已在队伍中
            Set<Long> teamMemberIds = StringUtils.stringJsonListToLongSet(team.getUsersId());
            if (teamMemberIds.contains(loginUser.getId())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "您已在该队伍中");
            }

            // 5.3. 校验队伍人数是否已满
            int currentMemberCount = teamMemberIds.size();
            if (team.getMaxNum() != null && currentMemberCount >= team.getMaxNum()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数已满");
            }

            targetUserId = loginUser.getId();
            operatorUserId = team.getUserId();
        }

        // 6. 使用分布式锁防止重复操作
        String lockKey = String.format("fontal:team:apply:lock:%s:%s", teamId, targetUserId);
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.tryLock(0, 10, TimeUnit.SECONDS)) {
                log.info("get lock for create join application, teamId: {}, userId: {}, isInvite: {}",
                    teamId, targetUserId, isInvite);

                // 7. 检查是否存在待处理的申请/邀请（防止重复）
                QueryWrapper<TeamJoinApply> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("teamId", teamId)
                        .eq("userId", targetUserId)
                        .eq("applyStatus", TeamApplyConstant.APPLY_STATUS_PENDING)
                        .eq("isDelete", 0)
                        .gt("expireTime", new Date());
                TeamJoinApply existingApply = this.getOne(queryWrapper);

                if (existingApply != null) {
                    if (isInvite) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "该用户已有待处理的邀请");
                    } else {
                        // 检查最后一次申请时间，防止60秒内重复申请
                        Date lastApplyTime = existingApply.getCreateTime();
                        long secondsSinceLastApply = (System.currentTimeMillis() - lastApplyTime.getTime()) / 1000;
                        if (secondsSinceLastApply < TeamApplyConstant.REAPPLY_INTERVAL_SECONDS) {
                            throw new BusinessException(ErrorCode.PARAMS_ERROR,
                                String.format("您已提交过申请，请%d秒后再试",
                                    TeamApplyConstant.REAPPLY_INTERVAL_SECONDS - (int) secondsSinceLastApply));
                        }
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "您已有待审批的申请，请等待队长审批");
                    }
                }

                // 8. 创建申请/邀请记录
                TeamJoinApply apply = new TeamJoinApply();
                apply.setTeamId(teamId);
                apply.setUserId(targetUserId);
                apply.setLeaderId(operatorUserId);
                apply.setApplyType(isInvite ? 1 : 0);  // 0-申请, 1-邀请
                apply.setApplyStatus(TeamApplyConstant.APPLY_STATUS_PENDING);
                apply.setApplyMessage(teamJoinApplyRequest.getApplyMessage());

                // 设置过期时间（默认7天后过期）
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_MONTH, TeamApplyConstant.APPLY_EXPIRE_DAYS);
                apply.setExpireTime(calendar.getTime());
                apply.setIsDelete(0);

                // 9. 保存记录
                boolean result = this.save(apply);
                if (!result) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, isInvite ? "创建邀请失败" : "创建申请失败");
                }

                log.info("{}成功，队伍: {}, 用户: {}, 操作者: {}, 记录ID: {}",
                    isInvite ? "邀请" : "申请", teamId, targetUserId, operatorUserId, apply.getId());

                // TODO: 10. 发送通知（申请→队长，邀请→被邀请人）
                // if (isInvite) {
                //     webSocketService.sendToUser(targetUserId, "您收到新的队伍邀请", applyVO);
                // } else {
                //     webSocketService.sendToUser(operatorUserId, "您有新的队伍加入申请", applyVO);
                // }

                return apply.getId();
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "操作过于频繁，请稍后再试");
            }
        } catch (InterruptedException e) {
            log.error("createJoinApplication error", e);
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, isInvite ? "创建邀请失败" : "创建申请失败");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                log.info("unLock for create join application, teamId: {}, userId: {}", teamId, targetUserId);
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean approveApplication(TeamApplyApproveRequest approveRequest, HttpServletRequest request) {
        // 1. 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "请先登录");
        }

        // 2. 参数校验
        Long applyId = approveRequest.getApplyId();
        if (applyId == null || applyId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "申请ID不能为空");
        }

        Integer status = approveRequest.getStatus();
        if (status == null ||
            (status != TeamApplyConstant.APPLY_STATUS_APPROVED &&
             status != TeamApplyConstant.APPLY_STATUS_REJECTED)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "状态参数错误");
        }

        // 3. 查询申请/邀请记录
        TeamJoinApply apply = this.getById(applyId);
        if (apply == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "记录不存在");
        }

        // 4. 校验记录是否已删除或已过期
        if (apply.getIsDelete() != null && apply.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "记录已删除");
        }
        if (apply.getExpireTime() != null && apply.getExpireTime().before(new Date())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "记录已过期");
        }

        // 5. 校验状态（只能处理待处理的记录）
        if (TeamApplyConstant.APPLY_STATUS_PENDING != apply.getApplyStatus()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该记录已被处理");
        }

        // 6. 判断是申请还是邀请，进行权限校验
        boolean isInvite = apply.getApplyType() != null && apply.getApplyType() == 1;

        if (isInvite) {
            // ============ 邀请场景：被邀请人处理 ============
            // 校验当前用户是否是被邀请人
            if (loginUser.getId() != apply.getUserId()) {
                throw new BusinessException(ErrorCode.NO_AUTH, "只有被邀请人才能处理邀请");
            }
        } else {
            // ============ 申请场景：队长审批 ============
            // 校验当前用户是否是队长
            if (loginUser.getId() != (apply.getLeaderId())) {
                throw new BusinessException(ErrorCode.NO_AUTH, "只有队长才能审批申请");
            }
        }

        // 7. 使用分布式锁保证并发安全
        String lockKey = String.format("fontal:team:approve:lock:%s", apply.getTeamId());
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.tryLock(0, 10, TimeUnit.SECONDS)) {
                log.info("get lock for approve application, applyId: {}, userId: {}, isInvite: {}",
                    applyId, loginUser.getId(), isInvite);

                // 8. 从数据库重新获取记录，确保数据一致性
                TeamJoinApply applyFromDb = this.getById(applyId);
                if (applyFromDb == null ||
                    TeamApplyConstant.APPLY_STATUS_PENDING != applyFromDb.getApplyStatus()) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "记录已被处理或不存在");
                }

                // 9. 根据处理结果操作
                if (TeamApplyConstant.APPLY_STATUS_APPROVED == status) {
                    // 通过/接受：将用户加入队伍
                    return handleApproveApplication(applyFromDb, approveRequest.getRejectReason(), isInvite);
                } else {
                    // 拒绝
                    return handleRejectApplication(applyFromDb, approveRequest.getRejectReason(), isInvite);
                }
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "操作过于频繁，请稍后再试");
            }
        } catch (InterruptedException e) {
            log.error("approveApplication error", e);
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "处理失败");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                log.info("unLock for approve application, applyId: {}, userId: {}", applyId, loginUser.getId());
                lock.unlock();
            }
        }
    }

    /**
     * 处理通过申请/接受邀请
     */
    private Boolean handleApproveApplication(TeamJoinApply apply, String rejectReason, boolean isInvite) {
        // 1. 查询队伍信息
        Team team = teamService.getById(apply.getTeamId());
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
        }

        // 2. 再次校验队伍人数（防止并发审批导致人数超限）
        Set<Long> teamMemberIds = StringUtils.stringJsonListToLongSet(team.getUsersId());
        if (teamMemberIds.size() >= team.getMaxNum()) {
            // 更新申请状态为已拒绝
            apply.setApplyStatus(TeamApplyConstant.APPLY_STATUS_REJECTED);
            apply.setRejectReason("队伍人数已满");
            this.updateById(apply);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数已满");
        }

        // 3. 检查用户是否已在队伍中（防止重复加入）
        if (teamMemberIds.contains(apply.getUserId())) {
            apply.setApplyStatus(TeamApplyConstant.APPLY_STATUS_REJECTED);
            apply.setRejectReason("用户已在队伍中");
            this.updateById(apply);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户已在队伍中");
        }

        // 4. 将用户加入队伍
        teamMemberIds.add(apply.getUserId());
        team.setUsersId(GSON.toJson(teamMemberIds));
        team.setUpdateTime(new Date());
        boolean updateTeamResult = teamService.updateById(team);
        if (!updateTeamResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "加入队伍失败");
        }

        // 5. 更新用户的teamIds字段
        User applicant = userService.getById(apply.getUserId());
        if (applicant == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "申请用户不存在");
        }
        Set<Long> userTeamIds = StringUtils.stringJsonListToLongSet(applicant.getTeamIds());
        userTeamIds.add(apply.getTeamId());
        applicant.setTeamIds(GSON.toJson(userTeamIds));
        boolean updateUserResult = userService.updateById(applicant);
        if (!updateUserResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新用户队伍信息失败");
        }

        // 6. 更新申请状态为已通过
        apply.setApplyStatus(TeamApplyConstant.APPLY_STATUS_APPROVED);
        apply.setRejectReason(rejectReason);
        boolean updateApplyResult = this.updateById(apply);
        if (!updateApplyResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新申请状态失败");
        }

        // 7. 清除缓存
        try {
            String teamsKey = "fontal:team:getTeams:getTeams";
            redisTemplate.delete(teamsKey);
            String teamIdKey = String.format("fontal:team:getUsersByTeamId:%s", apply.getTeamId());
            redisTemplate.delete(teamIdKey);
            log.info("清除队伍缓存成功, teamId: {}", apply.getTeamId());
        } catch (Exception e) {
            log.error("清除队伍缓存失败", e);
        }

        // TODO: 8. 发送通知给申请者（WebSocket或消息表）
        // webSocketService.sendToUser(apply.getUserId(), "您的队伍加入申请已通过", null);

        log.info("申请通过，用户 {} 已加入队伍 {}", apply.getUserId(), apply.getTeamId());
        return true;
    }

    /**
     * 处理拒绝申请/邀请
     */
    private Boolean handleRejectApplication(TeamJoinApply apply, String rejectReason, boolean isInvite) {
        // 1. 更新申请状态为已拒绝
        apply.setApplyStatus(TeamApplyConstant.APPLY_STATUS_REJECTED);
        apply.setRejectReason(rejectReason);
        boolean result = this.updateById(apply);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "拒绝申请失败");
        }

        // TODO: 2. 发送通知给申请者（WebSocket或消息表）
        // webSocketService.sendToUser(apply.getUserId(), "您的队伍加入申请已被拒绝", rejectReason);

        log.info("申请被拒绝，申请ID: {}, 拒绝原因: {}", apply.getId(), rejectReason);
        return true;
    }

    @Override
    public List<TeamJoinApplyVO> getPendingApplications(Long teamId, HttpServletRequest request) {
        // 1. 获取当前登录用户（队长）
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "请先登录");
        }

        // 2. 参数校验
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍ID不能为空");
        }

        // 3. 查询队伍信息
        Team team = teamService.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
        }

        // 4. 权限校验（只有队长可以查看）
        if (loginUser.getId() != team.getUserId()) {
            throw new BusinessException(ErrorCode.NO_AUTH, "只有队长可以查看待审批申请");
        }

        // 5. 查询所有待审批的申请
        QueryWrapper<TeamJoinApply> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teamId", teamId)
                .eq("applyStatus", TeamApplyConstant.APPLY_STATUS_PENDING)
                .eq("isDelete", 0)
                .gt("expireTime", new Date())
                .orderByDesc("createTime");
        List<TeamJoinApply> applyList = this.list(queryWrapper);

        // 6. 转换为VO并返回
        return applyListToVOList(applyList);
    }

    @Override
    public List<TeamJoinApplyVO> getUserApplications(HttpServletRequest request) {
        // 1. 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "请先登录");
        }

        // 2. 查询用户的所有申请
        QueryWrapper<TeamJoinApply> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", loginUser.getId())
                .eq("isDelete", 0)
                .orderByDesc("createTime");
        List<TeamJoinApply> applyList = this.list(queryWrapper);

        // 3. 转换为VO并返回
        return applyListToVOList(applyList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean cancelApplication(Long teamId, HttpServletRequest request) {
        // 1. 获取当前登录用户（申请者）
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "请先登录");
        }

        // 2. 参数校验
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍ID不能为空");
        }

        // 3. 查询待审批的申请
        QueryWrapper<TeamJoinApply> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teamId", teamId)
                .eq("userId", loginUser.getId())
                .eq("applyStatus", TeamApplyConstant.APPLY_STATUS_PENDING)
                .eq("isDelete", 0)
                .gt("expireTime", new Date());
        TeamJoinApply apply = this.getOne(queryWrapper);

        if (apply == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "待审批的申请不存在");
        }

        // 4. 更新申请状态为已取消
        apply.setApplyStatus(TeamApplyConstant.APPLY_STATUS_CANCELLED);
        boolean result = this.updateById(apply);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "取消申请失败");
        }

        // TODO: 5. 发送通知给队长（WebSocket或消息表）
        // webSocketService.sendToUser(apply.getLeaderId(), "用户取消了加入申请", null);

        log.info("用户 {} 取消了加入队伍 {} 的申请", loginUser.getId(), teamId);
        return true;
    }

    @Override
    public TeamJoinApplyVO getApplicationById(Long applyId, HttpServletRequest request) {
        // 1. 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "请先登录");
        }

        // 2. 参数校验
        if (applyId == null || applyId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "申请ID不能为空");
        }

        // 3. 查询申请记录
        TeamJoinApply apply = this.getById(applyId);
        if (apply == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "申请不存在");
        }

        // 4. 权限校验（只有申请者、队长或管理员可以查看）
        boolean isApplicant = loginUser.getId() == apply.getUserId();
        boolean isLeader = loginUser.getId() == apply.getLeaderId();
        boolean isAdmin = userService.isAdmin(loginUser);

        if (!isApplicant && !isLeader && !isAdmin) {
            throw new BusinessException(ErrorCode.NO_AUTH, "暂无权限查看该申请");
        }

        // 5. 转换为VO并返回
        List<TeamJoinApplyVO> voList = applyListToVOList(Collections.singletonList(apply));
        return voList.isEmpty() ? null : voList.get(0);
    }

    /**
     * 将申请列表转换为VO列表
     */
    private List<TeamJoinApplyVO> applyListToVOList(List<TeamJoinApply> applyList) {
        if (applyList == null || applyList.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. 收集所有需要的用户ID和队伍ID
        Set<Long> userIds = new HashSet<>();
        Set<Long> teamIds = new HashSet<>();
        Map<Long, TeamJoinApply> applyMap = new HashMap<>();

        for (TeamJoinApply apply : applyList) {
            userIds.add(apply.getUserId());
            userIds.add(apply.getLeaderId());
            teamIds.add(apply.getTeamId());
            applyMap.put(apply.getId(), apply);
        }

        // 2. 批量查询用户信息
        List<User> users = userService.listByIds(userIds);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        // 3. 批量查询队伍信息
        List<Team> teams = teamService.listByIds(teamIds);
        Map<Long, Team> teamMap = teams.stream()
                .collect(Collectors.toMap(Team::getId, team -> team));

        // 4. 组装VO
        List<TeamJoinApplyVO> voList = new ArrayList<>();
        for (TeamJoinApply apply : applyList) {
            TeamJoinApplyVO vo = new TeamJoinApplyVO();
            BeanUtils.copyProperties(apply, vo);

            // 设置申请类型和状态描述
            vo.setApplyTypeDesc(getApplyTypeDesc(apply.getApplyType()));
            vo.setStatusDesc(getStatusDesc(apply.getApplyStatus()));

            // 设置队伍信息
            Team team = teamMap.get(apply.getTeamId());
            if (team != null) {
                vo.setTeamName(team.getTeamName());
                vo.setTeamAvatar(team.getTeamAvatarUrl());
            }

            // 设置用户信息（申请人/被邀请人）
            User user = userMap.get(apply.getUserId());
            if (user != null) {
                vo.setUserName(user.getUsername());
                vo.setUserAvatar(user.getUserAvatarUrl());
            }

            // 设置邀请人/队长信息
            User leader = userMap.get(apply.getLeaderId());
            if (leader != null) {
                vo.setLeaderName(leader.getUsername());
                vo.setLeaderAvatar(leader.getUserAvatarUrl());
            }

            voList.add(vo);
        }

        return voList;
    }

    /**
     * 获取申请类型描述
     */
    private String getApplyTypeDesc(Integer applyType) {
        if (applyType == null) {
            return "申请";
        }
        switch (applyType) {
            case 0:
                return "申请";
            case 1:
                return "邀请";
            default:
                return "申请";
        }
    }

    /**
     * 获取申请状态描述
     */
    private String getStatusDesc(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case TeamApplyConstant.APPLY_STATUS_PENDING:
                return "待审批";
            case TeamApplyConstant.APPLY_STATUS_APPROVED:
                return "已通过";
            case TeamApplyConstant.APPLY_STATUS_REJECTED:
                return "已拒绝";
            case TeamApplyConstant.APPLY_STATUS_CANCELLED:
                return "已取消";
            default:
                return "未知";
        }
    }
}