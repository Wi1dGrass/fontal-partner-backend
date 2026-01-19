import request from '@/utils/request'
import type {
  Friend,
  FriendListResponse,
  FriendApplicationListResponse,
  FriendApplication,
  HandleFriendRequest,
  MarkReadRequest,
  BaseResponse
} from '@/types/friend'

/**
 * 获取好友列表（后端真实接口）
 */
export const getFriendList = () => {
  return request.get<FriendListResponse>('/friend/list')
}

/**
 * 搜索好友
 */
export const searchFriends = (keyword: string) => {
  return request.get<BaseResponse<Friend[]>>(`/friend/search?keyword=${keyword}`)
}

/**
 * 删除好友
 */
export const deleteFriend = (friendId: number) => {
  return request.delete<BaseResponse<string>>(`/friend/${friendId}`)
}

/**
 * 申请添加好友
 */
export interface AddFriendRequest {
  toUserId: number
  remark?: string
}

export const addFriend = (data: AddFriendRequest) => {
  return request.post<BaseResponse<number>>('/friend/apply', data)
}

/**
 * 获取收到的好友申请列表
 */
export const getReceivedApplications = () => {
  return request.get<FriendApplicationListResponse>('/friend/received')
}

/**
 * 获取发送的好友申请列表
 */
export const getSentApplications = () => {
  return request.get<FriendApplicationListResponse>('/friend/sent')
}

/**
 * 处理好友申请（同意/拒绝）
 */
export const handleApplication = (data: HandleFriendRequest) => {
  return request.post<BaseResponse<number>>('/friend/handle', data)
}

/**
 * 撤销好友申请
 */
export const revokeApplication = (applicationId: number) => {
  return request.delete<BaseResponse<string>>(`/friend/revoke/${applicationId}`)
}

/**
 * 标记好友申请已读
 */
export const markApplicationsAsRead = (data: MarkReadRequest) => {
  return request.post<BaseResponse<boolean>>('/friend/mark-read', data)
}
