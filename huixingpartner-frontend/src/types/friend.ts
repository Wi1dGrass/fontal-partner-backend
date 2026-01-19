/**
 * 好友信息（匹配后端返回结构）
 */
export interface Friend {
  id: number                    // 好友用户ID
  name: string                  // 好友昵称
  avatar: string                // 头像URL
  userAccount?: string          // 账号
  userDesc?: string             // 个人简介
  tags?: string                 // 标签（JSON字符串数组）
  lastMessage?: string          // 最后一条消息（可选）
  lastMessageTime?: string      // 最后消息时间（可选）
}

/**
 * 好友信息VO（后端返回）
 */
export interface FriendVO {
  avatar: string                // 头像URL
  id: number                    // 好友用户ID
  lastMessage?: string          // 最后一条消息内容
  lastMessageTime?: string      // 最后消息时间（格式：HH:mm 或 MM-DD HH:mm）
  name: string                  // 好友昵称
  tags?: string                 // 标签（JSON字符串数组）
  userAccount: string           // 账号
  userDesc?: string             // 个人简介
}

/**
 * 好友列表返回VO（后端返回）
 */
export interface FriendListData {
  friends: FriendVO[]           // 好友列表
  total: number                 // 好友总数
}

/**
 * 好友列表响应（后端实际返回）
 */
export interface FriendListResponse {
  code: number
  data: FriendListData
  description?: string
  message?: string
}

/**
 * 好友申请中的用户信息
 */
export interface FriendApplicationUser {
  id: number
  username: string
  userAccount: string
  userAvatarUrl?: string         // 头像URL（可能为null）
}

/**
 * 好友申请记录（后端返回）
 */
export interface FriendApplication {
  id: number                    // 申请记录ID
  fromUserId: number            // 申请人ID
  toUserId: number              // 被申请人ID
  remark?: string               // 申请备注/验证消息
  status: number                // 状态：0-待处理，1-已接受，2-已拒绝，3-已撤销
  createTime: string            // 申请时间
  applyUser: FriendApplicationUser // 申请人信息（注意：后端返回的是applyUser不是fromUser）
}

/**
 * 好友申请列表响应
 */
export interface FriendApplicationListResponse {
  code: number
  data: FriendApplication[]
  description?: string
  message?: string
}

/**
 * 处理好友申请请求
 */
export interface HandleFriendRequest {
  id: number                    // 申请记录ID
  status: number                // 状态：1-接受，2-拒绝
}

/**
 * 标记已读请求
 */
export interface MarkReadRequest {
  friendId?: number             // 申请ID，为空则标记所有未读申请
}

/**
 * 基础响应结构
 */
export interface BaseResponse<T> {
  code: number
  data: T
  description?: string
  message?: string
}
