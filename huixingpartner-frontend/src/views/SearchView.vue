<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { queryUser, searchUsersByTags, type UserVO } from '@/api/user'

const router = useRouter()

// 搜索类型：'text' | 'tag'
const searchType = ref<'text' | 'tag'>('text')

// 搜索关键词
const keyword = ref('')

// 已选标签列表
const selectedTags = ref<string[]>([])

// 搜索结果列表
const userList = ref<UserVO[]>([])

// 分页状态
const currentPage = ref(1)
const pageSize = 20
const total = ref(0)
const loading = ref(false)
const finished = ref(false)
const isEmpty = ref(false)

// 搜索类型选项
const searchTypeOptions = [
  { text: '文本搜索', value: 'text' },
  { text: '标签搜索', value: 'tag' }
]

// 热门标签分类
const hotTagCategories = [
  {
    category: '编程语言',
    tags: ['Java', 'Python', 'JavaScript', 'C++', 'Go', 'Rust', 'TypeScript']
  },
  {
    category: '技术方向',
    tags: ['前端', '后端', '全栈', '移动端', '算法', '数据分析']
  },
  {
    category: '框架技术',
    tags: ['Vue', 'React', 'Spring Boot', 'Django', 'Flutter']
  },
  {
    category: '职业状态',
    tags: ['学生', '求职中', '工程师', '产品经理', '设计师']
  },
  {
    category: '兴趣爱好',
    tags: ['篮球', '游戏', '音乐', '摄影', '旅行', '阅读', '健身']
  }
]

// 返回上一页
const onClickLeft = () => {
  router.back()
}

// 搜索类型切换
const onSearchTypeChange = (value: 'text' | 'tag') => {
  searchType.value = value
  // 清空搜索结果
  resetSearch()
}

// 重置搜索状态
const resetSearch = () => {
  currentPage.value = 1
  userList.value = []
  total.value = 0
  finished.value = false
  isEmpty.value = false
  keyword.value = ''
  selectedTags.value = []
}

// 执行搜索（首次加载）
const onSearch = async () => {
  // 文本搜索：验证关键词
  if (searchType.value === 'text') {
    if (!keyword.value.trim()) {
      showToast('请输入搜索内容')
      return
    }
  }

  // 标签搜索：验证已选标签
  if (searchType.value === 'tag') {
    if (selectedTags.value.length === 0) {
      showToast('请选择至少一个标签')
      return
    }
  }

  // 重置分页状态
  currentPage.value = 1
  userList.value = []
  finished.value = false
  isEmpty.value = false

  // 加载第一页
  await onLoad()
}

// 加载搜索结果（分页）
const onLoad = async () => {
  if (finished.value) return

  loading.value = true

  try {
    let response

    if (searchType.value === 'text') {
      // 文本搜索
      response = await queryUser({
        searchText: keyword.value.trim(),
        pageNum: currentPage.value,
        pageSize
      })
    } else {
      // 标签搜索
      response = await searchUsersByTags({
        tags: selectedTags.value.join(','),
        pageNum: currentPage.value,
        pageSize
      })
    }

    // 打印响应数据，用于调试
    console.log('搜索响应:', response)

    // 响应拦截器已经返回了 response.data，所以这里直接访问 response
    if (response.code === 0 && response.data) {
      const { records, total: totalCount } = response.data

      console.log('用户列表:', records)
      console.log('总数:', totalCount)

      // 追加到列表
      userList.value.push(...records || [])
      total.value = totalCount || 0

      // 判断是否为空
      if (currentPage.value === 1 && (!records || records.length === 0)) {
        isEmpty.value = true
      }

      // 判断是否加载完成
      if (userList.value.length >= totalCount) {
        finished.value = true
      }

      // 下一页
      currentPage.value++
    } else {
      console.error('响应格式错误:', response)
      showToast('搜索失败，请稍后重试')
      finished.value = true
    }
  } catch (error) {
    console.error('搜索失败:', error)
    showToast('搜索失败，请稍后重试')
    finished.value = true
  } finally {
    loading.value = false
  }
}

