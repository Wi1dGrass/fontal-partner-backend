<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showLoadingToast, showConfirmDialog, closeToast, showSuccessToast } from 'vant'
import { useUserStore } from '@/stores/user'
import {
  getTeamById,
  getTeamBasicInfo,
  getTeamMembership,
  joinTeam,
  applyToJoinTeam,
  updateTeam,
  transferTeam,
  quitTeam,
  deleteTeam,
  kickOutUser,
  type TeamVO,
  type TeamBasicVO,
  type TeamUserVO,
  type UpdateTeamRequest
} from '@/api/team'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 队伍信息（可能是 TeamVO 或 TeamBasicVO）
const teamInfo = ref<TeamVO | TeamBasicVO | null>(null)

// 加载状态
const loading = ref(false)

// 显示操作菜单
const showActionSheet = ref(false)

// 更新队伍弹窗
const showUpdateDialog = ref(false)
const updateForm = ref<UpdateTeamRequest>({
  id: 0,
  teamName: '',
  teamDesc: '',
  teamAvatarUrl: '',
  maxNum: 0,
  expireTime: '',
  teamStatus: 0,
  teamPassword: '',
  announce: ''
})

// 转让队长弹窗
const showTransferDialog = ref(false)
const transferForm = ref({
  userAccount: ''
})

// 转让队长选择成员弹窗
const showTransferMemberSheet = ref(false)

// 日期时间选择
const showDatePicker = ref(false)
const selectedDateArray = ref<string[]>([])
const selectedTimeArray = ref<string[]>(['00', '00'])

// 踢出模式
const kickMode = ref(false)

// 队伍状态选项
const statusOptions = [
  { text: '公开', value: 0 },
  { text: '私有', value: 1 },
  { text: '加密', value: 2 }
]

// 用户信息
const userInfo = computed(() => userStore.userInfo)

// 判断当前用户是否是队长
const isCaptain = computed(() => {
  if (!userInfo.value || !teamInfo.value) return false

  // 使用 captainInfo 计算属性，兼容 TeamVO 和 TeamBasicVO
  return userInfo.value.id === captainInfo.value?.id
})

// 判断是否可以转让队长（有其他成员）
const canTransferCaptain = computed(() => {
  return (teamInfo.value?.userSet?.length || 0) > 1
})

// 判断是否只剩队长一人
const isOnlyCaptain = computed(() => {
  return isCaptain.value && (teamInfo.value?.userSet?.length || 0) === 1
})

// 判断当前用户是否是队伍成员
const isTeamMember = computed(() => {
  if (!userInfo.value || !teamInfo.value) return false

  // 如果有 userSet 字段，说明是 TeamVO（成员数据）
  if ('userSet' in teamInfo.value) {
    return teamInfo.value.userSet?.some(member => member.id === userInfo.value.id) || false
  }

  // 如果是 TeamBasicVO，说明用户不是成员（因为只有非成员才能获取基础信息）
  return false
})

// 获取队长信息（兼容 TeamVO 和 TeamBasicVO）
const captainInfo = computed(() => {
  if (!teamInfo.value) return null

  // TeamBasicVO 有 captain 字段
  if ('captain' in teamInfo.value) {
    return teamInfo.value.captain
  }

  // TeamVO 有 user 字段
  if ('user' in teamInfo.value) {
    return teamInfo.value.user
  }

  return null
})

// 获取当前队伍人数（兼容 TeamVO 和 TeamBasicVO）
const currentTeamNum = computed(() => {
  if (!teamInfo.value) return 0

  // TeamBasicVO 有 currentNum 字段
  if ('currentNum' in teamInfo.value) {
    return teamInfo.value.currentNum
  }

  // TeamVO 需要从 userSet 计算
  if ('userSet' in teamInfo.value) {
    return teamInfo.value.userSet?.length || 0
  }

  return 0
})

// 获取队伍标签（兼容 TeamVO 和 TeamBasicVO）
const teamTags = computed(() => {
  if (!teamInfo.value) return []

  // TeamBasicVO 有 tags 字段
  if ('tags' in teamInfo.value) {
    return teamInfo.value.tags?.filter(Boolean) || []
  }

  // TeamVO 没有标签
  return []
})

// 获取队伍要求（TeamBasicVO 专用）
const teamRequirements = computed(() => {
  if (!teamInfo.value) return ''

  // TeamBasicVO 有 requirements 字段
  if ('requirements' in teamInfo.value) {
    return teamInfo.value.requirements || ''
  }

  return ''
})

// 密码输入弹窗
const showPasswordDialog = ref(false)
const passwordForm = ref({
  password: ''
})

