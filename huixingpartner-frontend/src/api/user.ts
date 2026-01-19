import request from '@/utils/request'
import type { UserInfo } from '@/stores/user'

/**
 * 基础响应结构
 */
export interface BaseResponse<T> {
  code: number
  data: T
  description?: string
  message?: string
}

/**
 * 用户登录
 */
export interface LoginRequest {
  userAccount: string
  userPassword: string
}

export interface LoginResponse {
  id: number
  username: string
  userAccount: string
  userAvatarUrl: string
  userDesc: string
  gender: number
  userRole: number
  userStatus: number
  tags: string
  teamIds: string
  contactInfo: string
  email: string
  createTime: string
  updateTime: string
}

export const userLogin = (data: LoginRequest) => {
  return request.post<LoginResponse>('/user/login', data)
}

/**
 * 用户注册
 */
export interface RegisterRequest {
  username: string
  userAccount: string
  userPassword: string
  checkPassword: string
}

export const userRegister = (data: RegisterRequest) => {
  return request.post<number>('/user/register', data)
}

/**
 * 获取当前登录用户信息
 */
export const getCurrentUser = () => {
  return request.get<BaseResponse<UserInfo>>('/user/current')
}

/**
 * 搜索用户（推荐用户列表）
 */
export interface SearchUserVO {
  id: number
  username: string
  userAvatarUrl: string
  gender: number
  profile: string
  tags: string
}

export const searchUsers = (limit: number = 20) => {
  return request.get<SearchUserVO[]>('/user/search', { params: { limit } })
}

/**
 * 根据用户名搜索用户（保留原接口，供搜索功能使用）
 */
export const searchUsersByUsername = (username: string) => {
  return request.get<UserInfo[]>('/user/search', { params: { username } })
}

/**
 * 用户退出登录
 */
export const userLogout = () => {
  return request.post<number>('/user/loginOut')
}

/**
 * 更新用户信息
 */
export interface UpdateUserRequest {
  username?: string
  gender?: number
  userDesc?: string
  contactInfo?: string
  email?: string
  userAvatarUrl?: string
}

export const updateUser = (data: UpdateUserRequest) => {
  return request.post<boolean>('/user/update', data)
}

/**
 * 更新用户标签
 */
export interface UpdateTagsRequest {
  id: number
  tagList: string[]
}

export const updateTags = (data: UpdateTagsRequest) => {
  return request.post<number>('/user/update/tags', data)
}

/**
 * 修改用户密码
 */
export interface UpdatePasswordRequest {
  oldPassword: string
  newPassword: string
  checkPassword: string
}

export const updatePassword = (data: UpdatePasswordRequest) => {
  return request.post<boolean>('/user/updatePassword', data)
}

/**
 * 获取用户详情
 */
export interface UserDetailVO {
  id: number
  username: string
  userAvatarUrl: string
  gender: number
  profile: string
  tags: string
  contactInfo: string
  email: string
}

export const getUserDetail = (id: number) => {
  return request.get<UserDetailVO>(`/user/${id}/detail`)
}

/**
 * 发送好友申请
 */
export interface FriendApplyRequest {
  receiveId: number
  remark?: string
}

export const applyFriend = (data: FriendApplyRequest) => {
  return request.post<boolean>('/friend/apply', data)
}

/**
 * 搜索功能相关接口
 */

/**
 * 用户VO（搜索结果）
 */
export interface UserVO {
  id: number
  username: string
  userAvatarUrl: string
  gender: number
  profile: string
  tags: string
}

/**
 * 分页结果
 */
export interface PageResult<T> {
  current: number
  pages: number
  records: T[]
  size: number
  total: number
}

/**
 * 用户搜索请求（文本搜索）
 */
export interface UserQueryRequest {
  pageNum?: number
  pageSize?: number
  searchText?: string
}

/**
 * 根据用户名/文本搜索用户（支持分页）
 */
export const queryUser = (data: UserQueryRequest) => {
  return request.post<PageResult<UserVO>>('/user/search', data)
}

/**
 * 标签搜索请求
 */
export interface TagSearchRequest {
  tags?: string
  pageNum?: number
  pageSize?: number
}

/**
 * 根据标签搜索用户（支持分页）
 */
export const searchUsersByTags = (params: TagSearchRequest) => {
  return request.get<PageResult<UserVO>>('/user/search/tags', { params })
}
