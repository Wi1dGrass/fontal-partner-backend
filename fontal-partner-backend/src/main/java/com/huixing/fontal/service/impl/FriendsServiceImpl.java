package com.huixing.fontal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huixing.fontal.common.ErrorCode;
import com.huixing.fontal.contant.FriendConstant;
import com.huixing.fontal.exception.BusinessException;
import com.huixing.fontal.mapper.ChatMapper;
import com.huixing.fontal.mapper.FriendsMapper;
import com.huixing.fontal.mapper.UserMapper;
import com.huixing.fontal.model.entity.Chat;
import com.huixing.fontal.model.entity.Friends;
import com.huixing.fontal.model.entity.User;
import com.huixing.fontal.model.request.FriendApplyRequest;
import com.huixing.fontal.model.request.FriendHandleRequest;
import com.huixing.fontal.model.request.FriendMarkReadRequest;
import com.huixing.fontal.model.vo.FriendListVO;
import com.huixing.fontal.model.vo.FriendVO;
import com.huixing.fontal.model.vo.FriendsRecordVO;
import com.huixing.fontal.service.FriendsService;
import com.huixing.fontal.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

import static com.huixing.fontal.contant.UserConstant.LOGIN_USER_STATUS;

/**
 * 好友服务实现类
 */
@Service
@Slf4j
public class FriendsServiceImpl extends ServiceImpl<FriendsMapper, Friends> implements FriendsService {

    @Resource
    private FriendsMapper friendsMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private ChatMapper chatMapper;

    /**
     * 从HttpServletRequest获取登录用户ID
     *
     * @param request HTTP请求
     * @return 用户ID
     */
    private Long getLoginUserId(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(LOGIN_USER_STATUS);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        return ((User) userObj).getId();
    }

