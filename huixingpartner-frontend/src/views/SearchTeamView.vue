<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { searchTeams, type TeamVO } from '@/api/team'

const router = useRouter()

// 搜索关键词
const searchKeyword = ref('')

// 搜索结果
const searchResults = ref<TeamVO[]>([])

// 搜索状态
const searching = ref(false)

// 是否有搜索结果
const hasSearched = ref(false)

/**
 * 执行搜索
 */
const onSearch = async () => {
  const keyword = searchKeyword.value.trim()

  if (!keyword) {
    showToast('请输入搜索关键词')
    return
  }

  searching.value = true
  hasSearched.value = true

  try {
    const response = await searchTeams(keyword)

    if (response.code === 0 && response.data) {
      searchResults.value = response.data
      console.log('搜索成功，找到', response.data.length, '个队伍')
    } else {
      searchResults.value = []
      showToast('搜索失败')
    }
  } catch (error: any) {
    console.error('搜索失败', error)
    showToast(error.message || '搜索失败，请重试')
    searchResults.value = []
  } finally {
    searching.value = false
  }
}

/**
 * 取消搜索
 */
const onCancel = () => {
  router.back()
}

/**
 * 跳转到队伍详情页
 */
const goToTeamDetail = (teamId: number) => {
  router.push(`/team/detail/${teamId}`)
}

/**
 * 格式化过期时间
 * 支持中文日期格式：2026年05月19日 00:00:00
 */
const formatExpireTime = (time?: string) => {
  if (!time) return '未设置'

  // 处理中文日期格式：2026年05月19日 00:00:00
  let parsedTime = time
  if (time.includes('年') && time.includes('月')) {
    parsedTime = time.replace(/(\d{4})年(\d{2})月(\d{2})日/, '$1-$2-$3')
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

/**
 * 获取队伍状态文本
 */
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
</script>

<template>
  <div class="search-team-view">
    <!-- 搜索栏 -->
    <van-search
      v-model="searchKeyword"
      placeholder="搜索队伍名称、描述"
      show-action
      shape="round"
      @search="onSearch"
      @cancel="onCancel"
    />

    <!-- 搜索结果 -->
    <div v-if="hasSearched" class="search-results">
      <!-- 搜索中 -->
      <div v-if="searching" class="loading-state">
        <van-loading type="spinner" size="24px" vertical>搜索中...</van-loading>
      </div>

      <!-- 无结果 -->
      <div v-else-if="searchResults.length === 0" class="empty-state">
        <van-empty description="未找到相关队伍" />
      </div>

      <!-- 搜索结果列表 -->
      <div v-else class="result-list">
        <div v-for="team in searchResults" :key="team.id" class="team-card" @click="goToTeamDetail(team.id)">
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
              <div class="team-title-row">
                <h3 class="team-name">{{ team.teamName }}</h3>
                <van-tag :type="team.teamStatus === 0 ? 'primary' : 'default'" size="medium">
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
      </div>
    </div>

    <!-- 搜索提示 -->
    <div v-else class="search-hint">
      <van-empty description="输入关键词搜索队伍">
        <template #image>
          <van-icon name="search" size="60" color="#dcdee0" />
        </template>
      </van-empty>
    </div>
  </div>
</template>

<style scoped>
.search-team-view {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.search-results {
  padding: 12px;
}

.loading-state {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 60px 20px;
}

.empty-state {
  padding: 60px 20px;
}

.search-hint {
  padding: 60px 20px;
}

.result-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.team-card {
  background: white;
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

.team-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}

.team-name {
  font-size: 16px;
  font-weight: bold;
  color: #323233;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
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
</style>
