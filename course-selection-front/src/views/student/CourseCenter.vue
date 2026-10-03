<template>
  <div>
    <el-card>
      <div slot="header">
        <span style="font-size:16px;font-weight:bold">选课中心</span>
        <span style="margin-left:10px;color:#999;font-size:13px">选择您感兴趣的课程</span>
      </div>
      <el-table :data="courseList" border stripe v-loading="loading">
        <el-table-column prop="name" label="课程名称" width="180"></el-table-column>
        <el-table-column prop="credit" label="学分" width="80"></el-table-column>
        <el-table-column label="已选/容量" width="120">
          <template slot-scope="scope">
            <el-progress
              :percentage="scope.row.capacity > 0 ? Math.round(scope.row.selected / scope.row.capacity * 100) : 0"
              :text-inside="true"
              :stroke-width="18"
              :status="scope.row.selected >= scope.row.capacity ? 'exception' : undefined"
            >
              {{ scope.row.selected }}/{{ scope.row.capacity }}
            </el-progress>
          </template>
        </el-table-column>
        <el-table-column prop="term" label="学期" width="120"></el-table-column>
        <el-table-column prop="schedule" label="上课时间地点"></el-table-column>
        <el-table-column label="操作" width="100">
          <template slot-scope="scope">
            <el-button
              type="primary"
              size="small"
              :disabled="scope.row.selected >= scope.row.capacity"
              @click="selectCourse(scope.row)"
            >选课</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'CourseCenter',
  data() {
    return {
      courseList: [],
      loading: false
    }
  },
  created() {
    this.fetchCourses()  // 组件创建时自动加载可选课程列表
  },
  methods: {
    fetchCourses() {
      this.loading = true
      request.get('/api/student/available-courses').then(res => {  // 后端: status=1 AND selected<capacity
        this.courseList = res.data  // 前端展示课程名/学分/容量进度条/上课时间
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    selectCourse(course) {
      this.$confirm(`确认选择课程「${course.name}」吗？`, '确认选课', {
        confirmButtonText: '确定', cancelButtonText: '取消', type: 'info'
      }).then(() => {
        request.post(`/api/student/select?courseId=${course.id}`).then(() => {  // POST请求，courseId作为query参数
          this.$message.success('选课成功！')
          this.fetchCourses()  // 刷新列表(更新已选人数进度条)
        })
        // 错误已由request.js拦截器提示(如"课程已满员"、"选课已达上限")
      }).catch(() => {})  // 用户取消确认，不做任何操作
    }
  }
}
</script>
