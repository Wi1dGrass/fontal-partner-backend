package com.huixing.fontal.controller;

import com.huixing.fontal.common.BaseResponse;
import com.huixing.fontal.common.ResultUtil;
import com.huixing.fontal.model.request.FriendApplyRequest;
import com.huixing.fontal.model.request.FriendHandleRequest;
import com.huixing.fontal.model.request.FriendMarkReadRequest;
import com.huixing.fontal.model.vo.FriendListVO;
import com.huixing.fontal.model.vo.FriendsRecordVO;
import com.huixing.fontal.service.FriendsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 好友接口
 *
 * @author fontal
 */
@Api(tags = "好友管理")
@RestController
@RequestMapping("/friend")
public class FriendsController {

    @Resource
    private FriendsService friendsService;

    /**
     * 发送好友申请
     *
     * @param friendApplyRequest 好友申请请求
     * @param request            HTTP请求
     * @return 申请ID
     */
    @ApiOperation("发送好友申请")
    @PostMapping("/apply")
    public BaseResponse<Boolean> applyFriend(@RequestBody FriendApplyRequest friendApplyRequest,
                                               HttpServletRequest request) {
        Boolean result = friendsService.applyFriend(friendApplyRequest, request);
        return ResultUtil.success(result);
    }

    /**
     * 处理好友申请（同意/拒绝）
     *
     * @param friendHandleRequest 处理请求
     * @param request             HTTP请求
     * @return 是否成功
     */
    @ApiOperation("处理好友申请")
    @PostMapping("/handle")
    public BaseResponse<Boolean> handleFriend(@RequestBody FriendHandleRequest friendHandleRequest,
                                               HttpServletRequest request) {
        Boolean result = friendsService.handleFriend(friendHandleRequest, request);
        return ResultUtil.success(result);
    }

    /**
     * 获取收到的好友申请列表
     *
     * @param request HTTP请求
     * @return 好友申请列表
     */
    @ApiOperation("获取收到的好友申请列表")
    @GetMapping("/received")
    public BaseResponse<List<FriendsRecordVO>> getReceivedFriendRequests(HttpServletRequest request) {
        List<FriendsRecordVO> result = friendsService.getReceivedFriendRequests(request);
        return ResultUtil.success(result);
    }

    /**
     * 获取发送的好友申请列表
     *
     * @param request HTTP请求
     * @return 好友申请列表
     */
    @ApiOperation("获取发送的好友申请列表")
    @GetMapping("/sent")
    public BaseResponse<List<FriendsRecordVO>> getSentFriendRequests(HttpServletRequest request) {
        List<FriendsRecordVO> result = friendsService.getSentFriendRequests(request);
        return ResultUtil.success(result);
    }

    /**
     * 获取好友列表
     *
     * @param request HTTP请求
     * @return 好友列表
     */
    @ApiOperation("获取好友列表")
    @GetMapping("/list")
    public BaseResponse<FriendListVO> getFriendList(HttpServletRequest request) {
        FriendListVO result = friendsService.getFriendList(request);
        return ResultUtil.success(result);
    }

    /**
     * 删除好友
     *
     * @param friendId 好友用户ID
     * @param request  HTTP请求
     * @return 是否成功
     */
    @ApiOperation("删除好友")
    @DeleteMapping("/delete/{friendId}")
    public BaseResponse<Boolean> deleteFriend(@PathVariable("friendId") Long friendId,
                                                HttpServletRequest request) {
        Boolean result = friendsService.deleteFriend(friendId, request);
        return ResultUtil.success(result);
    }

    /**
     * 撤销好友申请
     *
     * @param applicationId 申请ID
     * @param request       HTTP请求
     * @return 是否成功
     */
    @ApiOperation("撤销好友申请")
    @DeleteMapping("/revoke/{applicationId}")
    public BaseResponse<Boolean> revokeFriendApplication(@PathVariable("applicationId") Long applicationId,
                                                          HttpServletRequest request) {
        Boolean result = friendsService.revokeFriendApplication(applicationId, request);
        return ResultUtil.success(result);
    }

    /**
     * 标记好友申请已读
     *
     * @param friendMarkReadRequest 标记已读请求
     * @param request               HTTP请求
     * @return 是否成功
     */
    @ApiOperation("标记好友申请已读")
    @PostMapping("/mark/read")
    public BaseResponse<Boolean> markFriendRequestAsRead(@RequestBody FriendMarkReadRequest friendMarkReadRequest,
                                                          HttpServletRequest request) {
        Boolean result = friendsService.markFriendRequestAsRead(friendMarkReadRequest, request);
        return ResultUtil.success(result);
    }
}
