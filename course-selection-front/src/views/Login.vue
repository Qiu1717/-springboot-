<template>
  <div class="login-page">
    <!-- 左侧品牌区 -->
    <div class="login-left">
      <div class="left-overlay"></div>
      <div class="left-content">
        <h1 class="system-name">学生选课管理系统</h1>
        <p class="system-desc">Course Selection System</p>
        <p class="system-sub">在线选课 · 成绩查询 · 教务管理</p>
      </div>
    </div>

    <!-- 右侧登录表单 -->
    <div class="login-right">
      <div class="form-wrapper">
        <h2 class="form-title">欢迎回来</h2>
        <p class="form-subtitle">请使用学校统一分配的账号登录</p>

        <el-form :model="loginForm" :rules="rules" ref="loginFormRef" class="login-form">
          <el-form-item prop="username">
            <div class="input-icon-group">
              <i class="input-icon">👤</i>
              <el-input
                v-model="loginForm.username"
                placeholder="请输入账号"
                class="custom-input"
              ></el-input>
            </div>
          </el-form-item>

          <el-form-item prop="password">
            <div class="input-icon-group">
              <i class="input-icon">🔒</i>
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="请输入密码"
                class="custom-input"
                @keyup.enter.native="submitLogin"
              ></el-input>
            </div>
          </el-form-item>

          <div class="form-options">
            <el-checkbox v-model="rememberMe">记住账号</el-checkbox>
            <el-checkbox v-model="rememberPwd">记住密码</el-checkbox>
            <a class="forgot-link" @click="openForgotPwd">忘记密码？</a>
          </div>

          <el-button
            type="primary"
            :loading="loading"
            class="login-btn"
            @click="submitLogin"
          >
            登 录
          </el-button>
        </el-form>

        <p class="login-tip">测试账号：admin / teacher1 / student1，密码均为 123456</p>
      </div>
    </div>

    <!-- 忘记密码弹窗 -->
    <el-dialog title="找回密码" :visible.sync="forgotVisible" width="420px" center @closed="resetDone = false">
      <el-form :model="forgotForm" :rules="forgotRules" ref="forgotFormRef" label-width="80px">
        <!-- 角色选择：学生自助重置 / 教师提交申请 -->
        <el-form-item label="身份">
          <el-radio-group v-model="forgotRole" @change="onForgotRoleChange">
            <el-radio label="student">我是学生</el-radio>
            <el-radio label="teacher">我是教师</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="forgotRole === 'teacher' ? '教师账号' : '账号'" prop="username">
          <el-input v-model="forgotForm.username" :placeholder="forgotRole === 'teacher' ? '请输入您的教师账号（如 teacher1）' : '请输入您的学号'"></el-input>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="forgotForm.phone" placeholder="请输入预留手机号"></el-input>
        </el-form-item>
        <el-form-item v-if="resetDone" label="结果">
          <p v-if="forgotRole === 'student'" style="font-size:20px;color:#409EFF;font-weight:bold;text-align:center">密码已重置为 123456</p>
          <p v-else style="font-size:15px;color:#409EFF;font-weight:bold;text-align:center">申请已提交，请联系管理员</p>
          <p style="color:#E6A23C;font-size:12px;text-align:center;margin-top:6px">
            {{ forgotRole === 'student' ? '请使用新密码登录后及时修改' : '管理员收到邮件后会为您处理' }}
          </p>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="forgotVisible = false">取消</el-button>
        <el-button type="primary" :loading="forgotLoading" @click="submitForgotPwd">
          {{ forgotRole === 'teacher' ? '提交申请' : (resetDone ? '重新获取' : '重置密码') }}
        </el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'Login',
  data() {
    return {
      loginForm: { username: '', password: '' },
      rules: {
        username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
      },
      loading: false,
      rememberMe: false,
      rememberPwd: false,  // 记住密码

      forgotVisible: false,
      forgotLoading: false,
      resetDone: false,
      forgotRole: 'student',  // 'student'=学生自助重置, 'teacher'=教师提交申请
      forgotForm: { username: '', phone: '' },
      forgotRules: {
        username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
        phone: [{ required: true, pattern: /^1\d{10}$/, message: '请输入正确的11位手机号', trigger: 'blur' }]
      }
    }
  },
  mounted() {
    const savedUser = localStorage.getItem('rememberedUsername')
    const savedPwd = localStorage.getItem('rememberedPassword')
    if (savedUser) {
      this.loginForm.username = savedUser
      this.rememberMe = true
    }
    if (savedPwd) {
      this.loginForm.password = savedPwd
      this.rememberPwd = true
    }
  },
  methods: {
    onForgotRoleChange() {
      // 切换学生/教师时清空表单和结果
      this.forgotForm = { username: '', phone: '' }
      this.resetDone = false
      this.$nextTick(() => {
        if (this.$refs.forgotFormRef) this.$refs.forgotFormRef.clearValidate()
      })
    },
    /**
     * 登录 — 整个系统的入口
     * 流程: 表单验证 → POST /api/auth/login → 存token+userInfo → 按角色跳转
     */
    submitLogin() {
      this.$refs.loginFormRef.validate(valid => {  // Element UI表单验证(非空检查)
        if (!valid) return
        this.loading = true  // 按钮显示loading状态防重复提交
        request.post('/api/auth/login', this.loginForm).then(res => {
          this.loading = false
          const { token, role, realName, userId } = res.data  // 解构后端返回
          // 存入sessionStorage(浏览器标签页关闭后自动清除)
          sessionStorage.setItem('token', token)  // JWT Token: 后续所有请求通过request.js拦截器自动携带
          sessionStorage.setItem('userInfo', JSON.stringify({ role, realName, userId }))  // 用户信息: Layout.vue读取
          // "记住账号"功能: 存入 localStorage（浏览器长期保留）
          if (this.rememberMe) {
            localStorage.setItem('rememberedUsername', this.loginForm.username)
          } else {
            localStorage.removeItem('rememberedUsername')
          }
          // "记住密码"功能: 存入 localStorage
          if (this.rememberPwd) {
            localStorage.setItem('rememberedPassword', this.loginForm.password)
          } else {
            localStorage.removeItem('rememberedPassword')
          }
          this.$message.success('登录成功，欢迎 ' + realName)
          // 按角色跳转不同首页(路由配置中redirect自动跳到默认子页面)
          if (role === 'ADMIN') {
            this.$router.push('/admin')     // → /admin/notices
          } else if (role === 'TEACHER') {
            this.$router.push('/teacher')   // → /teacher/score
          } else {
            this.$router.push('/student')   // → /student/courses
          }
        }).catch(() => { this.loading = false })  // 错误已由request.js拦截器提示，这里只需恢复loading
      })
    },
    /**
     * 忘记密码 — 学生自助重置 / 教师提交申请给管理员
     */
    submitForgotPwd() {
      this.$refs.forgotFormRef.validate(valid => {
        if (!valid) return
        this.forgotLoading = true
        // 根据身份调用不同接口
        const url = this.forgotRole === 'teacher'
          ? '/api/auth/forgot-password-teacher'  // 教师：发邮件给管理员
          : '/api/auth/forgot-password'           // 学生：自助重置为123456
        request.post(url, this.forgotForm).then(res => {
          this.forgotLoading = false
          this.resetDone = true
          // 教师和学生的成功提示不同
          const msg = this.forgotRole === 'teacher'
            ? '申请已提交，请联系管理员处理'
            : (res.data || '密码已重置为 123456')
          this.$message.success(msg)
        }).catch(() => { this.forgotLoading = false })
      })
    },
    openForgotPwd() {
      this.forgotRole = 'student'  // 默认学生身份
      this.resetDone = false
      this.forgotForm = { username: '', phone: '' }
      this.forgotVisible = true
      this.$nextTick(() => {
        if (this.$refs.forgotFormRef) this.$refs.forgotFormRef.clearValidate()
      })
    }
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

/* ===== 左侧品牌区 ===== */
.login-left {
  flex: 1;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.left-overlay {
  position: absolute;
  top: -50%;
  right: -20%;
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, rgba(64,158,255,0.15) 0%, transparent 70%);
  border-radius: 50%;
}
.left-content {
  position: relative;
  z-index: 1;
  text-align: center;
  padding: 40px;
}
.system-name {
  color: #fff;
  font-size: 32px;
  font-weight: 700;
  margin: 0;
  letter-spacing: 2px;
}
.system-desc {
  color: rgba(255,255,255,0.5);
  font-size: 14px;
  margin: 12px 0 8px;
  letter-spacing: 4px;
  text-transform: uppercase;
}
.system-sub {
  color: rgba(255,255,255,0.35);
  font-size: 13px;
  margin: 24px 0 0;
}

/* ===== 右侧表单区 ===== */
.login-right {
  width: 480px;
  max-width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  padding: 40px;
}
.form-wrapper {
  width: 360px;
  max-width: 100%;
}
.form-title {
  font-size: 26px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0;
}
.form-subtitle {
  color: #999;
  font-size: 13px;
  margin: 8px 0 32px;
}

/* 输入框组 */
.input-icon-group {
  position: relative;
  display: flex;
  align-items: center;
  border: 1px solid #dcdfe6;
  border-radius: 24px;
  overflow: hidden;
  padding: 0 16px;
  transition: border-color 0.3s;
}
.input-icon-group:focus-within {
  border-color: #409EFF;
}
.input-icon {
  font-size: 16px;
  margin-right: 8px;
  font-style: normal;
}
.custom-input {
  flex: 1;
}
.custom-input >>> .el-input__inner {
  border: none !important;
  padding: 0 8px !important;
  height: 44px !important;
  background: transparent !important;
  font-size: 14px;
}

/* 表单选项行 */
.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 8px 0 24px;
}
.forgot-link {
  color: #409EFF;
  font-size: 13px;
  cursor: pointer;
}
.forgot-link:hover {
  text-decoration: underline;
}

/* 登录按钮 */
.login-btn {
  width: 100%;
  height: 44px;
  border-radius: 24px;
  font-size: 16px;
  letter-spacing: 4px;
}

.login-tip {
  text-align: center;
  color: #bbb;
  font-size: 12px;
  margin-top: 20px;
}

/* ===== 移动端 ===== */
@media (max-width: 768px) {
  .login-left {
    display: none;
  }
  .login-right {
    width: 100%;
    padding: 24px 20px;
  }
  .form-wrapper {
    width: 100%;
  }
  .form-title {
    font-size: 22px;
  }
}
</style>