// 操作菜单选项（只保留更新队伍）
const actionSheetOptions = computed(() => {
  return [
    { name: '更新队伍信息', value: 'update' }
  ]
})

// 返回上一页
const onClickLeft = () => {
  router.back()
}

// 点击更新队伍按钮
const onClickUpdate = () => {
  if (teamInfo.value) {
    updateForm.value = {
      id: teamInfo.value.id,
      teamName: teamInfo.value.teamName,
      teamDesc: teamInfo.value.teamDesc,
      teamAvatarUrl: teamInfo.value.teamAvatarUrl,
      maxNum: teamInfo.value.maxNum,
      expireTime: teamInfo.value.expireTime,
      teamStatus: teamInfo.value.teamStatus,
      teamPassword: teamInfo.value.teamPassword || '',
      announce: teamInfo.value.announce
    }
    showUpdateDialog.value = true
  }
}

// 点击操作按钮（保留，但不使用）
const onClickAction = () => {
  showActionSheet.value = true
}

// 选择操作项
const onSelectAction = async (item: any) => {
  showActionSheet.value = false

  switch (item.value) {
    case 'update':
      // 打开更新队伍信息弹窗
      if (teamInfo.value) {
        updateForm.value = {
          id: teamInfo.value.id,
          teamName: teamInfo.value.teamName,
          teamDesc: teamInfo.value.teamDesc,
          teamAvatarUrl: teamInfo.value.teamAvatarUrl,
          maxNum: teamInfo.value.maxNum,
          expireTime: teamInfo.value.expireTime,
          teamStatus: teamInfo.value.teamStatus,
          teamPassword: teamInfo.value.teamPassword || '',
          announce: teamInfo.value.announce
        }
        showUpdateDialog.value = true
      }
      break
  }
}

// 更新队伍信息
const handleUpdateTeam = async () => {
  // 验证表单
  if (!updateForm.value.teamName?.trim()) {
    showToast('请输入队伍名称')
    return
  }

  if (!updateForm.value.maxNum || updateForm.value.maxNum < 1) {
    showToast('队伍人数必须大于0')
    return
  }

  try {
    showLoadingToast({
      message: '更新中...',
      forbidClick: true,
      duration: 0
    })

    await updateTeam(updateForm.value)

    closeToast()

    // 更新本地队伍信息
    if (teamInfo.value) {
      Object.assign(teamInfo.value, {
        teamName: updateForm.value.teamName,
        teamDesc: updateForm.value.teamDesc,
        teamAvatarUrl: updateForm.value.teamAvatarUrl,
        maxNum: updateForm.value.maxNum,
        expireTime: updateForm.value.expireTime,
        teamStatus: updateForm.value.teamStatus,
        teamPassword: updateForm.value.teamPassword,
        announce: updateForm.value.announce
      })
    }

    showUpdateDialog.value = false
    showSuccessToast('队伍信息已更新')
  } catch (error: any) {
    closeToast()
    console.error('更新队伍失败：', error)
    showToast(error.message || '更新失败，请重试')
  }
}

// 转让队长
const handleTransferTeam = async () => {
  if (!transferForm.value.userAccount?.trim()) {
    showToast('请输入对方账号')
    return
  }

  if (transferForm.value.userAccount === userInfo.value?.userAccount) {
    showToast('不能转让给自己')
    return
  }

  try {
    showLoadingToast({
      message: '转让中...',
      forbidClick: true,
      duration: 0
    })

    await transferTeam({
      teamId: teamInfo.value!.id,
      userAccount: transferForm.value.userAccount
    })

    closeToast()

    showTransferDialog.value = false
    showSuccessToast('队长已转让')

    // 延迟后返回上一页
    setTimeout(() => {
      router.back()
    }, 1500)
  } catch (error: any) {
    closeToast()
    console.error('转让队长失败：', error)
    showToast(error.message || '转让失败，请重试')
  }
}

// 打开转让队长成员选择弹窗
const openTransferMemberSheet = () => {
  if (!canTransferCaptain.value) {
    showToast('无其他成员可转让')
    return
  }
  showTransferMemberSheet.value = true
}

// 转让队长成员列表（排除当前队长）
const transferMemberActions = computed(() => {
  if (!teamInfo.value) return []

  // 只有 TeamVO 才有 userSet 字段
  if (!('userSet' in teamInfo.value)) return []

  return teamInfo.value.userSet
    .filter(member => member.id !== captainInfo.value?.id)
    .map(member => ({
      name: member.username,
      subtext: member.userAccount,
      onClick: () => handleTransferToMember(member)
    }))
})

