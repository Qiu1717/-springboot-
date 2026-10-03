<template>
  <div>
    <el-card>
      <div slot="header">
        <span style="font-size:16px;font-weight:bold">我的课程</span>
      </div>
      <el-table :data="courseList" border stripe v-loading="loading">
        <el-table-column prop="name" label="课程名称" width="200"></el-table-column>
        <el-table-column prop="credit" label="学分" width="80"></el-table-column>
        <el-table-column prop="term" label="学期" width="120"></el-table-column>
        <el-table-column label="选课情况" width="200">
          <template slot-scope="scope">
            <el-progress
              :percentage="scope.row.capacity > 0 ? Math.round(scope.row.selected / scope.row.capacity * 100) : 0"
              :text-inside="true"
              :stroke-width="18"
              :status="scope.row.selected >= scope.row.capacity ? 'success' : undefined"
            >
              {{ scope.row.selected }}/{{ scope.row.capacity }}
            </el-progress>
          </template>
        </el-table-column>
        <el-table-column prop="schedule" label="上课时间地点"></el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template slot-scope="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" size="small">
              {{ scope.row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'TeacherCourses',
  data() {
    return {
      courseList: [],
      loading: false
    }
  },
  created() {
    this.fetchCourses()
  },
  methods: {
    fetchCourses() {
      this.loading = true
      request.get('/api/teacher/my-courses').then(res => {
        this.courseList = res.data
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    }
  }
}
</script>
