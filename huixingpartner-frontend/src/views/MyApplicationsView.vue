<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showSuccessToast, showLoadingToast, closeToast, showConfirmDialog } from 'vant'
import { getSentApplications, revokeApplication } from '@/api/friend'
import type { FriendApplicationSent } from '@/types/message'

const router = useRouter()

const activeTab = ref(0)
const loading = ref(false)
const refreshing = ref(false)

// 好友申请数据
const friendApplications = ref<FriendApplicationSent[]>([])
const friendLoading = ref(false)

// 队伍申请数据（待后端API）
const teamApplications = ref<any[]>([])
const teamLoading = ref(false)

// 默认头像
const defaultAvatar = 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'

/**
 * 加载好友申请
 */
const loadFriendApplications = async () => {
  try {
    friendLoading.value = true
    const response = await getSentApplications()
    if (response.code === 0 && response.data) {
      friendApplications.value = response.data
    }
  } catch (error) {
    console.error('加载好友申请失败：', error)
    showToast('加载失败，请重试')
  } finally {
    friendLoading.value = false
  }
}

/**
 * 加载队伍申请（待后端API实现）
 */
const loadTeamApplications = async () => {
  try {
    teamLoading.value = true
    // TODO: 待后端提供API
    // const response = await getMyTeamApplications()
    // if (response.code === 0 && response.data) {
    //   teamApplications.value = response.data
    // }
    teamApplications.value = []
  } catch (error) {
    console.error('加载队伍申请失败：', error)
  } finally {
    teamLoading.value = false
  }
}

/**
 * 刷新当前Tab
 */
const onRefresh = async () => {
  refreshing.value = true
  try {
    if (activeTab.value === 0) {
      await loadFriendApplications()
    } else {
      await loadTeamApplications()
    }
  } finally {
    refreshing.value = false
  }
}

/**
 * Tab切换时加载数据
 */
const onTabChange = async () => {
  if (activeTab.value === 0 && friendApplications.value.length === 0) {
    await loadFriendApplications()
  } else if (activeTab.value === 1 && teamApplications.value.length === 0) {
    await loadTeamApplications()
  }
}

/**
 * 撤销好友申请
 */
const onRevokeFriend = async (application: FriendApplicationSent) => {
  if (application.status !== 0) {
    showToast('只能撤销待处理的申请')
    return
  }

  try {
    await showConfirmDialog({
      title: '确认撤销',
      message: '确定要撤销这个好友申请吗？'
    })

    showLoadingToast({
      message: '撤销中...',
      forbidClick: true,
      duration: 0
    })

    await revokeApplication(application.id)

    closeToast()
    showSuccessToast('已撤销')

    // 从列表移除
    const index = friendApplications.value.findIndex(item => item.id === application.id)
    if (index !== -1) {
      friendApplications.value.splice(index, 1)
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      closeToast()
      console.error('撤销失败：', error)
      showToast(error.message || '撤销失败，请重试')
    }
  }
}

/**
 * 取消队伍申请（待后端API）
 */
const onCancelTeam = async (application: any) => {
  if (application.status !== 0) {
    showToast('只能取消待处理的申请')
    return
  }

  try {
    await showConfirmDialog({
      title: '确认取消',
      message: '确定要取消这个队伍申请吗？'
    })

    showLoadingToast({
      message: '取消中...',
      forbidClick: true,
      duration: 0
    })

    // TODO: 待后端提供API
    // await cancelTeamApplication(application.teamId)

    closeToast()
    showSuccessToast('已取消')

    // 从列表移除
    const index = teamApplications.value.findIndex(item => item.id === application.id)
    if (index !== -1) {
      teamApplications.value.splice(index, 1)
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      closeToast()
      console.error('取消失败：', error)
      showToast(error.message || '取消失败，请重试')
    }
  }
}

/**
 * 格式化时间
 */
const formatTime = (time: string) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  if (diff < 60 * 1000) return '刚刚'
  if (diff < 60 * 60 * 1000) return `${Math.floor(diff / (60 * 1000))}分钟前`
  if (diff < 24 * 60 * 60 * 1000) return `${Math.floor(diff / (60 * 60 * 1000))}小时前`
  if (diff < 7 * 24 * 60 * 60 * 1000) return `${Math.floor(diff / (24 * 60 * 60 * 1000))}天前`

  return `${date.getMonth() + 1}-${date.getDate()}`
}

/**
 * 获取状态文本
 */
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

/**
 * 获取状态类型
 */
