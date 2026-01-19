import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    redirect: '/layout/home'
  },
  // 主布局路由（带底部导航）
  {
    path: '/layout',
    component: () => import('@/views/LayoutView.vue'),
    children: [
      {
        path: 'home',
        name: 'LayoutHome',
        component: () => import('@/views/HomeTabView.vue'),
        meta: { title: '首页', keepAlive: true, showTabbar: true, showNavbar: true }
      },
      {
        path: 'team',
        name: 'LayoutTeam',
        component: () => import('@/views/TeamTabView.vue'),
        meta: { title: '队伍', keepAlive: true, showTabbar: true, showNavbar: false }
      },
      {
        path: 'publish',
        name: 'LayoutPublish',
        component: () => import('@/views/PublishTabView.vue'),
        meta: { title: '发布', showTabbar: true, showNavbar: true }
      },
      {
        path: 'message',
        name: 'LayoutMessage',
        component: () => import('@/views/MessageTabView.vue'),
        meta: { title: '消息', keepAlive: true, showTabbar: true, showNavbar: true }
      },
      {
        path: 'profile',
        name: 'LayoutProfile',
        component: () => import('@/views/ProfileTabView.vue'),
        meta: { title: '我的', keepAlive: true, showTabbar: true, showNavbar: true }
      }
    ]
  },
  // 登录相关路由（不带底部导航）
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录', showTabbar: false, showNavbar: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/RegisterView.vue'),
    meta: { title: '注册', showTabbar: false, showNavbar: false }
  },
  {
    path: '/home',
    redirect: '/layout/home'
  },
  // 用户相关路由（不带底部导航）
  {
    path: '/user/:id',
    name: 'UserProfile',
    component: () => import('@/views/UserProfileView.vue'),
    meta: { title: '用户详情', showTabbar: false }
  },
  {
    path: '/user/profile',
    name: 'Profile',
    component: () => import('@/views/ProfileView.vue'),
    meta: { title: '个人中心', showTabbar: false }
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('@/views/SettingsView.vue'),
    meta: { title: '设置', showTabbar: false }
  },
  {
    path: '/search',
    name: 'Search',
    component: () => import('@/views/SearchView.vue'),
    meta: { title: '搜索', showTabbar: false }
  },
  // 队伍相关路由（不带底部导航）
  {
    path: '/team/detail/:id',
    name: 'TeamDetail',
    component: () => import('@/views/TeamDetailView.vue'),
    meta: { title: '队伍详情', showTabbar: false }
  },
  {
    path: '/team/square',
    name: 'TeamSquare',
    component: () => import('@/views/TeamSquareView.vue'),
    meta: { title: '队伍广场', showTabbar: false }
  },
  {
    path: '/team/create',
    name: 'TeamCreate',
    component: () => import('@/views/TeamCreateView.vue'),
    meta: { title: '创建队伍', showTabbar: false }
  },
  {
    path: '/team/my',
    name: 'MyTeams',
    component: () => import('@/views/MyTeamsView.vue'),
    meta: { title: '我的队伍', showTabbar: false }
  },
  {
    path: '/team/search',
    name: 'TeamSearch',
    component: () => import('@/views/SearchTeamView.vue'),
    meta: { title: '搜索队伍', showTabbar: false }
  },
  {
    path: '/team/create',
    name: 'CreateTeam',
    component: () => import('@/views/TeamCreateView.vue'),
    meta: { title: '创建队伍', showTabbar: false }
  },
  {
    path: '/team/:id',
    name: 'TeamView',
    component: () => import('@/views/TeamDetailView.vue'),
    meta: { title: '队伍详情', showTabbar: false }
  },
  {
    path: '/team/invitations',
    name: 'TeamInvitations',
    component: () => import('@/views/TeamInvitationsView.vue'),
    meta: { title: '队伍邀请', showTabbar: false }
  },
  {
    path: '/team/applications/received',
    name: 'TeamApplicationsReceived',
    component: () => import('@/views/TeamApplicationsReceivedView.vue'),
    meta: { title: '队伍审批', showTabbar: false }
  },
  // 好友相关路由（不带底部导航）
  {
    path: '/friend/list',
    name: 'FriendList',
    component: () => import('@/views/FriendListView.vue'),
    meta: { title: '好友列表', showTabbar: false }
  },
  {
    path: '/friend/request',
    name: 'FriendRequest',
    component: () => import('@/views/FriendRequestView.vue'),
    meta: { title: '好友申请', showTabbar: false }
  },
  {
    path: '/friend/add',
    name: 'FriendAdd',
    component: () => import('@/views/FriendListView.vue'),
    meta: { title: '添加好友', showTabbar: false }
  },
  {
    path: '/my-applications',
    name: 'MyApplications',
    component: () => import('@/views/MyApplicationsView.vue'),
    meta: { title: '我的申请', showTabbar: false }
  },
  // 聊天相关路由（不带底部导航）
  {
    path: '/chat/:type/:id',
    name: 'ChatDetail',
    component: () => import('@/views/ChatDetailView.vue'),
    meta: { title: '聊天详情', showTabbar: false }
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// 路由守卫 - 设置页面标题
router.beforeEach((to, from, next) => {
  if (to.meta?.title) {
    document.title = `${to.meta.title} - 惠星伙伴匹配`
  }
  next()
})

export default router
