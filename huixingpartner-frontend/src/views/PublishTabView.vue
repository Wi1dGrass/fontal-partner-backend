<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'

const router = useRouter()

const options = [
  { name: '创建队伍', icon: 'friends', path: '/team/create', color: '#FB7299' },
  { name: '发布动态', icon: 'chat', path: '', color: '#07c160' }
]

const onClick = (option: any) => {
  if (option.path) {
    router.push(option.path)
  } else {
    showToast('功能开发中...')
  }
}
</script>

<template>
  <div class="publish-tab">
    <div class="header">
      <h2>快速创建</h2>
      <p>选择一个功能开始</p>
    </div>

    <div class="options">
      <div
        v-for="option in options"
        :key="option.name"
        class="option-card"
        @click="onClick(option)"
      >
        <div class="icon-wrapper" :style="{ backgroundColor: option.color + '20' }">
          <van-icon :name="option.icon" :color="option.color" size="32" />
        </div>
        <span class="option-name">{{ option.name }}</span>
      </div>
    </div>

    <div class="placeholder">
      <van-empty description="更多功能敬请期待" />
    </div>
  </div>
</template>

<style scoped>
.publish-tab {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding: 16px;
}

.header {
  text-align: center;
  padding: 20px 0;
}

.header h2 {
  color: #323233;
  margin-bottom: 8px;
}

.header p {
  color: #969799;
  font-size: 14px;
}

.options {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.option-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 10px;
  background: white;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.option-card:active {
  transform: scale(0.95);
  opacity: 0.8;
}

.icon-wrapper {
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  margin-bottom: 12px;
}

.option-name {
  font-size: 14px;
  color: #323233;
}

.placeholder {
  padding: 40px 0;
}
</style>
