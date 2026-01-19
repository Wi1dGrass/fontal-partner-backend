<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showLoadingToast, closeToast, showSuccessToast } from 'vant'
import { useUserStore } from '@/stores/user'
import { createTeam, type CreateTeamRequest } from '@/api/team'

const router = useRouter()
const userStore = useUserStore()

// 表单数据
const form = ref<CreateTeamRequest>({
  teamName: '',
  teamDesc: '',
  teamAvatarUrl: 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg',
  maxNum: 6,
  expireTime: '',
  teamStatus: 0,
  teamPassword: '',
  announce: ''
})

// 日期时间选择器
const showDatePicker = ref(false)
const selectedDateArray = ref<string[]>([])
const selectedTimeArray = ref<string[]>(['00', '00'])

// 计算默认过期时间（7天后）
const defaultExpireTime = new Date()
defaultExpireTime.setDate(defaultExpireTime.getDate() + 7)

// 格式化日期时间为 yyyy年MM月dd日 HH:mm:ss（后端期望格式）
const formatDateTime = (dateArray: string[], timeArray: string[]) => {
  const [year, month, day] = dateArray
  const [hour, minute] = timeArray
  return `${year}年${month}月${day}日 ${hour}:${minute}:00`
}

// 初始化表单
onMounted(() => {
  // 设置默认过期时间为7天后
  const year = String(defaultExpireTime.getFullYear())
  const month = String(defaultExpireTime.getMonth() + 1).padStart(2, '0')
  const day = String(defaultExpireTime.getDate()).padStart(2, '0')
  selectedDateArray.value = [year, month, day]
  form.value.expireTime = formatDateTime(selectedDateArray.value, selectedTimeArray.value)
})

// 返回上一页
const onClickLeft = () => {
  router.back()
}

// 打开日期选择器
const openDatePicker = () => {
  showDatePicker.value = true
}

// 确认选择日期时间
const onConfirmDateTime = () => {
  form.value.expireTime = formatDateTime(selectedDateArray.value, selectedTimeArray.value)
  showDatePicker.value = false
}

// 取消选择日期时间
const onCancelDateTime = () => {
  showDatePicker.value = false
}

// 表单验证
const validateForm = (): boolean => {
  // 验证队伍名称
  if (!form.value.teamName?.trim()) {
    showToast('队伍名称不能为空')
    return false
  }

  if (form.value.teamName.length > 256) {
    showToast('队伍名称不能超过256个字符')
    return false
  }

  // 验证队伍描述
  if (form.value.teamDesc && form.value.teamDesc.length > 512) {
    showToast('队伍描述不能超过512个字符')
    return false
  }

  // 验证队伍公告
  if (form.value.announce && form.value.announce.length > 512) {
    showToast('队伍公告不能超过512个字符')
    return false
  }

  // 验证最大人数
  if (!form.value.maxNum || form.value.maxNum < 1) {
    showToast('最大人数至少为1人')
    return false
  }

  if (form.value.maxNum > 6) {
    showToast('队伍最多只能有6人')
    return false
  }

  // 验证过期时间
  const expireDate = new Date(form.value.expireTime)
  if (expireDate <= new Date()) {
    showToast('过期时间必须是未来时间')
    return false
  }

  // 验证密码（加密队伍必填）
  if (form.value.teamStatus === 2) {
    if (!form.value.teamPassword?.trim()) {
      showToast('加密队伍必须设置密码')
      return false
    }
  }

  return true
}

// 创建队伍
const handleCreateTeam = async () => {
  // 验证表单
  if (!validateForm()) {
    return
  }

  // 检查登录状态
  if (!userStore.userInfo) {
    showToast('请先登录')
    router.push('/login')
    return
  }

  try {
    showLoadingToast({
      message: '创建中...',
      forbidClick: true,
      duration: 0
    })

    // 构造请求数据
    const requestData: CreateTeamRequest = {
      teamName: form.value.teamName.trim(),
      teamDesc: form.value.teamDesc?.trim() || '',
      teamAvatarUrl: form.value.teamAvatarUrl,
      maxNum: form.value.maxNum,
      expireTime: form.value.expireTime,
      teamStatus: form.value.teamStatus,
      teamPassword: form.value.teamPassword?.trim() || '',
      announce: form.value.announce?.trim() || ''
    }

    // 调用创建队伍 API
    await createTeam(requestData)

    closeToast()
    showSuccessToast('队伍创建成功')

    // 延迟后跳转到"我的队伍"页面
    setTimeout(() => {
      router.replace('/team/my')
    }, 500)
  } catch (error: any) {
    closeToast()
    console.error('创建队伍失败：', error)
    showToast(error.message || '创建失败，请重试')
  }
}