// 转让队长给指定成员
const handleTransferToMember = async (member: TeamUserVO) => {
  showTransferMemberSheet.value = false

  try {
    await showConfirmDialog({
      title: '转让队长',
      message: `确定要将队长转让给 ${member.username} 吗？\n转让后您将失去队长权限。`,
      confirmButtonText: '确认转让',
      confirmButtonColor: '#ee0a24'
    })

    showLoadingToast({
      message: '转让中...',
      forbidClick: true,
      duration: 0
    })

    await transferTeam({
      teamId: teamInfo.value!.id,
      userAccount: member.userAccount
    })

    closeToast()
    showSuccessToast('队长已转让')

    // 延迟后返回上一页
    setTimeout(() => {
      router.back()
    }, 1500)
  } catch (error: any) {
    closeToast()
    if (error !== 'cancel') {
      console.error('转让队长失败：', error)
      showToast(error.message || '转让失败，请重试')
    }
  }
}

// 退出队伍
const handleQuitTeam = async () => {
  try {
    showLoadingToast({
      message: '退出中...',
      forbidClick: true,
      duration: 0
    })

    await quitTeam(teamInfo.value!.id)

    closeToast()
    showSuccessToast('已退出队伍')

    // 延迟后返回上一页
    setTimeout(() => {
      router.back()
    }, 1500)
  } catch (error: any) {
    closeToast()
    console.error('退出队伍失败：', error)
    showToast(error.message || '退出失败，请重试')
  }
}

// 退出队伍（带确认）
const handleQuitTeamWithConfirm = async () => {
  try {
    await showConfirmDialog({
      title: '退出队伍',
      message: '确定要退出队伍吗？',
      confirmButtonText: '确认退出',
      confirmButtonColor: '#ee0a24'
    })

    await handleQuitTeam()
  } catch {
    // 用户取消
  }
}

// 解散队伍（带确认）
const handleDissolveTeamWithConfirm = async () => {
  try {
    await showConfirmDialog({
      title: '解散队伍',
      message: '解散后无法恢复，所有成员将被移出，确定要解散队伍吗？',
      confirmButtonText: '确认解散',
      confirmButtonColor: '#ee0a24'
    })

    await handleDissolveTeam()
  } catch {
    // 用户取消
  }
}

// 切换踢出模式
const toggleKickMode = () => {
  kickMode.value = !kickMode.value
}

// 判断是否显示踢出按钮
const showKickButton = (member: TeamUserVO) => {
  return kickMode.value &&
         isCaptain.value &&
         member.id !== captainInfo.value?.id
}

// 踢出成员
const onKickMember = async (member: TeamUserVO) => {
  if (!showKickButton(member)) return

  try {
    await showConfirmDialog({
      title: '踢出成员',
      message: `确定要踢出 ${member.username} 吗？`
    })

    showLoadingToast({
      message: '踢出中...',
      forbidClick: true,
      duration: 0
    })

    await kickOutUser({
      teamId: teamInfo.value!.id,
      userId: member.id
    })

    closeToast()
    showSuccessToast('已踢出成员')

    // 刷新队伍详情
    await loadTeamDetail()
    // 保持踢出模式，可继续踢出其他人
  } catch (error: any) {
    closeToast()
    console.error('踢出成员失败：', error)
    if (error !== 'cancel') {
      showToast(error.message || '踢出失败，请重试')
    }
  }
}

// 解散队伍
const handleDissolveTeam = async () => {
  try {
    showLoadingToast({
      message: '解散中...',
      forbidClick: true,
      duration: 0
    })

    await deleteTeam({ teamId: teamInfo.value!.id })

    closeToast()
    showSuccessToast('队伍已解散')

    // 延迟后返回上一页
    setTimeout(() => {
      router.back()
    }, 1500)
  } catch (error: any) {
    closeToast()
    console.error('解散队伍失败：', error)
    showToast(error.message || '解散失败，请重试')
  }
}

// 邀请好友
const onInviteFriend = () => {
  showToast('邀请好友功能开发中...')
}

// 点击队伍聊天
const onTeamChat = () => {
  if (!teamInfo.value) return
  router.push(`/chat/team/${teamInfo.value.id}`)
}

