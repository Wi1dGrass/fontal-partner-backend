<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import type { Message, ChatType } from '@/types/chat'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 获取路由参数
const chatType = computed(() => route.params.type as ChatType)
const chatId = computed(() => route.params.id as string)

// 用户信息
const userInfo = computed(() => userStore.userInfo)

// 聊天信息
const chatInfo = ref({
  name: '',
  avatar: 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg',
  memberCount: 0
})

// 消息列表
const messages = ref<Message[]>([])

// 输入框内容
const inputText = ref('')

// 消息列表容器引用
const messageListRef = ref<HTMLElement>()

// 返回上一页
const onClickLeft = () => {
  router.back()
}

// 生成chatId
const generateChatId = () => {
  return `${chatType.value}_${chatId.value}`
}

// 初始化聊天信息
const initChatInfo = () => {
  const type = chatType.value
  const id = chatId.value

  if (type === 'user') {
    // 好友聊天
    chatInfo.value = {
      name: '好友', // 实际应该从API获取
      avatar: 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg',
      memberCount: 0
    }
  } else if (type === 'team') {
    // 队伍聊天
    chatInfo.value = {
      name: 'Java学习小组', // 实际应该从API获取
      avatar: 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg',
      memberCount: 6
    }
  }
}

// 创建模拟消息数据
const createMockMessages = (): Message[] => {
  const now = Date.now()
  const myUserId = userInfo.value?.id || 1

  return [
    {
      id: 1,
      chatId: generateChatId(),
      senderId: 2,
      senderName: '张三',
      senderAvatar: 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg',
      senderRole: 1, // 队长
      content: '大家好！欢迎加入Java学习小组',
      messageType: 'text',
      isSelf: false,
      timestamp: now - 1000 * 60 * 30 // 30分钟前
    },
    {
      id: 2,
      chatId: generateChatId(),
      senderId: 3,
      senderName: '李四',
      senderAvatar: 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg',
      senderRole: 0, // 成员
      content: '很高兴认识大家！',
      messageType: 'text',
      isSelf: false,
      timestamp: now - 1000 * 60 * 25 // 25分钟前
    },
    {
      id: 3,
      chatId: generateChatId(),
      senderId: myUserId,
      senderName: userInfo.value?.username || '我',
      senderAvatar: userInfo.value?.userAvatarUrl || 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg',
      content: '你好，请问我们这周要学什么内容？',
      messageType: 'text',
      isSelf: true,
      timestamp: now - 1000 * 60 * 5 // 5分钟前
    },
    {
      id: 4,
      chatId: generateChatId(),
      senderId: 2,
      senderName: '张三',
      senderAvatar: 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg',
      senderRole: 1,
      content: '这周我们学习Spring Boot实战，会做一个完整的项目',
      messageType: 'text',
      isSelf: false,
      timestamp: now - 1000 * 60 * 2 // 2分钟前
    }
  ]
}

// 格式化时间
const formatTime = (timestamp: number) => {
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  // 今天的时间，只显示 HH:mm
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const messageDate = new Date(date.getFullYear(), date.getMonth(), date.getDate())

  if (messageDate.getTime() === today.getTime()) {
    // 今天
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    return `${hours}:${minutes}`
  } else {
    // 更早，显示 MM-DD HH:mm
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    return `${month}-${day} ${hours}:${minutes}`
  }
}

