<template>
  <div>
    <!-- ===== 筛选区 ===== -->
    <el-card style="margin-bottom:16px">
      <el-form :inline="true" :model="query" size="small">
        <el-form-item label="学期">
          <el-select v-model="queryStartYear" placeholder="年份" style="width:130px" @change="buildTerm" clearable>
            <el-option v-for="y in yearOptions" :key="y" :label="y + '-' + (y+1)" :value="y"></el-option>
          </el-select>
          <el-select v-model="querySemester" placeholder="学期" style="width:130px;margin-left:8px" @change="buildTerm" clearable>
            <el-option label="第一学期" :value="1"></el-option>
            <el-option label="第二学期" :value="2"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="课程">
          <el-select v-model="query.courseId" placeholder="全部课程" clearable style="width:180px">
            <el-option v-for="c in myCourses" :key="c.id" :label="c.name" :value="c.id"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- ===== 课程纵向行式列表 ===== -->
    <el-card v-loading="courseLoading">
      <div slot="header">
        <span style="font-size:16px;font-weight:bold">我的课程</span>
        <span style="color:#999;font-size:13px;margin-left:8px">{{ filteredCourses.length }} 门</span>
      </div>

      <!-- 表头 -->
      <div class="course-row course-header">
        <span class="col-name">课程名称</span>
        <span class="col-term">学期</span>
        <span class="col-credit">学分</span>
        <span class="col-count">参课人数</span>
        <span class="col-status">状态</span>
      </div>

      <!-- 每行课程 -->
      <div
        v-for="c in filteredCourses" :key="c.id"
        class="course-row course-item"
        @click="goDetail(c)"
      >
        <span class="col-name">{{ c.name }}</span>
        <span class="col-term">{{ formatTerm(c.term) }}</span>
        <span class="col-credit">{{ c.credit }}</span>
        <span class="col-count">
          <el-progress
            :percentage="c.capacity > 0 ? Math.round(c.selected / c.capacity * 100) : 0"
            :stroke-width="8"
            :show-text="false"
            style="width:80px;display:inline-block;vertical-align:middle"
          ></el-progress>
          <span style="font-size:12px;color:#909399;margin-left:6px">{{ c.selected }}/{{ c.capacity }}</span>
        </span>
        <span class="col-status">
          <el-tag size="mini" :type="c.status === 1 ? 'success' : 'danger'">
            {{ c.status === 1 ? '进行中' : '停用' }}
          </el-tag>
        </span>
      </div>

      <el-empty v-if="!courseLoading && filteredCourses.length === 0" description="暂无课程数据"></el-empty>
    </el-card>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'ScoreEntry',
  data() {
    return {
      query: { term: '', courseId: null },
      queryStartYear: null,
      querySemester: null,
      yearOptions: [],
      myCourses: [],
      filteredCourses: [],
      courseLoading: false
    }
  },
  created() {
    const now = new Date().getFullYear()
    for (let i = now - 5; i <= now + 3; i++) { this.yearOptions.push(i) }
    this.loadCourses()
  },
  methods: {
    formatTerm(term) {
      if (!term) return ''
      const parts = term.split('-')
      if (parts.length === 3) {
        return parts[0] + ' 年第' + (parts[2] === '1' ? '一' : '二') + '学期'
      }
      return term
    },
    buildTerm() {
      if (this.queryStartYear && this.querySemester) {
        this.query.term = this.queryStartYear + '-' + (this.queryStartYear + 1) + '-' + this.querySemester
      } else {
        this.query.term = ''
      }
    },
    loadCourses() {
      this.courseLoading = true
      request.get('/api/teacher/my-courses').then(res => {
        this.myCourses = res.data || []
        this.filteredCourses = this.myCourses.filter(c => c.status === 1)
        this.courseLoading = false
      }).catch(() => { this.courseLoading = false })
    },
    search() {
      this.courseLoading = true
      request.get('/api/teacher/my-courses').then(res => {
        const all = res.data || []
        let list = all.filter(c => c.status === 1)
        if (this.query.term) list = list.filter(c => c.term === this.query.term)
        if (this.query.courseId) list = list.filter(c => c.id === this.query.courseId)
        this.filteredCourses = list
        this.courseLoading = false
      }).catch(() => { this.courseLoading = false })
    },
    resetQuery() {
      this.query = { term: '', courseId: null }
      this.queryStartYear = null
      this.querySemester = null
      this.filteredCourses = this.myCourses.filter(c => c.status === 1)
    },
    goDetail(course) {
      this.$router.push('/teacher/score/' + course.id)
    }
  }
}
</script>

<style scoped>
.course-row {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
}
.course-header {
  background: #f5f7fa;
  font-weight: 700;
  color: #606266;
  font-size: 13px;
  border-radius: 4px 4px 0 0;
}
.course-item {
  cursor: pointer;
  transition: background .2s;
}
.course-item:hover { background: #ecf5ff; }

.col-name { flex: 1; font-size: 14px; font-weight: 600; color: #303133; min-width: 120px; }
.col-term { width: 160px; font-size: 13px; color: #606266; }
.col-credit { width: 80px; font-size: 13px; color: #606266; text-align: center; }
.col-count { width: 180px; display: flex; align-items: center; }
.col-status { width: 80px; text-align: center; }

@media (max-width: 768px) {
  .col-term, .col-credit { width: 100px; }
  .col-count { width: auto; }
}
</style>
