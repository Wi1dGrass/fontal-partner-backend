package com.huixing.fontal.controller;

import com.huixing.fontal.common.BaseResponse;
import com.huixing.fontal.common.ErrorCode;
import com.huixing.fontal.common.ResultUtil;
import com.huixing.fontal.exception.BusinessException;
import com.huixing.fontal.model.entity.User;
import com.huixing.fontal.model.request.KickOutUserRequest;
import com.huixing.fontal.model.request.TeamApplyApproveRequest;
import com.huixing.fontal.model.request.TeamCreateRequest;
import com.huixing.fontal.model.request.TeamDeleteRequest;
import com.huixing.fontal.model.request.TeamJoinApplyRequest;
import com.huixing.fontal.model.request.TeamJoinRequest;
import com.huixing.fontal.model.request.TeamUpdateRequest;
import com.huixing.fontal.model.request.TransferTeamRequest;
import com.huixing.fontal.model.vo.TeamBasicVO;
import com.huixing.fontal.model.vo.TeamJoinApplyVO;
import com.huixing.fontal.model.vo.TeamMembershipVO;
import com.huixing.fontal.model.vo.TeamUserVo;
import com.huixing.fontal.model.vo.TeamVo;
import com.huixing.fontal.service.TeamJoinApplyService;
import com.huixing.fontal.service.TeamMatchService;
import com.huixing.fontal.service.TeamService;
import com.huixing.fontal.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

@Slf4j
@Api(tags = "队伍管理")
@RestController
@RequestMapping("/team")
public class TeamController {
    @Resource
    private UserService userService;

    @Resource
    private TeamService teamService;

    @Resource
    private TeamMatchService teamMatchService;

    @Resource
    private TeamJoinApplyService teamJoinApplyService;

