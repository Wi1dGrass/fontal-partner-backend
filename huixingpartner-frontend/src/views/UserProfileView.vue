<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showSuccessToast, showLoadingToast, closeToast, showDialog } from 'vant'
import { useUserStore } from '@/stores/user'
import { getUserDetail, applyFriend, type UserDetailVO } from '@/api/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 用户详情数据
const userInfo = ref<UserDetailVO | null>(null)
const loading = ref(false)

// 好友申请备注
const friendRemark = ref('')
const showFriendDialog = ref(false)

// 好友关系判断
const isFriend = ref(false)
const checkingFriendship = ref(false)

// 判断是否是自己的详情页
const isOwnProfile = computed(() => {
  const targetUserId = Number(route.params.id)
  return targetUserId === userStore.userInfo?.id
})

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
      return 'male'
    case 2:
      return 'female'
    default:
      return 'question'
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

// 返回上一页
const onClickLeft = () => {
  router.back()
}

// 点击编辑按钮（自己的详情页）
const onEdit = () => {
  router.push('/user/profile')
}

// 点击发消息按钮
const onMessage = () => {
  const targetUserId = Number(route.params.id)
  router.push(`/chat/user/${targetUserId}`)
}

// 点击加好友按钮
const onAddFriend = () => {
  friendRemark.value = ''
  showFriendDialog.value = true
}

// 检查好友关系（使用 Store 缓存）
const checkFriendship = async () => {
  if (isOwnProfile.value) return

  checkingFriendship.value = true
  try {
    // 加载好友ID列表（仅首次）
    await userStore.loadFriendIds()

    // 判断目标用户是否是好友
    const targetUserId = Number(route.params.id)
    isFriend.value = userStore.isFriend(targetUserId)
  } catch (error) {
    console.error('检查好友关系失败：', error)
    // API调用失败时，默认为不是好友
    isFriend.value = false
  } finally {
    checkingFriendship.value = false
  }
}

// 发送好友申请
const onSubmitFriend = async () => {
  if (!friendRemark.value.trim()) {
    showToast('请输入申请备注')
    return
  }

  try {
    showLoadingToast({
      message: '发送中...',
      forbidClick: true,
      duration: 0
    })

    const targetUserId = Number(route.params.id)
    await applyFriend({
      receiveId: targetUserId,
      remark: friendRemark.value
    })

    closeToast()
    showFriendDialog.value = false

    showSuccessToast('好友申请已发送')
  } catch (error: any) {
    closeToast()
    console.error('发送好友申请失败：', error)
    showToast(error.message || '发送失败，请重试')
  }
}

// 加载用户详情
const loadUserDetail = async () => {
  const userId = Number(route.params.id)

  if (!userId) {
    showToast('用户ID不存在')
    router.back()
    return
  }

  try {
    loading.value = true

    const response = await getUserDetail(userId)

    if (response.data) {
      userInfo.value = response.data
    } else {
      showToast('用户不存在')
      router.back()
    }
  } catch (error: any) {
    console.error('加载用户详情失败：', error)
    showToast(error.message || '加载失败，请重试')
    router.back()
  } finally {
    loading.value = false
  }
}

// 页面挂载时加载数据
onMounted(() => {
  loadUserDetail()
  checkFriendship() // 检查好友关系
})
</script>