// 加载队伍详情
const loadTeamDetail = async () => {
  const teamId = Number(route.params.id)

  if (!teamId) {
    showToast('队伍ID无效')
    router.back()
    return
  }

  loading.value = true

  try {
    showLoadingToast({
      message: '加载中...',
      forbidClick: true,
      duration: 0
    })

    // 第1步：先判断用户身份（轻量级接口）
    console.log('第1步：调用 membership 接口判断身份 /team/' + teamId + '/membership')
    const membershipResponse = await getTeamMembership(teamId)

    console.log('身份判断结果：', membershipResponse.data)
    console.log('是否成员：', membershipResponse.data.isMember)
    console.log('角色：', membershipResponse.data.role)

    // 第2步：根据身份调用对应接口
    if (membershipResponse.data.isMember) {
      // 是成员 → 调用详细接口
      console.log('用户是成员，调用详细接口：/team/' + teamId)
      const detailResponse = await getTeamById(teamId)

      closeToast()

      if (detailResponse.code === 0 && detailResponse.data) {
        teamInfo.value = detailResponse.data
        console.log('✅ 详细信息加载成功，显示成员视图')
        console.log('成员数量：', detailResponse.data.userSet?.length)
      } else {
        showToast('加载失败，请稍后重试')
      }
    } else {
      // 不是成员 → 调用基础接口
      console.log('用户不是成员，调用基础接口：/team/' + teamId + '/basic')
      const basicResponse = await getTeamBasicInfo(teamId)

      closeToast()

      if (basicResponse.code === 0 && basicResponse.data) {
        teamInfo.value = basicResponse.data
        console.log('✅ 基础信息加载成功，显示非成员视图')
        console.log('队伍信息：', basicResponse.data)
      } else {
        showToast('加载失败，请稍后重试')
      }
    }
  } catch (error: any) {
    closeToast()
    console.error('加载队伍详情失败：', error)

    // 兼容处理：如果 membership 接口失败，降级为试错法
    console.warn('membership 接口失败，降级使用试错法')

    try {
      showLoadingToast({
        message: '加载中...',
        forbidClick: true,
        duration: 0
      })

      // 先尝试详细信息接口
      const detailResponse = await getTeamById(teamId)
      closeToast()

      if (detailResponse.code === 0 && detailResponse.data) {
        teamInfo.value = detailResponse.data
        console.log('✅ 详细信息加载成功（降级），显示成员视图')
      } else {
        showToast('加载失败，请稍后重试')
      }
    } catch (detailError: any) {
      closeToast()

      // 如果是 40101 错误，再调用基础接口
      if (detailError.response?.data?.code === 40101) {
        console.log('用户不是成员，调用基础接口')

        try {
          showLoadingToast({
            message: '加载中...',
            forbidClick: true,
            duration: 0
          })

          const basicResponse = await getTeamBasicInfo(teamId)
          closeToast()

          if (basicResponse.code === 0 && basicResponse.data) {
            teamInfo.value = basicResponse.data
            console.log('✅ 基础信息加载成功（降级），显示非成员视图')
          } else {
            showToast('加载失败，请稍后重试')
          }
        } catch (basicError: any) {
          closeToast()
          console.error('加载基础信息也失败：', basicError)
          showToast(basicError.message || '加载失败，请稍后重试')
        }
      } else {
        showToast(error.message || '加载失败，请稍后重试')
      }
    }
  } finally {
    loading.value = false
  }
}

// 获取队伍状态文本
const getTeamStatusText = (status?: number) => {
  switch (status) {
    case 0:
      return '公开'
    case 1:
      return '私有'
    case 2:
      return '加密'
    default:
      return '未知'
  }
}

