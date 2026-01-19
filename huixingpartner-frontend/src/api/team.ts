import request from '@/utils/request'

/**
 * 队伍成员信息
 */
export interface TeamUserVO {
  id: number
  username: string
  userAccount: string
  userAvatarUrl: string | null
  gender: number | null
  email: string
  contactInfo: string
  userDesc: string
  userStatus: number
  userRole: number
  tags: string
  teamIds: string
  userIds: string
  createTime: string
  updateTime: string | null
  isDelete: number | null
}

/**
 * 队伍用户信息(队长)
 */
export interface TeamUserInfo {
  id: number
  username: string
  userAccount: string
  userAvatarUrl: string | null
  gender: number | null
  userPassword: string | null
  email: string
  contactInfo: string
  userDesc: string
  userStatus: number
  userRole: number
  tags: string
  teamIds: string
  userIds: string
  createTime: string
  updateTime: string | null
  isDelete: number | null
}

/**
 * 队伍信息
 */
export interface TeamVO {
  id: number
  teamName: string
  teamAvatarUrl: string
  teamPassword: string | null
  teamDesc: string
  maxNum: number
  expireTime: string
  teamStatus: number
  createTime: string
  announce: string
  user: TeamUserInfo       // 队长信息
  userSet: TeamUserVO[]    // 成员列表
}

/**
 * 获取队伍列表的响应结构
 */
export interface TeamListResponse {
  teamSet: TeamVO[]
}

/**
 * API 通用响应结构
 */
export interface ApiResponse<T = any> {
  code: number
  data: T
  message: string
  description: string
}

/**
 * 根据用户ID获取队伍列表
 */
export const getTeamsByUserId = (userId: number) => {
  return request.get<TeamListResponse>(`/team/user/${userId}`)
}

/**
 * 根据队伍ID获取队伍详情
 */
export const getTeamById = (teamId: number) => {
  return request.get<TeamVO>(`/team/${teamId}`)
}

/**
 * 更新队伍信息请求
 */
export interface UpdateTeamRequest {
  id: number
  teamName?: string
  teamDesc?: string
  teamAvatarUrl?: string
  maxNum?: number
  expireTime?: string
  teamStatus?: number
  teamPassword?: string
  announce?: string
}

/**
 * 更新队伍信息
 */
export const updateTeam = (data: UpdateTeamRequest) => {
  return request.post<boolean>('/team/update', data)
}

/**
 * 转让队伍请求
 */
export interface TransferTeamRequest {
  teamId: number
  userAccount: string
}

/**
 * 转让队长
 */
export const transferTeam = (data: TransferTeamRequest) => {
  return request.post<boolean>('/team/transfer', data)
}

/**
 * 退出队伍
 */
export const quitTeam = (teamId: number) => {
  return request.post<boolean>(`/team/quit/${teamId}`)
}

/**
 * 删除队伍请求
 */
export interface DeleteTeamRequest {
  teamId: number
}

/**
 * 解散队伍
 */
export const deleteTeam = (data: DeleteTeamRequest) => {
  return request.post<boolean>('/team/delete', data)
}

/**
 * 踢出成员请求
 */
export interface KickOutUserRequest {
  teamId: number
  userId: number
}

/**
 * 踢出成员
 */
export const kickOutUser = (data: KickOutUserRequest) => {
  return request.post<boolean>('/team/kickOutUser', data)
}

/**
 * 获取推荐队伍列表
 */
export const getRecommendTeams = (limit: number = 20, pageNum: number = 1) => {
  return request.get<ApiResponse<TeamVO[]>>(`/team/recommend?limit=${limit}&pageNum=${pageNum}`)
}

/**
 * 获取热门队伍列表
 */
export const getHotTeams = (limit: number = 20, pageNum: number = 1) => {
  return request.get<ApiResponse<TeamVO[]>>(`/team/hot?limit=${limit}&pageNum=${pageNum}`)
}

/**
 * 获取最新队伍列表
 */
