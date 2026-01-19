<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showSuccessToast, showLoadingToast, closeToast, showConfirmDialog } from 'vant'
import { getTeamApplications, handleTeamApplication, cancelJoinApplication } from '@/api/team'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const refreshing = ref(false)
const applications = ref<any[]>([])

// 默认头像
const defaultAvatar = 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'

/**
 * 加载队伍审批列表（获取用户创建的所有队伍的申请）
 */
const loadApplications = async () => {
  try {
    loading.value = true

    // 获取用户创建的队伍列表
    const myTeams = userStore.myTeams || []
    if (myTeams.length === 0) {
      applications.value = []
      return
    }

    // 并行获取所有队伍的申请
    const promises = myTeams.map((team: any) =>
      getTeamApplications(team.id)
    )

    const results = await Promise.all(promises)

    // 合并所有队伍的申请，并添加队伍信息
    const allApplications: any[] = []
    results.forEach((result, index) => {
      if (result.code === 0 && result.data) {
        const teamId = myTeams[index].id
        const teamName = myTeams[index].name
        const teamAvatar = myTeams[index].avatarUrl

        result.data.forEach((app: any) => {
          allApplications.push({
            ...app,
            teamId,
            teamName,
            teamAvatar
          })
        })
      }
    })

    // 按时间倒序排序
    applications.value = allApplications.sort((a, b) =>
      new Date(b.createTime).getTime() - new Date(a.createTime).getTime()
    )
  } catch (error) {
    console.error('加载队伍审批失败：', error)
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
    await loadApplications()
  } finally {
    refreshing.value = false
  }
}

/**
 * 同意申请
 */
const onAccept = async (application: any) => {
  if (application.status !== 0) {
    showToast('该申请已处理')
    return
  }

  try {
    await showConfirmDialog({
      title: '确认同意',
      message: `确定同意 ${application.userName} 加入 ${application.teamName} 吗？`
    })

    showLoadingToast({
      message: '处理中...',
      forbidClick: true,
      duration: 0
    })

    await handleTeamApplication({
      teamId: application.teamId,
      applicationId: application.id,
      status: 1
    })

    closeToast()
    showSuccessToast('已同意')

    // 从列表移除
    const index = applications.value.findIndex(item => item.id === application.id)
    if (index !== -1) {
      applications.value.splice(index, 1)
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      closeToast()
      console.error('处理失败：', error)
      showToast(error.message || '处理失败，请重试')
    }
  }
}

/**
 * 拒绝申请
 */
const onReject = async (application: any) => {
  if (application.status !== 0) {
    showToast('该申请已处理')
    return
  }

  try {
    await showConfirmDialog({
      title: '确认拒绝',
      message: '确定要拒绝这个申请吗？'
    })

    showLoadingToast({
      message: '处理中...',
      forbidClick: true,
      duration: 0
    })

    await handleTeamApplication({
      teamId: application.teamId,
      applicationId: application.id,
      status: 2
    })

    closeToast()
    showSuccessToast('已拒绝')

    // 从列表移除
    const index = applications.value.findIndex(item => item.id === application.id)
    if (index !== -1) {
      applications.value.splice(index, 1)
    }
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
      return '已同意'
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
 * 点击用户头像
 */
const onUserClick = (userId: number) => {
  router.push(`/user/${userId}`)
}

/**
 * 点击队伍卡片
 */
const onTeamClick = (teamId: number) => {
  router.push(`/team/${teamId}`)
}

/**
 * 返回
 */
const onClickLeft = () => {
  router.back()
}

// 页面挂载时加载数据
onMounted(() => {
  loadApplications()
})
</script>

<template>
  <div class="team-applications-received-page">
    <!-- 顶部导航栏 -->
    <van-nav-bar
      title="队伍审批"
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

      <!-- 空状态：没有队伍 -->
      <van-empty
        v-else-if="userStore.myTeams?.length === 0"
        description="你还没有创建任何队伍"
      >
        <template #description>
          <div>你还没有创建任何队伍</div>
          <div class="tip-text">创建队伍后可以在这里查看加入申请</div>
        </template>
      </van-empty>

      <!-- 空状态：没有申请 -->
      <van-empty
        v-else-if="applications.length === 0"
        description="暂无申请"
      >
        <template #description>
          <div>暂无队伍加入申请</div>
          <div class="tip-text">当有人申请加入你的队伍时会显示在这里</div>
        </template>
      </van-empty>

      <!-- 申请列表 -->
      <div v-else class="application-list">
        <div
          v-for="application in applications"
          :key="application.id"
          class="application-item"
        >
          <div class="application-header">
            <van-image
              round
              width="48"
              height="48"
              :src="application.userAvatar || defaultAvatar"
              @click="onUserClick(application.userId)"
            />
            <div class="application-info">
              <div class="application-title">
                <span class="username" @click="onUserClick(application.userId)">
                  {{ application.userName }}
                </span>
                <van-tag :type="getStatusType(application.status)">
                  {{ getStatusText(application.status) }}
                </van-tag>
              </div>
              <div class="application-meta">
                <span class="team-name" @click="onTeamClick(application.teamId)">
                  <van-icon name="cluster-o" size="14" />
                  申请加入 {{ application.teamName }}
                </span>
              </div>
            </div>
          </div>

          <div v-if="application.applyMessage" class="application-message">
            {{ application.applyMessage }}
          </div>

          <div class="application-footer">
            <span class="time">{{ formatTime(application.createTime) }}</span>

            <!-- 待处理：显示操作按钮 -->
            <div v-if="application.status === 0" class="actions">
              <van-button
                size="small"
                plain
                type="danger"
                @click="onReject(application)"
              >
                拒绝
              </van-button>
              <van-button
                size="small"
                type="primary"
                @click="onAccept(application)"
              >
                同意
              </van-button>
            </div>

            <!-- 已处理：显示状态 -->
            <div v-else class="status-text">
              {{ getStatusText(application.status) }}
            </div>
          </div>
        </div>
      </div>
    </van-pull-refresh>
  </div>
</template>

<style scoped>
.team-applications-received-page {
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

.application-list {
  padding: 12px 16px;
}

.application-item {
  background: white;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.application-header {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.application-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.application-title {
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

.application-meta {
  display: flex;
  align-items: center;
}

.team-name {
  font-size: 13px;
  color: #646566;
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
}

.application-message {
  font-size: 14px;
  color: #646566;
  background: #f5f5f5;
  padding: 10px 12px;
  border-radius: 8px;
  margin-bottom: 12px;
}

.application-footer {
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
