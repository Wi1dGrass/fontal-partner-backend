import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getFriendList } from '@/api/friend'

export interface UserInfo {
  id: number
  username: string
  userAccount: string
  userAvatarUrl?: string  // 后端字段名：userAvatarUrl
  avatarUrl?: string       // 别名，兼容旧代码
  userDesc?: string
  gender?: number
  phone?: string
  email?: string
  contactInfo?: string
  userRole?: number
  userStatus?: number
  createTime?: string
  updateTime?: string
  isDelete?: number
  tags?: string        // 后端返回的是 JSON 字符串，不是数组
  tagsArray?: string[] // 解析后的数组（用于前端使用）
  teamIds?: string     // 队伍ID列表，格式："[100004,100005,100013]"
  userIds?: string     // 好友ID列表，格式："[70571,70572]"
}

export const useUserStore = defineStore('user', () => {
  const userInfo = ref<UserInfo | null>(null)
  const isLoggedIn = ref(false)

  // 好友ID列表缓存（不持久化，每次登录后重新加载）
  const friendIds = ref<Set<number>>(new Set())
  const friendIdsLoaded = ref(false)

  function setUserInfo(info: UserInfo) {
    // 兼容性处理：后端返回的是 userAvatarUrl，前端代码使用 avatarUrl
    const normalizedInfo = {
      ...info,
      avatarUrl: info.userAvatarUrl || info.avatarUrl
    }

    userInfo.value = normalizedInfo
    isLoggedIn.value = true

    // 如果包含 userIds 且不为空字符串，自动解析并存储好友ID列表
    if (info.userIds && info.userIds.trim() !== '') {
      try {
        const userIds = JSON.parse(info.userIds)
        friendIds.value = new Set(userIds)
        friendIdsLoaded.value = true
      } catch (error) {
        console.error('解析好友ID列表失败：', error)
      }
    } else {
      // userIds 为空或空字符串，清空好友列表
      friendIds.value.clear()
      friendIdsLoaded.value = true
    }
  }

  function clearUserInfo() {
    userInfo.value = null
    isLoggedIn.value = false
    // 清除好友缓存
    friendIds.value.clear()
    friendIdsLoaded.value = false
  }

  function updateUserInfo(info: Partial<UserInfo>) {
    if (userInfo.value) {
      userInfo.value = { ...userInfo.value, ...info }
    }
  }

  // 加载好友ID列表（仅首次）
  async function loadFriendIds() {
    if (friendIdsLoaded.value) return

    try {
      // 优先从 userInfo.userIds 解析（如果已登录且数据存在）
      if (isLoggedIn.value && userInfo.value?.userIds) {
        const userIds = JSON.parse(userInfo.value.userIds)
        friendIds.value = new Set(userIds)
        friendIdsLoaded.value = true
        return
      }

      // 如果没有，则调用好友列表API
      const response = await getFriendList()
      if (response.data?.friends) {
        friendIds.value = new Set(response.data.friends.map((f: any) => f.id))
        friendIdsLoaded.value = true
      }
    } catch (error) {
      console.error('加载好友ID列表失败：', error)
    }
  }

  // 判断是否是好友
  function isFriend(userId: number): boolean {
    return friendIds.value.has(userId)
  }

  // 清除好友缓存（在添加/删除好友后调用）
  function clearFriendIds() {
    friendIds.value.clear()
    friendIdsLoaded.value = false
  }

  return {
    userInfo,
    isLoggedIn,
    friendIds,
    friendIdsLoaded,
    setUserInfo,
    clearUserInfo,
    updateUserInfo,
    loadFriendIds,
    isFriend,
    clearFriendIds
  }
}, {
  persist: {
    key: 'user-store',
    storage: localStorage,
    paths: ['userInfo', 'isLoggedIn'] // 不持久化 friendIds
  }
})
