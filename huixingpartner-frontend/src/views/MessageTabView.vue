<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { useMessageStore } from '@/stores/message'

const router = useRouter()
const messageStore = useMessageStore()

const loading = ref(true)

// 消息类型配置
const messageTypes = ref([
  {
    id: 'friend-received',
    name: '好友申请',
    icon: 'friends-o',
    route: '/friend/request',
    count: 0
  },
  {
    id: 'my-applications',
    name: '我的申请',
    icon: 'send-gift-o',
    route: '/my-applications',
    count: 0,
    description: '我发送的好友申请和队伍申请'
  },
  {
    id: 'team-invitations',
    name: '队伍邀请',
    icon: 'gift-o',
    route: '/team/invitations',
    count: 0
  },
  {
    id: 'team-received',
    name: '队伍审批',
    icon: 'manager-o',
    route: '/team/applications/received',
    count: 0
  }
])

// 加载未读数量
const loadUnreadCounts = async () => {
  try {
    loading.value = true

    // 并行加载所有消息类型的未读数量
    await Promise.all([
      messageStore.loadCategory('friend-received'),
      messageStore.loadCategory('friend-sent')
      // 队伍相关的待后端API实现
    ])

    // 更新未读数量
    messageTypes.value[0].count = messageStore.categories[0].count // 好友申请
    messageTypes.value[1].count = 0 // 我的申请不显示红点
    messageTypes.value[2].count = 0 // 队伍邀请（待实现）
    messageTypes.value[3].count = messageStore.categories[3].count // 队伍审批
  } catch (error) {
    console.error('加载消息数量失败：', error)
  } finally {
    loading.value = false
  }
}

// 点击消息类型
const onClick = (item: any) => {
  if (item.route) {
    router.push(item.route)
  } else {
    showToast('功能开发中，敬请期待...')
  }
}

// 页面挂载时加载未读数量
onMounted(() => {
  loadUnreadCounts()
})
</script>

<template>
  <div class="message-tab">
    <!-- 通知区域 -->
    <div class="notice-section">
      <div class="section-title">
        <h3>通知</h3>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="loading-wrapper">
        <van-loading size="24px">加载中...</van-loading>
      </div>

      <!-- 消息类型列表 -->
      <div v-else class="message-types">
        <div
          v-for="item in messageTypes"
          :key="item.id"
          class="message-type-item"
          @click="onClick(item)"
        >
          <div class="type-icon">
            <van-icon :name="item.icon" size="24" color="#FB7299" />
            <van-badge
              v-if="item.count > 0"
              :content="item.count > 99 ? '99+' : item.count"
              style="position: absolute; top: -4px; right: -4px;"
            />
          </div>
          <div class="type-content">
            <div class="type-info">
              <span class="type-name">{{ item.name }}</span>
              <span v-if="item.description" class="type-description">{{ item.description }}</span>
            </div>
            <van-icon name="arrow" size="16" color="#969799" />
          </div>
        </div>
      </div>
    </div>

    <!-- 聊天列表区域 -->
    <div class="chat-section">
      <div class="section-title">
        <h3>聊天</h3>
      </div>
      <div class="placeholder">
        <van-empty description="暂无聊天记录" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.message-tab {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.notice-section {
  background: white;
  margin-bottom: 12px;
}

.section-title {
  padding: 16px;
  border-bottom: 1px solid #ebedf0;
}

.section-title h3 {
  font-size: 16px;
  color: #323233;
  margin: 0;
}

.loading-wrapper {
  padding: 40px 0;
  display: flex;
  justify-content: center;
  align-items: center;
}

.message-types {
  padding: 0 16px;
}

.message-type-item {
  display: flex;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
  transition: background-color 0.2s;
}

.message-type-item:active {
  background-color: #f5f5f5;
}

.message-type-item:last-child {
  border-bottom: none;
}

.type-icon {
  position: relative;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  border-radius: 50%;
  margin-right: 12px;
}

.type-content {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.type-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.type-name {
  font-size: 15px;
  color: #323233;
  font-weight: 500;
}

.type-description {
  font-size: 12px;
  color: #969799;
}

.chat-section {
  background: white;
}

.placeholder {
  padding: 40px 16px;
}
</style>
