<template>
  <div>
    <el-card>
      <div slot="header">
        <span style="font-size:16px;font-weight:bold">学生管理</span>
        <el-input
          v-model="keyword"
          placeholder="搜索学号或姓名"
          style="width:220px;float:right"
          clearable
          @clear="loadStudents"
          @keyup.enter.native="loadStudents"
        >
          <el-button slot="append" icon="el-icon-search" @click="loadStudents"></el-button>
        </el-input>
      </div>
      <el-table :data="students" border stripe v-loading="loading">
        <el-table-column label="学号" width="120">
          <template slot-scope="scope">{{ scope.row.displayId }}</template>
        </el-table-column>
        <el-table-column prop="username" label="账号" width="120"></el-table-column>
        <el-table-column prop="realName" label="姓名" width="100"></el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="180"></el-table-column>
        <el-table-column prop="phone" label="手机号" width="130"></el-table-column>
        <el-table-column prop="disabled" label="状态" width="80">
          <template slot-scope="scope">
            <el-tag :type="scope.row.disabled === 0 ? 'success' : 'danger'" size="small">
              {{ scope.row.disabled === 0 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="160"></el-table-column>
        <el-table-column label="操作" width="220" align="center">
          <template slot-scope="scope">
            <el-button type="text" size="small" style="color:#409EFF" @click="openEdit(scope.row)">编辑</el-button>
            <span style="color:#dcdfe6">|</span>
            <el-button type="text" size="small" style="color:#E6A23C" @click="resetPwd(scope.row)">重置密码</el-button>
            <span style="color:#dcdfe6">|</span>
            <el-button
              type="text" size="small"
              :style="{color: scope.row.disabled === 0 ? '#E6A23C' : '#67C23A'}"
              @click="toggleStudent(scope.row.id, scope.row.disabled === 0 ? 1 : 0)"
            >
              {{ scope.row.disabled === 0 ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 编辑对话框 -->
    <el-dialog title="编辑学生信息" :visible.sync="editVisible" width="450px">
      <el-form :model="editForm" :rules="editRules" ref="editFormRef" label-width="80px">
        <el-form-item label="账号">
          <el-input v-model="editForm.username" disabled></el-input>
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="editForm.realName" placeholder="请输入姓名"></el-input>
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="editForm.email" placeholder="请输入邮箱"></el-input>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="editForm.phone" placeholder="请输入手机号（用于找回密码）"></el-input>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEdit">保存</el-button>
      </span>
    </el-dialog>

    <!-- 重置密码结果弹窗 -->
    <el-dialog title="重置密码" :visible.sync="pwdVisible" width="400px" center>
      <div style="text-align:center;padding:20px 0">
        <p style="font-size:15px;color:#303133;margin-bottom:8px">
          学生 <b>{{ pwdStudentName }}</b> 的密码已重置为：
        </p>
        <p style="font-size:28px;color:#409EFF;font-weight:bold;margin:12px 0">123456</p>
        <p style="color:#E6A23C;font-size:12px">请提醒学生登录后及时修改密码</p>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'StudentManage',
  data() {
    return {
      students: [],
      loading: false,
      keyword: '',

      editVisible: false,
      editForm: { id: null, username: '', realName: '', email: '', phone: '' },
      editRules: {
        realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
        phone: [{ pattern: /^1\d{10}$/, message: '请输入正确的11位手机号', trigger: 'blur' }]
      },

      pwdVisible: false,
      pwdStudentName: ''
    }
  },
  created() {
    this.loadStudents()
  },
  methods: {
    loadStudents() {
      this.loading = true
      const params = {}
      if (this.keyword) params.keyword = this.keyword
      request.get('/api/admin/students', { params }).then(res => {
        const list = (res.data || []).sort((a, b) => a.username.localeCompare(b.username))
        list.forEach((s, i) => { s.displayId = '202600' + String(i + 1).padStart(2, '0') })
        this.students = list
        this.loading = false
      }).catch(() => { this.loading = false })
    },

    openEdit(row) {
      this.editForm = { id: row.id, username: row.username, realName: row.realName, email: row.email || '', phone: row.phone || '' }
      this.editVisible = true
      this.$nextTick(() => {
        if (this.$refs.editFormRef) this.$refs.editFormRef.clearValidate()
      })
    },

    saveEdit() {
      this.$refs.editFormRef.validate(valid => {
        if (!valid) return
        request.put('/api/admin/student/update', this.editForm).then(() => {
          this.$message.success('学生信息更新成功')
          this.editVisible = false
          this.loadStudents()
        })
      })
    },

    resetPwd(row) {
      this.$confirm(`确认重置学生「${row.realName}」的密码吗？`, '提示', { type: 'warning' }).then(() => {
        request.put(`/api/admin/student/reset-pwd/${row.id}`).then(() => {
          this.pwdStudentName = row.realName
          this.pwdVisible = true
        })
      }).catch(() => {})
    },

    toggleStudent(id, disabled) {
      const action = disabled === 1 ? '禁用' : '启用'
      this.$confirm(`确认${action}该学生吗？`, '提示', { type: 'warning' }).then(() => {
        const url = disabled === 1
          ? `/api/admin/student/disable/${id}`
          : `/api/admin/student/enable/${id}`
        request.put(url).then(() => {
          this.$message.success(`${action}成功`)
          this.loadStudents()
        })
      }).catch(() => {})
    }
  }
}
</script>