    /**
     * 根据队伍Id获取Team信息
     */
    @ApiOperation("根据队伍ID获取队伍信息")
    @GetMapping("/{teamId}")
    public BaseResponse<TeamVo> getUsersByTeamId(@PathVariable("teamId") Long teamId, HttpServletRequest request) {
        if(teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"未加入队伍");
        }
        TeamVo teamVo = teamService.getUsersByTeamId(teamId,request);
        return ResultUtil.success(teamVo);
    }

    /**
     * 获取队伍基础信息（非成员可访问）
     * 不需要登录，返回队伍基本信息（包含队长信息但不包含完整成员列表）
     */
    @ApiOperation("获取队伍基础信息")
    @GetMapping("/{teamId}/basic")
    public BaseResponse<TeamBasicVO> getTeamBasicInfo(@PathVariable("teamId") Long teamId) {
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍ID不能为空");
        }
        TeamBasicVO teamBasicInfo = teamService.getTeamBasicInfo(teamId);
        return ResultUtil.success(teamBasicInfo);
    }

    /**
     * 获取用户在队伍中的成员身份
     * 轻量级接口，快速判断用户是否是队伍成员及其角色
     * 允许未登录用户访问，返回非成员状态
     */
    @ApiOperation("获取用户在队伍中的成员身份")
    @GetMapping("/{teamId}/membership")
    public BaseResponse<TeamMembershipVO> getTeamMembership(
            @PathVariable("teamId") Long teamId,
            HttpServletRequest request) {
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍ID不能为空");
        }
        TeamMembershipVO membership = teamService.getTeamMembership(teamId, request);
        return ResultUtil.success(membership);
    }

    /**
     * 获取所有队伍信息
     */
    @ApiOperation("获取所有队伍信息")
    @GetMapping("/team")
    public BaseResponse<TeamUserVo> getTeams(){
        TeamUserVo teams = teamService.getTeams();
        return ResultUtil.success(teams);
    }

    /**
     * 通过队伍Id获取队伍列表
     */
    @ApiOperation("通过队伍ID列表获取队伍信息")
    @GetMapping("/teamsByIds")
    public BaseResponse<TeamUserVo> getTeamsByIds(@RequestParam(required = false)Set<Long> teamIds,HttpServletRequest request) {
        if (CollectionUtils.isEmpty(teamIds)) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        TeamUserVo teamUserVo = teamService.getTeamsByIds(teamIds,request);
        return ResultUtil.success(teamUserVo);
    }

    /**
     * 创建队伍
     */
    @ApiOperation("创建队伍")
    @PostMapping("/create")
    public BaseResponse<Boolean> createTeam(@RequestBody TeamCreateRequest teamCreateRequest, HttpServletRequest request) {
        if (teamCreateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Boolean result = teamService.createTeam(teamCreateRequest, request);
        return ResultUtil.success(result);
    }

    /**
     * 加入队伍
     */
    @ApiOperation("加入队伍")
    @PostMapping("/join")
    public BaseResponse<Boolean> joinTeam(@RequestBody TeamJoinRequest joinTeam, HttpServletRequest request) {
        if (joinTeam == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Boolean result = teamService.joinTeam(joinTeam, request);
        return ResultUtil.success(result, "加入队伍成功");
    }
    /**
     * 退出队伍
     */
    @ApiOperation("退出队伍")
    @PostMapping("/quit/{teamId}")
    public BaseResponse<Boolean> quitTeam(@PathVariable("teamId") Long teamId, HttpServletRequest request) {
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Boolean result = teamService.quitTeam(teamId, request);
        return ResultUtil.success(result, "退出队伍成功");
    }
    /**
     * 踢出队伍成员
     */
    @ApiOperation("踢出队伍成员")
    @PostMapping("/kickOutUser")
    public BaseResponse<Boolean> kickOutByUserId(@RequestBody KickOutUserRequest kickOutUserRequest, HttpServletRequest request) {
        if (kickOutUserRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Boolean result = teamService.kickOutUser(kickOutUserRequest, request);
        return ResultUtil.success(result, "踢出成员成功");
    }

    /**
     * 更新队伍
     */
    @ApiOperation("更新队伍信息")
    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest, HttpServletRequest request) {
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Boolean result = teamService.updateTeam(teamUpdateRequest, request);
        return ResultUtil.success(result, "更新队伍成功");
    }

    /**
     * 转让队长
     */
    @ApiOperation("转让队长")
    @PostMapping("/transfer")
    public BaseResponse<Boolean> transferTeam(@RequestBody TransferTeamRequest transferTeamRequest, HttpServletRequest request) {
        if (transferTeamRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Boolean result = teamService.transferTeam(transferTeamRequest, request);
        return ResultUtil.success(result, "转让队长成功");
    }

    /**
     * 删除队伍
     */
    @ApiOperation("删除队伍")
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(@RequestBody TeamDeleteRequest teamDeleteRequest, HttpServletRequest request) {
        if (teamDeleteRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Boolean result = teamService.deleteTeam(teamDeleteRequest, request);
        return ResultUtil.success(result, "删除队伍成功");
    }

    /**
     * 获取推荐队伍（混合策略）
     * 考虑标签相似度、队伍活跃度、时间等因素
     */
    @ApiOperation("获取推荐队伍")
    @GetMapping("/recommend")
    public BaseResponse<List<TeamVo>> getRecommendTeams(
            @RequestParam(defaultValue = "20") int limit,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "请先登录");
        }
        List<TeamVo> recommendTeams = teamMatchService.getRecommendTeams(loginUser.getId(), limit);
        return ResultUtil.success(recommendTeams);
    }

    /**
     * 获取热门队伍
     * 根据队伍成员数、创建时间等因素排序
     */
    @ApiOperation("获取热门队伍")
    @GetMapping("/hot")
    public BaseResponse<List<TeamVo>> getHotTeams(@RequestParam(defaultValue = "20") int limit) {
        List<TeamVo> hotTeams = teamMatchService.getHotTeams(limit);
        return ResultUtil.success(hotTeams);
    }

    /**
     * 获取最新队伍
     * 按创建时间倒序
     */
    @ApiOperation("获取最新队伍")
    @GetMapping("/new")
    public BaseResponse<List<TeamVo>> getNewTeams(@RequestParam(defaultValue = "20") int limit) {
        List<TeamVo> newTeams = teamMatchService.getNewTeams(limit);
        return ResultUtil.success(newTeams);
    }

    /**
     * 根据用户ID获取其创建和加入的队伍列表
     * 用户可以查看自己创建和加入的所有队伍
     */
    @ApiOperation("根据用户ID获取其创建和加入的队伍列表")
    @GetMapping("/user/{userId}")
    public BaseResponse<TeamUserVo> getTeamsByUserId(@PathVariable("userId") Long userId, HttpServletRequest request) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }
        TeamUserVo teamUserVo = teamService.getTeamsByUserId(userId, request);
        return ResultUtil.success(teamUserVo);
    }

    // ==================== 队伍加入申请/邀请相关接口 ====================

    /**
     * 申请加入私有队伍
     * 用户通过该接口向私有队伍提交加入申请，需要队长审批
     */
    @ApiOperation("申请加入私有队伍")
    @PostMapping("/apply")
    public BaseResponse<Long> applyToJoinTeam(@RequestBody TeamJoinApplyRequest applyRequest, HttpServletRequest request) {
        if (applyRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "申请参数不能为空");
        }
        Long applyId = teamJoinApplyService.createJoinApplication(applyRequest, request);
        return ResultUtil.success(applyId, "申请已提交，请等待队长审批");
    }

    /**
     * 邀请用户加入队伍
     * 队长或成员通过该接口邀请用户加入队伍
     */
    @ApiOperation("邀请用户加入队伍")
    @PostMapping("/invite")
    public BaseResponse<Long> inviteUserToTeam(@RequestBody TeamJoinApplyRequest inviteRequest, HttpServletRequest request) {
        if (inviteRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邀请参数不能为空");
        }
        if (inviteRequest.getInviteeId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "被邀请用户ID不能为空");
        }
        Long inviteId = teamJoinApplyService.createJoinApplication(inviteRequest, request);
        return ResultUtil.success(inviteId, "邀请已发送");
    }

    /**
     * 处理队伍邀请
     * 被邀请用户通过该接口接受或拒绝邀请
     */
    @ApiOperation("处理队伍邀请")
    @PostMapping("/invite/handle")
    public BaseResponse<Boolean> handleTeamInvite(@RequestBody TeamApplyApproveRequest handleRequest, HttpServletRequest request) {
        if (handleRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "处理参数不能为空");
        }
        Boolean result = teamJoinApplyService.approveApplication(handleRequest, request);
        String message = handleRequest.getStatus() == 1 ? "已接受邀请" : "已拒绝邀请";
        return ResultUtil.success(result, message);
    }

    /**
     * 审批队伍加入申请
     * 队长通过该接口审批用户的加入申请（通过或拒绝）
     */
    @ApiOperation("审批队伍加入申请")
    @PostMapping("/approve")
    public BaseResponse<Boolean> approveApplication(@RequestBody TeamApplyApproveRequest approveRequest, HttpServletRequest request) {
        if (approveRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "审批参数不能为空");
        }
        Boolean result = teamJoinApplyService.approveApplication(approveRequest, request);
        String message = approveRequest.getStatus() == 1 ? "已通过申请" : "已拒绝申请";
        return ResultUtil.success(result, message);
    }

    /**
     * 获取队伍的待审批申请列表
     * 队长查看该队伍所有待审批的加入申请
     */
    @ApiOperation("获取队伍的待审批申请列表")
    @GetMapping("/applications/{teamId}")
    public BaseResponse<List<TeamJoinApplyVO>> getPendingApplications(
            @PathVariable("teamId") Long teamId,
            HttpServletRequest request) {
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍ID不能为空");
        }
        List<TeamJoinApplyVO> applications = teamJoinApplyService.getPendingApplications(teamId, request);
        return ResultUtil.success(applications);
    }

    /**
     * 获取我的所有申请列表
     * 用户查看自己提交的所有队伍加入申请（包括待审批、已通过、已拒绝）
     */
    @ApiOperation("获取我的所有申请列表")
    @GetMapping("/my-applications")
    public BaseResponse<List<TeamJoinApplyVO>> getMyApplications(HttpServletRequest request) {
        List<TeamJoinApplyVO> applications = teamJoinApplyService.getUserApplications(request);
        return ResultUtil.success(applications);
    }

    /**
     * 取消加入申请
     * 用户取消自己提交的待审批申请
     */
    @ApiOperation("取消加入申请")
    @PostMapping("/apply/cancel/{teamId}")
    public BaseResponse<Boolean> cancelApplication(
            @PathVariable("teamId") Long teamId,
            HttpServletRequest request) {
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍ID不能为空");
        }
        Boolean result = teamJoinApplyService.cancelApplication(teamId, request);
        return ResultUtil.success(result, "已取消申请");
    }

    /**
     * 获取申请详情
     * 查看某个申请的详细信息（只有申请者、队长或管理员可以查看）
     */
    @ApiOperation("获取申请详情")
    @GetMapping("/application/{applyId}")
    public BaseResponse<TeamJoinApplyVO> getApplicationDetail(
            @PathVariable("applyId") Long applyId,
            HttpServletRequest request) {
        if (applyId == null || applyId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "申请ID不能为空");
        }
        TeamJoinApplyVO application = teamJoinApplyService.getApplicationById(applyId, request);
        return ResultUtil.success(application);
    }

    /**
     * 搜索队伍
     * 根据队伍名称、ID、描述或公告搜索队伍
     */
    @ApiOperation("搜索队伍")
    @GetMapping("/search")
    public BaseResponse<List<TeamVo>> searchTeams(@RequestParam("text") String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "搜索内容不能为空");
        }
        List<TeamVo> teams = teamService.searchTeams(searchText);
        return ResultUtil.success(teams);
    }
}