    /**
     * 发送好友申请
     *
     * @param friendApplyRequest 好友申请请求
     * @param request            HTTP请求
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean applyFriend(FriendApplyRequest friendApplyRequest, HttpServletRequest request) {
        Long loginUserId = getLoginUserId(request);
        Long receiveId = friendApplyRequest.getReceiveId();
        String remark = friendApplyRequest.getRemark();

        // 1. 校验参数
        if (receiveId == null || receiveId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接收用户ID不能为空");
        }
        if (receiveId.equals(loginUserId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能添加自己为好友");
        }

        // 2. 检查接收用户是否存在
        User receiveUser = userMapper.selectById(receiveId);
        if (receiveUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        // 3. 检查是否已经是好友
        User loginUser = userMapper.selectById(loginUserId);
        Set<Long> loginUserFriendIds = StringUtils.stringJsonListToLongSet(loginUser.getUserIds());
        if (loginUserFriendIds.contains(receiveId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "已经是好友关系");
        }

        // 4. 检查是否已经发送过申请且未处理
        QueryWrapper<Friends> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("fromId", loginUserId)
                .eq("receiveId", receiveId)
                .in("status", FriendConstant.DEFAULT_STATUS, FriendConstant.AGREE_STATUS);
        Friends existFriends = friendsMapper.selectOne(queryWrapper);
        if (existFriends != null) {
            if (existFriends.getStatus().equals(FriendConstant.DEFAULT_STATUS)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "已经发送过好友申请，请等待对方处理");
            }
            if (existFriends.getStatus().equals(FriendConstant.AGREE_STATUS)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "已经是好友关系");
            }
        }

        // 5. 创建好友申请记录
        Friends friends = new Friends();
        friends.setFromId(loginUserId);
        friends.setReceiveId(receiveId);
        friends.setStatus(FriendConstant.DEFAULT_STATUS);
        friends.setIsRead(FriendConstant.NOT_READ);
        friends.setRemark(remark);
        boolean saveResult = this.save(friends);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "发送好友申请失败");
        }

        log.info("用户{}向用户{}发送好友申请，申请ID：{}", loginUserId, receiveId, friends.getId());
        return true;
    }

    /**
     * 处理好友申请（同意/拒绝）
     *
     * @param friendHandleRequest 处理请求
     * @param request             HTTP请求
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean handleFriend(FriendHandleRequest friendHandleRequest, HttpServletRequest request) {
        Long loginUserId = getLoginUserId(request);
        Long id = friendHandleRequest.getId();
        Integer status = friendHandleRequest.getStatus();

        // 1. 校验参数
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "申请ID不能为空");
        }
        if (status == null || (!status.equals(FriendConstant.AGREE_STATUS)
                && !status.equals(FriendConstant.EXPIRED_STATUS))) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "状态参数错误");
        }

        // 2. 查询好友申请记录
        Friends friends = friendsMapper.selectById(id);
        if (friends == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "好友申请记录不存在");
        }

        // 3. 校验权限：只有接收人可以处理申请
        if (!friends.getReceiveId().equals(loginUserId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权处理此申请");
        }

        // 4. 校验申请状态
        if (!friends.getStatus().equals(FriendConstant.DEFAULT_STATUS)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该申请已被处理");
        }

        // 5. 更新申请状态
        friends.setStatus(status);
        friends.setIsRead(FriendConstant.READ);
        boolean updateResult = this.updateById(friends);
        if (!updateResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "处理好友申请失败");
        }

        // 6. 如果同意，更新双方的好友列表
        if (status.equals(FriendConstant.AGREE_STATUS)) {
            // 更新发送人的好友列表
            User fromUser = userMapper.selectById(friends.getFromId());
            Set<Long> fromUserFriendIds = StringUtils.stringJsonListToLongSet(fromUser.getUserIds());
            fromUserFriendIds.add(friends.getReceiveId());
            fromUser.setUserIds(StringUtils.longSetToStringJsonList(fromUserFriendIds));
            userMapper.updateById(fromUser);

            // 更新接收人的好友列表
            User receiveUser = userMapper.selectById(friends.getReceiveId());
            Set<Long> receiveUserFriendIds = StringUtils.stringJsonListToLongSet(receiveUser.getUserIds());
            receiveUserFriendIds.add(friends.getFromId());
            receiveUser.setUserIds(StringUtils.longSetToStringJsonList(receiveUserFriendIds));
            userMapper.updateById(receiveUser);

            log.info("用户{}同意了用户{}的好友申请", loginUserId, friends.getFromId());
        } else {
            log.info("用户{}拒绝了用户{}的好友申请", loginUserId, friends.getFromId());
        }

        return true;
    }

    /**
     * 获取收到的好友申请列表
     *
     * @param request HTTP请求
     * @return 好友申请列表
     */
    @Override
    public List<FriendsRecordVO> getReceivedFriendRequests(HttpServletRequest request) {
        Long loginUserId = getLoginUserId(request);

        // 查询收到的好友申请（未通过的）
        QueryWrapper<Friends> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("receiveId", loginUserId)
                .eq("status", FriendConstant.DEFAULT_STATUS)
                .orderByDesc("createTime");
        List<Friends> friendsList = friendsMapper.selectList(queryWrapper);

        // 转换为VO
        List<FriendsRecordVO> voList = new ArrayList<>();
        for (Friends friends : friendsList) {
            FriendsRecordVO vo = new FriendsRecordVO();
            BeanUtils.copyProperties(friends, vo);

            // 查询申请人信息
            User fromUser = userMapper.selectById(friends.getFromId());
            if (fromUser != null) {
                vo.setApplyUser(fromUser);
            }

            voList.add(vo);
        }

        return voList;
    }

    /**
     * 获取发送的好友申请列表
     *
     * @param request HTTP请求
     * @return 好友申请列表
     */
    @Override
    public List<FriendsRecordVO> getSentFriendRequests(HttpServletRequest request) {
        Long loginUserId = getLoginUserId(request);

        // 查询发送的好友申请
        QueryWrapper<Friends> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("fromId", loginUserId)
                .in("status", FriendConstant.DEFAULT_STATUS,
                        FriendConstant.AGREE_STATUS,
                        FriendConstant.EXPIRED_STATUS,
                        FriendConstant.REVOKE_STATUS)
                .orderByDesc("createTime");
        List<Friends> friendsList = friendsMapper.selectList(queryWrapper);

        // 转换为VO
        List<FriendsRecordVO> voList = new ArrayList<>();
        for (Friends friends : friendsList) {
            FriendsRecordVO vo = new FriendsRecordVO();
            BeanUtils.copyProperties(friends, vo);

            // 查询接收人信息
            User receiveUser = userMapper.selectById(friends.getReceiveId());
            if (receiveUser != null) {
                vo.setApplyUser(receiveUser);
            }

            voList.add(vo);
        }

        return voList;
    }