// 选择标签
const onSelectTag = (tag: string) => {
  const index = selectedTags.value.indexOf(tag)
  if (index > -1) {
    // 已选中，移除
    selectedTags.value.splice(index, 1)
  } else {
    // 未选中，添加
    selectedTags.value.push(tag)
  }
}

// 移除已选标签
const onRemoveTag = (tag: string) => {
  const index = selectedTags.value.indexOf(tag)
  if (index > -1) {
    selectedTags.value.splice(index, 1)
  }
}

// 判断标签是否已选中
const isTagSelected = (tag: string) => {
  return selectedTags.value.includes(tag)
}

// 获取性别图标
const getGenderIcon = (gender?: number) => {
  switch (gender) {
    case 1:
      return 'male' // 男
    case 2:
      return 'female' // 女
    default:
      return 'question' // 保密
  }
}

// 获取性别颜色
const getGenderColor = (gender?: number) => {
  switch (gender) {
    case 1:
      return '#1989fa' // 男 - 蓝色
    case 2:
      return '#fb7299' // 女 - 粉色
    default:
      return '#969799' // 保密 - 灰色
  }
}

// 解析标签
const parseTags = (tagsStr?: string) => {
  if (!tagsStr) return []
  try {
    return JSON.parse(tagsStr)
  } catch {
    return []
  }
}

// 跳转到用户详情页
const onUserClick = (user: UserVO) => {
  router.push(`/user/${user.id}`)
}

// 默认头像
const defaultAvatar = 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'
</script>

<template>
  <div class="search-page">
    <!-- 顶部导航栏 -->
    <van-nav-bar
      title="搜索用户"
      left-text="返回"
      left-arrow
      @click-left="onClickLeft"
      fixed
      placeholder
      safe-area-inset-top
    />

    <div class="search-content">
      <!-- 搜索栏 -->
      <div class="search-bar">
        <!-- 左边：下拉选择框 -->
        <van-dropdown-menu class="search-type-selector">
          <van-dropdown-item
            v-model="searchType"
            :options="searchTypeOptions"
            @change="onSearchTypeChange"
          />
        </van-dropdown-menu>

        <!-- 中间：搜索框 -->
        <van-search
          v-model="keyword"
          :placeholder="searchType === 'text' ? '请输入用户名或昵称' : '请输入标签（或从下方选择）'"
          shape="round"
          clearable
          @keyup.enter="onSearch"
        />

        <!-- 右边：搜索按钮 -->
        <van-button
          type="primary"
          size="small"
          class="search-button"
          @click="onSearch"
        >
          搜索
        </van-button>
      </div>

      <!-- 热门标签区（仅标签搜索显示） -->
      <div v-if="searchType === 'tag'" class="hot-tags-section">
        <!-- 已选标签 -->
        <div v-if="selectedTags.length > 0" class="selected-tags">
          <div class="section-title">已选标签：</div>
          <div class="tags-container">
            <van-tag
              v-for="tag in selectedTags"
              :key="tag"
              closeable
              type="primary"
              size="medium"
              class="selected-tag"
              @close="onRemoveTag(tag)"
            >
              {{ tag }}
            </van-tag>
          </div>
        </div>

        <!-- 热门标签分类 -->
        <div v-for="category in hotTagCategories" :key="category.category" class="tag-category">
          <div class="section-title">{{ category.category }}</div>
          <div class="tags-container">
            <van-tag
              v-for="tag in category.tags"
              :key="tag"
              :type="isTagSelected(tag) ? 'primary' : 'default'"
              size="medium"
              class="hot-tag"
              :class="{ 'tag-selected': isTagSelected(tag) }"
              @click="onSelectTag(tag)"
            >
              {{ tag }}
            </van-tag>
          </div>
        </div>
      </div>

      <!-- 搜索结果列表 -->
      <div v-if="userList.length > 0 || isEmpty" class="search-results">
        <!-- 搜索结果数量 -->
        <div v-if="!isEmpty" class="result-count">
          搜索结果 (共{{ total }}条)
        </div>

        <!-- 用户列表 -->
        <van-list
          v-model:loading="loading"
          :finished="finished"
          finished-text="没有更多了"
          @load="onLoad"
        >
          <div
            v-for="user in userList"
            :key="user.id"
            class="user-card"
            @click="onUserClick(user)"
          >
            <!-- 头像 -->
            <van-image
              round
              width="50"
              height="50"
              :src="user.userAvatarUrl || defaultAvatar"
              class="user-avatar"
            />

            <!-- 用户信息 -->
            <div class="user-info">
              <div class="user-header">
                <span class="user-name">{{ user.username }}</span>
                <van-icon
                  :name="getGenderIcon(user.gender)"
                  :color="getGenderColor(user.gender)"
                  size="16"
                />
              </div>

              <!-- 个人简介 -->
              <div v-if="user.profile" class="user-profile">
                {{ user.profile }}
              </div>
              <div v-else class="user-profile empty">
                这个人很懒，什么都没留下
              </div>

              <!-- 标签 -->
              <div v-if="user.tags" class="user-tags">
                <van-tag
                  v-for="(tag, index) in parseTags(user.tags).slice(0, 3)"
                  :key="index"
                  type="primary"
                  size="small"
                  plain
                >
                  {{ tag }}
                </van-tag>
              </div>
            </div>
          </div>
        </van-list>

        <!-- 空状态 -->
        <van-empty
          v-if="isEmpty"
          image="search"
          description="没有找到相关用户"
        >
          <template #description>
            <div class="empty-tips">
              没有找到相关用户
              <br />
              试试搜索其他关键词吧
            </div>
          </template>
        </van-empty>
      </div>

      <!-- 初始提示 -->
      <div v-else-if="userList.length === 0 && !isEmpty" class="initial-hint">
        <van-empty image="search" description="搜索用户">
          <template #description>
            <div class="hint-text">
              {{ searchType === 'text' ? '输入用户名或昵称搜索' : '选择标签开始搜索' }}
            </div>
          </template>
        </van-empty>
      </div>
    </div>
  </div>
