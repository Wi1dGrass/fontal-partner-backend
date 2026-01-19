<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showLoadingToast, closeToast } from 'vant'
import { useUserStore } from '@/stores/user'
import { getTeamsByUserId, type TeamVO, type TeamListResponse } from '@/api/team'

const router = useRouter()
const userStore = useUserStore()

// 队伍列表
const teamList = ref<TeamVO[]>([])

// 加载状态
const loading = ref(false)
const isEmpty = ref(false)

// 用户信息
const userInfo = computed(() => userStore.userInfo)

// 返回上一页
const onClickLeft = () => {
  router.back()
}

// 加载我的队伍列表
const loadMyTeams = async () => {
  if (!userInfo.value?.id) {
    showToast('请先登录')
    router.push('/login')
    return
  }

  loading.value = true

  try {
    showLoadingToast({
      message: '加载中...',
      forbidClick: true,
      duration: 0
    })

    const response = await getTeamsByUserId(userInfo.value.id)

    closeToast()

    if (response.code === 0 && response.data) {
      const data = response.data as TeamListResponse
      teamList.value = data.teamSet || []

      // 判断是否为空
      if (data.teamSet.length === 0) {
        isEmpty.value = true
      }
    } else {
      showToast('加载失败，请稍后重试')
    }
  } catch (error: any) {
    closeToast()
    console.error('加载队伍列表失败：', error)
    showToast(error.message || '加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 点击队伍卡片，跳转到队伍详情
const onTeamClick = (team: TeamVO) => {
  router.push(`/team/detail/${team.id}`)
}

// 判断用户是否是队长
const isTeamCaptain = (team: TeamVO) => {
  return userInfo.value?.id === team.user?.id
}

// 获取队伍状态文本
const getTeamStatusText = (status?: number) => {
  switch (status) {
    case 0:
      return '公开'
    case 1:
      return '私有'
    case 2:
      return '加密'
    default:
      return '未知'
  }
}

// 解析标签
const parseTags = (tagsStr?: string) => {
  if (!tagsStr) return []
  try {
    return JSON.parse(tagsStr)
  } catch {
    return []
  }
}

// 格式化过期时间
const formatExpireTime = (time?: string) => {
  if (!time) return '未设置'

  // 处理中文日期格式：2026年05月19日 00:00:00
  let parsedTime = time
  if (time.includes('年') && time.includes('月')) {
    // 将中文日期转换为标准格式：2026-05-19 00:00:00
    parsedTime = time
      .replace(/(\d{4})年(\d{2})月(\d{2})日/, '$1-$2-$3')
      .replace(/(\d{2})月(\d{2})日/, '$1-$2')
  }

  const date = new Date(parsedTime)
  const now = new Date()

  // 检查日期是否有效
  if (isNaN(date.getTime())) {
    return '时间格式错误'
  }

  const diff = date.getTime() - now.getTime()

  if (diff <= 0) {
    return '已过期'
  }

  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  if (days > 0) {
    return `${days}天后过期`
  }

  const hours = Math.floor(diff / (1000 * 60 * 60))
  if (hours > 0) {
    return `${hours}小时后过期`
  }

  const minutes = Math.floor(diff / (1000 * 60))
  return `${minutes}分钟后过期`
}

// 组件挂载时加载队伍列表
onMounted(() => {
  loadMyTeams()
})
</script>

<template>
  <div class="my-teams-page">
    <!-- 顶部导航栏 -->
    <van-nav-bar
      title="我的队伍"
      left-text="返回"
      left-arrow
      @click-left="onClickLeft"
      fixed
      placeholder
      safe-area-inset-top
    />

    <div class="my-teams-content">
      <!-- 队伍列表 -->
      <div v-if="teamList.length > 0 || isEmpty" class="teams-list">
        <!-- 队伍数量 -->
        <div v-if="!isEmpty" class="team-count">
          共加入 {{ teamList.length }} 个队伍
        </div>

        <!-- 队伍卡片 -->
        <div
          v-for="team in teamList"
          :key="team.id"
          class="team-card"
          @click="onTeamClick(team)"
        >
          <!-- 队伍信息 -->
          <div class="team-info">
            <div class="team-header">
              <h3 class="team-name">{{ team.teamName }}</h3>
              <div class="team-tags">
                <van-tag v-if="isTeamCaptain(team)" type="danger" size="medium">
                  队长
                </van-tag>
                <van-tag :type="team.teamStatus === 0 ? 'primary' : 'default'" size="medium">
                  {{ getTeamStatusText(team.teamStatus) }}
                </van-tag>
              </div>
            </div>

            <!-- 队伍描述 -->
            <div v-if="team.teamDesc" class="team-description">
              {{ team.teamDesc }}
            </div>
            <div v-else class="team-description empty">
              暂无描述
            </div>

            <!-- 队伍元信息 -->
            <div class="team-meta">
              <div class="meta-item">
                <van-icon name="friends-o" size="14" />
                <span>{{ team.userSet?.length || 0 }}/{{ team.maxNum }}人</span>
              </div>
              <div class="meta-item">
                <van-icon name="clock-o" size="14" />
                <span>{{ formatExpireTime(team.expireTime) }}</span>
              </div>
            </div>

            <!-- 成员头像列表 -->
            <div v-if="team.userSet && team.userSet.length > 0" class="member-avatars">
              <van-image
                v-for="(member, index) in team.userSet.slice(0, 5)"
                :key="index"
                round
                width="32"
                height="32"
                :src="member.userAvatarUrl || 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'"
                class="member-avatar"
              />
              <span v-if="team.userSet.length > 5" class="more-members">
                +{{ team.userSet.length - 5 }}
              </span>
            </div>
          </div>

          <!-- 右箭头 -->
          <van-icon name="arrow" class="arrow-icon" />
        </div>

        <!-- 空状态 -->
        <van-empty
          v-if="isEmpty"
          image="search"
          description="还没有加入任何队伍"
        >
          <template #description>
            <div class="empty-tips">
              还没有加入任何队伍
              <br />
              快去创建或加入队伍吧
            </div>
          </template>
        </van-empty>
      </div>
    </div>
  </div>
</template>

<style scoped>
.my-teams-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.my-teams-content {
  padding: 16px;
}

/* 队伍数量 */
.team-count {
  font-size: 14px;
  color: #969799;
  margin-bottom: 12px;
  padding: 0 4px;
}

/* 队伍卡片 */
.team-card {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: all 0.2s;
}

.team-card:active {
  transform: scale(0.98);
  background: #f7f8fa;
}

.team-card:last-child {
  margin-bottom: 0;
}

.team-info {
  flex: 1;
  min-width: 0;
}

/* 队伍头部 */
.team-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.team-tags {
  display: flex;
  gap: 4px;
}

.team-name {
  font-size: 16px;
  font-weight: 600;
  color: #323233;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 队伍描述 */
.team-description {
  font-size: 13px;
  color: #646566;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.5;
}

.team-description.empty {
  color: #c8c9cc;
  font-style: italic;
}

/* 队伍元信息 */
.team-meta {
  display: flex;
  gap: 16px;
  margin-bottom: 8px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #969799;
}

/* 成员头像列表 */
.member-avatars {
  display: flex;
  align-items: center;
  gap: -8px;
}

.member-avatar {
  border: 2px solid #fff;
  margin-left: -8px;
}

.member-avatar:first-child {
  margin-left: 0;
}

.more-members {
  margin-left: 4px;
  font-size: 12px;
  color: #969799;
  background: #f7f8fa;
  padding: 4px 8px;
  border-radius: 12px;
  border: 2px solid #fff;
}

/* 右箭头 */
.arrow-icon {
  flex-shrink: 0;
  color: #c8c9cc;
  font-size: 16px;
}

/* 空状态 */
.empty-tips {
  text-align: center;
  color: #969799;
  font-size: 14px;
  line-height: 1.6;
}
</style>