// 判断是否显示时间戳（两个消息间隔超过5分钟显示一次）
const shouldShowTime = (index: number) => {
  if (index === 0) return true
  const currentMsg = messages.value[index]
  const prevMsg = messages.value[index - 1]
  const diff = currentMsg.timestamp - prevMsg.timestamp
  return diff > 5 * 60 * 1000 // 5分钟
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

// 发送消息
const onSend = () => {
  const content = inputText.value.trim()
  if (!content) {
    return
  }

  const myUserId = userInfo.value?.id || 1

  // 创建新消息
  const newMessage: Message = {
    id: Date.now(),
    chatId: generateChatId(),
    senderId: myUserId,
    senderName: userInfo.value?.username || '我',
    senderAvatar: userInfo.value?.userAvatarUrl || 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg',
    content,
    messageType: 'text',
    isSelf: true,
    timestamp: Date.now(),
    status: 'success' // 暂时直接设为成功，等对接API时改为发送状态
  }

  // 添加到消息列表
  messages.value.push(newMessage)

  // 清空输入框
  inputText.value = ''

  // 滚动到底部
  scrollToBottom()

  // TODO: 调用API发送消息
  // sendMessage({ chatId: generateChatId(), content, messageType: 'text' })
}

// 计算属性：导航栏标题
const navTitle = computed(() => {
  if (chatType.value === 'team' && chatInfo.value.memberCount > 0) {
    return `${chatInfo.value.name} (${chatInfo.value.memberCount}人)`
  }
  return chatInfo.value.name
})

// 组件挂载
onMounted(() => {
  initChatInfo()
  messages.value = createMockMessages()
  scrollToBottom()
})
</script>

<template>
  <div class="chat-detail-page">
    <!-- 顶部导航栏 -->
    <van-nav-bar
      :title="navTitle"
      left-text="返回"
      left-arrow
      @click-left="onClickLeft"
      fixed
      placeholder
      safe-area-inset-top
    />

    <!-- 消息列表 -->
    <div class="message-list" ref="messageListRef">
      <div
        v-for="(msg, index) in messages"
        :key="msg.id"
        class="message-item"
      >
        <!-- 时间戳（独立行，居中显示） -->
        <div v-if="shouldShowTime(index)" class="message-time">
          {{ formatTime(msg.timestamp) }}
        </div>

        <!-- 消息内容（左对齐或右对齐） -->
        <div :class="['message-content-wrapper', msg.isSelf ? 'self' : 'other']">
          <!-- 头像 -->
          <van-image
            round
            width="40"
            height="40"
            :src="msg.senderAvatar || 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'"
            :class="['avatar', msg.isSelf ? 'my-avatar' : 'other-avatar']"
          />

          <!-- 消息气泡区域 -->
          <div class="message-bubble-wrapper">
            <!-- 发送者信息（仅队伍聊天且对方消息时显示） -->
            <div
              v-if="chatType === 'team' && !msg.isSelf"
              class="sender-info"
            >
              <span class="sender-name">{{ msg.senderName }}</span>
              <van-tag
                v-if="msg.senderRole !== undefined"
                :type="msg.senderRole === 1 ? 'danger' : 'default'"
                size="mini"
              >
                {{ msg.senderRole === 1 ? '队长' : '成员' }}
              </van-tag>
            </div>

            <!-- 消息气泡 -->
            <div :class="['message-bubble', msg.isSelf ? 'self' : 'other']">
              {{ msg.content }}
            </div>

            <!-- 发送状态（仅自己的消息） -->
            <div v-if="msg.isSelf && msg.status" class="message-status">
              <van-icon v-if="msg.status === 'sending'" name="loading" />
              <van-icon v-else-if="msg.status === 'success'" name="success" color="#07c160" />
              <van-icon v-else-if="msg.status === 'failed'" name="close" color="#ee0a24" />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部输入区域 -->
    <div class="input-area">
      <van-field
        v-model="inputText"
        placeholder="请输入消息..."
        :border="false"
        @keyup.enter="onSend"
        class="input-field"
      />
      <van-button
        type="primary"
        size="small"
        :disabled="!inputText.trim()"
        @click="onSend"
        class="send-button"
      >
        发送
      </van-button>
    </div>
  </div>
</template>

<style scoped>
.chat-detail-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  display: flex;
  flex-direction: column;
}

/* 消息列表 */
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background-color: #f5f5f5;
}

/* 消息项 */
.message-item {
  margin-bottom: 16px;
}

/* 时间戳 */
.message-time {
  text-align: center;
  font-size: 12px;
  color: #c8c9cc;
  margin-bottom: 12px;
}

/* 消息内容包装器 */
.message-content-wrapper {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  width: 100%;
}

.message-content-wrapper.other {
  /* 对方消息：整体靠左，头像在前 */
  justify-content: flex-start;
  flex-direction: row;
}

.message-content-wrapper.self {
  /* 我的消息：整体靠右，气泡在前 */
  justify-content: flex-end;
  flex-direction: row;
}

/* 发送者信息 */
.sender-info {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #969799;
  margin-bottom: 2px;
}

/* 头像样式 */
.avatar {
  flex-shrink: 0; /* 头像不缩放 */
  width: 40px !important;
  height: 40px !important;
}

/* 头像顺序控制 */
.other-avatar {
  order: 0; /* 对方头像在前 */
}

.my-avatar {
  order: 2; /* 我的头像在后 */
}

/* 消息气泡包装器 */
.message-bubble-wrapper {
  display: flex;
  flex-direction: column;
  gap: 4px;
  order: 1; /* 气泡在中间 */
  max-width: calc(100vw - 32px - 40px - 8px); /* 屏幕宽度 - 内边距 - 头像 - gap */
}

.message-content-wrapper.self .message-bubble-wrapper {
  align-items: flex-end;
}

.message-content-wrapper.other .message-bubble-wrapper {
  align-items: flex-start;
}

.sender-name {
  font-size: 12px;
  color: #323233;
}

/* 消息气泡 */
.message-bubble {
  max-width: 260px; /* 限制气泡最大宽度 */
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 15px;
  line-height: 1.5;
  word-wrap: break-word;
  word-break: break-all;
}

.message-bubble.other {
  background-color: #ffffff;
  color: #323233;
}

.message-bubble.self {
  background-color: #95ec69; /* QQ绿色 */
  color: #000000;
}

/* 发送状态 */
.message-status {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: #969799;
  margin-top: 2px;
}

/* 底部输入区域 */
.input-area {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  padding-bottom: calc(12px + env(safe-area-inset-bottom));
  background-color: #ffffff;
  border-top: 1px solid #ebedf0;
}

.input-field {
  flex: 1;
  background-color: #f5f5f5;
  border-radius: 20px;
  padding: 8px 16px;
}

.input-field :deep(.van-field__control) {
  font-size: 15px;
}

.send-button {
  flex-shrink: 0;
  border-radius: 20px;
  padding: 0 20px;
}
</style>
