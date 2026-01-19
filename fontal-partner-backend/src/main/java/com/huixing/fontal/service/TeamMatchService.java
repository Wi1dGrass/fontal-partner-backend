package com.huixing.fontal.service;

import com.huixing.fontal.model.vo.TeamVo;

import java.util.List;
import java.util.Set;

/**
 * 队伍匹配服务接口
 * 提供基于标签相似度的队伍推荐功能
 *
 * @author fontal
 */
public interface TeamMatchService {

    /**
     * 获取推荐队伍（混合策略）
     * 考虑标签相似度、队伍活跃度、时间等因素
     *
     * @param userId 用户ID
     * @param limit  返回数量限制
     * @return 推荐队伍列表
     */
    List<TeamVo> getRecommendTeams(Long userId, int limit);

    /**
     * 按标签推荐队伍
     * 根据用户标签查找包含相似标签的队伍
     *
     * @param tags  用户标签集合
     * @param limit 返回数量限制
     * @return 推荐队伍列表
     */
    List<TeamVo> getTeamsByTags(Set<String> tags, int limit);

    /**
     * 获取热门队伍
     * 根据队伍成员数、创建时间等因素排序
     *
     * @param limit 返回数量限制
     * @return 热门队伍列表
     */
    List<TeamVo> getHotTeams(int limit);

    /**
     * 获取最新队伍
     * 按创建时间倒序
     *
     * @param limit 返回数量限制
     * @return 最新队伍列表
     */
    List<TeamVo> getNewTeams(int limit);
}
