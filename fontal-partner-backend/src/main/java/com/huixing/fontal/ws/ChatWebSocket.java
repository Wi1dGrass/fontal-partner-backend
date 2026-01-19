package com.huixing.fontal.ws;

import com.google.gson.Gson;
import com.huixing.fontal.contant.ChatConstant;
import com.huixing.fontal.mapper.ChatMapper;
import com.huixing.fontal.mapper.UserMapper;
import com.huixing.fontal.model.entity.Chat;
import com.huixing.fontal.model.entity.User;
import com.huixing.fontal.model.request.MessageRequest;
import com.huixing.fontal.model.vo.MessageVo;
import com.huixing.fontal.model.vo.WebSocketVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket聊天服务
 *
 * @author fontal
 */
@Slf4j
@Component
@ServerEndpoint("/ws/chat/{userId}")
public class ChatWebSocket {

    /**
     * 静态变量，用于记录当前在线连接数
     */
    private static int onlineCount = 0;

    /**
     * concurrent包的线程安全Set，用来存放每个客户端对应的WebSocket对象
     */
    private static final ConcurrentHashMap<Long, ChatWebSocket> webSocketMap = new ConcurrentHashMap<>();
    
    /**
     * Gson实例用于JSON序列化
     */
    private static final Gson gson = new Gson();

    /**
     * 与某个客户端的连接会话，需要通过它来给客户端发送数据
     */
    private Session session;

    /**
     * 接收userId
     */
    private Long userId;

    /**
     * ChatMapper注入（静态方法需要）
     */
    private static ChatMapper chatMapper;

    /**
     * UserMapper注入（静态方法需要）
     */
    private static UserMapper userMapper;

    /**
     * 注入ChatMapper
     */
    @Resource
    public void setChatMapper(ChatMapper chatMapper) {
        ChatWebSocket.chatMapper = chatMapper;
    }