    /**
     * 获取好友列表
     *
     * @param request HTTP请求
     * @return 好友列表
     */
    @Override
    public FriendListVO getFriendList(HttpServletRequest request) {
        Long loginUserId = getLoginUserId(request);

        // 查询用户的好友ID列表
        User loginUser = userMapper.selectById(loginUserId);
        Set<Long> friendIds = StringUtils.stringJsonListToLongSet(loginUser.getUserIds());

        if (friendIds == null || friendIds.isEmpty()) {
            FriendListVO result = new FriendListVO();
            result.setFriends(new ArrayList<>());
            result.setTotal(0);
            return result;
        }

        // 批量查询好友信息
        List<User> friendList = userMapper.selectBatchIds(friendIds);

        // 移除已删除的用户
        friendList = friendList.stream()
                .filter(user -> user.getIsDelete() == 0)
                .collect(Collectors.toList());

        // 转换为FriendVO并获取最后一条消息
        List<FriendVO> friendVOList = new ArrayList<>();
        for (User friend : friendList) {
            FriendVO friendVO = new FriendVO();
            friendVO.setId(friend.getId());
            friendVO.setName(friend.getUsername());
            friendVO.setAvatar(friend.getUserAvatarUrl());
            friendVO.setUserAccount(friend.getUserAccount());
            friendVO.setUserDesc(friend.getUserDesc());
            friendVO.setTags(friend.getTags());

            // 查询最后一条私聊消息
            QueryWrapper<Chat> chatQueryWrapper = new QueryWrapper<>();
            chatQueryWrapper.and(wrapper -> wrapper
                    .eq("fromId", loginUserId).eq("toId", friend.getId())
                    .or()
                    .eq("fromId", friend.getId()).eq("toId", loginUserId))
                    .eq("chatType", 1) // 私聊
                    .orderByDesc("createTime")
                    .last("LIMIT 1");

            Chat lastChat = chatMapper.selectOne(chatQueryWrapper);
            if (lastChat != null) {
                friendVO.setLastMessage(lastChat.getText());
                friendVO.setLastMessageTime(formatMessageTime(lastChat.getCreateTime()));
            } else {
                friendVO.setLastMessage("");
                friendVO.setLastMessageTime("");
            }

            friendVOList.add(friendVO);
        }

        // 封装返回结果
        FriendListVO result = new FriendListVO();
        result.setFriends(friendVOList);
        result.setTotal(friendVOList.size());

        return result;
    }

