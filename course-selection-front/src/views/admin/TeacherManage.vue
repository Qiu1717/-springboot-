<template>
  <div>
    <el-card>
      <div slot="header">
        <span style="font-size:16px;font-weight:bold">教师管理</span>
        <el-button type="primary" size="small" style="float:right" @click="openAddDialog">
          新增教师
        </el-button>
      </div>
      <el-table :data="teachers" border stripe v-loading="loading">
        <el-table-column label="工号" width="80">
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
        <el-table-column prop="createTime" label="创建时间" width="160"></el-table-column>
        <el-table-column label="操作" width="150">
          <template slot-scope="scope">
            <el-button
              v-if="scope.row.disabled === 0"
              type="warning"
              size="small"
              @click="toggleTeacher(scope.row.id, 1)"
            >禁用</el-button>
            <el-button
              v-else
              type="success"
              size="small"
              @click="toggleTeacher(scope.row.id, 0)"
            >启用</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增教师对话框 -->
    <el-dialog title="新增教师" :visible.sync="dialogVisible" width="500px">
      <el-form :model="newTeacher" :rules="rules" ref="teacherForm">
        <el-form-item label="账号" prop="username">
          <el-input v-model="newTeacher.username" placeholder="请输入账号"></el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="newTeacher.password" type="password" placeholder="请输入密码"></el-input>
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="newTeacher.realName" placeholder="请输入姓名"></el-input>
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="newTeacher.email" placeholder="请输入邮箱"></el-input>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="addTeacher">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'TeacherManage',
  data() {
    return {
      teachers: [],
      loading: false,
      dialogVisible: false,
      newTeacher: { username: '', password: '', realName: '', email: '' },
      rules: {
        username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
        realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
        email: [{ type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.loadTeachers()
  },
  methods: {
    loadTeachers() {
      this.loading = true
      request.get('/api/admin/teachers').then(res => {
        const list = (res.data || []).sort((a, b) => a.username.localeCompare(b.username))
        list.forEach((t, i) => { t.displayId = String(i + 1).padStart(3, '0') })
        this.teachers = list
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    toggleTeacher(id, disabled) {
      const action = disabled === 1 ? '禁用' : '启用'
      const url = disabled === 1
        ? `/api/admin/teacher/disable/${id}`
        : `/api/admin/teacher/enable/${id}`

      this.$confirm(`确认${action}该教师吗？`, '提示', { type: 'warning' }).then(() => {
        request.put(url).then(() => {
          this.$message.success(`${action}成功`)
          this.loadTeachers()
        })
      }).catch(() => {})
    },
    openAddDialog() {
      this.newTeacher = { username: '', password: '', realName: '', email: '' }
      this.dialogVisible = true
      this.$nextTick(() => {
        if (this.$refs.teacherForm) this.$refs.teacherForm.clearValidate()
      })
    },
    addTeacher() {
      this.$refs.teacherForm.validate(valid => {
        if (!valid) return
        request.post('/api/admin/teacher/add', this.newTeacher).then(() => {
          this.$message.success('教师添加成功')
          this.dialogVisible = false
          this.loadTeachers()
        })
      })
    }
  }
}
</script>
