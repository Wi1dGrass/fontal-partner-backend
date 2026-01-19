<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showSuccessToast, showLoadingToast, closeToast, showDialog, showConfirmDialog } from 'vant'
import { useUserStore } from '@/stores/user'
import { userLogout, updatePassword } from '@/api/user'

const router = useRouter()
const userStore = useUserStore()

// 修改密码弹窗
const showPasswordDialog = ref(false)

// 密码修改表单
const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  checkPassword: ''
})

// 返回上一页
const onClickLeft = () => {
  router.back()
}

// 点击修改密码
const onChangePassword = () => {
  passwordForm.value = {
    oldPassword: '',
    newPassword: '',
    checkPassword: ''
  }
  showPasswordDialog.value = true
}

// 保存密码
const onSavePassword = async () => {
  // 表单验证
  if (!passwordForm.value.oldPassword) {
    showToast('请输入原密码')
    return
  }
  if (!passwordForm.value.newPassword) {
    showToast('请输入新密码')
    return
  }
  if (!passwordForm.value.checkPassword) {
    showToast('请再次输入新密码')
    return
  }

  // 密码格式验证（6-20位字母或数字）
  const passwordRegex = /^[a-zA-Z0-9]{6,20}$/
  if (!passwordRegex.test(passwordForm.value.newPassword)) {
    showToast('新密码必须为6-20位字母或数字')
    return
  }

  // 确认密码验证
  if (passwordForm.value.newPassword !== passwordForm.value.checkPassword) {
    showToast('两次输入的新密码不一致')
    return
  }

  try {
    showLoadingToast({
      message: '修改中...',
      forbidClick: true,
      duration: 0
    })

    await updatePassword(passwordForm.value)

    closeToast()
    showPasswordDialog.value = false

    // 修改成功后退出登录
    await showDialog({
      title: '密码修改成功',
      message: '请使用新密码重新登录',
      confirmButtonText: '确定'
    })

    // 清除用户信息并跳转到登录页
    userStore.clearUserInfo()
    router.replace('/login')
  } catch (error: any) {
    closeToast()
    console.error('修改密码失败：', error)
    showToast(error.message || '修改失败，请重试')
  }
}

// 退出登录
const onLogout = async () => {
  try {
    // 显示确认提示框
    await showConfirmDialog({
      title: '退出登录',
      message: '确定要退出登录吗？',
      confirmButtonText: '确认退出',
      confirmButtonColor: '#ee0a24',
      cancelButtonText: '取消'
    })

    // 用户确认后，执行退出登录
    showLoadingToast({
      message: '退出中...',
      forbidClick: true,
      duration: 0
    })

    // 调用退出登录 API
    await userLogout()

    closeToast()

    // 清除本地用户信息
    userStore.clearUserInfo()

    showSuccessToast('已退出登录')

    // 延迟后跳转到"我的"页面
    setTimeout(() => {
      router.replace('/layout/profile')
    }, 500)
  } catch (error: any) {
    // 用户取消或出错
    if (error === 'cancel') {
      // 用户点击了取消，不做任何操作
      return
    }

    closeToast()
    console.error('退出登录失败：', error)

    // 即使 API 调用失败，也清除本地用户信息
    userStore.clearUserInfo()
    showSuccessToast('已退出登录')

    // 延迟后跳转到"我的"页面
    setTimeout(() => {
      router.replace('/layout/profile')
    }, 500)
  }
}
</script>

<template>
  <div class="settings-page">
    <!-- 顶部导航栏 -->
    <van-nav-bar
      title="设置"
      left-text="返回"
      left-arrow
      @click-left="onClickLeft"
      fixed
      placeholder
      safe-area-inset-top
    />

    <div class="settings-content">
      <!-- 功能菜单 -->
      <div class="menu-section">
        <van-cell-group inset>
          <van-cell
            title="修改密码"
            icon="lock"
            is-link
            @click="onChangePassword"
          />
          <van-cell
            title="退出登录"
            icon="exchange"
            is-link
            @click="onLogout"
          />
        </van-cell-group>
      </div>
    </div>

    <!-- 修改密码弹窗 -->
    <van-dialog
      v-model:show="showPasswordDialog"
      title="修改密码"
      show-cancel-button
      @confirm="onSavePassword"
    >
      <div class="password-dialog-content">
        <van-field
          v-model="passwordForm.oldPassword"
          type="password"
          label="原密码"
          placeholder="请输入原密码"
          maxlength="20"
        />
        <van-field
          v-model="passwordForm.newPassword"
          type="password"
          label="新密码"
          placeholder="请输入新密码（6-20位字母或数字）"
          maxlength="20"
        />
        <van-field
          v-model="passwordForm.checkPassword"
          type="password"
          label="确认密码"
          placeholder="请再次输入新密码"
          maxlength="20"
        />
      </div>
    </van-dialog>
  </div>
</template>

<style scoped>
.settings-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.settings-content {
  padding: 16px;
}

.menu-section {
  margin-bottom: 12px;
}

.password-dialog-content {
  padding: 16px;
}
</style>
