/**
 * 聊天类型
 */
export type ChatType = 'user' | 'team'

/**
 * 消息类型
 */
export type MessageType = 'text' | 'image' | 'voice'

/**
 * 消息发送状态
 */
export type MessageStatus = 'sending' | 'success' | 'failed'

/**
 * 聊天信息
 */
export interface ChatInfo {
  type: ChatType           // 聊天类型：user=好友聊天，team=队伍聊天
  id: number               // 用户ID或队伍ID
  name: string             // 聊天对象名称
  avatar: string           // 聊天对象头像
  memberCount?: number     // 队伍聊天时的成员数量
}

/**
 * 消息结构
 */
export interface Message {
  id: number               // 消息ID
  chatId: string           // 聊天ID（user_xxx 或 team_xxx）
  senderId: number         // 发送者ID
  senderName: string       // 发送者昵称
  senderAvatar: string     // 发送者头像
  senderRole?: number      // 发送者角色（队伍聊天时：0=普通用户，1=队长）
  content: string          // 消息内容
  messageType: MessageType // 消息类型
  isSelf: boolean          // 是否是自己发的
  timestamp: number        // 时间戳
  status?: MessageStatus   // 发送状态（仅自己发的消息）
}

/**
 * 发送消息请求
 */
export interface SendMessageRequest {
  chatId: string           // 聊天ID
  content: string          // 消息内容
  messageType: MessageType // 消息类型
}

/**
 * 发送消息响应
 */
export interface SendMessageResponse {
  messageId: number        // 消息ID
}
