export interface TabBarItem {
  name: string
  routeName: string
  icon: string
  label: string
}

export const tabbarConfig: TabBarItem[] = [
  {
    name: 'home',
    routeName: 'LayoutHome',
    icon: 'wap-home-o',
    label: '首页'
  },
  {
    name: 'team',
    routeName: 'LayoutTeam',
    icon: 'friends-o',
    label: '队伍'
  },
  {
    name: 'publish',
    routeName: 'LayoutPublish',
    icon: 'add-o',
    label: '发布'
  },
  {
    name: 'message',
    routeName: 'LayoutMessage',
    icon: 'chat-o',
    label: '消息'
  },
  {
    name: 'profile',
    routeName: 'LayoutProfile',
    icon: 'user-o',
    label: '我的'
  }
]

// 根据路由名称获取激活的 Tab 索引
export function getActiveTabIndex(routeName: string): number {
  const index = tabbarConfig.findIndex(tab => tab.routeName === routeName)
  return index >= 0 ? index : 0
}
