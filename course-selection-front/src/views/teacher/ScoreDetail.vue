<template>
  <div>
    <!-- 顶部标题栏 -->
    <div class="detail-header">
      <el-button type="text" icon="el-icon-arrow-left" style="color:#409EFF;font-size:15px" @click="$router.push('/teacher/score')">
        返回课程列表
      </el-button>
      <span class="detail-title">{{ courseName }}</span>
      <span style="color:#999;font-size:13px">{{ students.length }} 名学生</span>
    </div>

    <el-card style="margin-top:16px">
      <!-- 搜索 + 批量录入工具栏 -->
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索学生姓名"
          clearable
          size="small"
          style="width:200px"
          @keyup.enter.native="search"
        >
          <el-button slot="append" icon="el-icon-search" @click="search"></el-button>
        </el-input>
        <span style="font-size:13px;color:#666;margin-left:16px">
          已选 <b style="color:#409EFF">{{ checkedIds.length }}</b> 人
        </span>
        <el-input-number
          v-model="batchScore"
          :min="0" :max="100" :precision="1"
          size="small" placeholder="成绩"
          style="width:120px;margin-left:12px"
        ></el-input-number>
        <el-button
          type="primary" size="small"
          :disabled="checkedIds.length === 0 || batchScore === undefined"
          @click="batchSave" style="margin-left:8px"
        >批量录入</el-button>
      </div>

      <!-- 学生表格 -->
      <el-table
        :data="students" border stripe v-loading="loading"
        ref="table" @selection-change="onSelectionChange"
        style="margin-top:12px"
      >
        <el-table-column type="selection" width="45"></el-table-column>
        <el-table-column prop="studentName" label="学生姓名" width="120"></el-table-column>
        <el-table-column prop="term" label="学期" width="140"></el-table-column>
        <el-table-column prop="credit" label="学分" width="80"></el-table-column>
        <el-table-column label="成绩" width="200">
          <template slot-scope="scope">
            <el-input-number
              v-model="scope.row.score" :min="0" :max="100" :precision="1"
              size="small" style="width:120px"
            ></el-input-number>
            <el-button type="text" size="small" style="color:#409EFF;margin-left:6px" @click="saveOne(scope.row)">
              保存
            </el-button>
          </template>
        </el-table-column>
        <el-table-column prop="selectTime" label="选课时间" width="160"></el-table-column>
      </el-table>
      <el-empty v-if="!loading && students.length === 0" description="暂无学生选课"></el-empty>
    </el-card>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'ScoreDetail',
  data() {
    return {
      courseName: '',
      students: [],
      loading: false,
      keyword: '',
      checkedIds: [],
      batchScore: undefined
    }
  },
  created() {
    const courseId = this.$route.params.courseId  // 从路由参数获取课程ID (/teacher/score/:courseId)
    this.loadStudents(courseId)  // 加载该课程下所有已选学生
  },
  methods: {
    loadStudents(courseId) {
      this.loading = true
      const params = { courseId }  // 必传: 课程ID
      if (this.keyword) params.studentName = this.keyword  // 可选: 搜索学生姓名
      request.get('/api/teacher/selections', { params }).then(res => {
        const list = res.data || []  // SelectionVO列表(含studentName/courseName/score等)
        this.students = list
        this.courseName = list.length > 0 ? list[0].courseName : ''  // 从第一条记录取课程名
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    search() {
      const courseId = this.$route.params.courseId
      this.loadStudents(courseId)
    },
    onSelectionChange(rows) {
      this.checkedIds = rows.map(r => r.id)  // el-table的@selection-change事件: 获取勾选行的selection.id列表
    },
    saveOne(row) {
      if (row.score === undefined || row.score === null) {
        this.$message.warning('请输入成绩')
        return
      }
      // 单条录入: PUT { selectionId, score }
      request.put('/api/teacher/score', { selectionId: row.id, score: row.score }).then(() => {
        this.$message.success('成绩保存成功')
      })
    },
    batchSave() {
      if (this.batchScore === undefined || this.batchScore === null) {
        this.$message.warning('请输入要录入的成绩')
        return
      }
      // 批量录入: 将勾选的selectionId列表 + 统一成绩 → PUT { scores: [...] }
      const scores = this.checkedIds.map(id => ({ selectionId: id, score: this.batchScore }))
      request.put('/api/teacher/score/batch', { scores }).then(() => {
        this.$message.success(`已为 ${scores.length} 名学生录入成绩 ${this.batchScore} 分`)
        // 前端同步更新: 避免重新请求数据
        this.students.forEach(s => { if (this.checkedIds.includes(s.id)) s.score = this.batchScore })
        this.batchScore = undefined  // 清空批量输入框
        this.checkedIds = []         // 清空勾选列表
        this.$refs.table.clearSelection()  // 清空表格勾选状态
      })
    }
  }
}
</script>

<style scoped>
.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 0;
}
.detail-title {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
}
.toolbar {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  background: #f5f7fa;
  border-radius: 6px;
}
</style>
