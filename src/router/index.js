import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/admin'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '管理员登录' }
  },
  {
    path: '/admin',
    redirect: '/admin/stats',
    component: () => import('@/views/admin/Dashboard.vue'),
    meta: { requiresAuth: true, role: 'admin' },
    children: [
      {
        path: 'stats',
        name: 'Stats',
        component: () => import('@/views/admin/StatsView.vue'),
        meta: { title: '数据统计' }
      },
      {
        path: 'users',
        name: 'Users',
        component: () => import('@/views/admin/UsersView.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'merchants',
        name: 'Merchants',
        component: () => import('@/views/admin/MerchantsView.vue'),
        meta: { title: '商家管理' }
      },
      {
        path: 'riders',
        name: 'Riders',
        component: () => import('@/views/admin/RidersView.vue'),
        meta: { title: '骑手管理' }
      },
      {
        path: 'qualifications',
        name: 'Qualifications',
        component: () => import('@/views/admin/QualificationsView.vue'),
        meta: { title: '资质审核' }
      },
      {
        path: 'orders',
        name: 'Orders',
        component: () => import('@/views/admin/OrdersView.vue'),
        meta: { title: '全平台订单' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} · 管理后台` : '外卖平台 · 管理后台'

  if (to.matched.some(record => record.meta.requiresAuth)) {
    const token = localStorage.getItem('accessToken')
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || 'null')

    if (!token || !userInfo) {
      return next('/login')
    }

    if (to.meta.role && userInfo.role !== to.meta.role) {
      return next('/login')
    }

    return next()
  }

  // 已登录用户访问登录页 → 重定向到后台
  if (to.path === '/login') {
    const token = localStorage.getItem('accessToken')
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || 'null')
    if (token && userInfo?.role === 'admin') {
      return next('/admin/stats')
    }
  }

  next()
})

export default router
