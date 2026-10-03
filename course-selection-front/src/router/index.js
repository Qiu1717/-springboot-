import Vue from 'vue'
import VueRouter from 'vue-router'

Vue.use(VueRouter)

// ===== 路由配置: 三种角色各有独立的路由前缀 =====
// /student/* → 学生端 | /teacher/* → 教师端 | /admin/* → 管理员端
// 所有登录后页面共用 Layout.vue（侧边栏+顶栏+内容区）
const routes = [
  { path: '/', redirect: '/login' },  // 根路径 → 自动跳登录页
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')  // 懒加载: 访问时才加载组件，减小首屏体积
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/Layout.vue'),
    redirect: '/dashboard/home',
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/Home.vue')
      }
    ]
  },
  // 学生路由
  {
    path: '/student',
    name: 'StudentLayout',
    component: () => import('@/views/Layout.vue'),
    redirect: '/student/courses',
    children: [
      {
        path: 'courses',
        name: 'CourseCenter',
        component: () => import('@/views/student/CourseCenter.vue')
      },
      {
        path: 'my-selections',
        name: 'MySelections',
        component: () => import('@/views/student/MySelections.vue')
      },
      {
        path: 'notices',
        name: 'StudentNotices',
        component: () => import('@/views/student/Notices.vue')
      },
      {
        path: 'files',
        name: 'StudentFiles',
        component: () => import('@/views/CourseFile.vue')
      },
      {
        path: 'messages',
        name: 'StudentMessages',
        component: () => import('@/views/common/Messages.vue')
      }
    ]
  },
  // 教师路由
  {
    path: '/teacher',
    name: 'TeacherLayout',
    component: () => import('@/views/Layout.vue'),
    redirect: '/teacher/score',
    children: [
      {
        path: 'score',
        name: 'ScoreEntry',
        component: () => import('@/views/teacher/ScoreEntry.vue')
      },
      {
        path: 'score/:courseId',  // 动态路由参数: 课程ID
        name: 'ScoreDetail',
        component: () => import('@/views/teacher/ScoreDetail.vue')
      },
      {
        path: 'my-courses',
        name: 'TeacherCourses',
        component: () => import('@/views/teacher/MyCourses.vue')
      },
      {
        path: 'files',
        name: 'TeacherFiles',
        component: () => import('@/views/CourseFile.vue')
      },
      {
        path: 'messages',
        name: 'TeacherMessages',
        component: () => import('@/views/common/Messages.vue')
      }
    ]
  },
  // 管理员路由
  {
    path: '/admin',
    name: 'AdminLayout',
    component: () => import('@/views/Layout.vue'),
    redirect: '/admin/notices',
    children: [
      {
        path: 'notices',
        name: 'NoticeManage',
        component: () => import('@/views/admin/NoticeManage.vue')
      },
      {
        path: 'teachers',
        name: 'TeacherManage',
        component: () => import('@/views/admin/TeacherManage.vue')
      },
      {
        path: 'courses',
        name: 'CourseManage',
        component: () => import('@/views/admin/CourseManage.vue')
      },
      {
        path: 'menus',
        name: 'MenuManage',
        component: () => import('@/views/admin/MenuManage.vue')
      },
      {
        path: 'roles',
        name: 'RoleManage',
        component: () => import('@/views/admin/RoleManage.vue')
      },
      {
        path: 'students',
        name: 'StudentManage',
        component: () => import('@/views/admin/StudentManage.vue')
      },
      {
        path: 'files',
        name: 'AdminFiles',
        component: () => import('@/views/CourseFile.vue')
      }
    ]
  }
]

const router = new VueRouter({
  mode: 'hash',  // hash模式: URL带#号，兼容性好，无需服务器端配置
  routes
})

// ===== 全局前置守卫: 每次路由跳转前执行 =====
router.beforeEach((to, from, next) => {
  const token = sessionStorage.getItem('token')  // 检查是否已登录
  if (to.path === '/login') {
    next()  // 去登录页: 直接放行
  } else if (!token) {
    next('/login')  // 未登录去其他页: 强制跳转登录
  } else {
    next()  // 已登录: 放行
  }
})

export default router
