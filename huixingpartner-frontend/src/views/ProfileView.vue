<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showSuccessToast, showLoadingToast, closeToast } from 'vant'
import { useUserStore } from '@/stores/user'
import { updateUser, updateTags } from '@/api/user'

const router = useRouter()
const userStore = useUserStore()

// 用户信息
const userInfo = computed(() => userStore.userInfo)

// 编辑状态
const showNicknameDialog = ref(false)
const showGenderAction = ref(false)
const showBioDialog = ref(false)
const showContactDialog = ref(false)
const showEmailDialog = ref(false)
const showTagsDialog = ref(false)

// 表单数据
const nicknameForm = ref({ nickname: '' })
const bioForm = ref({ bio: '' })
const contactForm = ref({ contact: '' })
const emailForm = ref({ email: '' })

// 性别选项
const genderOptions = [
  { text: '男', value: 1 },
  { text: '女', value: 2 },
  { text: '保密', value: 0 }
]

// 标签输入
const tagInput = ref('')

// 热门标签折叠面板状态
const activeCollapse = ref(['hotTags'])

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

// 点击头像
const onClickAvatar = () => {
  showToast('头像上传功能开发中...')
}

// 编辑昵称
const onEditNickname = () => {
  nicknameForm.value.nickname = userInfo.value?.username || ''
  showNicknameDialog.value = true
}

// 保存昵称
const onSaveNickname = async () => {
  if (!nicknameForm.value.nickname.trim()) {
    showToast('昵称不能为空')
    return
  }

  try {
    showLoadingToast({
      message: '保存中...',
      forbidClick: true,
      duration: 0
    })

    await updateUser({ username: nicknameForm.value.nickname })

    closeToast()

    // 更新本地用户信息
    userStore.updateUserInfo({ username: nicknameForm.value.nickname })

    showNicknameDialog.value = false
    showSuccessToast('昵称已更新')
  } catch (error: any) {
    closeToast()
    console.error('更新昵称失败：', error)
    showToast(error.message || '更新失败，请重试')
  }
}

// 编辑性别
const onEditGender = () => {
  showGenderAction.value = true
}

// 选择性别
const onSelectGender = async (item: any) => {
  showGenderAction.value = false

  try {
    showLoadingToast({
      message: '保存中...',
      forbidClick: true,
      duration: 0
    })

    await updateUser({ gender: item.value })

    closeToast()

    // 更新本地用户信息
    userStore.updateUserInfo({ gender: item.value })

    showSuccessToast(`性别已设置为：${item.text}`)
  } catch (error: any) {
    closeToast()
    console.error('更新性别失败：', error)
    showToast(error.message || '更新失败，请重试')
  }
}

// 编辑个人简介
const onEditBio = () => {
  bioForm.value.bio = userInfo.value?.userDesc || ''
  showBioDialog.value = true
}

// 保存个人简介
const onSaveBio = async () => {
  try {
    showLoadingToast({
      message: '保存中...',
      forbidClick: true,
      duration: 0
    })

    await updateUser({ userDesc: bioForm.value.bio })

    closeToast()

    // 更新本地用户信息
    userStore.updateUserInfo({ userDesc: bioForm.value.bio })

    showBioDialog.value = false
    showSuccessToast('个人简介已更新')
  } catch (error: any) {
    closeToast()
    console.error('更新简介失败：', error)
    showToast(error.message || '更新失败，请重试')
  }
}

// 编辑联系方式
const onEditContact = () => {
  contactForm.value.contact = userInfo.value?.contactInfo || ''
  showContactDialog.value = true
}

// 保存联系方式
const onSaveContact = async () => {
  try {
    showLoadingToast({
      message: '保存中...',
      forbidClick: true,
      duration: 0
    })

    await updateUser({ contactInfo: contactForm.value.contact })

    closeToast()

    // 更新本地用户信息
    userStore.updateUserInfo({ contactInfo: contactForm.value.contact })

    showContactDialog.value = false
    showSuccessToast('联系方式已更新')
  } catch (error: any) {
    closeToast()
    console.error('更新联系方式失败：', error)
    showToast(error.message || '更新失败，请重试')
  }
}

// 编辑邮箱
const onEditEmail = () => {
  emailForm.value.email = userInfo.value?.email || ''
  showEmailDialog.value = true
}