// 格式化时间
const formatTime = (time?: string) => {
  if (!time) return '未设置'

  // 处理中文日期格式：2026年05月19日 00:00:00
  let parsedTime = time
  if (time.includes('年') && time.includes('月')) {
    // 将中文日期转换为标准格式：2026-05-19 00:00:00
    parsedTime = time
      .replace(/(\d{4})年(\d{2})月(\d{2})日/, '$1-$2-$3')
  }

  const date = new Date(parsedTime)

  // 检查日期是否有效
  if (isNaN(date.getTime())) {
    return '时间格式错误'
  }

  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 格式化过期时间
const formatExpireTime = (time?: string) => {
  if (!time) return '未设置'

  // 处理中文日期格式：2026年05月19日 00:00:00
  let parsedTime = time
  if (time.includes('年') && time.includes('月')) {
    // 将中文日期转换为标准格式：2026-05-19 00:00:00
    parsedTime = time
      .replace(/(\d{4})年(\d{2})月(\d{2})日/, '$1-$2-$3')
  }

  const date = new Date(parsedTime)
  const now = new Date()

  // 检查日期是否有效
  if (isNaN(date.getTime())) {
    return '时间格式错误'
  }

  const diff = date.getTime() - now.getTime()

  if (diff <= 0) {
    return '已过期'
  }

  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  if (days > 0) {
    return `${days}天后过期`
  }

  const hours = Math.floor(diff / (1000 * 60 * 60))
  if (hours > 0) {
    return `${hours}小时后过期`
  }

  const minutes = Math.floor(diff / (1000 * 60))
  return `${minutes}分钟后过期`
}

// 打开日期时间选择器
const openDatePicker = () => {
  showDatePicker.value = true

  // 如果已有值，解析为日期数组和时间
  if (updateForm.value.expireTime) {
    // 处理中文日期格式：2026年05月19日 00:00:00
    let parsedTime = updateForm.value.expireTime
    if (updateForm.value.expireTime.includes('年') && updateForm.value.expireTime.includes('月')) {
      parsedTime = updateForm.value.expireTime
        .replace(/(\d{4})年(\d{2})月(\d{2})日/, '$1-$2-$3')
    }

    const date = new Date(parsedTime)
    if (!isNaN(date.getTime())) {
      const year = String(date.getFullYear())
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      selectedDateArray.value = [year, month, day]

      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      selectedTimeArray.value = [hours, minutes]
    }
  } else {
    // 默认当前时间
    const now = new Date()
    const year = String(now.getFullYear())
    const month = String(now.getMonth() + 1).padStart(2, '0')
    const day = String(now.getDate()).padStart(2, '0')
    selectedDateArray.value = [year, month, day]
    selectedTimeArray.value = ['00', '00']
  }
}

// 确认选择日期时间
const onConfirmDateTime = () => {
  const [year, month, day] = selectedDateArray.value
  const [hours, minutes] = selectedTimeArray.value

  // 组合成 API 格式：yyyy年MM月dd日 HH:mm:ss（后端期望格式）
  updateForm.value.expireTime = `${year}年${month}月${day}日 ${hours}:${minutes}:00`

  showDatePicker.value = false
}

// 取消选择日期时间
const onCancelDateTime = () => {
  showDatePicker.value = false
}

// 加入队伍（公开队伍）
const onJoinTeam = async () => {
  if (!userInfo.value) {
    showToast('请先登录')
    router.push('/login')
    return
  }

  if (!teamInfo.value) return

  try {
    showLoadingToast({
      message: '加入中...',
      forbidClick: true,
      duration: 0
    })

    await joinTeam({
      teamId: teamInfo.value.id
    })

    closeToast()
    showSuccessToast('已加入队伍')

    // 延迟后刷新页面数据
    setTimeout(() => {
      loadTeamDetail()
    }, 1000)
  } catch (error: any) {
    closeToast()
    console.error('加入队伍失败：', error)
    showToast(error.message || '加入失败，请重试')
  }
}

// 申请加入（私人队伍）
const onApplyJoin = async () => {
  if (!userInfo.value) {
    showToast('请先登录')
    router.push('/login')
    return
  }

  if (!teamInfo.value) return

  try {
    await showConfirmDialog({
      title: '申请加入',
      message: '是否向队长发送加入申请？',
      confirmButtonText: '发送申请',
      confirmButtonColor: '#FB7299'
    })

    showLoadingToast({
      message: '发送中...',
      forbidClick: true,
      duration: 0
    })

    await applyToJoinTeam({
      teamId: teamInfo.value.id
    })

    closeToast()
    showSuccessToast('申请已发送，等待队长审核')
  } catch (error: any) {
    closeToast()
    if (error !== 'cancel') {
      console.error('申请加入失败：', error)
      showToast(error.message || '申请失败，请重试')
    }
  }
}

// 打开密码输入弹窗（加密队伍）
const onOpenPasswordDialog = () => {
  if (!userInfo.value) {
    showToast('请先登录')
    router.push('/login')
    return
  }

  passwordForm.value.password = ''
  showPasswordDialog.value = true
}

// 密码加入队伍
const onPasswordJoin = async () => {
  if (!passwordForm.value.password) {
    showToast('请输入密码')
    return
  }

  if (!teamInfo.value) return

  try {
    showLoadingToast({
      message: '加入中...',
      forbidClick: true,
      duration: 0
    })

    await joinTeam({
      teamId: teamInfo.value.id,
      password: passwordForm.value.password
    })

    closeToast()
    showPasswordDialog.value = false
    showSuccessToast('已加入队伍')

    // 延迟后刷新页面数据
    setTimeout(() => {
      loadTeamDetail()
    }, 1000)
  } catch (error: any) {
    closeToast()
    console.error('密码加入失败：', error)
    showToast(error.message || '密码错误或加入失败')
  }
}

// 组件挂载时加载队伍详情
onMounted(() => {
  loadTeamDetail()
})
</script>

<template>
  <div class="team-detail-page">
    <!-- 顶部导航栏 -->
    <van-nav-bar
      :title="teamInfo?.teamName || '队伍详情'"
      left-text="返回"
      left-arrow
      @click-left="onClickLeft"
    >
      <template #right>
        <van-icon v-if="isCaptain" name="edit" size="18" @click="onClickUpdate" />
      </template>
    </van-nav-bar>

    <div v-if="teamInfo" class="team-detail-content">
      <!-- 队伍基本信息卡片 -->
      <div class="info-card">
        <!-- 队伍名称和状态 -->
        <div class="team-header">
          <h2 class="team-name">{{ teamInfo.teamName }}</h2>
          <van-tag :type="teamInfo.teamStatus === 0 ? 'primary' : 'default'" size="medium">
            {{ getTeamStatusText(teamInfo.teamStatus) }}
          </van-tag>
        </div>

        <!-- 队伍描述 -->
        <div class="team-description">
          <div class="section-title">队伍简介</div>
          <div v-if="teamInfo.teamDesc" class="description-text">
            {{ teamInfo.teamDesc }}
          </div>
          <div v-else class="description-text empty">
            暂无描述
          </div>
        </div>

        <!-- 队伍公告 -->
        <div v-if="teamInfo.announce" class="team-description">
          <div class="section-title">队伍公告</div>
          <div class="description-text">
            {{ teamInfo.announce }}
          </div>
        </div>

        <!-- 队伍信息 -->
        <div class="team-info-list">
          <div class="info-item">
            <span class="info-label">队伍人数</span>
            <span class="info-value">
              {{ currentTeamNum }}/{{ teamInfo.maxNum }}人
            </span>
          </div>
          <div class="info-item">
            <span class="info-label">过期时间</span>
            <span class="info-value">{{ formatExpireTime(teamInfo.expireTime) }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">创建时间</span>
            <span class="info-value">{{ formatTime(teamInfo.createTime) }}</span>
          </div>
        </div>
      </div>

      <!-- 成员列表（队伍成员可见） -->
      <div v-if="isTeamMember" class="members-section">
        <div class="section-header">
          <h3 class="section-title">队伍成员</h3>
          <van-tag type="primary" size="medium">
            {{ teamInfo.userSet?.length || 0 }}人
          </van-tag>
        </div>

        <!-- 操作按钮组 -->
        <div class="member-actions">
          <!-- 邀请好友按钮 -->
          <van-button
            size="small"
            type="primary"
            icon="plus"
            @click="onInviteFriend"
          >
            邀请好友
          </van-button>

          <!-- 转让队长按钮 (仅队长可见) -->
          <van-button
            v-if="isCaptain"
            size="small"
            type="warning"
            icon="exchange"
            :disabled="!canTransferCaptain"
            @click="openTransferMemberSheet"
          >
            转让队长
          </van-button>

          <!-- 踢出队友按钮 (仅队长可见) -->
          <van-button
            v-if="isCaptain"
            size="small"
            :type="kickMode ? 'danger' : 'default'"
            :icon="kickMode ? 'cross' : 'delete'"
            @click="toggleKickMode"
          >
            {{ kickMode ? '取消' : '踢出队友' }}
          </van-button>
        </div>

        <div class="members-list">
          <div
            v-for="(member, index) in teamInfo.userSet"
            :key="member.id"
            class="member-item"
          >
            <div class="member-rank">{{ index + 1 }}</div>

            <van-image
              round
              width="48"
              height="48"
              :src="member.userAvatarUrl || 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'"
              class="member-avatar"
            />

            <div class="member-info">
              <div class="member-header">
                <span class="member-name">{{ member.username }}</span>
                <van-tag v-if="captainInfo && member.id === captainInfo.id" type="danger" size="small">
                  队长
                </van-tag>
              </div>
            </div>

            <!-- 踢出按钮 (踢出模式 + 队长 + 不是自己) -->
            <van-icon
              v-if="showKickButton(member)"
              name="close"
              size="20"
              color="#ee0a24"
              class="kick-icon"
              @click="onKickMember(member)"
            />
          </div>
        </div>
      </div>

      <!-- 队长信息（非成员可见） -->
      <div v-else class="members-section">
        <div class="section-header">
          <h3 class="section-title">队长信息</h3>
        </div>

        <div v-if="captainInfo" class="captain-info-card">
          <van-image
            round
            width="64"
            height="64"
            :src="captainInfo.userAvatarUrl || 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'"
            class="captain-avatar"
          />
          <div class="captain-details">
            <div class="captain-name">{{ captainInfo.username }}</div>
            <div class="captain-account">账号：{{ captainInfo.userAccount }}</div>
            <div v-if="captainInfo.userDesc" class="captain-desc">
              {{ captainInfo.userDesc }}
            </div>
          </div>
        </div>
      </div>

      <!-- 队伍标签（非成员可见） -->
      <div v-if="!isTeamMember && teamTags.length > 0" class="members-section">
        <div class="section-header">
          <h3 class="section-title">⭐ 队伍标签</h3>
        </div>

        <div class="tags-container">
          <van-tag
            v-for="(tag, index) in teamTags"
            :key="index"
            type="primary"
            size="medium"
            plain
            class="team-tag"
          >
            {{ tag }}
          </van-tag>
        </div>
      </div>

      <!-- 队伍要求（非成员可见） -->
      <div v-if="!isTeamMember && teamRequirements" class="members-section">
        <div class="section-header">
          <h3 class="section-title">📋 队伍要求</h3>
        </div>

        <div class="requirements-text">
          {{ teamRequirements }}
        </div>
      </div>

      <!-- 加入队伍按钮（非成员可见） -->
      <div v-if="!isTeamMember" class="join-section">
        <!-- 公开队伍：直接加入 -->
        <van-button
          v-if="teamInfo.teamStatus === 0"
          type="primary"
          block
          round
          icon="plus"
          size="large"
          @click="onJoinTeam"
        >
          加入队伍
        </van-button>

        <!-- 私人队伍：申请加入 -->
        <van-button
          v-else-if="teamInfo.teamStatus === 1"
          type="primary"
          block
          round
          icon="send-gift-o"
          size="large"
          @click="onApplyJoin"
        >
          申请加入
        </van-button>

        <!-- 加密队伍：输入密码加入 -->
        <van-button
          v-else-if="teamInfo.teamStatus === 2"
          type="primary"
          block
          round
          icon="lock"
          size="large"
          @click="onOpenPasswordDialog"
        >
          输入密码加入
        </van-button>
      </div>

      <!-- 队伍聊天按钮（成员可见） -->
      <div v-if="isTeamMember" class="chat-section">
        <van-button
          type="primary"
          block
          round
          icon="chat-o"
          @click="onTeamChat"
        >
          队伍聊天
        </van-button>
      </div>

      <!-- 退出/解散队伍按钮（成员可见） -->
      <div v-if="isTeamMember" class="quit-section">
        <van-button
          v-if="isOnlyCaptain"
          type="danger"
          block
          round
          icon="delete-o"
          @click="handleDissolveTeamWithConfirm"
        >
          解散队伍
        </van-button>
        <van-button
          v-else
          type="danger"
          block
          round
          icon="sign-out"
          @click="handleQuitTeamWithConfirm"
        >
          退出队伍
        </van-button>
      </div>
    </div>

    <!-- 操作菜单 -->
    <van-action-sheet
      v-model:show="showActionSheet"
      :actions="actionSheetOptions"
      @select="onSelectAction"
      cancel-text="取消"
    />

    <!-- 转让队长成员选择弹窗 -->
    <van-action-sheet
      v-model:show="showTransferMemberSheet"
      :actions="transferMemberActions"
      cancel-text="取消"
      title="选择新队长"
    />

    <!-- 更新队伍信息弹窗 -->
    <van-dialog
      v-model:show="showUpdateDialog"
      title="更新队伍信息"
      show-cancel-button
      @confirm="handleUpdateTeam"
    >
      <div class="update-form">
        <van-field
          v-model="updateForm.teamName"
          label="队伍名称"
          placeholder="请输入队伍名称"
          maxlength="20"
          show-word-limit
        />
        <van-field
          v-model="updateForm.teamDesc"
          type="textarea"
          label="队伍描述"
          placeholder="请输入队伍描述"
          maxlength="200"
          show-word-limit
          rows="3"
        />
        <van-field
          v-model="updateForm.maxNum"
          type="number"
          label="最大人数"
          placeholder="请输入最大人数"
          :min="1"
        />
        <van-field
          :model-value="updateForm.expireTime"
          label="过期时间"
          placeholder="请选择过期时间"
          readonly
          is-link
          @click="openDatePicker"
        />
        <van-field name="teamStatus" label="队伍状态">
          <template #input>
            <van-radio-group v-model="updateForm.teamStatus" direction="horizontal">
              <van-radio :name="0">公开</van-radio>
              <van-radio :name="1">私有</van-radio>
              <van-radio :name="2">加密</van-radio>
            </van-radio-group>
          </template>
        </van-field>
        <van-field
          v-model="updateForm.teamPassword"
          type="password"
          label="队伍密码"
          placeholder="加密队伍需要密码"
        />
        <van-field
          v-model="updateForm.announce"
          type="textarea"
          label="队伍公告"
          placeholder="请输入队伍公告"
          maxlength="200"
          show-word-limit
          rows="2"
        />
      </div>
    </van-dialog>

    <!-- 转让队长弹窗 -->
    <van-dialog
      v-model:show="showTransferDialog"
      title="转让队长"
      show-cancel-button
      confirm-button-text="转让"
      @confirm="handleTransferTeam"
    >
      <div class="transfer-form">
        <van-field
          v-model="transferForm.userAccount"
          label="对方账号"
          placeholder="请输入要转让给的成员账号"
        />
        <div class="tips">
          <van-icon name="info-o" />
          <span>转让后您将失去队长权限，请谨慎操作</span>
        </div>
      </div>
    </van-dialog>

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

    <!-- 密码输入弹窗 -->
    <van-dialog
      v-model:show="showPasswordDialog"
      title="输入队伍密码"
      show-cancel-button
      confirm-button-text="确认加入"
      @confirm="onPasswordJoin"
    >
      <van-field
        v-model="passwordForm.password"
        type="password"
        label="密码"
        placeholder="请输入队伍密码"
        clearable
      />
    </van-dialog>
  </div>
</template>

<style scoped>
.team-detail-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.team-detail-content {
  padding: 16px;
  padding-bottom: 30px;
}

/* 队伍基本信息卡片 */
.info-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

/* 队伍头部 */
.team-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebedf0;
}

.team-name {
  font-size: 20px;
  font-weight: 600;
  color: #323233;
  margin: 0;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 队伍描述 */
.team-description {
  margin-bottom: 16px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #323233;
  margin-bottom: 8px;
}

.description-text {
  font-size: 14px;
  color: #646566;
  line-height: 1.6;
  white-space: pre-wrap;
}

.description-text.empty {
  color: #c8c9cc;
  font-style: italic;
}

/* 队伍信息列表 */
.team-info-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
}

.info-label {
  color: #646566;
}

.info-value {
  color: #323233;
  font-weight: 500;
}

/* 成员列表 */
.members-section {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.members-section .section-title {
  margin-bottom: 12px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

/* 操作按钮组 */
.member-actions {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.members-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.member-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f7f8fa;
  border-radius: 8px;
  position: relative;
}

/* 踢出按钮 */
.kick-icon {
  flex-shrink: 0;
  cursor: pointer;
  padding: 4px;
  margin-left: 4px;
  transition: all 0.2s;
}

.kick-icon:active {
  transform: scale(0.9);
}

.member-rank {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  color: #969799;
  background: #fff;
  border-radius: 50%;
}

.member-item:nth-child(1) .member-rank {
  background: linear-gradient(135deg, #ffd700 0%, #ffed4e 100%);
  color: #fff;
}

.member-item:nth-child(2) .member-rank {
  background: linear-gradient(135deg, #c0c0c0 0%, #e8e8e8 100%);
  color: #fff;
}

.member-item:nth-child(3) .member-rank {
  background: linear-gradient(135deg, #cd7f32 0%, #e5a865 100%);
  color: #fff;
}

.member-avatar {
  flex-shrink: 0;
}

.member-info {
  flex: 1;
  min-width: 0;
}

.member-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.member-name {
  font-size: 15px;
  font-weight: 500;
  color: #323233;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member-join-time {
  font-size: 12px;
  color: #969799;
}

/* 聊天按钮 */
.chat-section {
  margin-top: 16px;
  margin-bottom: 12px;
}

/* 退出/解散队伍按钮 */
.quit-section {
  margin-top: 0;
}

/* 队长信息卡片 */
.captain-info-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #f7f8fa;
  border-radius: 8px;
}

.captain-avatar {
  flex-shrink: 0;
}

.captain-details {
  flex: 1;
  min-width: 0;
}

.captain-name {
  font-size: 16px;
  font-weight: bold;
  color: #323233;
  margin-bottom: 4px;
}

.captain-account {
  font-size: 13px;
  color: #969799;
  margin-bottom: 4px;
}

.captain-desc {
  font-size: 13px;
  color: #646566;
  line-height: 1.5;
}

/* 队伍标签容器 */
.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 4px 0;
}

.team-tag {
  transition: all 0.2s;
}

.team-tag:active {
  transform: scale(0.95);
}

/* 队伍要求文字 */
.requirements-text {
  font-size: 14px;
  color: #646566;
  line-height: 1.6;
  white-space: pre-wrap;
  padding: 12px;
  background: #fffbe8;
  border-radius: 8px;
  border-left: 3px solid #ff976a;
}

/* 加入队伍按钮区域 */
.join-section {
  margin-top: 16px;
  padding: 0 16px 16px;
}

/* 更新表单 */
.update-form {
  padding: 16px;
}

.transfer-form {
  padding: 16px;
}

.tips {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: #fffbe8;
  border-radius: 4px;
  font-size: 13px;
  color: #ed6a0c;
  margin-top: 12px;
}
</style>
