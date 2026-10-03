<template>
  <div>
    <!-- 统计卡片 -->
    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :xs="12" :sm="6">
        <el-card shadow="never" style="text-align:center">
          <div style="font-size:28px;color:#409EFF;font-weight:bold">{{ stats.totalCourses }}</div>
          <div style="color:#999;font-size:13px;margin-top:4px">已选课程</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="never" style="text-align:center">
          <div style="font-size:28px;color:#67C23A;font-weight:bold">{{ stats.totalCredits }}</div>
          <div style="color:#999;font-size:13px;margin-top:4px">已获学分</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="never" style="text-align:center">
          <div style="font-size:28px;color:#E6A23C;font-weight:bold">{{ stats.avgScore }}</div>
          <div style="color:#999;font-size:13px;margin-top:4px">平均分</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="never" style="text-align:center">
          <div style="font-size:13px;color:#606266;line-height:2.2">
            <span>已通过 </span>
            <b style="color:#67C23A;font-size:20px">{{ stats.passedCourses }}</b>
            <span style="color:#999"> / {{ stats.totalCourses }}</span>
          </div>
          <div style="font-size:13px;color:#606266;line-height:2.2">
            <span>已出分 </span>
            <b style="color:#409EFF;font-size:20px">{{ stats.gradedCourses }}</b>
            <span style="color:#999"> / {{ stats.totalCourses }}</span>
          </div>
          <div style="color:#999;font-size:11px;margin-top:2px">通过 / 出分</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 选课列表 -->
    <el-card>
      <div slot="header">
        <span style="font-size:16px;font-weight:bold">我的选课记录</span>
      </div>
      <el-table :data="selectionList" border stripe v-loading="loading">
        <el-table-column prop="courseName" label="课程名称" width="200"></el-table-column>
        <el-table-column prop="credit" label="学分" width="80"></el-table-column>
        <el-table-column prop="term" label="学期" width="120"></el-table-column>
        <el-table-column prop="score" label="成绩" width="100">
          <template slot-scope="scope">
            <span v-if="scope.row.score != null" :style="{color: scope.row.score >= 60 ? '#67C23A' : '#F56C6C'}">
              {{ scope.row.score }}
            </span>
            <span v-else style="color:#999">暂无成绩</span>
          </template>
        </el-table-column>
        <el-table-column prop="selectTime" label="选课时间"></el-table-column>
        <el-table-column label="操作" width="100">
          <template slot-scope="scope">
            <el-button
              v-if="scope.row.score == null"
              type="text" size="small" style="color:#F56C6C"
              @click="dropCourse(scope.row)"
            >退课</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'MySelections',
  data() {
    return {
      selectionList: [],
      loading: false,
      stats: { totalCourses: 0, totalCredits: 0, avgScore: 0, passedCourses: 0, gradedCourses: 0 }
    }
  },
  created() {
    this.fetchSelections()
    this.fetchStats()
  },
  methods: {
    fetchStats() {
      request.get('/api/student/stats').then(res => {
        this.stats = res.data
      })
    },
    fetchSelections() {
      this.loading = true
      request.get('/api/student/my-selections').then(res => {
        this.selectionList = res.data
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    dropCourse(row) {
      this.$confirm(`确认退选课程「${row.courseName}」吗？`, '确认退课', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        request.post(`/api/student/drop?courseId=${row.courseId}`).then(() => {
          this.$message.success('退课成功')
          this.fetchSelections()
        })
      }).catch(() => {})
    }
  }
}
</script>