<template>
  <div class="user-profile-page">
    <!-- 顶部导航栏 -->
    <van-nav-bar
      :title="isOwnProfile ? '我的资料' : '用户详情'"
      left-text="返回"
      left-arrow
      @click-left="onClickLeft"
      :fixed="true"
      :placeholder="true"
      :safe-area-inset-top="true"
    >
      <template #right>
        <!-- 自己的详情页：显示编辑按钮 -->
        <van-icon
          v-if="isOwnProfile"
          name="edit"
          size="18"
          @click="onEdit"
        />
        <!-- 别人的详情页：显示发消息按钮 -->
        <van-icon
          v-else
          name="chat-o"
          size="18"
          @click="onMessage"
        />
      </template>
    </van-nav-bar>

    <!-- 加载中状态 -->
    <div v-if="loading" class="loading-container">
      <van-loading size="24px" vertical>加载中...</van-loading>
    </div>

    <!-- 用户详情内容 -->
    <div v-else-if="userInfo" class="profile-content">
      <!-- 用户头像区域 -->
      <div class="avatar-section">
        <van-image
          round
          width="80"
          height="80"
          :src="userInfo.userAvatarUrl || 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'"
          class="user-avatar"
        />
        <div class="user-basic">
          <div class="user-name-row">
            <span class="user-name">{{ userInfo.username }}</span>
            <van-icon
              :name="getGenderIcon(userInfo.gender)"
              :color="userInfo.gender === 1 ? '#1989fa' : userInfo.gender === 2 ? '#fb7299' : '#969799'"
              size="16"
            />
          </div>
          <p class="user-id">UID: {{ userInfo.id }}</p>
        </div>
      </div>

      <!-- 详细信息卡片 -->
      <div class="info-section">
        <!-- 个人简介 -->
        <div class="info-item">
          <div class="info-label">
            <van-icon name="description" />
            <span>个人简介</span>
          </div>
          <p class="info-content" :class="{ 'empty-text': !userInfo.profile }">
            {{ userInfo.profile || '这个家伙很懒，什么都没留下~' }}
          </p>
        </div>

        <!-- 联系方式 -->
        <div class="info-item">
          <div class="info-label">
            <van-icon name="phone-o" />
            <span>联系方式</span>
          </div>
          <p class="info-content" :class="{ 'empty-text': !userInfo.contactInfo }">
            {{ userInfo.contactInfo || '暂未填写联系方式' }}
          </p>
        </div>

        <!-- 邮箱 -->
        <div class="info-item">
          <div class="info-label">
            <van-icon name="envelop-o" />
            <span>邮箱</span>
          </div>
          <p class="info-content" :class="{ 'empty-text': !userInfo.email }">
            {{ userInfo.email || '暂未填写邮箱' }}
          </p>
        </div>

        <!-- 标签 -->
        <div class="info-item">
          <div class="info-label">
            <van-icon name="tag-o" />
            <span>标签</span>
          </div>
          <div class="tags-content" v-if="parseTags(userInfo.tags).length > 0">
            <van-tag
              v-for="(tag, index) in parseTags(userInfo.tags)"
              :key="index"
              type="primary"
              size="medium"
              plain
            >
              {{ tag }}
            </van-tag>
          </div>
          <p class="info-content empty-text" v-else>
            这个家伙很懒，还没有添加标签~
          </p>
        </div>
      </div>

      <!-- 底部操作按钮（不是自己的详情页） -->
      <div v-if="!isOwnProfile" class="action-section">
        <!-- 是好友：显示发消息按钮 -->
        <van-button
          v-if="isFriend"
          round
          block
          type="primary"
          color="#FB7299"
          size="large"
          icon="chat-o"
          @click="onMessage"
        >
          发消息
        </van-button>

        <!-- 不是好友：显示加好友按钮 -->
        <van-button
          v-else
          round
          block
          type="primary"
          color="#FB7299"
          size="large"
          icon="friends-o"
          @click="onAddFriend"
        >
          加好友
        </van-button>
      </div>
    </div>

    <!-- 好友申请弹窗 -->
    <van-dialog
      v-model:show="showFriendDialog"
      title="申请好友"
      show-cancel-button
      @confirm="onSubmitFriend"
    >
      <div class="friend-dialog-content">
        <van-field
          v-model="friendRemark"
          type="textarea"
          label="备注"
          placeholder="请输入申请备注（自我介绍、申请理由等）"
          maxlength="100"
          show-word-limit
          rows="3"
        />
      </div>
    </van-dialog>
  </div>
</template>

<style scoped>
.user-profile-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.loading-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
}

.profile-content {
  padding: 16px;
}

/* 头像区域 */
.avatar-section {
  background: white;
  border-radius: 12px;
  padding: 24px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
}

.user-avatar {
  flex-shrink: 0;
}

.user-basic {
  flex: 1;
  min-width: 0;
}

.user-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.user-name {
  font-size: 20px;
  font-weight: bold;
  color: #323233;
}

.user-id {
  font-size: 13px;
  color: #969799;
  margin: 0;
}

/* 信息区域 */
.info-section {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 12px;
}

.info-item {
  padding: 16px;
  border-bottom: 1px solid #f5f5f5;
}

.info-item:last-child {
  border-bottom: none;
}

.info-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: bold;
  color: #323233;
  margin-bottom: 8px;
}

.info-content {
  font-size: 14px;
  color: #646566;
  line-height: 1.6;
  margin: 0;
  padding-left: 22px;
}

/* 空状态文本样式 */
.info-content.empty-text {
  color: #969799;
  font-style: italic;
  font-size: 13px;
}

.tags-content {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding-left: 22px;
}

/* 操作区域 */
.action-section {
  padding: 16px 0;
}

.friend-dialog-content {
  padding: 16px;
}
</style>