const getStatusType = (status: number) => {
  switch (status) {
    case 0:
      return 'warning'
    case 1:
      return 'success'
    case 2:
      return 'danger'
    case 3:
      return 'primary'
    default:
      return 'default'
  }
}

/**
 * 点击用户头像
 */
const onUserClick = (userId: number) => {
  router.push(`/user/${userId}`)
}

/**
 * 返回
 */
const onClickLeft = () => {
  router.back()
}

// 页面挂载时加载好友申请
onMounted(() => {
  loadFriendApplications()
})
</script>

<template>
  <div class="my-applications-page">
    <!-- 顶部导航栏 -->
    <van-nav-bar
      title="我的申请"
      left-text="返回"
      left-arrow
      @click-left="onClickLeft"
    />

    <!-- Tabs -->
    <van-tabs v-model:active="activeTab" @change="onTabChange" class="custom-tabs">
      <!-- 好友申请 -->
      <van-tab title="好友申请" name="friend">
        <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
          <!-- 加载中 -->
          <div v-if="friendLoading" class="loading-wrapper">
            <van-loading size="24px">加载中...</van-loading>
          </div>

          <!-- 空状态 -->
          <van-empty
            v-else-if="friendApplications.length === 0"
            description="暂无好友申请"
          />

          <!-- 列表 -->
          <div v-else class="application-list">
            <div
              v-for="application in friendApplications"
              :key="application.id"
              class="application-item"
            >
              <van-image
                round
                width="48"
                height="48"
                :src="application.applyUser.userAvatarUrl || defaultAvatar"
                @click="onUserClick(application.applyUser.id)"
              />
              <div class="application-content">
                <div class="application-header">
                  <span class="username">{{ application.applyUser.username }}</span>
                  <van-tag :type="getStatusType(application.status)">
                    {{ getStatusText(application.status) }}
                  </van-tag>
                </div>
                <p class="remark">{{ application.remark || '申请添加好友' }}</p>
                <div class="application-footer">
                  <span class="time">{{ formatTime(application.createTime) }}</span>
                  <van-button
                    v-if="application.status === 0"
                    size="small"
                    plain
                    type="warning"
                    @click="onRevokeFriend(application)"
                  >
                    撤销申请
                  </van-button>
                </div>
              </div>
            </div>
          </div>
        </van-pull-refresh>
      </van-tab>

      <!-- 队伍申请 -->
      <van-tab title="队伍申请" name="team">
        <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
          <!-- 加载中 -->
          <div v-if="teamLoading" class="loading-wrapper">
            <van-loading size="24px">加载中...</van-loading>
          </div>

          <!-- 空状态 -->
          <van-empty
            v-else-if="teamApplications.length === 0"
            description="暂无队伍申请"
          >
            <template #description>
              <div>暂无队伍申请</div>
              <div class="tip-text">功能开发中，敬请期待...</div>
            </template>
          </van-empty>

          <!-- 列表 -->
          <div v-else class="application-list">
            <div
              v-for="application in teamApplications"
              :key="application.id"
              class="application-item"
            >
              <van-image
                round
                width="48"
                height="48"
                :src="application.teamAvatar || defaultAvatar"
              />
              <div class="application-content">
                <div class="application-header">
                  <span class="username">{{ application.teamName }}</span>
                  <van-tag :type="getStatusType(application.status)">
                    {{ getStatusText(application.status) }}
                  </van-tag>
                </div>
                <p class="remark">{{ application.applyMessage || '申请加入队伍' }}</p>
                <div class="application-footer">
                  <span class="time">{{ formatTime(application.createTime) }}</span>
                  <van-button
                    v-if="application.status === 0"
                    size="small"
                    plain
                    type="warning"
                    @click="onCancelTeam(application)"
                  >
                    取消申请
                  </van-button>
                </div>
              </div>
            </div>
          </div>
        </van-pull-refresh>
      </van-tab>
    </van-tabs>
  </div>
</template>

<style scoped>
.my-applications-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.custom-tabs {
  background: white;
}

.loading-wrapper {
  padding: 40px 0;
  display: flex;
  justify-content: center;
  align-items: center;
}

.tip-text {
  font-size: 13px;
  color: #969799;
  margin-top: 8px;
}

.application-list {
  padding: 12px 16px;
}

.application-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: white;
  border-radius: 8px;
  margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.application-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.application-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.username {
  font-size: 15px;
  font-weight: 500;
  color: #323233;
}

.remark {
  font-size: 13px;
  color: #646566;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.application-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.time {
  font-size: 12px;
  color: #969799;
}
</style>
