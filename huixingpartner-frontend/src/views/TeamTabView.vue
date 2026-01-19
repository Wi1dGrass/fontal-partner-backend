<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Toast } from 'vant'
import { getRecommendTeams, getHotTeams, getNewTeams, type TeamVO, type ApiResponse } from '@/api/team'

const router = useRouter()

// 当前激活的标签页
const activeTab = ref(0)

// 列表数据
const recommendList = ref<TeamVO[]>([])
const hotList = ref<TeamVO[]>([])
const newList = ref<TeamVO[]>([])

// 加载状态
const recommendLoading = ref(false)
const hotLoading = ref(false)
const newLoading = ref(false)

// 刷新状态
const recommendRefreshing = ref(false)
const hotRefreshing = ref(false)
const newRefreshing = ref(false)

// 是否还有更多数据
const recommendFinished = ref(false)
const hotFinished = ref(false)
const newFinished = ref(false)

// 分页
const recommendPageNum = ref(1)
const hotPageNum = ref(1)
const newPageNum = ref(1)

const pageSize = 20

/**
 * 刷新推荐队伍
 */
const onRefreshRecommend = async () => {
  recommendPageNum.value = 1
  recommendFinished.value = false
  recommendList.value = []

  await loadRecommendTeams()
  recommendRefreshing.value = false
}

/**
 * 刷新热门队伍
 */
const onRefreshHot = async () => {
  hotPageNum.value = 1
  hotFinished.value = false
  hotList.value = []

  await loadHotTeams()
  hotRefreshing.value = false
}

/**
 * 刷新最新队伍
 */
const onRefreshNew = async () => {
  newPageNum.value = 1
  newFinished.value = false
  newList.value = []

  await loadNewTeams()
  newRefreshing.value = false
}

/**
 * 加载推荐队伍
 */
const loadRecommendTeams = async () => {
  if (recommendLoading.value || recommendFinished.value) return

  try {
    recommendLoading.value = true
    const response = await getRecommendTeams(pageSize, recommendPageNum.value) as ApiResponse<TeamVO[]>

    if (response && response.code === 0 && response.data) {
      const teams = response.data
      recommendList.value.push(...teams)

      if (teams.length < pageSize) {
        recommendFinished.value = true
      } else {
        recommendPageNum.value++
      }
    } else {
      recommendFinished.value = true
    }
  } catch (error) {
    console.error('加载推荐队伍失败', error)
    Toast.fail('加载失败，请重试')
  } finally {
    recommendLoading.value = false
  }
}

/**
 * 加载热门队伍
 */
const loadHotTeams = async () => {
  if (hotLoading.value || hotFinished.value) return

  try {
    hotLoading.value = true
    const response = await getHotTeams(pageSize, hotPageNum.value) as ApiResponse<TeamVO[]>

    if (response && response.code === 0 && response.data) {
      const teams = response.data
      hotList.value.push(...teams)

      if (teams.length < pageSize) {
        hotFinished.value = true
      } else {
        hotPageNum.value++
      }
    } else {
      hotFinished.value = true
    }
  } catch (error) {
    console.error('加载热门队伍失败', error)
    Toast.fail('加载失败，请重试')
  } finally {
    hotLoading.value = false
  }
}

/**
 * 加载最新队伍
 */
const loadNewTeams = async () => {
  if (newLoading.value || newFinished.value) return

  try {
    newLoading.value = true
    const response = await getNewTeams(pageSize, newPageNum.value) as ApiResponse<TeamVO[]>

    if (response && response.code === 0 && response.data) {
      const teams = response.data
      newList.value.push(...teams)

      if (teams.length < pageSize) {
        newFinished.value = true
      } else {
        newPageNum.value++
      }
    } else {
      newFinished.value = true
    }
  } catch (error) {
    console.error('加载最新队伍失败', error)
    Toast.fail('加载失败，请重试')
  } finally {
    newLoading.value = false
  }
}

/**
 * 标签页切换事件
 */
const onTabChange = (name: number | string) => {
  const tabIndex = Number(name)

  // 懒加载：切换到某个标签页时，如果该页面还没有数据，则加载第一页
  if (tabIndex === 0 && recommendList.value.length === 0) {
    loadRecommendTeams()
  } else if (tabIndex === 1 && hotList.value.length === 0) {
    loadHotTeams()
  } else if (tabIndex === 2 && newList.value.length === 0) {
    loadNewTeams()
  }
}

