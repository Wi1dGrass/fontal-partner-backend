<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showSuccessToast, showLoadingToast, closeToast, showConfirmDialog } from 'vant'
import {
  getReceivedApplications,
  handleApplication,
  markApplicationsAsRead
} from '@/api/friend'
import type { FriendApplication } from '@/types/friend'

const router = useRouter()

// 好友申请列表
const applications = ref<FriendApplication[]>([])

// 下拉刷新
const refreshing = ref(false)

// 列表加载
const loading = ref(false)
const finished = ref(false)

// 是否有待处理申请
const hasPending = computed(() => {
  return applications.value.some(app => app.status === 0)
})

// 是否有未读申请
const hasUnread = computed(() => {
  return applications.value.length > 0
})

// 返回上一页
const onClickLeft = () => {
  router.back()
}

// 格式化时间
const formatTime = (timeString: string) => {
  const createTime = new Date(timeString).getTime()
  const now = Date.now()
  const diff = now - createTime

  // 小于1分钟
  if (diff < 60 * 1000) {
    return '刚刚'
  }

  // 小于1小时
  if (diff < 60 * 60 * 1000) {
    const minutes = Math.floor(diff / (60 * 1000))
    return `${minutes}分钟前`
  }

  // 小于24小时
  if (diff < 24 * 60 * 60 * 1000) {
    const hours = Math.floor(diff / (60 * 60 * 1000))
    return `${hours}小时前`
  }

  // 小于7天
  if (diff < 7 * 24 * 60 * 60 * 1000) {
    const days = Math.floor(diff / (24 * 60 * 60 * 1000))
    return `${days}天前`
  }

  // 大于7天，显示日期
  const date = new Date(timeString)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${month}-${day}`
}

// 获取状态文本
const getStatusText = (status: number) => {
  switch (status) {
    case 0:
      return '待处理'
    case 1:
      return '已接受'
    case 2:
      return '已拒绝'
    case 3:
      return '已撤销'
    default:
      return '未知'
  }
}

// 获取状态颜色
const getStatusColor = (status: number) => {
  switch (status) {
    case 0:
      return '#FB7299' // 粉色 - 待处理
    case 1:
      return '#07c160' // 绿色 - 已接受
    case 2:
      return '#969799' // 灰色 - 已拒绝
    case 3:
      return '#969799' // 灰色 - 已撤销
    default:
      return '#969799'
  }
}

// 加载好友申请列表
const loadApplications = async () => {
  try {
    loading.value = true

    const response = await getReceivedApplications()

    if (response.code === 0 && response.data) {
      applications.value = response.data
      finished.value = true
    } else {
      showToast('加载失败，请稍后重试')
      finished.value = true
    }
  } catch (error: any) {
    console.error('加载好友申请失败：', error)

    // API调用失败，使用模拟数据
    showToast('API连接失败，使用模拟数据')
    loadMockData()
    finished.value = true
  } finally {
    loading.value = false
  }
}

// 加载模拟数据（降级方案）
const loadMockData = () => {
  applications.value = [
    {
      id: 1,
      fromUserId: 2,
      toUserId: 1,
      remark: '你好，想加你为好友',
      status: 0, // 待处理
      createTime: '2026-01-19 10:30:00',
      applyUser: {
        id: 2,
        username: '张三',
        userAccount: 'zhangsan',
        userAvatarUrl: 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'
      }
    },
    {
      id: 2,
      fromUserId: 3,
      toUserId: 1,
      remark: '看到你的标签，想和你交流一下',
      status: 0, // 待处理
      createTime: '2026-01-19 09:15:00',
      applyUser: {
        id: 3,
        username: '李四',
        userAccount: 'lisi',
        userAvatarUrl: 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'
      }
    },
    {
      id: 3,
      fromUserId: 4,
      toUserId: 1,
      remark: '你好，我也是学Java的',
      status: 1, // 已接受
      createTime: '2026-01-18 15:20:00',
      applyUser: {
        id: 4,
        username: '王五',
        userAccount: 'wangwu',
        userAvatarUrl: 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'
      }
    },
    {
      id: 4,
      fromUserId: 5,
      toUserId: 1,
      remark: '想了解一下前端开发',
      status: 2, // 已拒绝
      createTime: '2026-01-17 12:00:00',
      applyUser: {
        id: 5,
        username: '赵六',
        userAccount: 'zhaoliu',
        userAvatarUrl: 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'
      }
    }
  ]
}

// 下拉刷新
const onRefresh = async () => {
  refreshing.value = true
  finished.value = false
  await loadApplications()
  refreshing.value = false
}

// 上拉加载更多
const onLoad = () => {
  loadApplications()
}

// 处理好友申请（同意/拒绝）
const onHandleApplication = async (application: FriendApplication, action: 'accept' | 'reject') => {
  // 如果是拒绝，二次确认
  if (action === 'reject') {
    try {
      await showConfirmDialog({
        title: '确认拒绝',
        message: '确定要拒绝此好友申请吗？'
      })
    } catch {
      // 用户取消
      return
    }
  }

  try {
    showLoadingToast({
      message: action === 'accept' ? '接受中...' : '拒绝中...',
      forbidClick: true,
      duration: 0
    })

    await handleApplication({
      id: application.id,
      status: action === 'accept' ? 1 : 2
    })

    closeToast()

    // 更新本地状态
    application.status = action === 'accept' ? 1 : 2

    showSuccessToast(action === 'accept' ? '已接受好友申请' : '已拒绝好友申请')
  } catch (error: any) {
    closeToast()
    console.error('处理好友申请失败：', error)
    showToast(error.message || '操作失败，请重试')
  }
}

// 标记所有已读
const onMarkAllAsRead = async () => {
  if (!hasUnread.value) {
    return
  }

  try {
    // 不传friendId参数，标记所有未读申请
    await markApplicationsAsRead({})
    showSuccessToast('已标记全部已读')
  } catch (error: any) {
    console.error('标记已读失败：', error)
    showToast('标记失败，请重试')
  }
}

// 点击用户头像，查看用户资料
const onUserClick = (userId: number) => {
  router.push(`/user/${userId}`)
}

// 初始加载
loadApplications()
</script>

<template>
  <div class="friend-request-page">
    <!-- 顶部导航栏 -->
    <van-nav-bar
      title="好友申请"
      left-text="返回"
      left-arrow
      @click-left="onClickLeft"
      fixed
      placeholder
      safe-area-inset-top
    >
      <template #right>
        <van-button
          size="small"
          type="primary"
          @click="onMarkAllAsRead"
          :disabled="!hasUnread"
        >
          标记已读
        </van-button>
      </template>
    </van-nav-bar>

    <!-- 申请列表 -->
    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <van-list
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="onLoad"
      >
        <!-- 申请项列表 -->
        <div
          v-for="application in applications"
          :key="application.id"
          class="request-item"
        >
          <!-- 用户头像 -->
          <van-image
            round
            width="50"
            height="50"
            :src="application.applyUser.userAvatarUrl || 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'"
            class="user-avatar"
            @click="onUserClick(application.applyUser.id)"
          />

          <!-- 申请信息 -->
          <div class="request-info">
            <!-- 头部：用户名 + 时间 -->
            <div class="request-header">
              <span class="username">{{ application.applyUser.username }}</span>
              <div class="header-right">
                <span
                  class="status-badge"
                  :style="{ backgroundColor: getStatusColor(application.status) }"
                >
                  {{ getStatusText(application.status) }}
                </span>
                <span class="time">{{ formatTime(application.createTime) }}</span>
              </div>
            </div>

            <!-- 申请备注 -->
            <div v-if="application.remark" class="remark">
              {{ application.remark }}
            </div>
            <div v-else class="remark no-remark">
              没有填写验证消息
            </div>

            <!-- 待处理：显示按钮 -->
            <div v-if="application.status === 0" class="actions">
              <van-button
                size="small"
                type="primary"
                @click="onHandleApplication(application, 'accept')"
              >
                同意
              </van-button>
              <van-button
                size="small"
                plain
                @click="onHandleApplication(application, 'reject')"
              >
                拒绝
              </van-button>
            </div>

            <!-- 已处理：显示状态文本 -->
            <div v-else class="status-text">
              {{ application.status === 1 ? '已接受好友申请' : application.status === 2 ? '已拒绝此申请' : '申请已撤销' }}
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <van-empty
          v-if="applications.length === 0 && !loading"
          description="暂无好友申请"
        />
      </van-list>
    </van-pull-refresh>
  </div>
</template>

<style scoped>
.friend-request-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

/* 申请项 */
.request-item {
  display: flex;
  align-items: flex-start;
  padding: 16px;
  background-color: #ffffff;
  border-bottom: 1px solid #f5f5f5;
  gap: 12px;
}

.request-item:last-child {
  border-bottom: none;
}

/* 头像 */
.user-avatar {
  flex-shrink: 0;
  cursor: pointer;
}

/* 申请信息 */
.request-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

/* 头部 */
.request-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.username {
  font-size: 15px;
  font-weight: 500;
  color: #323233;
  flex-shrink: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

/* 状态标签 */
.status-badge {
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 11px;
  color: #ffffff;
  white-space: nowrap;
}

/* 时间 */
.time {
  font-size: 12px;
  color: #969799;
  white-space: nowrap;
}

/* 申请备注 */
.remark {
  font-size: 14px;
  color: #646566;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.remark.no-remark {
  color: #969799;
  font-style: italic;
}

/* 操作按钮 */
.actions {
  display: flex;
  gap: 8px;
  margin-top: 4px;
}

.actions .van-button {
  flex: 1;
}

/* 状态文本 */
.status-text {
  font-size: 13px;
  color: #969799;
  margin-top: 4px;
}
</style>
