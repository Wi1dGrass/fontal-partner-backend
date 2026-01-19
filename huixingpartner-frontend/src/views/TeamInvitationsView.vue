<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showSuccessToast, showLoadingToast, closeToast, showConfirmDialog } from 'vant'

const router = useRouter()

const loading = ref(false)
const refreshing = ref(false)
const invitations = ref<any[]>([])

// 默认头像
const defaultAvatar = 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'

/**
 * 加载队伍邀请列表（待后端API实现）
 */
const loadInvitations = async () => {
  try {
    loading.value = true
    // TODO: 待后端提供API
    // const response = await getTeamInvitations()
    // if (response.code === 0 && response.data) {
    //   invitations.value = response.data
    // }
    invitations.value = []
  } catch (error) {
    console.error('加载队伍邀请失败：', error)
    showToast('加载失败，请重试')
  } finally {
    loading.value = false
  }
}

/**
 * 刷新
 */
const onRefresh = async () => {
  refreshing.value = true
  try {
    await loadInvitations()
  } finally {
    refreshing.value = false
  }
}

/**
 * 同意邀请
 */
const onAccept = async (invitation: any) => {
  if (invitation.status !== 0) {
    showToast('该邀请已处理')
    return
  }

  try {
    await showConfirmDialog({
      title: '确认加入',
      message: `确定要加入 ${invitation.teamName} 吗？`
    })

    showLoadingToast({
      message: '处理中...',
      forbidClick: true,
      duration: 0
    })

    // TODO: 待后端提供API
    // await handleTeamInvitation({
    //   teamId: invitation.teamId,
    //   invitationId: invitation.id,
    //   status: 1
    // })

    closeToast()
    showSuccessToast('已加入队伍')

    // 更新状态
    invitation.status = 1
  } catch (error: any) {
    if (error !== 'cancel') {
      closeToast()
      console.error('处理失败：', error)
      showToast(error.message || '处理失败，请重试')
    }
  }
}

/**
 * 拒绝邀请
 */
const onReject = async (invitation: any) => {
  if (invitation.status !== 0) {
    showToast('该邀请已处理')
    return
  }

  try {
    await showConfirmDialog({
      title: '确认拒绝',
      message: '确定要拒绝这个邀请吗？'
    })

    showLoadingToast({
      message: '处理中...',
      forbidClick: true,
      duration: 0
    })

    // TODO: 待后端提供API
    // await handleTeamInvitation({
    //   teamId: invitation.teamId,
    //   invitationId: invitation.id,
    //   status: 2
    // })

    closeToast()
    showSuccessToast('已拒绝')

    // 更新状态
    invitation.status = 2
  } catch (error: any) {
    if (error !== 'cancel') {
      closeToast()
      console.error('处理失败：', error)
      showToast(error.message || '处理失败，请重试')
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
    default:
      return 'default'
  }
}

/**
 * 点击队伍卡片跳转到队伍详情
 */
const onTeamClick = (teamId: number) => {
  router.push(`/team/${teamId}`)
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

// 页面挂载时加载数据
onMounted(() => {
  loadInvitations()
})
</script>

<template>
  <div class="team-invitations-page">
    <!-- 顶部导航栏 -->
    <van-nav-bar
      title="队伍邀请"
      left-text="返回"
      left-arrow
      @click-left="onClickLeft"
    />

    <!-- 内容区域 -->
    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <!-- 加载中 -->
      <div v-if="loading" class="loading-wrapper">
        <van-loading size="24px">加载中...</van-loading>
      </div>

      <!-- 空状态 -->
      <van-empty
        v-else-if="invitations.length === 0"
        description="暂无队伍邀请"
      >
        <template #description>
          <div>暂无队伍邀请</div>
          <div class="tip-text">功能开发中，敬请期待...</div>
        </template>
      </van-empty>

      <!-- 邀请列表 -->
      <div v-else class="invitation-list">
        <div
          v-for="invitation in invitations"
          :key="invitation.id"
          class="invitation-item"
        >
          <div class="invitation-header">
            <van-image
              round
              width="48"
              height="48"
              :src="invitation.leaderAvatar || defaultAvatar"
              @click="onUserClick(invitation.leaderId)"
            />
            <div class="invitation-info">
              <div class="invitation-title">
                <van-icon name="cluster-o" size="20" color="#FB7299" />
                <span class="team-name" @click="onTeamClick(invitation.teamId)">
                  {{ invitation.teamName }}
                </span>
              </div>
              <div class="invitation-meta">
                <span class="leader-name">{{ invitation.leaderName }} 邀请你加入</span>
                <van-tag :type="getStatusType(invitation.status)">
                  {{ getStatusText(invitation.status) }}
                </van-tag>
              </div>
            </div>
          </div>

          <div v-if="invitation.applyMessage" class="invitation-message">
            {{ invitation.applyMessage }}
          </div>

          <div class="invitation-footer">
            <span class="time">{{ formatTime(invitation.createTime) }}</span>

            <!-- 待处理：显示操作按钮 -->
            <div v-if="invitation.status === 0" class="actions">
              <van-button
                size="small"
                plain
                type="danger"
                @click="onReject(invitation)"
              >
                拒绝
              </van-button>
              <van-button
                size="small"
                type="primary"
                @click="onAccept(invitation)"
              >
                同意
              </van-button>
            </div>

            <!-- 已处理：显示状态 -->
            <div v-else class="status-text">
              {{ getStatusText(invitation.status) }}
            </div>
          </div>
        </div>
      </div>
    </van-pull-refresh>
  </div>
</template>

<style scoped>
.team-invitations-page {
  min-height: 100vh;
  background-color: #f5f5f5;
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

.invitation-list {
  padding: 12px 16px;
}

.invitation-item {
  background: white;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.invitation-header {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.invitation-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.invitation-title {
  display: flex;
  align-items: center;
  gap: 6px;
}

.team-name {
  font-size: 15px;
  font-weight: 500;
  color: #323233;
}

.invitation-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.leader-name {
  font-size: 13px;
  color: #646566;
}

.invitation-message {
  font-size: 14px;
  color: #646566;
  background: #f5f5f5;
  padding: 10px 12px;
  border-radius: 8px;
  margin-bottom: 12px;
}

.invitation-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.time {
  font-size: 12px;
  color: #969799;
}

.actions {
  display: flex;
  gap: 8px;
}

.status-text {
  font-size: 13px;
  color: #969799;
}
</style>