    /**
     * 格式化消息时间
     *
     * @param time 消息时间
     * @return 格式化后的时间字符串
     */
    private String formatMessageTime(Date time) {
        if (time == null) {
            return "";
        }

        Calendar now = Calendar.getInstance();
        Calendar msgTime = Calendar.getInstance();
        msgTime.setTime(time);

        SimpleDateFormat sdf;

        // 判断是否是今天
        if (now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR)
                && now.get(Calendar.DAY_OF_YEAR) == msgTime.get(Calendar.DAY_OF_YEAR)) {
            // 今天 - 显示 HH:mm
            sdf = new SimpleDateFormat("HH:mm");
            return sdf.format(time);
        } else if (now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR)
                && now.get(Calendar.DAY_OF_YEAR) - 1 == msgTime.get(Calendar.DAY_OF_YEAR)) {
            // 昨天 - 显示 "昨天"
            return "昨天";
        } else {
            // 更早 - 显示 MM-DD HH:mm
            sdf = new SimpleDateFormat("MM-dd HH:mm");
            return sdf.format(time);
        }
    }

    /**
     * 删除好友
     *
     * @param friendId 好友用户ID
     * @param request  HTTP请求
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteFriend(Long friendId, HttpServletRequest request) {
        Long loginUserId = getLoginUserId(request);

        // 1. 校验参数
        if (friendId == null || friendId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "好友ID不能为空");
        }

        // 2. 检查好友是否存在
        User friendUser = userMapper.selectById(friendId);
        if (friendUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        // 3. 检查是否是好友关系
        User loginUser = userMapper.selectById(loginUserId);
        Set<Long> loginUserFriendIds = StringUtils.stringJsonListToLongSet(loginUser.getUserIds());
        if (!loginUserFriendIds.contains(friendId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该用户不是你的好友");
        }

        // 4. 从当前用户的好友列表中移除
        loginUserFriendIds.remove(friendId);
        loginUser.setUserIds(StringUtils.longSetToStringJsonList(loginUserFriendIds));
        userMapper.updateById(loginUser);

        // 5. 从好友的好友列表中移除
        Set<Long> friendUserFriendIds = StringUtils.stringJsonListToLongSet(friendUser.getUserIds());
        friendUserFriendIds.remove(loginUserId);
        friendUser.setUserIds(StringUtils.longSetToStringJsonList(friendUserFriendIds));
        userMapper.updateById(friendUser);

        // 6. 删除或更新好友申请记录
        QueryWrapper<Friends> queryWrapper = new QueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper
                .eq("fromId", loginUserId).eq("receiveId", friendId)
                .or()
                .eq("fromId", friendId).eq("receiveId", loginUserId));
        List<Friends> friendsList = friendsMapper.selectList(queryWrapper);
        for (Friends friends : friendsList) {
            friendsMapper.deleteById(friends.getId());
        }

        log.info("用户{}删除了好友{}", loginUserId, friendId);
        return true;
    }

    /**
     * 撤销好友申请
     *
     * @param applicationId 申请ID
     * @param request       HTTP请求
     * @return 是否成功
     */
    @Override
    public Boolean revokeFriendApplication(Long applicationId, HttpServletRequest request) {
        Long loginUserId = getLoginUserId(request);

        // 1. 校验参数
        if (applicationId == null || applicationId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "申请ID不能为空");
        }

        // 2. 查询好友申请记录
        Friends friends = friendsMapper.selectById(applicationId);
        if (friends == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "好友申请记录不存在");
        }

        // 3. 校验权限：只有申请人可以撤销
        if (!friends.getFromId().equals(loginUserId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权撤销此申请");
        }

        // 4. 校验申请状态
        if (!friends.getStatus().equals(FriendConstant.DEFAULT_STATUS)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "只能撤销待处理的申请");
        }

        // 5. 更新申请状态为已撤销
        friends.setStatus(FriendConstant.REVOKE_STATUS);
        boolean updateResult = this.updateById(friends);
        if (!updateResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "撤销好友申请失败");
        }

        log.info("用户{}撤销了好友申请，申请ID：{}", loginUserId, applicationId);
        return true;
    }

    /**
     * 标记好友申请已读
     *
     * @param friendMarkReadRequest 标记已读请求
     * @param request               HTTP请求
     * @return 是否成功
     */
    @Override
    public Boolean markFriendRequestAsRead(FriendMarkReadRequest friendMarkReadRequest, HttpServletRequest request) {
        Long loginUserId = getLoginUserId(request);
        Long friendId = friendMarkReadRequest.getFriendId();

        // 如果friendId不为空，标记指定的申请为已读
        if (friendId != null && friendId > 0) {
            // 1. 校验参数
            if (friendId <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "申请ID不能为空");
            }

            // 2. 查询好友申请记录
            Friends friends = friendsMapper.selectById(friendId);
            if (friends == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "好友申请记录不存在");
            }

            // 3. 校验权限：只有接收人可以标记为已读
            if (!friends.getReceiveId().equals(loginUserId)) {
                throw new BusinessException(ErrorCode.NO_AUTH, "无权标记此申请");
            }

            // 4. 更新为已读
            friends.setIsRead(FriendConstant.READ);
            boolean updateResult = this.updateById(friends);
            if (!updateResult) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "标记已读失败");
            }

            log.info("用户{}标记好友申请{}为已读", loginUserId, friendId);
        } else {
            // 标记所有未读申请为已读
            // 1. 查询当前用户收到的所有未读申请
            QueryWrapper<Friends> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("receiveId", loginUserId)
                    .eq("isRead", FriendConstant.NOT_READ);
            List<Friends> unreadRequests = friendsMapper.selectList(queryWrapper);

            if (unreadRequests == null || unreadRequests.isEmpty()) {
                log.info("用户{}没有未读的好友申请", loginUserId);
                return true;
            }

            // 2. 批量更新为已读
            for (Friends friends : unreadRequests) {
                friends.setIsRead(FriendConstant.READ);
            }
            boolean updateResult = this.updateBatchById(unreadRequests);
            if (!updateResult) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "批量标记已读失败");
            }

            log.info("用户{}批量标记{}条好友申请为已读", loginUserId, unreadRequests.size());
        }

        return true;
    }
}