</template>

<style scoped>
.search-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.search-content {
  padding: 16px;
  padding-bottom: 30px;
}

/* 搜索栏 */
.search-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  background: #fff;
  padding: 8px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.search-type-selector {
  flex-shrink: 0;
  width: 100px;
}

.search-bar :deep(.van-search) {
  flex: 1;
  padding: 0;
}

.search-button {
  flex-shrink: 0;
}

/* 热门标签区 */
.hot-tags-section {
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #323233;
  margin-bottom: 12px;
}

.tag-category {
  margin-bottom: 20px;
}

.tag-category:last-child {
  margin-bottom: 0;
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

/* 已选标签 */
.selected-tags {
  padding: 12px;
  background: #f7f8fa;
  border-radius: 6px;
  margin-bottom: 16px;
}

.selected-tags .section-title {
  margin-bottom: 8px;
}

.selected-tag {
  cursor: pointer;
}

/* 热门标签 */
.hot-tag {
  cursor: pointer;
  transition: all 0.2s;
}

.hot-tag.tag-selected {
  transform: scale(1.05);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

/* 搜索结果 */
.search-results {
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.result-count {
  font-size: 14px;
  color: #969799;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebedf0;
}

/* 用户卡片 */
.user-card {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: #f7f8fa;
  border-radius: 8px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.user-card:last-child {
  margin-bottom: 0;
}

.user-card:active {
  transform: scale(0.98);
  background: #f0f2f5;
}

.user-avatar {
  flex-shrink: 0;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}

.user-name {
  font-size: 16px;
  font-weight: 600;
  color: #323233;
}

.user-profile {
  font-size: 13px;
  color: #646566;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.user-profile.empty {
  color: #c8c9cc;
  font-style: italic;
}

.user-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

/* 空状态 */
.empty-tips {
  text-align: center;
  color: #969799;
  font-size: 14px;
  line-height: 1.6;
}

/* 初始提示 */
.initial-hint {
  background: #fff;
  padding: 32px 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.hint-text {
  text-align: center;
  color: #969799;
  font-size: 14px;
  margin-top: 8px;
}
</style>
