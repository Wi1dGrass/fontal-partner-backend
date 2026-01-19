package com.huixing.fontal.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.huixing.fontal.model.entity.Team;
import com.huixing.fontal.model.request.KickOutUserRequest;
import com.huixing.fontal.model.request.TeamCreateRequest;
import com.huixing.fontal.model.request.TeamDeleteRequest;
import com.huixing.fontal.model.request.TeamJoinRequest;
import com.huixing.fontal.model.request.TeamUpdateRequest;
import com.huixing.fontal.model.request.TransferTeamRequest;
import com.huixing.fontal.model.vo.TeamBasicVO;
import com.huixing.fontal.model.vo.TeamJoinApplyVO;
import com.huixing.fontal.model.vo.TeamMembershipVO;
import com.huixing.fontal.model.vo.TeamUserVo;
import com.huixing.fontal.model.vo.TeamVo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

public interface TeamService extends IService<Team> {
    TeamVo getUsersByTeamId(Long teamId, HttpServletRequest request);

    TeamUserVo getTeams();

    TeamUserVo teamSet(List<Team> teamList);

    TeamUserVo getTeamsByIds(Set<Long> teamIds, HttpServletRequest request);

    Boolean createTeam(TeamCreateRequest teamCreateRequest, HttpServletRequest request);

    Boolean joinTeam(TeamJoinRequest teamJoinRequest, HttpServletRequest request);

    Boolean quitTeam(Long teamId, HttpServletRequest request);

    Boolean kickOutUser(KickOutUserRequest kickOutUserRequest, HttpServletRequest request);

    Boolean updateTeam(TeamUpdateRequest teamUpdateRequest, HttpServletRequest request);

    Boolean transferTeam(TransferTeamRequest transferTeamRequest, HttpServletRequest request);

    Boolean deleteTeam(TeamDeleteRequest teamDeleteRequest, HttpServletRequest request);

    /**
     * 根据用户ID获取其创建和加入的队伍列表
     * @param userId 用户ID
     * @param request HTTP请求
     * @return 队伍用户视图对象
     */
    TeamUserVo getTeamsByUserId(Long userId, HttpServletRequest request);

    /**
     * 获取队伍基础信息（非成员可访问）
     * @param teamId 队伍ID
     * @return 队伍基础信息VO
     */
    TeamBasicVO getTeamBasicInfo(Long teamId);

    /**
     * 获取用户在队伍中的成员身份
     * @param teamId 队伍ID
     * @param request HTTP请求
     * @return 队伍成员身份VO
     */
    TeamMembershipVO getTeamMembership(Long teamId, HttpServletRequest request);

    /**
     * 搜索队伍（按队伍名、ID、描述、公告搜索）
     * @param searchText 搜索文本
     * @return 队伍列表
     */
    List<TeamVo> searchTeams(String searchText);
}
