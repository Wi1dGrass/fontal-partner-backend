<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showLoadingToast, closeToast } from 'vant'
import { getFriendList } from '@/api/friend'
import type { Friend } from '@/types/friend'

const router = useRouter()

// 好友列表
const friendList = ref<Friend[]>([])

// 搜索关键词
const searchKeyword = ref('')

// 加载状态
const loading = ref(false)
const isEmpty = ref(false)

// 返回上一页
const onClickLeft = () => {
  router.back()
}

// 加载好友列表
const loadFriendList = async () => {
  loading.value = true

  try {
    showLoadingToast({
      message: '加载中...',
      forbidClick: true,
      duration: 0
    })

    // 调用后端API获取好友列表
    const response = await getFriendList()

    closeToast()

    if (response.code === 0 && response.data) {
      // 后端返回的是 { friends: Friend[], total: number }
      friendList.value = response.data.friends

      // 判断是否为空
      if (response.data.friends.length === 0) {
        isEmpty.value = true
      }
    } else {
      showToast('加载失败，请稍后重试')
    }
  } catch (error: any) {
    closeToast()
    console.error('加载好友列表失败：', error)

    // 如果API调用失败，使用模拟数据作为降级方案
    showToast('API连接失败，使用模拟数据')
    loadMockData()
  } finally {
    loading.value = false
  }
}

// 加载模拟数据（降级方案）
const loadMockData = () => {
  friendList.value = [
    {
      id: 2,
      name: '张三',
      avatar: 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg',
      userAccount: 'zhangsan',
      userDesc: '热爱编程的Java开发者',
      tags: '["Java", "Spring Boot", "MySQL"]',
      lastMessage: '你好，在吗？',
      lastMessageTime: '12:30'
    },
    {
      id: 3,
      name: '李四',
      avatar: 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg',
      userAccount: 'lisi',
      userDesc: '前端工程师',
      tags: '["Vue", "React", "TypeScript"]',
      lastMessage: '好的，没问题',
      lastMessageTime: '昨天'
    },
    {
      id: 4,
      name: '王五',
      avatar: 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg',
      userAccount: 'wangwu',
      userDesc: '全栈开发',
      tags: '["Node.js", "Python", "Docker"]',
      lastMessage: '收到，谢谢！',
      lastMessageTime: '星期一'
    }
  ]

  if (friendList.value.length === 0) {
    isEmpty.value = true
  }
}

// 过滤后的好友列表（搜索功能）
const filteredFriends = computed(() => {
  if (!searchKeyword.value.trim()) {
    return friendList.value
  }

  const keyword = searchKeyword.value.toLowerCase()
  return friendList.value.filter(friend => {
    return friend.name.toLowerCase().includes(keyword) ||
           friend.userAccount?.toLowerCase().includes(keyword) ||
           friend.userDesc?.toLowerCase().includes(keyword)
  })
})

// 判断是否显示空状态
const showEmpty = computed(() => {
  return !loading.value && filteredFriends.value.length === 0
})

// 点击好友，跳转到用户详情页
const onFriendClick = (friend: Friend) => {
  router.push(`/user/${friend.id}`)
}

// 组件挂载
onMounted(() => {
  loadFriendList()
})
</script>

<template>
  <div class="friend-list-page">
    <!-- 顶部导航栏 -->
    <van-nav-bar
      title="好友列表"
      left-text="返回"
      left-arrow
      @click-left="onClickLeft"
      fixed
      placeholder
      safe-area-inset-top
    />

    <!-- 搜索框 -->
    <div class="search-section">
      <van-search
        v-model="searchKeyword"
        placeholder="搜索好友"
        shape="round"
        background="#ffffff"
      />
    </div>

    <!-- 好友列表 -->
    <div class="friend-list">
      <!-- 加载状态 -->
      <van-loading v-if="loading" class="loading" size="24px" vertical>加载中...</van-loading>

      <!-- 空状态 -->
      <van-empty v-else-if="showEmpty" description="暂无好友" />

      <!-- 好友列表项 -->
      <div v-else class="friend-items">
        <div
          v-for="friend in filteredFriends"
          :key="friend.id"
          class="friend-item"
          @click="onFriendClick(friend)"
        >
          <!-- 头像 -->
          <van-image
            round
            width="48"
            height="48"
            :src="friend.avatar"
            class="friend-avatar"
          />

          <!-- 信息区域 -->
          <div class="friend-info">
            <!-- 昵称和时间 -->
            <div class="friend-header">
              <span class="friend-name">{{ friend.name }}</span>
              <span v-if="friend.lastMessageTime" class="friend-time">
                {{ friend.lastMessageTime }}
              </span>
            </div>

            <!-- 最后一条消息 -->
            <div v-if="friend.lastMessage" class="friend-message">
              {{ friend.lastMessage }}
            </div>
            <div v-else class="friend-message friend-desc">
              {{ friend.userDesc || '这个人很懒，什么都没留下' }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.friend-list-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

/* 搜索区域 */
.search-section {
  background-color: #ffffff;
  padding: 8px 16px;
  border-bottom: 1px solid #ebedf0;
}

/* 好友列表 */
.friend-list {
  padding: 8px 0;
}

.loading {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
}

/* 好友列表项 */
.friend-items {
  background-color: #ffffff;
}

.friend-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background-color: #ffffff;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
  transition: background-color 0.2s;
}

.friend-item:active {
  background-color: #f5f5f5;
}

.friend-item:last-child {
  border-bottom: none;
}

/* 头像 */
.friend-avatar {
  flex-shrink: 0;
  margin-right: 12px;
}

/* 信息区域 */
.friend-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0; /* 防止文本溢出 */
}

/* 头部（昵称 + 时间） */
.friend-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.friend-name {
  font-size: 15px;
  font-weight: 500;
  color: #323233;
  flex-shrink: 0;
}

.friend-time {
  font-size: 12px;
  color: #969799;
  flex-shrink: 0;
}

/* 消息/简介 */
.friend-message {
  font-size: 13px;
  color: #969799;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.friend-desc {
  font-style: italic;
}
</style>
