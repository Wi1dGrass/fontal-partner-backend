/**
 * 消息分类
 */
export interface MessageCategory {
  id: string
  name: string
  icon: string
  count: number        // 未读数量
  items: MessageItem[] // 消息列表
  loading: boolean     // 加载状态
  error?: string       // 错误信息
  loaded: boolean      // 是否已加载过（用于缓存）
}

/**
 * 消息项（联合类型）
 */
export type MessageItem = FriendApplicationReceived | FriendApplicationSent | TeamApplicationSent | TeamApplicationReceived

/**
 * 好友申请（收到的）
 */
export interface FriendApplicationReceived {
  id: number
  applyUser: {
    id: number
    username: string
    userAccount: string
    userAvatarUrl?: string
  }
  remark: string
  status: number  // 0-待处理，1-已接受，2-已拒绝
  createTime: string
}

/**
 * 好友申请（发送的）
 */
export interface FriendApplicationSent {
  id: number
  applyUser: {
    id: number
    username: string
    userAccount: string
    userAvatarUrl?: string
  }
  remark: string
  status: number  // 0-待处理，1-已接受，2-已拒绝，3-已撤销
  createTime: string
}

/**
 * 队伍申请（我发出的）
 */
export interface TeamApplicationSent {
  id: number
  teamId: number
  teamName: string
  teamAvatar?: string
  teamCaptain?: string
  applyMessage: string
  status: number  // 0-待处理，1-已同意，2-已拒绝，3-已取消
  createTime: string
}

/**
 * 队伍审批（我收到的）
 */
export interface TeamApplicationReceived {
  id: number
  teamId: number
  teamName: string
  userId: number
  userName: string
  userAvatar?: string
  applyMessage: string
  status: number  // 0-待处理，1-已同意，2-已拒绝
  createTime: string
}
