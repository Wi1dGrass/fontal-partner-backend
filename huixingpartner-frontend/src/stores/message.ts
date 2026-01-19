import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import type { MessageCategory, MessageItem } from '@/types/message'
import {
  getReceivedApplications,
  getSentApplications,
  handleApplication,
  revokeApplication
} from '@/api/friend'
import { useUserStore } from './user'

export const useMessageStore = defineStore('message', () => {
  const userStore = useUserStore()

  // 4种消息类型的数据
  const categories = ref<MessageCategory[]>([
    {
      id: 'friend-received',
      name: '好友申请',
      icon: 'friends-o',
      count: 0,
      items: [],
      loading: false,
      loaded: false
    },
    {
      id: 'friend-sent',
      name: '我的申请',
      icon: 'send-gift-o',
      count: 0,
      items: [],
      loading: false,
      loaded: false
    },
    {
      id: 'team-sent',
      name: '队伍申请',
      icon: 'cluster-o',
      count: 0,
      items: [],
      loading: false,
      loaded: false
    },
    {
      id: 'team-received',
      name: '队伍审批',
      icon: 'manager-o',
      count: 0,
      items: [],
      loading: false,
      loaded: false
    }
  ])

  // 总未读数量（可用于底部Tab，虽然用户目前不需要）
  const totalUnreadCount = computed(() => {
    return categories.value.reduce((sum, cat) => sum + cat.count, 0)
  })

  /**
   * 加载指定分类的消息
   */
  const loadCategory = async (categoryId: string) => {
    const category = categories.value.find(c => c.id === categoryId)
    if (!category || category.loading) return

    category.loading = true
    category.error = undefined

    try {
      switch (categoryId) {
        case 'friend-received':
          await loadFriendReceived(category)
          break
        case 'friend-sent':
          await loadFriendSent(category)
          break
        case 'team-sent':
          await loadTeamSent(category)
          break
        case 'team-received':
          await loadTeamReceived(category)
          break
      }
      category.loaded = true
    } catch (error: any) {
      console.error(`加载${category.name}失败：`, error)
      category.error = error.message || '加载失败，请重试'
    } finally {
      category.loading = false
    }
  }

  /**
   * 加载收到的好友申请
   */
  const loadFriendReceived = async (category: MessageCategory) => {
    const response = await getReceivedApplications()
    if (response.code === 0 && response.data) {
      category.items = response.data
      updateCategoryCount(category.id)
    }
  }

  /**
   * 加载发送的好友申请
   */
  const loadFriendSent = async (category: MessageCategory) => {
    const response = await getSentApplications()
    if (response.code === 0 && response.data) {
      category.items = response.data
      // 发送的申请不显示红点
      category.count = 0
    }
  }

  /**
   * 加载队伍申请（我发出的）
   * 注意：需要后端提供 GET /api/team/my-applications 接口
   */
  const loadTeamSent = async (category: MessageCategory) => {
    // 暂时返回空数组，等待后端API
    category.items = []
    category.count = 0
    // TODO: 实现后取消注释
    // const response = await getMyTeamApplications()
    // if (response.code === 0 && response.data) {
    //   category.items = response.data
    //   category.count = 0
    // }
  }

  /**
   * 加载队伍审批（我收到的）
   * 需要获取用户创建的所有队伍，然后分别获取每个队伍的申请
   */
  const loadTeamReceived = async (category: MessageCategory) => {
    // 暂时返回空数组，需要完整的实现
    category.items = []
    category.count = 0

    // TODO: 实现完整逻辑
    // 1. 获取用户创建的队伍列表
    // 2. 遍历队伍ID，调用 /api/team/applications/{teamId}
    // 3. 合并所有申请并更新列表
  }

  /**
   * 更新某个分类的未读数量
   */
  const updateCategoryCount = (categoryId: string) => {
    const category = categories.value.find(c => c.id === categoryId)
    if (!category) return

    // 对于收到的申请，status=0 表示未读
    if (categoryId === 'friend-received' || categoryId === 'team-received') {
      category.count = category.items.filter(item => item.status === 0).length
    }
    // 对于发送的申请，不显示红点
    else {
      category.count = 0
    }
  }

  /**
   * 处理消息操作后的状态更新
   */
  const handleItemAction = (categoryId: string, itemId: number, newStatus: number) => {
    const category = categories.value.find(c => c.id === categoryId)
    if (!category) return

    const item = category.items.find(i => i.id === itemId)
    if (item) {
      item.status = newStatus
      updateCategoryCount(categoryId)
    }
  }

  /**
   * 删除某个消息项（用于撤销后从列表移除）
   */
  const removeItem = (categoryId: string, itemId: number) => {
    const category = categories.value.find(c => c.id === categoryId)
    if (!category) return

    const index = category.items.findIndex(i => i.id === itemId)
    if (index !== -1) {
      category.items.splice(index, 1)
      updateCategoryCount(categoryId)
    }
  }

  /**
   * 刷新指定分类
   */
  const refreshCategory = async (categoryId: string) => {
    const category = categories.value.find(c => c.id === categoryId)
    if (!category) return

    category.loaded = false // 重置loaded状态
    await loadCategory(categoryId)
  }

  /**
   * 刷新所有分类
   */
  const refreshAll = async () => {
    const promises = categories.value.map(cat => refreshCategory(cat.id))
    await Promise.all(promises)
  }

  return {
    categories,
    totalUnreadCount,
    loadCategory,
    updateCategoryCount,
    handleItemAction,
    removeItem,
    refreshCategory,
    refreshAll
  }
})
