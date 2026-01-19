<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showLoadingToast, closeToast } from 'vant'
import { userRegister } from '@/api/user'

const router = useRouter()

// 表单数据
const registerForm = ref({
  username: '',
  userAccount: '',
  userPassword: '',
  checkPassword: ''
})

// 表单验证规则
const rules = {
  username: [
    { required: true, message: '请输入用户名' }
  ],
  userAccount: [
    { required: true, message: '请输入账号' }
  ],
  userPassword: [
    { required: true, message: '请输入密码' },
    { pattern: /^[a-zA-Z0-9]{6,20}$/, message: '密码为6-20位数字或字母' }
  ],
  checkPassword: [
    { required: true, message: '请确认密码' },
    {
      validator: (value: string) => {
        return value === registerForm.value.userPassword
      },
      message: '两次密码输入不一致'
    }
  ]
}

// 表单引用
const formRef = ref()

// 提交注册
const onSubmit = async () => {
  try {
    await formRef.value?.validate()

    // 表单验证通过
    showLoadingToast({
      message: '注册中...',
      forbidClick: true,
      duration: 0
    })

    // 调用注册 API
    const response = await userRegister(registerForm.value)

    closeToast()

    // 注册成功返回用户ID
    if (response.code === 0 && response.data !== null) {
      showToast('注册成功')

      // 跳转到登录页面，并传递账号和密码
      setTimeout(() => {
        router.replace({
          path: '/login',
          query: {
            account: registerForm.value.userAccount,
            password: registerForm.value.userPassword
          }
        })
      }, 500)
    }
  } catch (error: any) {
    closeToast()
    console.error('注册失败：', error)
    showToast(error.message || '注册失败，请重试')
  }
}

// 返回登录页面
const goToLogin = () => {
  router.back()
}

// 点击顶部返回按钮
const onClickLeft = () => {
  router.back()
}
</script>

<template>
  <div class="register-page">
    <!-- 顶部导航栏 -->
    <van-nav-bar
      title="注册"
      left-text="返回"
      left-arrow
      @click-left="onClickLeft"
      fixed
      placeholder
      safe-area-inset-top
      class="register-navbar"
    />

    <div class="register-container">
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
        <h1 class="title">注册账号</h1>
        <p class="subtitle">加入惠星伙伴匹配</p>
      </div>

      <!-- 注册表单 -->
      <van-form @submit="onSubmit" ref="formRef">
        <van-cell-group inset>
          <van-field
            v-model="registerForm.username"
            name="username"
            label="用户名"
            placeholder="请输入用户名"
            :rules="rules.username"
            clearable
          />
          <van-field
            v-model="registerForm.userAccount"
            name="userAccount"
            label="账号"
            placeholder="请输入账号"
            :rules="rules.userAccount"
            clearable
          />
          <van-field
            v-model="registerForm.userPassword"
            type="password"
            name="userPassword"
            label="密码"
            placeholder="请输入密码（6-20位数字或字母）"
            :rules="rules.userPassword"
            clearable
          />
          <van-field
            v-model="registerForm.checkPassword"
            type="password"
            name="checkPassword"
            label="确认密码"
            placeholder="请再次输入密码"
            :rules="rules.checkPassword"
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
            注 册
          </van-button>
          <van-button
            round
            block
            plain
            type="primary"
            color="#FB7299"
            size="large"
            @click="goToLogin"
          >
            返回登录
          </van-button>
        </div>
      </van-form>

      <!-- 用户协议 -->
      <div class="agreement">
        <span class="text">注册即表示同意</span>
        <span class="link">《用户协议》</span>
        <span class="text">和</span>
        <span class="link">《隐私政策》</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.register-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

/* 顶部导航栏渐变色 */
.register-navbar :deep(.van-nav-bar) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.register-navbar :deep(.van-nav-bar__title) {
  color: white;
}

.register-navbar :deep(.van-nav-bar__text) {
  color: white;
}

.register-navbar :deep(.van-icon) {
  color: white;
}

.register-container {
  width: 100%;
  max-width: 400px;
}

.header {
  text-align: center;
  margin-bottom: 30px;
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

.button-group .van-button {
  margin-bottom: 12px;
}

.button-group .van-button:last-child {
  margin-bottom: 0;
}

.agreement {
  text-align: center;
  margin-top: 20px;
  padding: 0 16px;
  font-size: 12px;
}

.agreement .text {
  color: rgba(255, 255, 255, 0.7);
}

.agreement .link {
  color: white;
  text-decoration: underline;
}
</style>