export const getNewTeams = (limit: number = 20, pageNum: number = 1) => {
  return request.get<ApiResponse<TeamVO[]>>(`/team/new?limit=${limit}&pageNum=${pageNum}`)
}

/**
 * 队长信息（基础信息接口返回）
 */
export interface CaptainInfo {
  id: number
  userAccount: string
  userAvatarUrl: string
  userDesc: string
  username: string
}

/**
 * 队伍基础信息（非成员可见）
 */
export interface TeamBasicVO {
  id: number
  teamName: string
  teamAvatarUrl: string
  teamDesc: string
  maxNum: number
  currentNum: number
  expireTime: string
  teamStatus: number
  createTime: string
  announce: string
  captain: CaptainInfo
  tags: string[]
  requirements: string
  statusDesc: string
}

/**
 * 获取队伍基础信息（非成员）
 */
export const getTeamBasicInfo = (teamId: number) => {
  return request.get<TeamBasicVO>(`/team/${teamId}/basic`)
}

/**
 * 加入队伍请求
 */
export interface JoinTeamRequest {
  teamId: number
  password?: string
  applyMessage?: string
}

/**
 * 加入队伍（公开/加密）
 */
export const joinTeam = (data: JoinTeamRequest) => {
  return request.post<boolean>('/team/join', data)
}

/**
 * 申请加入队伍请求
 */
export interface ApplyTeamRequest {
  teamId: number
  applyMessage?: string
}

/**
 * 申请加入队伍（私有）
 */
export const applyToJoinTeam = (data: ApplyTeamRequest) => {
  return request.post<number>('/team/apply', data)
}

/**
 * 创建队伍请求
 */
export interface CreateTeamRequest {
  teamName: string
  teamDesc?: string
  teamAvatarUrl?: string
  maxNum: number
  expireTime: string
  teamStatus: number
  teamPassword?: string
  announce?: string
}

/**
 * 创建队伍
 */
export const createTeam = (data: CreateTeamRequest) => {
  return request.post<boolean>('/team/create', data)
}

/**
 * 队伍成员身份信息
 */
export interface TeamMembershipVO {
  isMember: boolean
  role: string
}

/**
 * 获取队伍成员身份
 */
export const getTeamMembership = (teamId: number) => {
  return request.get<TeamMembershipVO>(`/team/${teamId}/membership`)
}

/**
 * 搜索队伍
 * @param text 搜索文本（可匹配：队伍名称、ID、描述、公告）
 */
export const searchTeams = (text: string) => {
  return request.get<TeamVO[]>(`/team/search?text=${encodeURIComponent(text)}`)
}

/**
 * 队伍申请信息
 */
export interface TeamApplication {
  id: number
  teamId: number
  teamName: string
  teamAvatar: string
  userId: number
  userName: string
  userAvatar: string
  applyMessage: string
  applyStatus: number  // 0-待处理，1-已同意，2-已拒绝，3-已取消
  applyType: number
  applyTypeDesc: string
  statusDesc: string
  createTime: string
  updateTime: string
}

/**
 * 获取队伍申请列表响应
 */
export interface TeamApplicationsResponse {
  code: number
  data: TeamApplication[]
  message?: string
}

/**
 * 获取队伍的待审批申请列表
 */
export const getTeamApplications = (teamId: number) => {
  return request.get<TeamApplicationsResponse>(`/team/applications/${teamId}`)
}

/**
 * 处理队伍申请请求
 */
export interface HandleTeamApplicationRequest {
  teamId: number
  applicationId: number
  status: number  // 1-同意，2-拒绝
}

/**
 * 处理队伍申请（同意/拒绝）
 */
export const handleTeamApplication = (data: HandleTeamApplicationRequest) => {
  return request.post<boolean>('/team/application/handle', data)
}

/**
 * 取消加入队伍申请
 */
export const cancelJoinApplication = (teamId: number) => {
  return request.post<boolean>(`/team/apply/cancel/${teamId}`)
}
