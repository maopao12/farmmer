import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '数据驾驶舱', icon: 'Odometer' }
      },
      {
        path: 'monitor/:plotId',
        name: 'Monitor',
        component: () => import('@/views/Monitor.vue'),
        meta: { title: '地块实时监测' }
      },
      {
        path: 'trends',
        name: 'Trends',
        component: () => import('@/views/Trends.vue'),
        meta: { title: '历史趋势分析', icon: 'TrendCharts' }
      },
      {
        path: 'devices',
        name: 'Devices',
        component: () => import('@/views/Devices.vue'),
        meta: { title: '设备管理', icon: 'Cpu' }
      },
      {
        path: 'control',
        name: 'Control',
        component: () => import('@/views/Control.vue'),
        meta: { title: '设备控制', icon: 'Switch' }
      },
      {
        path: 'alerts',
        name: 'Alerts',
        component: () => import('@/views/Alerts.vue'),
        meta: { title: '告警中心', icon: 'Bell' }
      },
      {
        path: 'ai-assistant',
        name: 'AiAssistant',
        component: () => import('@/views/AiAssistant.vue'),
        meta: { title: 'AI农事助手', icon: 'ChatDotRound' }
      },
      {
        path: 'screen',
        name: 'Screen',
        component: () => import('@/views/FullScreen.vue'),
        meta: { title: '全屏数据大屏' }
      },
      {
        path: 'logs',
        name: 'Logs',
        component: () => import('@/views/Logs.vue'),
        meta: { title: '操作日志', icon: 'Document' }
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/Settings.vue'),
        meta: { title: '系统设置', icon: 'Setting', admin: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：未登录跳转
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
