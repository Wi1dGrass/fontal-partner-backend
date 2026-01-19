package com.huixing.fontal.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.huixing.fontal.model.entity.Friends;
import com.huixing.fontal.model.request.FriendApplyRequest;
import com.huixing.fontal.model.request.FriendHandleRequest;
import com.huixing.fontal.model.request.FriendMarkReadRequest;
import com.huixing.fontal.model.vo.FriendListVO;
import com.huixing.fontal.model.vo.FriendsRecordVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 好友服务接口
 *
 * @author fontal
 */
public interface FriendsService extends IService<Friends> {

    /**
     * 发送好友申请
     *
     * @param friendApplyRequest 好友申请请求
     * @param request            HTTP请求
     * @return 是否成功
     */
    Boolean applyFriend(FriendApplyRequest friendApplyRequest, HttpServletRequest request);

    /**
     * 处理好友申请（同意/拒绝）
     *
     * @param friendHandleRequest 处理请求
     * @param request             HTTP请求
     * @return 是否成功
     */
    Boolean handleFriend(FriendHandleRequest friendHandleRequest, HttpServletRequest request);

    /**
     * 获取收到的好友申请列表
     *
     * @param request HTTP请求
     * @return 好友申请列表
     */
    List<FriendsRecordVO> getReceivedFriendRequests(HttpServletRequest request);

    /**
     * 获取发送的好友申请列表
     *
     * @param request HTTP请求
     * @return 好友申请列表
     */
    List<FriendsRecordVO> getSentFriendRequests(HttpServletRequest request);

    /**
     * 获取好友列表
     *
     * @param request HTTP请求
     * @return 好友列表
     */
    FriendListVO getFriendList(HttpServletRequest request);

    /**
     * 删除好友
     *
     * @param friendId 好友用户ID
     * @param request  HTTP请求
     * @return 是否成功
     */
    Boolean deleteFriend(Long friendId, HttpServletRequest request);

    /**
     * 撤销好友申请
     *
     * @param applicationId 申请ID
     * @param request       HTTP请求
     * @return 是否成功
     */
    Boolean revokeFriendApplication(Long applicationId, HttpServletRequest request);

    /**
     * 标记好友申请为已读
     *
     * @param friendMarkReadRequest 标记已读请求
     * @param request               HTTP请求
     * @return 是否成功
     */
    Boolean markFriendRequestAsRead(FriendMarkReadRequest friendMarkReadRequest, HttpServletRequest request);
}
