<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast, showLoadingToast, closeToast } from 'vant'
import { userLogin, type LoginResponse } from '@/api/user'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 表单数据
const loginForm = ref({
  userAccount: '',
  userPassword: ''
})

// 表单验证规则
const rules = {
  userAccount: [
    { required: true, message: '请输入账号' }
  ],
  userPassword: [
    { required: true, message: '请输入密码' }
  ]
}

// 表单引用
const formRef = ref()

// 组件挂载时，检查是否有从注册页传递过来的账号和密码
onMounted(() => {
  if (route.query.account) {
    loginForm.value.userAccount = route.query.account as string
  }
  if (route.query.password) {
    loginForm.value.userPassword = route.query.password as string
  }
})

// 提交登录
const onSubmit = async () => {
  // 表单验证
  if (!loginForm.value.userAccount) {
    showToast('请输入账号')
    return
  }
  if (!loginForm.value.userPassword) {
    showToast('请输入密码')
    return
  }

  try {
    showLoadingToast({
      message: '登录中...',
      forbidClick: true,
      duration: 0
    })

    // 调用登录 API
    const response = await userLogin(loginForm.value)

    closeToast()

    // 登录成功，保存用户信息
    if (response.data) {
      // 转换后端返回的数据格式为 UserInfo 类型
      const userInfo = {
        id: response.data.id,
        username: response.data.username,
        userAccount: response.data.userAccount,
        avatarUrl: response.data.userAvatarUrl || '',
        userDesc: response.data.userDesc || '',
        gender: response.data.gender,
        email: response.data.email || '',
        contactInfo: response.data.contactInfo || '',
        userRole: response.data.userRole,
        userStatus: response.data.userStatus,
        tags: response.data.tags ? JSON.parse(response.data.tags) : [],
        createTime: response.data.createTime
      }

      userStore.setUserInfo(userInfo)

      showToast('登录成功')

      // 跳转到"我的"页面
      setTimeout(() => {
        router.replace('/layout/profile')
      }, 500)
    }
  } catch (error: any) {
    closeToast()
    console.error('登录失败：', error)
    showToast(error.message || '登录失败，请检查账号密码')
  }
}

// 跳转到注册页面
const goToRegister = () => {
  router.push('/register')
}

// 返回到"我的"页面
const onClickLeft = () => {
  router.back()
}
</script>

<template>
  <div class="login-page">
    <!-- 顶部导航栏 -->
    <van-nav-bar
      title="登录"
      left-text="返回"
      left-arrow
      @click-left="onClickLeft"
      fixed
      placeholder
      safe-area-inset-top
      class="login-navbar"
      z-index="9999"
    />

    <div class="login-container">
      <!-- Logo 和标题 -->
      <div class="header">
        <div class="logo">
          <van-image
            width="80"
            height="80"
            round
            src="https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg"
          />
        </div>
        <h1 class="title">惠星伙伴匹配</h1>
        <p class="subtitle">找到志同道合的伙伴</p>
      </div>

      <!-- 登录表单 -->
      <van-form @submit="onSubmit" ref="formRef">
        <van-cell-group inset>
          <van-field
            v-model="loginForm.userAccount"
            name="userAccount"
            label="账号"
            placeholder="请输入账号"
            :rules="rules.userAccount"
            clearable
          />
          <van-field
            v-model="loginForm.userPassword"
            type="password"
            name="userPassword"
            label="密码"
            placeholder="请输入密码"
            :rules="rules.userPassword"
            clearable
          />
        </van-cell-group>

        <div class="button-group">
          <van-button
            round
            block
            type="primary"
            native-type="submit"
            color="#FB7299"
            size="large"
          >
            登 录
          </van-button>
        </div>
      </van-form>

      <!-- 注册链接 -->
      <div class="register-link">
        <span class="text">还没有账号？</span>
        <span class="link" @click="goToRegister">注册账号</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

/* 顶部导航栏渐变色 */
.login-navbar :deep(.van-nav-bar) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-navbar :deep(.van-nav-bar__title) {
  color: white;
}

.login-navbar :deep(.van-nav-bar__text) {
  color: white;
}

.login-navbar :deep(.van-icon) {
  color: white;
}

.login-container {
  width: 100%;
  max-width: 400px;
}

.header {
  text-align: center;
  margin-bottom: 40px;
}

.logo {
  margin-bottom: 20px;
}

.title {
  font-size: 28px;
  font-weight: bold;
  color: white;
  margin: 0 0 10px 0;
}

.subtitle {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  margin: 0;
}

.button-group {
  padding: 20px 16px;
}

.register-link {
  text-align: center;
  margin-top: 20px;
}

.register-link .text {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

.register-link .link {
  font-size: 14px;
  color: white;
  font-weight: bold;
  text-decoration: underline;
  cursor: pointer;
}
</style>
