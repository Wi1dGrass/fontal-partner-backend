<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { searchUsers, type SearchUserVO } from '@/api/user'

const router = useRouter()

// Banner 数据
const banners = ref([
  {
    id: 1,
    title: '欢迎使用惠星伙伴匹配',
    gradient: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
  },
  {
    id: 2,
    title: '找到志同道合的伙伴',
    gradient: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)'
  },
  {
    id: 3,
    title: '创建你的队伍',
    gradient: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)'
  }
])

// 当前显示的 Banner 索引
const currentBanner = ref(0)
let bannerTimer: number | null = null

// 推荐用户列表
const recommendUsers = ref<SearchUserVO[]>([])
const loading = ref(false)

// 下拉刷新状态
const refreshing = ref(false)

// 获取性别文本
const getGenderText = (gender?: number) => {
  switch (gender) {
    case 1:
      return '男'
    case 2:
      return '女'
    default:
      return '保密'
  }
}

// 获取性别图标
const getGenderIcon = (gender?: number) => {
  switch (gender) {
    case 1:
      return 'male' // 男
    case 2:
      return 'female' // 女
    default:
      return 'question' // 保密
  }
}

// 解析标签
const parseTags = (tagsStr: string) => {
  try {
    return tagsStr ? JSON.parse(tagsStr) : []
  } catch {
    return []
  }
}

// 加载推荐用户
const loadRecommendUsers = async () => {
  if (loading.value) return

  try {
    loading.value = true

    const response = await searchUsers(20)

    if (response.data && response.data.length > 0) {
      recommendUsers.value = response.data
    } else {
      recommendUsers.value = []
    }
  } catch (error: any) {
    console.error('加载推荐用户失败：', error)
    showToast(error.message || '加载失败，请重试')
  } finally {
    loading.value = false
  }
}

// 点击用户卡片
const onUserClick = (userId: number) => {
  router.push(`/user/${userId}`)
}

// 下拉刷新 - 换一批
const onRefresh = async () => {
  try {
    const response = await searchUsers(20)

    if (response.data && response.data.length > 0) {
      recommendUsers.value = response.data
      showToast('已换一批推荐')
    } else {
      recommendUsers.value = []
      showToast('暂无推荐用户')
    }
  } catch (error: any) {
    console.error('刷新失败：', error)
    showToast(error.message || '刷新失败，请重试')
  } finally {
    refreshing.value = false
  }
}

// 页面挂载时加载数据
onMounted(() => {
  loadRecommendUsers()

  // 启动 Banner 自动轮播
  bannerTimer = window.setInterval(() => {
    currentBanner.value = (currentBanner.value + 1) % banners.value.length
  }, 3000)
})

// 页面卸载时清除定时器
onUnmounted(() => {
  if (bannerTimer !== null) {
    clearInterval(bannerTimer)
    bannerTimer = null
  }
})
</script>

<template>
  <div class="home-tab">
    <!-- Banner 轮播图 -->
    <div class="banner-section">
      <div class="banner-wrapper">
        <div
          v-for="(banner, index) in banners"
          :key="banner.id"
          v-show="currentBanner === index"
          class="banner-item"
          :style="{ background: banner.gradient }"
        >
          <div class="banner-content">
            <h2 class="banner-title">{{ banner.title }}</h2>
          </div>
        </div>

        <!-- 轮播指示器 -->
        <div class="banner-indicators">
          <span
            v-for="(banner, index) in banners"
            :key="index"
            :class="['indicator', { active: currentBanner === index }]"
          ></span>
        </div>
      </div>
    </div>

    <!-- 推荐用户区域 -->
    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <div class="recommend-section">
        <div class="section-header">
          <h3 class="section-title">推荐用户</h3>
          <span class="section-count">{{ recommendUsers.length }}位</span>
        </div>

        <!-- 用户列表 -->
        <div class="user-list">
          <!-- 加载中状态 -->
          <van-loading
            v-if="loading && recommendUsers.length === 0"
            size="24px"
            vertical
          >
            加载中...
          </van-loading>

          <!-- 空状态 -->
          <van-empty
            v-else-if="!loading && recommendUsers.length === 0"
            description="暂无推荐用户"
          />

          <!-- 用户卡片列表 -->
          <div
            v-for="user in recommendUsers"
            :key="user.id"
            class="user-card"
            @click="onUserClick(user.id)"
          >
            <!-- 用户头像 -->
            <van-image
              round
              width="50"
              height="50"
              :src="user.userAvatarUrl || 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'"
              class="user-avatar"
            />

            <!-- 用户信息 -->
            <div class="user-info">
              <div class="user-header">
                <span class="user-name">{{ user.username }}</span>
                <van-icon
                  :name="getGenderIcon(user.gender)"
                  :color="user.gender === 1 ? '#1989fa' : user.gender === 2 ? '#fb7299' : '#969799'"
                  size="14"
                />
              </div>

              <p class="user-profile">{{ user.profile || '这个人很懒，什么都没留下' }}</p>

              <!-- 标签 -->
              <div class="user-tags" v-if="parseTags(user.tags).length > 0">
                <van-tag
                  v-for="(tag, index) in parseTags(user.tags).slice(0, 3)"
                  :key="index"
                  type="primary"
                  size="medium"
                  plain
                >
                  {{ tag }}
                </van-tag>
              </div>
            </div>

            <!-- 右箭头 -->
            <van-icon name="arrow" class="arrow-icon" />
          </div>
        </div>
      </div>
    </van-pull-refresh>
  </div>
</template>

<style scoped>
.home-tab {
  min-height: 100vh;
  background-color: #f5f5f5;
}

/* Banner 区域 */
.banner-section {
  margin-bottom: 12px;
}

.banner-wrapper {
  position: relative;
  height: 180px;
  overflow: hidden;
  border-radius: 0 0 12px 12px;
}

.banner-item {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  transition: opacity 0.5s ease-in-out;
}

.banner-content {
  text-align: center;
}

.banner-title {
  font-size: 24px;
  font-weight: bold;
  color: white;
  margin: 0;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

/* 轮播指示器 */
.banner-indicators {
  position: absolute;
  bottom: 12px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 6px;
  z-index: 10;
}

.indicator {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: rgba(255, 255, 255, 0.5);
  transition: all 0.3s;
}

.indicator.active {
  width: 16px;
  border-radius: 3px;
  background-color: white;
}

/* 推荐用户区域 */
.recommend-section {
  padding: 0 16px 16px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  padding: 0 4px;
}

.section-title {
  font-size: 18px;
  font-weight: bold;
  color: #323233;
  margin: 0;
}

.section-count {
  font-size: 14px;
  color: #969799;
}

/* 用户列表 */
.user-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.user-card {
  background: white;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.user-card:active {
  background-color: #f5f5f5;
  transform: scale(0.98);
}

.user-avatar {
  flex-shrink: 0;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-header {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 4px;
}

.user-name {
  font-size: 16px;
  font-weight: bold;
  color: #323233;
}

.user-profile {
  font-size: 13px;
  color: #969799;
  margin: 0 0 8px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.arrow-icon {
  flex-shrink: 0;
  color: #c8c9cc;
  font-size: 14px;
}
</style>
