package com.huixing.fontal.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.huixing.fontal.model.entity.TeamJoinApply;
import com.huixing.fontal.model.request.TeamApplyApproveRequest;
import com.huixing.fontal.model.request.TeamJoinApplyRequest;
import com.huixing.fontal.model.vo.TeamJoinApplyVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 队伍加入申请服务
 *
 * @author fontal
 */
public interface TeamJoinApplyService extends IService<TeamJoinApply> {

    /**
     * 创建加入申请（用户申请加入私有队伍）
     *
     * @param teamJoinApplyRequest 申请请求
     * @param request HTTP请求
     * @return 申请ID
     */
    Long createJoinApplication(TeamJoinApplyRequest teamJoinApplyRequest, HttpServletRequest request);

    /**
     * 审批加入申请（队长审批）
     *
     * @param approveRequest 审批请求
     * @param request HTTP请求
     * @return 是否成功
     */
    Boolean approveApplication(TeamApplyApproveRequest approveRequest, HttpServletRequest request);

    /**
     * 获取队伍的待审批申请列表（队长查看）
     *
     * @param teamId 队伍ID
     * @param request HTTP请求
     * @return 申请列表
     */
    List<TeamJoinApplyVO> getPendingApplications(Long teamId, HttpServletRequest request);

    /**
     * 获取用户的申请列表（申请者查看自己的申请）
     *
     * @param request HTTP请求
     * @return 申请列表
     */
    List<TeamJoinApplyVO> getUserApplications(HttpServletRequest request);

    /**
     * 取消加入申请
     *
     * @param teamId 队伍ID
     * @param request HTTP请求
     * @return 是否成功
     */
    Boolean cancelApplication(Long teamId, HttpServletRequest request);

    /**
     * 根据ID获取申请详情
     *
     * @param applyId 申请ID
     * @param request HTTP请求
     * @return 申请详情
     */
    TeamJoinApplyVO getApplicationById(Long applyId, HttpServletRequest request);
}