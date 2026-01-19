<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { tabbarConfig, getActiveTabIndex } from '@/config/tabbar'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const active = ref(0)

// 监听路由变化，更新激活的 Tab
watch(
  () => route.name,
  (newRouteName) => {
    if (newRouteName && typeof newRouteName === 'string') {
      active.value = getActiveTabIndex(newRouteName)
    }
  },
  { immediate: true }
)

// 切换 Tab
const onChange = (index: number) => {
  const tab = tabbarConfig[index]
  if (tab) {
    router.push({ name: tab.routeName })
  }
}

// 获取页面标题
const pageTitle = computed(() => {
  return route.meta?.title || '惠星伙伴匹配'
})

// 检查是否显示设置按钮（仅"我的"页面显示）
const showSettingButton = computed(() => {
  return route.name === 'LayoutProfile' && userStore.isLoggedIn
})

// 检查是否显示搜索按钮（仅首页显示）
const showSearchButton = computed(() => {
  return route.name === 'LayoutHome'
})

// 左侧按钮点击（返回）
const onClickLeft = () => {
  // 如果有历史记录，返回上一页
  if (window.history.length > 1) {
    router.back()
  }
}

// 点击设置按钮 - 跳转到设置页面
const onSettingClick = () => {
  router.push('/settings')
}

// 点击搜索按钮 - 跳转到搜索页面
const onSearchClick = () => {
  router.push('/search')
}
</script>

<template>
  <div class="layout">
    <!-- 顶部导航栏 -->
    <van-nav-bar
      v-if="route.meta?.showNavbar !== false"
      :title="pageTitle"
      left-text="返回"
      left-arrow
      @click-left="onClickLeft"
      fixed
      placeholder
      safe-area-inset-top
    >
      <template #right>
        <van-icon
          v-if="showSearchButton"
          name="search"
          size="18"
          @click="onSearchClick"
        />
        <van-icon
          v-if="showSettingButton"
          name="setting-o"
          size="18"
          @click="onSettingClick"
        />
      </template>
    </van-nav-bar>

    <!-- 主内容区 -->
    <div class="layout-content">
      <router-view v-slot="{ Component }">
        <keep-alive>
          <component :is="Component" v-if="$route.meta.keepAlive" :key="$route.fullPath" />
        </keep-alive>
        <component :is="Component" v-if="!$route.meta.keepAlive" :key="$route.fullPath" />
      </router-view>
    </div>

    <!-- 底部导航栏 -->
    <van-tabbar
      v-model="active"
      @change="onChange"
      active-color="#FB7299"
      inactive-color="#646566"
      safe-area-inset-bottom
      fixed
      placeholder
    >
      <van-tabbar-item
        v-for="tab in tabbarConfig"
        :key="tab.name"
        :icon="tab.icon"
      >
        {{ tab.label }}
      </van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<style scoped>
.layout {
  position: relative;
  min-height: 100vh;
  background-color: #f5f5f5;
}

.layout-content {
  min-height: 100vh;
}
</style>