// 保存邮箱
const onSaveEmail = async () => {
  // 邮箱格式验证
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (emailForm.value.email && !emailRegex.test(emailForm.value.email)) {
    showToast('请输入正确的邮箱格式')
    return
  }

  try {
    showLoadingToast({
      message: '保存中...',
      forbidClick: true,
      duration: 0
    })

    await updateUser({ email: emailForm.value.email })

    closeToast()

    // 更新本地用户信息
    userStore.updateUserInfo({ email: emailForm.value.email })

    showEmailDialog.value = false
    showSuccessToast('邮箱已更新')
  } catch (error: any) {
    closeToast()
    console.error('更新邮箱失败：', error)
    showToast(error.message || '更新失败，请重试')
  }
}

// 编辑标签
const onEditTags = () => {
  showTagsDialog.value = true
}

// 添加标签
const onAddTag = async () => {
  if (!tagInput.value.trim()) {
    showToast('请输入标签内容')
    return
  }

  const tags = userInfo.value?.tags || []
  if (tags.length >= 12) {
    showToast('标签最多12个，已达到上限')
    return
  }

  const newTag = normalizeTag(tagInput.value.trim())

  if (tags.includes(newTag)) {
    showToast('标签已存在')
    return
  }

  try {
    showLoadingToast({
      message: '添加中...',
      forbidClick: true,
      duration: 0
    })

    // 添加标签到数组
    tags.push(newTag)

    // 调用API更新标签
    await updateTags({
      id: userInfo.value!.id,
      tagList: tags
    })

    closeToast()

    // 更新本地用户信息
    userStore.updateUserInfo({ tags })

    tagInput.value = ''
    showSuccessToast('标签已添加')
  } catch (error: any) {
    closeToast()
    console.error('添加标签失败：', error)

    // 失败则回滚
    const index = tags.indexOf(newTag)
    if (index > -1) {
      tags.splice(index, 1)
    }

    showToast(error.message || '添加失败，请重试')
  }
}

// 删除标签
const onDeleteTag = async (tag: string) => {
  const tags = userInfo.value?.tags || []
  const index = tags.indexOf(tag)

  if (index === -1) return

  try {
    showLoadingToast({
      message: '删除中...',
      forbidClick: true,
      duration: 0
    })

    // 从数组中删除标签
    tags.splice(index, 1)

    // 调用API更新标签
    await updateTags({
      id: userInfo.value!.id,
      tagList: tags
    })

    closeToast()

    // 更新本地用户信息
    userStore.updateUserInfo({ tags })

    showSuccessToast('标签已删除')
  } catch (error: any) {
    closeToast()
    console.error('删除标签失败：', error)

    // 失败则回滚
    tags.splice(index, 0, tag)

    showToast(error.message || '删除失败，请重试')
  }
}

// 选择热门标签
const onSelectHotTag = async (tag: string) => {
  const tags = userInfo.value?.tags || []

  // 如果已选中，则删除
  if (tags.includes(tag)) {
    await onDeleteTag(tag)
    return
  }

  // 检查数量限制
  if (tags.length >= 12) {
    showToast('标签最多12个，已达到上限')
    return
  }

  try {
    showLoadingToast({
      message: '添加中...',
      forbidClick: true,
      duration: 0
    })

    // 添加标签到数组
    tags.push(tag)

    // 调用API更新标签
    await updateTags({
      id: userInfo.value!.id,
      tagList: tags
    })

    closeToast()

    // 更新本地用户信息
    userStore.updateUserInfo({ tags })

    showSuccessToast('标签已添加')
  } catch (error: any) {
    closeToast()
    console.error('添加标签失败：', error)

    // 失败则回滚
    const index = tags.indexOf(tag)
    if (index > -1) {
      tags.splice(index, 1)
    }

    showToast(error.message || '添加失败，请重试')
  }
}

// 判断标签是否已选中
const isTagSelected = (tag: string) => {
  const tags = userInfo.value?.tags || []
  return tags.includes(tag)
}

// 标签规范化（首字母大写）
const normalizeTag = (tag: string) => {
  // 首字母大写，其余小写
  return tag.charAt(0).toUpperCase() + tag.slice(1).toLowerCase()
}

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
</script>