// 监听队伍状态变化，非加密队伍清空密码
const handleStatusChange = () => {
  if (form.value.teamStatus !== 2) {
    form.value.teamPassword = ''
  }
}

// 计算属性：是否需要密码
const needPassword = computed(() => {
  return form.value.teamStatus === 2
})
</script>

<template>
  <div class="create-team-page">
    <!-- 顶部导航栏 -->
    <van-nav-bar
      title="创建队伍"
      left-text="返回"
      left-arrow
      @click-left="onClickLeft"
    />

    <!-- 表单区域 -->
    <div class="form-container">
      <!-- 队伍头像 -->
      <div class="avatar-section">
        <van-image
          round
          width="80"
          height="80"
          :src="form.teamAvatarUrl"
          fit="cover"
        />
        <div class="avatar-tip">默认头像</div>
      </div>

      <!-- 队伍名称 -->
      <van-field
        v-model="form.teamName"
        label="队伍名称"
        placeholder="请输入队伍名称"
        maxlength="256"
        show-word-limit
        required
      />

      <!-- 队伍描述 -->
      <van-field
        v-model="form.teamDesc"
        type="textarea"
        label="队伍描述"
        placeholder="请输入队伍描述（可选）"
        maxlength="512"
        show-word-limit
        rows="3"
      />

      <!-- 队伍类型 -->
      <van-field name="teamStatus" label="队伍类型" required>
        <template #input>
          <van-radio-group v-model="form.teamStatus" direction="horizontal" @change="handleStatusChange">
            <van-radio :name="0">公开</van-radio>
            <van-radio :name="1">私有</van-radio>
            <van-radio :name="2">加密</van-radio>
          </van-radio-group>
        </template>
      </van-field>

      <!-- 最大人数 -->
      <van-field
        v-model="form.maxNum"
        type="number"
        label="最大人数"
        placeholder="请输入最大人数"
        :min="1"
        :max="6"
        required
      >
        <template #button>
          <span style="color: #999; font-size: 14px;">人</span>
        </template>
      </van-field>

      <!-- 过期时间 -->
      <van-field
        :model-value="form.expireTime"
        label="过期时间"
        placeholder="请选择过期时间"
        readonly
        is-link
        required
        @click="openDatePicker"
      />

      <!-- 队伍密码（加密队伍显示） -->
      <van-field
        v-if="needPassword"
        v-model="form.teamPassword"
        type="password"
        label="队伍密码"
        placeholder="加密队伍必须设置密码"
        required
      />

      <!-- 队伍公告 -->
      <van-field
        v-model="form.announce"
        type="textarea"
        label="队伍公告"
        placeholder="请输入队伍公告（可选）"
        maxlength="512"
        show-word-limit
        rows="3"
      />

      <!-- 提示信息 -->
      <div class="tips-section">
        <van-icon name="info-o" />
        <span>创建成功后，您将成为该队伍的队长</span>
      </div>

      <!-- 创建按钮 -->
      <div class="submit-section">
        <van-button
          type="primary"
          block
          round
          size="large"
          color="linear-gradient(to right, #ff6034, #ee0a24)"
          @click="handleCreateTeam"
        >
          创建队伍
        </van-button>
      </div>
    </div>

    <!-- 日期时间选择器弹窗 -->
    <van-popup
      v-model:show="showDatePicker"
      position="bottom"
      round
    >
      <van-date-picker
        v-model="selectedDateArray"
        title="选择日期"
        :min-date="new Date()"
      />
      <van-time-picker
        v-model="selectedTimeArray"
        title="选择时间"
      />
      <div style="padding: 16px">
        <van-button
          type="primary"
          block
          @click="onConfirmDateTime"
        >
          确认
        </van-button>
        <van-button
          block
          @click="onCancelDateTime"
        >
          取消
        </van-button>
      </div>
    </van-popup>
  </div>
</template>

<style scoped>
.create-team-page {
  min-height: 100vh;
  background: #f7f8fa;
}

.form-container {
  padding: 16px;
}

/* 队伍头像区域 */
.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px 0 16px;
  margin-bottom: 16px;
  background: white;
  border-radius: 8px;
}

.avatar-tip {
  margin-top: 8px;
  font-size: 13px;
  color: #999;
}

/* 提示信息 */
.tips-section {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: #fffbe8;
  border-radius: 4px;
  font-size: 13px;
  color: #ed6a0c;
  margin: 16px 0;
}

/* 提交按钮区域 */
.submit-section {
  margin-top: 24px;
}
</style>