/**
 * 跳转到搜索页面
 */
const goToSearch = () => {
  router.push('/team/search')
}

/**
 * 跳转到创建队伍页面
 */
const goToCreateTeam = () => {
  router.push('/team/create')
}

/**
 * 跳转到队伍详情页
 */
const goToTeamDetail = (teamId: number) => {
  router.push(`/team/detail/${teamId}`)
}

/**
 * 格式化过期时间（相对时间）
 */
const formatExpireTime = (time: string) => {
  if (!time) return '未知'

  const date = new Date(time.replace(/年/g, '-').replace(/月/g, '-').replace(/日/g, '').replace(/:/g, ':'))
  const now = new Date()
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

/**
 * 获取队伍状态文本
 */
const getTeamStatusText = (status: number) => {
  switch (status) {
    case 0:
      return '公开'
    case 1:
      return '私人'
    case 2:
      return '加密'
    default:
      return '未知'
  }
}

/**
 * 获取队伍状态类型
 */
const getTeamStatusType = (status: number) => {
  switch (status) {
    case 0:
      return 'success' // 绿色
    case 1:
      return 'warning' // 橙色
    case 2:
      return 'danger' // 红色
    default:
      return 'default'
  }
}

/**
 * 计算剩余人数
 */
const getRemainingCount = (team: TeamVO) => {
  const currentCount = (team.userSet?.length || 0) + 1 // +1 是队长
  return team.maxNum - currentCount
}

// 组件挂载时加载推荐队伍
onMounted(() => {
  loadRecommendTeams()
})
</script>

<template>
  <div class="team-tab-view">
    <!-- 标签页（带返回和搜索按钮） -->
    <van-tabs v-model:active="activeTab" @change="onTabChange" sticky fixed>
      <!-- 左侧返回按钮 -->
      <template #nav-left>
        <div class="nav-btn" @click="router.back()">
          <van-icon name="arrow-left" size="18" />
        </div>
      </template>

      <!-- 右侧搜索和创建按钮 -->
      <template #nav-right>
        <div class="nav-right-buttons">
          <div class="nav-btn" @click="goToCreateTeam">
            <van-icon name="plus" size="18" />
          </div>
          <div class="nav-btn" @click="goToSearch">
            <van-icon name="search" size="18" />
          </div>
        </div>
      </template>

      <!-- 推荐标签 -->
      <van-tab title="推荐" name="0">
        <van-pull-refresh v-model="recommendRefreshing" @refresh="onRefreshRecommend">
          <van-list
            v-model:loading="recommendLoading"
            :finished="recommendFinished"
            finished-text="没有更多了"
            @load="loadRecommendTeams"
          >
            <div v-if="recommendList.length === 0 && !recommendLoading" class="empty-state">
              <van-empty description="暂无推荐队伍" />
            </div>

            <div v-for="team in recommendList" :key="team.id" class="team-card" @click="goToTeamDetail(team.id)">
            <div class="team-header">
              <div class="team-avatar">
                <van-image
                  width="48"
                  height="48"
                  round
                  :src="team.teamAvatarUrl || 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'"
                />
              </div>
              <div class="team-info">
                <div class="team-name-row">
                  <h3 class="team-name">{{ team.teamName }}</h3>
                  <van-tag :type="getTeamStatusType(team.teamStatus)" size="medium">
                    {{ getTeamStatusText(team.teamStatus) }}
                  </van-tag>
                </div>
                <p class="team-desc">{{ team.teamDesc || '暂无描述' }}</p>
              </div>
            </div>

            <div class="team-meta">
              <div class="meta-item">
                <van-icon name="friends-o" />
                <span>{{ team.userSet?.length || 0 }}/{{ team.maxNum }}人</span>
              </div>
              <div class="meta-item">
                <van-icon name="clock-o" />
                <span>{{ formatExpireTime(team.expireTime) }}</span>
              </div>
            </div>
          </div>
          </van-list>
        </van-pull-refresh>
      </van-tab>

      <!-- 热门标签 -->
      <van-tab title="热门" name="1">
        <van-pull-refresh v-model="hotRefreshing" @refresh="onRefreshHot">
          <van-list
          v-model:loading="hotLoading"
          :finished="hotFinished"
          finished-text="没有更多了"
          @load="loadHotTeams"
        >
          <div v-if="hotList.length === 0 && !hotLoading" class="empty-state">
            <van-empty description="暂无热门队伍" />
          </div>

          <div v-for="team in hotList" :key="team.id" class="team-card" @click="goToTeamDetail(team.id)">
            <div class="team-header">
              <div class="team-avatar">
                <van-image
                  width="48"
                  height="48"
                  round
                  :src="team.teamAvatarUrl || 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'"
                />
              </div>
              <div class="team-info">
                <div class="team-name-row">
                  <h3 class="team-name">{{ team.teamName }}</h3>
                  <van-tag :type="getTeamStatusType(team.teamStatus)" size="medium">
                    {{ getTeamStatusText(team.teamStatus) }}
                  </van-tag>
                </div>
                <p class="team-desc">{{ team.teamDesc || '暂无描述' }}</p>
              </div>
            </div>

            <div class="team-meta">
              <div class="meta-item">
                <van-icon name="friends-o" />
                <span>{{ team.userSet?.length || 0 }}/{{ team.maxNum }}人</span>
              </div>
              <div class="meta-item">
                <van-icon name="clock-o" />
                <span>{{ formatExpireTime(team.expireTime) }}</span>
              </div>
            </div>
          </div>
          </van-list>
        </van-pull-refresh>
      </van-tab>

      <!-- 最新标签 -->
      <van-tab title="最新" name="2">
        <van-pull-refresh v-model="newRefreshing" @refresh="onRefreshNew">
          <van-list
          v-model:loading="newLoading"
          :finished="newFinished"
          finished-text="没有更多了"
          @load="loadNewTeams"
        >
          <div v-if="newList.length === 0 && !newLoading" class="empty-state">
            <van-empty description="暂无最新队伍" />
          </div>

          <div v-for="team in newList" :key="team.id" class="team-card" @click="goToTeamDetail(team.id)">
            <div class="team-header">
              <div class="team-avatar">
                <van-image
                  width="48"
                  height="48"
                  round
                  :src="team.teamAvatarUrl || 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'"
                />
              </div>
              <div class="team-info">
                <div class="team-name-row">
                  <h3 class="team-name">{{ team.teamName }}</h3>
                  <van-tag :type="getTeamStatusType(team.teamStatus)" size="medium">
                    {{ getTeamStatusText(team.teamStatus) }}
                  </van-tag>
                </div>
                <p class="team-desc">{{ team.teamDesc || '暂无描述' }}</p>
              </div>
            </div>

            <div class="team-meta">
              <div class="meta-item">
                <van-icon name="friends-o" />
                <span>{{ team.userSet?.length || 0 }}/{{ team.maxNum }}人</span>
              </div>
              <div class="meta-item">
                <van-icon name="clock-o" />
                <span>{{ formatExpireTime(team.expireTime) }}</span>
              </div>
            </div>
          </div>
          </van-list>
        </van-pull-refresh>
      </van-tab>
    </van-tabs>
  </div>
</template>

<style scoped>
.team-tab-view {
  min-height: 100vh;
  background-color: #f5f5f5;
}

/* 导航按钮 */
.nav-right-buttons {
  display: flex;
  align-items: center;
  height: 100%;
}

.nav-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 12px;
  height: 100%;
  cursor: pointer;
}

.nav-btn:active {
  opacity: 0.6;
}

.team-card {
  background: white;
  margin: 12px;
  padding: 16px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.team-card:active {
  transform: scale(0.98);
  background-color: #f8f8f8;
}

.team-header {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.team-avatar {
  flex-shrink: 0;
}

.team-info {
  flex: 1;
  min-width: 0;
}

.team-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.team-name {
  font-size: 16px;
  font-weight: bold;
  color: #323233;
  margin: 0;
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.team-desc {
  font-size: 13px;
  color: #969799;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.5;
}

.team-meta {
  display: flex;
  gap: 16px;
  padding-top: 8px;
  border-top: 1px solid #ebedf0;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #969799;
}

.empty-state {
  padding: 40px 20px;
}
</style>
