<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const goToLogin = () => {
  router.push('/login')
}
</script>

<template>
  <div class="home">
    <div class="header">
      <h1>惠星伙伴匹配系统</h1>
      <p>Vue 3 + TypeScript + Vite + Vant 4</p>
    </div>

    <div class="content">
      <div class="card">
        <h3>✅ 项目初始化完成</h3>
        <ul>
          <li>Vue 3 + TypeScript</li>
          <li>Vite 构建工具</li>
          <li>Vant 4 移动端 UI 库（主题色 #FB7299）</li>
          <li>Vue Router 4 路由管理</li>
          <li>Pinia 状态管理</li>
          <li>Axios 请求封装</li>
          <li>SCSS 样式预处理</li>
        </ul>
      </div>

      <button class="btn-primary" @click="goToLogin">
        前往登录页
      </button>
    </div>
  </div>
</template>

<style scoped>
.home {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.header {
  text-align: center;
  color: white;
  padding: 40px 0;
}

.header h1 {
  font-size: 28px;
  margin-bottom: 10px;
}

.header p {
  font-size: 14px;
  opacity: 0.9;
}

.content {
  max-width: 400px;
  margin: 0 auto;
}

.card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.card h3 {
  color: #FB7299;
  margin-bottom: 15px;
  font-size: 18px;
}

.card ul {
  list-style: none;
  padding: 0;
}

.card li {
  padding: 8px 0;
  color: #333;
  font-size: 14px;
  display: flex;
  align-items: center;
}

.card li::before {
  content: "✓";
  color: #07c160;
  font-weight: bold;
  margin-right: 10px;
}

.btn-primary {
  width: 100%;
  padding: 14px;
  background: #FB7299;
  color: white;
  border: none;
  border-radius: 24px;
  font-size: 16px;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-primary:hover {
  background: #e85c84;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(251, 114, 153, 0.4);
}
</style>
