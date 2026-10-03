<template>
  <el-container style="height: 100vh">
    <!-- 桌面端侧边栏 -->
    <el-aside class="desktop-aside" width="220px" style="background-color: #304156">
      <div class="logo-area">
        <h3 style="color:#fff;text-align:center;padding:20px 0;margin:0">选课管理系统</h3>
      </div>
      <el-menu
        :default-active="activeMenu"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        router
      >
        <!-- 管理员：动态菜单或硬编码 -->
        <template v-if="role === 'ADMIN' && menusLoaded">
          <el-menu-item v-for="m in (dynamicMenus.length > 0 ? dynamicMenus : fallbackMenus)" :key="m.id || m.path" :index="m.path">
            <i :class="menuIcon(m.name)"></i><span>{{ m.name }}</span>
          </el-menu-item>
        </template>
        <!-- 教师：基础菜单 + 额外分配的管理菜单 -->
        <template v-if="role === 'TEACHER'">
          <el-menu-item index="/teacher/my-courses"><i class="el-icon-reading"></i><span>我的课程</span></el-menu-item>
          <el-menu-item index="/teacher/score"><i class="el-icon-edit-outline"></i><span>成绩录入</span></el-menu-item>
          <el-menu-item index="/teacher/files"><i class="el-icon-folder-opened"></i><span>课程资料</span></el-menu-item>
          <el-menu-item index="/teacher/messages"><i class="el-icon-bell"></i><span>我的消息</span></el-menu-item>
          <el-menu-item v-if="menusLoaded" v-for="m in dynamicMenus" :key="m.id" :index="m.path">
            <i :class="menuIcon(m.name)"></i><span>{{ m.name }}</span>
          </el-menu-item>
        </template>
        <!-- 学生：基础菜单 -->
        <template v-if="role === 'STUDENT'">
          <el-menu-item index="/student/courses"><i class="el-icon-thumb"></i><span>选课中心</span></el-menu-item>
          <el-menu-item index="/student/my-selections"><i class="el-icon-document"></i><span>我的选课</span></el-menu-item>
          <el-menu-item index="/student/notices"><i class="el-icon-bell"></i><span>公告查看</span></el-menu-item>
          <el-menu-item index="/student/files"><i class="el-icon-folder-opened"></i><span>课程资料</span></el-menu-item>
          <el-menu-item index="/student/messages"><i class="el-icon-bell"></i><span>我的消息</span></el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <!-- 移动端抽屉菜单 -->
    <el-drawer
      :visible.sync="drawerVisible"
      direction="ltr"
      size="220px"
      :with-header="false"
      custom-class="mobile-drawer"
    >
      <div style="background:#304156;padding:24px 0 12px;text-align:center">
        <h3 style="color:#fff;margin:0;font-size:16px">选课管理系统</h3>
      </div>
      <el-menu
        :default-active="activeMenu"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        router
        @select="drawerVisible = false"
      >
        <template v-if="role === 'ADMIN' && menusLoaded">
          <el-menu-item v-for="m in (dynamicMenus.length > 0 ? dynamicMenus : fallbackMenus)" :key="m.id || m.path" :index="m.path">
            <i :class="menuIcon(m.name)"></i><span>{{ m.name }}</span>
          </el-menu-item>
        </template>
        <template v-if="role === 'TEACHER'">
          <el-menu-item index="/teacher/my-courses"><i class="el-icon-reading"></i><span>我的课程</span></el-menu-item>
          <el-menu-item index="/teacher/score"><i class="el-icon-edit-outline"></i><span>成绩录入</span></el-menu-item>
          <el-menu-item index="/teacher/files"><i class="el-icon-folder-opened"></i><span>课程资料</span></el-menu-item>
          <el-menu-item index="/teacher/messages"><i class="el-icon-bell"></i><span>我的消息</span></el-menu-item>
          <el-menu-item v-if="menusLoaded" v-for="m in dynamicMenus" :key="m.id" :index="m.path">
            <i :class="menuIcon(m.name)"></i><span>{{ m.name }}</span>
          </el-menu-item>
        </template>
        <template v-if="role === 'STUDENT'">
          <el-menu-item index="/student/courses"><i class="el-icon-thumb"></i><span>选课中心</span></el-menu-item>
          <el-menu-item index="/student/my-selections"><i class="el-icon-document"></i><span>我的选课</span></el-menu-item>
          <el-menu-item index="/student/notices"><i class="el-icon-bell"></i><span>公告查看</span></el-menu-item>
          <el-menu-item index="/student/files"><i class="el-icon-folder-opened"></i><span>课程资料</span></el-menu-item>
          <el-menu-item index="/student/messages"><i class="el-icon-bell"></i><span>我的消息</span></el-menu-item>
        </template>
      </el-menu>
    </el-drawer>

    <!-- 主内容区 -->
    <el-container>
      <el-header class="app-header">
        <div style="display:flex;align-items:center">
          <i class="el-icon-s-fold hamburger" @click="drawerVisible = true"></i>
          <span class="page-title">{{ pageTitle }}</span>
        </div>
        <div class="header-right">
          <span class="user-name">{{ userInfo.realName }}</span>
          <el-tag size="small" :type="roleTagType" class="role-tag">{{ roleText }}</el-tag>
          <!-- 消息铃铛：未读数红点徽章，点击跳转消息页 -->
          <el-badge :value="unreadMessageCount" :hidden="unreadMessageCount === 0" :max="99" style="margin:0 12px;cursor:pointer" @click.native="goMessages">
            <i class="el-icon-bell" style="font-size:18px;color:#606266"></i>
          </el-badge>
          <el-button type="text" style="margin-left:5px" @click="changePwdVisible = true">修改密码</el-button>
          <el-button type="text" style="margin-left:5px" @click="logout">退出</el-button>
        </div>
      </el-header>
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>

    <!-- 修改密码对话框 -->
    <el-dialog title="修改密码" :visible.sync="changePwdVisible" width="420px">
      <el-form :model="pwdForm" :rules="pwdRules" ref="pwdForm" label-width="80px">
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" placeholder="请输入旧密码"></el-input>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" placeholder="请输入新密码"></el-input>
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPwd">
          <el-input v-model="pwdForm.confirmPwd" type="password" placeholder="请再次输入新密码"></el-input>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="changePwdVisible = false">取消</el-button>
        <el-button type="primary" @click="submitChangePwd">确定</el-button>
      </span>
    </el-dialog>
  </el-container>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'Layout',
  data() {
    return {
      userInfo: {},           // 当前用户信息 {role, realName, userId} 从sessionStorage读取
      unreadMessageCount: 0, // 未读消息数（红点徽章）
      drawerVisible: false,   // 移动端抽屉菜单是否可见
      dynamicMenus: [],       // 后端/api/auth/menus返回的动态菜单(管理员/教师额外菜单)
      menusLoaded: false,     // 动态菜单是否加载完成(加载前不渲染避免闪烁)
      fallbackMenus: [        // 管理员默认菜单(如果动态菜单加载失败则显示这个)
        { name: '公告管理', path: '/admin/notices' },
        { name: '教师管理', path: '/admin/teachers' },
        { name: '课程管理', path: '/admin/courses' },
        { name: '菜单管理', path: '/admin/menus' },
        { name: '角色管理', path: '/admin/roles' },
        { name: '学生管理', path: '/admin/students' }
      ],
      changePwdVisible: false,
      pwdForm: { oldPassword: '', newPassword: '', confirmPwd: '' },
      pwdRules: {
        oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
        newPassword: [{ required: true, min: 6, message: '新密码至少6位', trigger: 'blur' }],
        confirmPwd: [
          { required: true, message: '请确认新密码', trigger: 'blur' },
          { validator: (rule, value, cb) => value === this.pwdForm.newPassword ? cb() : cb(new Error('两次密码不一致')), trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    role() {
      return this.userInfo.role || ''  // 当前角色: ADMIN/TEACHER/STUDENT
    },
    activeMenu() {
      return this.$route.path  // 当前路由路径，用于el-menu高亮
    },
    pageTitle() {
      // 根据当前路径显示页面标题(顶部栏左侧文字)
      const map = {
        '/admin/notices': '公告管理', '/admin/teachers': '教师管理',
        '/admin/courses': '课程管理', '/admin/menus': '菜单管理',
        '/admin/roles': '角色管理', '/admin/students': '学生管理',
        '/teacher/my-courses': '我的课程', '/teacher/score': '成绩录入',
        '/student/courses': '选课中心', '/student/my-selections': '我的选课',
        '/student/notices': '公告查看',
        '/student/files': '课程资料', '/teacher/files': '课程资料', '/admin/files': '课程资料',
        '/dashboard/home': '首页'
      }
      return map[this.$route.path] || '学生选课管理系统'
    },
    roleText() {
      const map = { ADMIN: '管理员', TEACHER: '教师', STUDENT: '学生' }
      return map[this.role] || ''
    },
    roleTagType() {
      const map = { ADMIN: 'danger', TEACHER: 'warning', STUDENT: 'success' }  // Element UI tag颜色
      return map[this.role] || 'info'
    }
  },
  created() {
    // 从sessionStorage恢复用户信息(Login.vue登录成功后存入)
    const info = sessionStorage.getItem('userInfo')
    if (info) {
      try {
        this.userInfo = JSON.parse(info)  // JSON字符串 → 对象
      } catch (e) { this.userInfo = {} }
    }
    this.loadMenus()  // 加载动态菜单(管理员和教师会请求/api/auth/menus)
    this.fetchUnreadCount()  // 获取未读消息数
  },
  methods: {
    fetchUnreadCount() {
      request.get('/api/messages/unread-count').then(res => {
        this.unreadMessageCount = res.data || 0
      }).catch(() => {})
    },
    goMessages() {
      // 根据角色跳转到对应的消息页面
      if (this.role === 'STUDENT') this.$router.push('/student/messages')
      else if (this.role === 'TEACHER') this.$router.push('/teacher/messages')
      else this.$router.push('/dashboard/messages')
    },
    loadMenus() {
      // 请求后端动态菜单(根据role_menu表分配的权限)
      request.get('/api/auth/menus').then(res => {
        this.dynamicMenus = res.data || []  // 动态菜单列表 (如管理员被分配了"公告管理"菜单)
        this.menusLoaded = true  // 标记加载完成，模板中的v-if="menusLoaded"开始渲染
      }).catch(() => { this.menusLoaded = true })  // 失败也标记完成，使用fallbackMenus
    },
    menuIcon(name) {  // 根据菜单名返回Element UI图标类名
      const map = {
        '公告管理': 'el-icon-s-order', '教师管理': 'el-icon-user',
        '课程管理': 'el-icon-reading', '菜单管理': 'el-icon-menu',
        '角色管理': 'el-icon-s-custom', '学生管理': 'el-icon-user-solid',
        '课程资料': 'el-icon-folder-opened'
      }
      return map[name] || 'el-icon-menu'
    },
    logout() {
      sessionStorage.removeItem('token')       // 清除Token
      sessionStorage.removeItem('userInfo')    // 清除用户信息
      this.$router.push('/login')              // 跳转登录页
      this.$message.success('已退出登录')
    },
    submitChangePwd() {
      this.$refs.pwdForm.validate(valid => {  // Element UI表单验证
        if (!valid) return
        request.post('/api/auth/change-password', {
          oldPassword: this.pwdForm.oldPassword,
          newPassword: this.pwdForm.newPassword
        }).then(() => {
          this.$message.success('密码修改成功，请重新登录')
          this.changePwdVisible = false
          this.logout()  // 修改密码后强制重新登录
        })
      })
    }
  }
}
</script>

<style scoped>
.desktop-aside { overflow: hidden; }
.el-menu { border-right: none; }

/* Header */
.app-header {
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 50px !important;
  line-height: 50px;
  padding: 0 16px;
}

.hamburger {
  display: none;
  font-size: 20px;
  color: #666;
  cursor: pointer;
  margin-right: 10px;
}
.hamburger:hover { color: #409EFF; }

.page-title { font-size: 16px; font-weight: bold; white-space: nowrap; }

.user-name { color: #666; }
.role-tag { margin-left: 8px; }

.app-main { background: #fff; padding: 12px; overflow-x: auto; }

/* ========== 移动端：≤768px ========== */
@media (max-width: 768px) {
  .desktop-aside { display: none !important; }
  .hamburger { display: inline-block; }
  .page-title { font-size: 14px; }
  .user-name, .role-tag { display: none; }
  .app-main { padding: 8px; }
}

/* 抽屉菜单无内边距 */
:deep(.mobile-drawer) .el-drawer__body { padding: 0; }
</style>

<style>
/* 全局：卡片扁平化 */
.el-card {
  box-shadow: none !important;
  border: 1px solid #ebeef5 !important;
  border-radius: 2px !important;
}
.el-card__header {
  border-bottom: 1px solid #ebeef5 !important;
}

/* 移动端表格可横向滚动 */
@media (max-width: 768px) {
  .el-card__body {
    overflow-x: auto;
  }
  .el-table {
    min-width: 600px;
    font-size: 12px;
  }
  .el-table .cell {
    padding-left: 6px;
    padding-right: 6px;
  }
  .el-form--inline .el-form-item {
    display: block;
    margin-right: 0;
    margin-bottom: 8px;
  }
}
</style>