    /**
     * 注入UserMapper
     */
    @Resource
    public void setUserMapper(UserMapper userMapper) {
        ChatWebSocket.userMapper = userMapper;
    }

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Long userId) {
        this.session = session;
        this.userId = userId;
        if (webSocketMap.containsKey(userId)) {
            webSocketMap.remove(userId);
            webSocketMap.put(userId, this);
        } else {
            webSocketMap.put(userId, this);
            addOnlineCount();
        }
        log.info("用户连接：" + userId + "，当前在线人数为：" + getOnlineCount());
        try {
            sendMessage("连接成功");
        } catch (IOException e) {
            log.error("用户：" + userId + "，网络异常！！！！");
        }
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        if (webSocketMap.containsKey(userId)) {
            webSocketMap.remove(userId);
            subOnlineCount();
        }
        log.info("用户退出：" + userId + "，当前在线人数为：" + getOnlineCount());
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("用户消息：" + userId + "，报文：" + message);
        // 可以在此做消息校验和过滤
        if (message == null || message.trim().isEmpty()) {
            return;
        }

        // 解析消息
        MessageRequest messageRequest = gson.fromJson(message, MessageRequest.class);
        if (messageRequest == null) {
            return;
        }

        Integer chatType = messageRequest.getChatType();
        if (chatType == null) {
            return;
        }

        // 根据聊天类型处理消息
        switch (chatType) {
            case ChatConstant.PRIVATE_CHAT:
                handlePrivateChat(messageRequest);
                break;
            case ChatConstant.TEAM_CHAT:
                handleTeamChat(messageRequest);
                break;
            case ChatConstant.HALL_CHAT:
                handleHallChat(messageRequest);
                break;
            default:
                log.warn("未知的聊天类型：" + chatType);
        }
    }

    /**
     * 发送错误
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("用户错误：" + this.userId + "，原因：" + error.getMessage());
    }

    /**
     * 发送消息
     *
     * @param message 消息内容
     * @throws IOException 发送异常
     */
    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }

    /**
     * 发送消息给指定用户
     *
     * @param message 消息内容
     * @param userId  接收用户ID
     */
    public static void sendMessage(String message, Long userId) {
        try {
            ChatWebSocket chatWebSocket = webSocketMap.get(userId);
            if (chatWebSocket != null) {
                chatWebSocket.sendMessage(message);
            } else {
                log.warn("用户：" + userId + "，不在线");
            }
        } catch (IOException e) {
            log.error("发送消息给用户：" + userId + "失败", e);
        }
    }

    /**
     * 处理私聊消息
     *
     * @param messageRequest 消息请求
     */
    private void handlePrivateChat(MessageRequest messageRequest) {
        Long toId = messageRequest.getToId();
        String text = messageRequest.getText();

        if (toId == null || text == null) {
            return;
        }

        // 保存聊天记录到数据库
        Chat chat = new Chat();
        chat.setFromId(userId);
        chat.setToId(toId);
        chat.setText(text);
        chat.setChatType(ChatConstant.PRIVATE_CHAT);
        chatMapper.insert(chat);

        // 构建消息VO
        MessageVo messageVo = buildMessageVo(chat);
        String messageJson = gson.toJson(messageVo);

        // 发送消息给接收方
        sendMessage(messageJson, toId);

        // 发送消息回发送方（确认消息已发送）
        sendMessage(messageJson, userId);
    }

    /**
     * 处理队伍群聊消息
     *
     * @param messageRequest 消息请求
     */
    private void handleTeamChat(MessageRequest messageRequest) {
        Long teamId = messageRequest.getTeamId();
        String text = messageRequest.getText();

        if (teamId == null || text == null) {
            return;
        }

        // 保存聊天记录到数据库
        Chat chat = new Chat();
        chat.setFromId(userId);
        chat.setTeamId(teamId);
        chat.setText(text);
        chat.setChatType(ChatConstant.TEAM_CHAT);
        chatMapper.insert(chat);

        // 构建消息VO
        MessageVo messageVo = buildMessageVo(chat);
        String messageJson = gson.toJson(messageVo);

        // 发送消息给队伍内所有成员
        // TODO: 需要查询队伍成员列表，并发送给所有在线成员
        // 这里简化处理，发送给所有在线用户（实际应该只发送给队伍成员）
        sendToAll(messageJson);
    }

    /**
     * 处理大厅聊天消息
     *
     * @param messageRequest 消息请求
     */
    private void handleHallChat(MessageRequest messageRequest) {
        String text = messageRequest.getText();

        if (text == null) {
            return;
        }

        // 保存聊天记录到数据库
        Chat chat = new Chat();
        chat.setFromId(userId);
        chat.setText(text);
        chat.setChatType(ChatConstant.HALL_CHAT);
        chatMapper.insert(chat);

        // 构建消息VO
        MessageVo messageVo = buildMessageVo(chat);
        String messageJson = gson.toJson(messageVo);

        // 发送消息给所有在线用户
        sendToAll(messageJson);
    }

    /**
     * 发送消息给所有在线用户
     *
     * @param message 消息内容
     */
    private void sendToAll(String message) {
        for (Map.Entry<Long, ChatWebSocket> entry : webSocketMap.entrySet()) {
            ChatWebSocket chatWebSocket = entry.getValue();
            try {
                chatWebSocket.sendMessage(message);
            } catch (IOException e) {
                log.error("发送消息给用户：" + entry.getKey() + "失败", e);
            }
        }
    }

    /**
     * 构建消息VO
     *
     * @param chat 聊天记录
     * @return 消息VO
     */
    private MessageVo buildMessageVo(Chat chat) {
        MessageVo messageVo = new MessageVo();
        messageVo.setTeamId(chat.getTeamId());
        messageVo.setText(chat.getText());
        messageVo.setChatType(chat.getChatType());
        messageVo.setIsMy(false);
        messageVo.setIsAdmin(false);
        
        // 将Date类型的createTime转换为String
        if (chat.getCreateTime() != null) {
            messageVo.setCreateTime(chat.getCreateTime().toString());
        }

        // 设置发送人信息（注意：MessageVo中的字段名是formUser，不是fromUser）
        User fromUser = userMapper.selectById(chat.getFromId());
        if (fromUser != null) {
            WebSocketVo webSocketVo = new WebSocketVo();
            webSocketVo.setId(fromUser.getId());
            webSocketVo.setUsername(fromUser.getUsername());
            webSocketVo.setUserAccount(fromUser.getUserAccount());
            webSocketVo.setUserAvatarUrl(fromUser.getUserAvatarUrl());
            messageVo.setFormUser(webSocketVo);
        }

        return messageVo;
    }

    /**
     * 获取在线人数
     *
     * @return 在线人数
     */
    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    /**
     * 增加在线人数
     */
    public static synchronized void addOnlineCount() {
        ChatWebSocket.onlineCount++;
    }

    /**
     * 减少在线人数
     */
    public static synchronized void subOnlineCount() {
        ChatWebSocket.onlineCount--;
    }
}