<template>
  <div class="profile-page">
    <!-- 顶部导航栏 -->
    <van-nav-bar
      title="个人资料"
      left-text="返回"
      left-arrow
      @click-left="onClickLeft"
      fixed
      placeholder
      safe-area-inset-top
    />

    <div class="profile-content">
      <!-- 头像区域 -->
      <div class="avatar-section" @click="onClickAvatar">
        <div class="avatar-wrapper">
          <van-image
            round
            width="80"
            height="80"
            :src="userInfo?.avatarUrl || 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'"
          />
          <van-icon name="photo-o" class="edit-icon" />
        </div>
        <div class="avatar-tip">点击更换头像</div>
      </div>

      <!-- 资料列表 -->
      <van-cell-group inset class="info-group">
        <!-- 昵称 -->
        <van-cell
          title="昵称"
          :value="userInfo?.username || '未设置'"
          is-link
          @click="onEditNickname"
        />

        <!-- UID -->
        <van-cell title="UID" :value="String(userInfo?.id || '')" />

        <!-- 性别 -->
        <van-cell
          title="性别"
          :value="getGenderText(userInfo?.gender)"
          is-link
          @click="onEditGender"
        />

        <!-- 个人简介 -->
        <van-cell
          title="个人简介"
          :value="userInfo?.userDesc || '未设置'"
          is-link
          @click="onEditBio"
        />

        <!-- 联系方式 -->
        <van-cell
          title="联系方式"
          :value="userInfo?.contactInfo || '未设置'"
          is-link
          @click="onEditContact"
        />

        <!-- 邮箱 -->
        <van-cell
          title="邮箱"
          :value="userInfo?.email || '未设置'"
          is-link
          @click="onEditEmail"
        />

        <!-- 标签 -->
        <van-cell
          title="标签"
          is-link
          @click="onEditTags"
        >
          <template #value>
            <div class="tags-preview">
              <van-tag
                v-for="(tag, index) in (userInfo?.tags || []).slice(0, 3)"
                :key="index"
                type="primary"
                size="medium"
                plain
              >
                {{ tag }}
              </van-tag>
              <span v-if="!userInfo?.tags || userInfo.tags.length === 0" class="empty-text">未设置</span>
              <span v-else-if="userInfo.tags.length > 3" class="more-text">
                等{{ userInfo.tags.length }}个
              </span>
            </div>
          </template>
        </van-cell>
      </van-cell-group>
    </div>

    <!-- 昵称编辑弹窗 -->
    <van-dialog
      v-model:show="showNicknameDialog"
      title="修改昵称"
      show-cancel-button
      @confirm="onSaveNickname"
    >
      <van-field
        v-model="nicknameForm.nickname"
        placeholder="请输入昵称"
        maxlength="20"
        show-word-limit
      />
    </van-dialog>

    <!-- 性别选择 -->
    <van-action-sheet
      v-model:show="showGenderAction"
      :actions="genderOptions"
      @select="onSelectGender"
      cancel-text="取消"
    />

    <!-- 个人简介编辑弹窗 -->
    <van-dialog
      v-model:show="showBioDialog"
      title="修改个人简介"
      show-cancel-button
      @confirm="onSaveBio"
    >
      <van-field
        v-model="bioForm.bio"
        type="textarea"
        placeholder="介绍一下自己吧"
        maxlength="200"
        show-word-limit
        rows="4"
      />
    </van-dialog>

    <!-- 联系方式编辑弹窗 -->
    <van-dialog
      v-model:show="showContactDialog"
      title="修改联系方式"
      show-cancel-button
      @confirm="onSaveContact"
    >
      <van-field
        v-model="contactForm.contact"
        type="textarea"
        placeholder="请输入联系方式（微信号、QQ号等）"
        maxlength="100"
        show-word-limit
        rows="3"
      />
    </van-dialog>

    <!-- 邮箱编辑弹窗 -->
    <van-dialog
      v-model:show="showEmailDialog"
      title="修改邮箱"
      show-cancel-button
      @confirm="onSaveEmail"
    >
      <van-field
        v-model="emailForm.email"
        type="email"
        placeholder="请输入邮箱地址"
        maxlength="50"
      />
    </van-dialog>

    <!-- 标签编辑弹窗 -->
    <van-dialog
      v-model:show="showTagsDialog"
      title="编辑标签"
      :show-confirm-button="false"
      close-on-click-overlay
    >
      <div class="tags-dialog-content">
        <!-- 进度提示 -->
        <div class="tags-progress">
          已选标签 <span class="count">{{ userInfo?.tags?.length || 0 }}</span> / 12
        </div>

        <!-- 已选标签列表 -->
        <div class="tags-list">
          <van-tag
            v-for="(tag, index) in (userInfo?.tags || [])"
            :key="index"
            closeable
            type="primary"
            size="large"
            class="tag-item"
            @close="onDeleteTag(tag)"
          >
            {{ tag }}
          </van-tag>
          <div v-if="!userInfo?.tags || userInfo.tags.length === 0" class="empty-tags">
            暂无标签，请从下方选择或手动添加
          </div>
        </div>

        <!-- 热门标签折叠面板 -->
        <van-collapse v-model="activeCollapse" class="hot-tags-collapse">
          <van-collapse-item title="热门标签" name="hotTags">
            <div class="hot-tags-content">
              <!-- 热门标签分类 -->
              <div
                v-for="category in hotTagCategories"
                :key="category.category"
                class="tag-category"
              >
                <div class="category-title">{{ category.category }}</div>
                <div class="tags-container">
                  <van-tag
                    v-for="tag in category.tags"
                    :key="tag"
                    :type="isTagSelected(tag) ? 'primary' : 'default'"
                    size="medium"
                    :class="{ 'tag-selected': isTagSelected(tag) }"
                    class="hot-tag"
                    @click="onSelectHotTag(tag)"
                  >
                    {{ tag }}
                  </van-tag>
                </div>
              </div>
            </div>
          </van-collapse-item>
        </van-collapse>

        <!-- 手动添加标签 -->
        <div class="add-tag-section">
          <van-field
            v-model="tagInput"
            placeholder="手动输入标签（首字母会自动大写）"
            maxlength="10"
          >
            <template #button>
              <van-button
                size="small"
                type="primary"
                :disabled="(userInfo?.tags?.length || 0) >= 12"
                @click="onAddTag"
              >
                添加
              </van-button>
            </template>
          </van-field>
        </div>

        <!-- 关闭按钮 -->
        <div class="dialog-footer">
          <van-button type="primary" block @click="showTagsDialog = false">
            完成
          </van-button>
        </div>
      </div>
    </van-dialog>
  </div>
