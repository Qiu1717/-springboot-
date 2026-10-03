<template>
  <div>
    <!-- ===== 第一层：课程卡片 ===== -->
    <template v-if="!activeCourseId">
      <el-card v-loading="courseLoading">
        <div slot="header">
          <span style="font-size:16px;font-weight:bold">课程资料</span>
          <span style="color:#999;font-size:13px;margin-left:8px">{{ courses.length }} 门课程</span>
        </div>
        <!-- 表头 -->
        <div class="course-row course-header">
          <span class="col-name">课程名称</span>
          <span class="col-term">学期</span>
          <span class="col-credit">学分</span>
          <span class="col-files">资料数</span>
        </div>
        <!-- 课程行 -->
        <div v-for="c in courses" :key="c.id" class="course-row course-item" @click="enterCourse(c)">
          <span class="col-name">{{ c.name }}</span>
          <span class="col-term">{{ formatTerm(c.term) }}</span>
          <span class="col-credit">{{ c.credit }}</span>
          <span class="col-files">{{ getFileCount(c.id) }}</span>
        </div>
        <el-empty v-if="!courseLoading && courses.length === 0" description="暂无课程"></el-empty>
      </el-card>
    </template>

    <!-- ===== 第二层：课程文件列表 ===== -->
    <template v-else>
      <div class="sub-header">
        <el-button type="text" icon="el-icon-arrow-left" style="color:#409EFF;font-size:15px" @click="activeCourseId = null">
          返回课程列表
        </el-button>
        <span class="sub-title">{{ activeCourseName }} — 课程资料</span>
        <el-upload
          v-if="canUpload"
          class="upload-btn"
          :action="uploadAction"
          :data="{ courseId: activeCourseId }"
          :headers="uploadHeaders"
          :show-file-list="false"
          :on-success="onUploadSuccess"
          :on-error="onUploadError"
          :before-upload="beforeUpload"
        >
          <el-button type="primary" size="small" icon="el-icon-upload2">上传资料</el-button>
        </el-upload>
      </div>

      <el-card style="margin-top:16px">
        <el-table :data="files" border stripe v-loading="loading">
          <el-table-column prop="fileName" label="文件名" min-width="220"></el-table-column>
          <el-table-column label="大小" width="100">
            <template slot-scope="scope">{{ formatSize(scope.row.fileSize) }}</template>
          </el-table-column>
          <el-table-column prop="uploaderName" label="上传者" width="120"></el-table-column>
          <el-table-column prop="uploadTime" label="上传时间" width="160"></el-table-column>
          <el-table-column label="操作" width="140" align="center">
            <template slot-scope="scope">
              <el-button type="text" size="small" style="color:#409EFF" @click="downloadFile(scope.row)">下载</el-button>
              <span v-if="canDelete(scope.row)" style="color:#dcdfe6">|</span>
              <el-button v-if="canDelete(scope.row)" type="text" size="small" style="color:#F56C6C" @click="deleteFile(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!loading && files.length === 0" description="暂无资料"></el-empty>
      </el-card>
    </template>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'CourseFile',
  data() {
    return {
      courses: [], courseLoading: false,
      activeCourseId: null, activeCourseName: '',
      files: [], loading: false,
      userInfo: {},
      fileCounts: {}
    }
  },
  computed: {
    canUpload() {
      // 只有管理员和教师可以看到"上传资料"按钮(学生只能下载)
      return this.userInfo.role === 'ADMIN' || this.userInfo.role === 'TEACHER'
    },
    uploadHeaders() {
      // el-upload组件的请求头: 手动设置Authorization(Bearer token)
      // el-upload不是通过axios发送，所以不会走request.js的拦截器
      return { Authorization: 'Bearer ' + sessionStorage.getItem('token') }
    },
    uploadAction() {
      return '/api/files/upload'  // 上传目标URL
    }
  },
  created() {
    // 从sessionStorage恢复用户信息(判断角色用)
    const info = sessionStorage.getItem('userInfo')
    if (info) { try { this.userInfo = JSON.parse(info) } catch (e) { this.userInfo = {} } }
    this.loadCourses()  // 加载课程列表(角色差异化: 管理员=全部/教师=自己的/学生=已选的)
  },
  methods: {
    loadCourses() {
      this.courseLoading = true
      request.get('/api/files/my-courses').then(res => {  // 后端根据role返回不同课程列表
        this.courses = res.data || []
        this.courseLoading = false
        // 为每个课程查文件数量(用于课程卡片上显示"资料数")
        this.courses.forEach(c => {
          request.get('/api/files/by-course/' + c.id).then(r => {
            this.$set(this.fileCounts, c.id, (r.data || []).length)  // Vue.set更新响应式对象
          })
        })
      }).catch(() => { this.courseLoading = false })
    },

    formatTerm(term) {
      if (!term) return ''
      const parts = term.split('-')
      if (parts.length === 3) return parts[0] + ' 年第' + (parts[2] === '1' ? '一' : '二') + '学期'
      return term
    },
    getFileCount(courseId) { return this.fileCounts[courseId] || 0 },

    enterCourse(course) {
      this.activeCourseId = course.id
      this.activeCourseName = course.name
      this.loadFiles()
    },

    loadFiles() {
      this.loading = true
      request.get('/api/files/by-course/' + this.activeCourseId).then(res => {
        this.files = res.data || []
        this.loading = false
      }).catch(() => { this.loading = false })
    },

    formatSize(bytes) {
      if (!bytes) return '0 B'
      const units = ['B', 'KB', 'MB', 'GB']
      let i = 0, size = bytes
      while (size >= 1024 && i < units.length - 1) { size /= 1024; i++ }
      return size.toFixed(i > 0 ? 1 : 0) + ' ' + units[i]
    },

    beforeUpload(file) {
      if (file.size > 100 * 1024 * 1024) { this.$message.error('文件不能超过 100MB'); return false }
      return true
    },
    onUploadSuccess() { this.$message.success('上传成功'); this.loadFiles(); this.loadCourses() },
    onUploadError() { this.$message.error('上传失败') },

    /**
     * 下载文件 — 用<a>标签触发浏览器下载
     * 不需要axios: 直接href="/api/files/download/{id}"，浏览器自动处理Content-Disposition
     */
    downloadFile(row) {
      const a = document.createElement('a')
      a.href = '/api/files/download/' + row.id  // 后端返回 Content-Disposition: attachment
      a.download = row.fileName  // 建议的文件名
      a.click()  // 触发下载
    },

    canDelete(row) {
      if (this.userInfo.role === 'ADMIN') return true  // 管理员可以删任何文件
      return row.uploaderName === this.userInfo.realName  // 教师只能删自己上传的
    },

    deleteFile(row) {
      this.$confirm('确认删除该文件吗？', '提示', { type: 'warning' }).then(() => {
        request.delete('/api/files/' + row.id).then(() => {
          this.$message.success('删除成功')
          this.loadFiles()
          this.loadCourses()
        })
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
/* 课程纵向行列表 */
.course-row { display:flex; align-items:center; padding:12px 16px; border-bottom:1px solid #ebeef5; }
.course-header { background:#f5f7fa; font-weight:700; color:#606266; font-size:13px; border-radius:4px 4px 0 0; }
.course-item { cursor:pointer; transition:background .2s; }
.course-item:hover { background:#ecf5ff; }
.col-name { flex:1; font-size:14px; font-weight:600; color:#303133; min-width:100px; }
.col-term { width:160px; font-size:13px; color:#606266; }
.col-credit { width:80px; font-size:13px; color:#606266; text-align:center; }
.col-files { width:80px; font-size:13px; color:#409EFF; text-align:center; }

.sub-header { display:flex; align-items:center; gap:12px; padding:12px 0; }
.sub-title { font-size:18px; font-weight:700; color:#303133; flex:1; }

@media (max-width:768px) { .col-term { width:100px; } }
</style>
