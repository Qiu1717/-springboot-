<template>
  <div>
    <el-card>
      <div slot="header">
        <span style="font-size:16px;font-weight:bold">课程管理</span>
        <el-button type="primary" size="small" style="float:right" @click="openDialog()">
          新增课程
        </el-button>
      </div>
      <el-table :data="courses" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60"></el-table-column>
        <el-table-column prop="name" label="课程名称" width="180"></el-table-column>
        <el-table-column prop="credit" label="学分" width="80"></el-table-column>
        <el-table-column prop="teacherName" label="执教老师" width="100"></el-table-column>
        <el-table-column label="选课情况" width="200">
          <template slot-scope="scope">
            <el-progress
              :percentage="scope.row.capacity > 0 ? Math.round(scope.row.selected / scope.row.capacity * 100) : 0"
              :text-inside="true"
              :stroke-width="18"
            >
              {{ scope.row.selected }}/{{ scope.row.capacity }}
            </el-progress>
          </template>
        </el-table-column>
        <el-table-column prop="term" label="学期" width="120"></el-table-column>
        <el-table-column prop="schedule" label="上课时间地点"></el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template slot-scope="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" size="small">
              {{ scope.row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center">
          <template slot-scope="scope">
            <el-button v-if="canEditDelete(scope.row)" type="text" size="small" style="color:#409EFF" @click="openDialog(scope.row)">编辑</el-button>
            <span v-if="canEditDelete(scope.row)" style="color:#dcdfe6">|</span>
            <el-button
              v-if="canEditDelete(scope.row)"
              type="text"
              size="small"
              :style="{color: scope.row.status === 1 ? '#E6A23C' : '#67C23A'}"
              @click="toggleCourse(scope.row.id, scope.row.status === 1 ? 0 : 1)"
            >
              {{ scope.row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <span v-if="canEditDelete(scope.row)" style="color:#dcdfe6">|</span>
            <el-button v-if="canEditDelete(scope.row)" type="text" size="small" style="color:#F56C6C" @click="deleteCourse(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 课程编辑对话框 -->
    <el-dialog :title="isEdit ? '编辑课程' : '新增课程'" :visible.sync="dialogVisible" width="550px">
      <el-form :model="currentCourse" :rules="rules" ref="courseForm">
        <el-form-item label="课程名称" prop="name">
          <el-input v-model="currentCourse.name" placeholder="请输入课程名称"></el-input>
        </el-form-item>
        <el-form-item label="学分" prop="credit">
          <el-input-number v-model="currentCourse.credit" :min="0.5" :max="10" :step="0.5"></el-input-number>
        </el-form-item>
        <el-form-item label="执教老师" prop="teacherId">
          <el-select v-model="currentCourse.teacherId" placeholder="输入ID或姓名搜索" filterable style="width:100%">
            <el-option v-for="t in teacherList" :key="t.id" :label="t.id + ' - ' + t.realName" :value="t.id"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="总容量" prop="capacity">
          <el-input-number v-model="currentCourse.capacity" :min="1" :max="500"></el-input-number>
        </el-form-item>
        <el-form-item label="学期" prop="startYear">
          <el-select v-model="startYear" placeholder="年份" style="width:130px" @change="onTermChange">
            <el-option v-for="y in yearOptions" :key="y" :label="y + '-' + (y+1)" :value="y"></el-option>
          </el-select>
          <el-select v-model="semester" placeholder="学期" style="width:130px;margin-left:8px" @change="onTermChange">
            <el-option label="第一学期" :value="1"></el-option>
            <el-option label="第二学期" :value="2"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="上课时间地点" prop="schedule">
          <el-input v-model="currentCourse.schedule" placeholder="如周一 1-2节 机房A101"></el-input>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCourse">保存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'CourseManage',
  data() {
    return {
      courses: [],
      loading: false,
      teacherList: [],
      userInfo: {},
      dialogVisible: false,
      isEdit: false,
      currentCourse: {
        id: null, name: '', credit: 2.0, teacherId: null,
        capacity: 40, term: '', schedule: ''
      },
      startYear: new Date().getFullYear(),
      semester: 1,
      yearOptions: [],
      rules: {
        name: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
        credit: [{ required: true, message: '请输入学分', trigger: 'blur' }],
        teacherId: [{ required: true, message: '请输入教师ID', trigger: 'blur' }],
        capacity: [{ required: true, message: '请输入容量', trigger: 'blur' }]
      }
    }
  },
  created() {
    const now = new Date().getFullYear()
    for (let i = now - 5; i <= now + 3; i++) {
      this.yearOptions.push(i)
    }
    this.startYear = now
    this.loadCourses()
    this.loadTeachers()
    const info = sessionStorage.getItem('userInfo')
    if (info) { try { this.userInfo = JSON.parse(info) } catch (e) { this.userInfo = {} } }
  },
  methods: {
    /**
     * 权限判断: 管理员可编辑/删除所有课程，教师只能操作自己教的课程
     * 与后端 AdminController 中的二次校验保持一致
     */
    canEditDelete(course) {
      if (this.userInfo.role === 'ADMIN') return true  // 管理员全权操作
      if (!this.userInfo.userId) return false           // 未登录
      return course.teacherId === this.userInfo.userId  // 教师只操作自己的课
    },
    loadTeachers() {
      request.get('/api/admin/teachers').then(res => {
        this.teacherList = res.data || []
      })
    },
    onTermChange() {
      this.currentCourse.term = this.startYear + '-' + (this.startYear + 1) + '-' + this.semester
    },
    loadCourses() {
      this.loading = true
      request.get('/api/admin/courses').then(res => {
        this.courses = res.data
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    openDialog(row) {
      const now = new Date().getFullYear()
      if (row) {
        this.isEdit = true
        this.currentCourse = { ...row }
        // 解析 term 如 "2025-2026-1" → startYear=2025, semester=1
        const parts = (row.term || '').split('-')
        this.startYear = parseInt(parts[0]) || now
        this.semester = parseInt(parts[2]) || 1
      } else {
        this.isEdit = false
        this.currentCourse = {
          id: null, name: '', credit: 2.0, teacherId: null,
          capacity: 40, term: '', schedule: ''
        }
        this.startYear = now
        this.semester = 1
      }
      this.dialogVisible = true
      this.$nextTick(() => {
        if (this.$refs.courseForm) this.$refs.courseForm.clearValidate()
      })
    },
    saveCourse() {
      this.onTermChange()
      this.$refs.courseForm.validate(valid => {
        if (!valid) return

        if (this.isEdit) {
          request.put('/api/admin/course/update', this.currentCourse).then(() => {
            this.$message.success('更新成功')
            this.dialogVisible = false
            this.loadCourses()
          })
        } else {
          request.post('/api/admin/course/add', this.currentCourse).then(() => {
            this.$message.success('添加成功')
            this.dialogVisible = false
            this.loadCourses()
          })
        }
      })
    },
    toggleCourse(id, status) {
      const action = status === 1 ? '启用' : '停用'
      this.$confirm(`确认${action}该课程吗？`, '提示', { type: 'warning' }).then(() => {
        const url = status === 1
          ? `/api/admin/course/enable/${id}`
          : `/api/admin/course/disable/${id}`
        request.put(url).then(() => {
          this.$message.success(`${action}成功`)
          this.loadCourses()
        })
      }).catch(() => {})
    },
    deleteCourse(id) {
      this.$confirm('确认删除该课程吗？', '提示', { type: 'warning' }).then(() => {
        request.delete(`/api/admin/course/delete/${id}`).then(() => {
          this.$message.success('删除成功')
          this.loadCourses()
        })
      }).catch(() => {})
    }
  }
}
</script>
