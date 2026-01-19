<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getTeamsByUserId } from '@/api/team'
import { getCurrentUser } from '@/api/user'

const router = useRouter()
const userStore = useUserStore()

// 队伍数量
const teamCount = ref(0)

// 判断是否已登录
const isLoggedIn = computed(() => userStore.isLoggedIn)

// 用户信息
const userInfo = computed(() => userStore.userInfo)

// 好友数量（从 Store 获取）
const friendCount = computed(() => userStore.friendIds.size)

// 加载队伍数量
const loadTeamCount = async () => {
  if (!userInfo.value?.id) {
    teamCount.value = 0
    return
  }

  try {
    const response = await getTeamsByUserId(userInfo.value.id)
    if (response.code === 0 && response.data) {
      teamCount.value = response.data.teamSet.length
    }
  } catch (error) {
    console.error('获取队伍数量失败', error)
    teamCount.value = 0
  }
}

// 刷新用户信息（包括好友ID列表）
const refreshUserInfo = async () => {
  if (!isLoggedIn.value) return

  try {
    const response = await getCurrentUser()
    if (response.data) {
      userStore.setUserInfo(response.data)
    }
  } catch (error) {
    console.error('刷新用户信息失败', error)
  }
}

// 组件挂载时加载用户信息和队伍数量
onMounted(() => {
  if (isLoggedIn.value) {
    refreshUserInfo() // 刷新用户信息（包括好友ID列表）
    loadTeamCount()
  }
})

// 登录按钮点击
const onLogin = () => {
  router.push('/login')
}

// 跳转到个人资料页面
const goToProfile = () => {
  router.push('/user/profile')
}

// 跳转到我的队伍
const goToMyTeams = () => {
  router.push('/team/my')
}

// 跳转到我的好友
const goToMyFriends = () => {
  router.push('/friend/list')
}

// 功能菜单点击
const onMenuClick = (action: string) => {
  switch (action) {
    case 'profile':
      goToProfile()
      break
    case 'teams':
      goToMyTeams()
      break
    case 'friends':
      goToMyFriends()
      break
  }
}
</script>

<template>
  <div class="profile-tab">
    <!-- 未登录状态 -->
    <div v-if="!isLoggedIn" class="not-logged-in">
      <div class="login-card">
        <div class="logo">
          <van-image
            width="80"
            height="80"
            round
            src="https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg"
          />
        </div>
        <h2 class="title">欢迎使用</h2>
        <p class="subtitle">登录后查看更多信息</p>
        <van-button
          round
          block
          type="primary"
          color="#FB7299"
          size="large"
          @click="onLogin"
        >
          立即登录
        </van-button>
      </div>
    </div>

    <!-- 已登录状态 -->
    <div v-else class="logged-in">
      <!-- 用户信息卡片 -->
      <div class="user-card">
        <div class="user-header">
          <van-image
            round
            width="64"
            height="64"
            :src="userInfo?.avatarUrl || 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'"
          />
          <div class="user-info">
            <h3 class="user-name">{{ userInfo?.username || '用户' }}</h3>
            <p class="user-id">ID: {{ userInfo?.id || '未知' }}</p>
          </div>
        </div>
      </div>

      <!-- 功能菜单 -->
      <div class="menu-section">
        <van-cell-group inset>
          <van-cell
            title="个人资料"
            icon="contact"
            is-link
            @click="onMenuClick('profile')"
          />
          <van-cell
            title="我的队伍"
            icon="friends-o"
            :value="`${teamCount}个`"
            is-link
            @click="onMenuClick('teams')"
          />
          <van-cell
            title="我的好友"
            icon="chat-o"
            :value="`${friendCount}个`"
            is-link
            @click="onMenuClick('friends')"
          />
        </van-cell-group>
      </div>
    </div>
  </div>
</template>

<style scoped>
.profile-tab {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: calc(50px + env(safe-area-inset-bottom));
}

/* 未登录状态 */
.not-logged-in {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 20px;
}

.login-card {
  width: 100%;
  max-width: 400px;
  background: white;
  border-radius: 16px;
  padding: 40px 20px;
  text-align: center;
}

.logo {
  margin-bottom: 20px;
}

.title {
  font-size: 24px;
  font-weight: bold;
  color: #323233;
  margin: 0 0 10px 0;
}

.subtitle {
  font-size: 14px;
  color: #969799;
  margin: 0 0 30px 0;
}

/* 已登录状态 */
.logged-in {
  padding: 16px;
}

.user-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;
}

.user-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-info {
  flex: 1;
}

.user-name {
  font-size: 18px;
  font-weight: bold;
  color: white;
  margin: 0 0 4px 0;
}

.user-id {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
  margin: 0;
}

.menu-section {
  margin-bottom: 12px;
}
</style>