</template>

<style scoped>
.profile-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.profile-content {
  padding: 16px;
}

/* 头像区域 */
.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 30px 0;
  background: white;
  border-radius: 12px;
  margin-bottom: 16px;
}

.avatar-wrapper {
  position: relative;
  margin-bottom: 12px;
}

.edit-icon {
  position: absolute;
  bottom: 0;
  right: 0;
  background: white;
  border-radius: 50%;
  padding: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  font-size: 20px;
}

.avatar-tip {
  font-size: 13px;
  color: #969799;
}

/* 信息组 */
.info-group {
  border-radius: 12px;
  overflow: hidden;
}

/* 标签预览 */
.tags-preview {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: nowrap;
}

.tags-preview .van-tag {
  flex-shrink: 0;
}

.empty-text {
  color: #969799;
  font-size: 14px;
}

.more-text {
  color: #969799;
  font-size: 12px;
  flex-shrink: 0;
}

/* 标签弹窗 */
.tags-dialog-content {
  padding: 16px;
  max-height: 70vh;
  overflow-y: auto;
}

/* 进度提示 */
.tags-progress {
  font-size: 14px;
  color: #323233;
  margin-bottom: 12px;
  padding: 8px 12px;
  background: #f7f8fa;
  border-radius: 6px;
  text-align: center;
}

.tags-progress .count {
  font-size: 18px;
  font-weight: 600;
  color: #fb7299;
}

.tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 60px;
  margin-bottom: 16px;
  padding: 12px;
  background: #f7f8fa;
  border-radius: 6px;
}

.tag-item {
  margin: 0;
}

.empty-tags {
  width: 100%;
  text-align: center;
  color: #969799;
  font-size: 14px;
  padding: 20px 0;
}

/* 热门标签折叠面板 */
.hot-tags-collapse {
  margin-bottom: 16px;
}

.hot-tags-content {
  padding: 8px 0;
}

.tag-category {
  margin-bottom: 16px;
}

.tag-category:last-child {
  margin-bottom: 0;
}

.category-title {
  font-size: 13px;
  font-weight: 600;
  color: #646566;
  margin-bottom: 8px;
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.hot-tag {
  cursor: pointer;
  transition: all 0.2s;
}

.hot-tag.tag-selected {
  transform: scale(1.05);
  box-shadow: 0 2px 4px rgba(251, 114, 153, 0.3);
}

/* 添加标签区域 */
.add-tag-section {
  margin-bottom: 16px;
}

.dialog-footer {
  margin-top: 16px;
}
</style>
