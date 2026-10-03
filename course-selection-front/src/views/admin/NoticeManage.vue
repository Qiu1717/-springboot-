<template>
  <div>
    <el-card>
      <div slot="header">
        <span style="font-size:16px;font-weight:bold">公告管理</span>
        <el-button type="primary" size="small" style="float:right" @click="openDialog()">
          新增公告
        </el-button>
      </div>
      <el-table :data="notices" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60"></el-table-column>
        <el-table-column prop="title" label="标题" width="180"></el-table-column>
        <el-table-column prop="author" label="发布者" width="120"></el-table-column>
        <el-table-column prop="content" label="内容" show-overflow-tooltip></el-table-column>
        <el-table-column prop="createTime" label="发布时间" width="160"></el-table-column>
        <el-table-column label="操作" width="200">
          <template slot-scope="scope">
            <el-button type="text" size="small" style="color:#409EFF" @click="viewNotice(scope.row)">查看</el-button>
            <span style="color:#dcdfe6">|</span>
            <el-button v-if="canEditDelete(scope.row)" type="text" size="small" style="color:#409EFF" @click="openDialog(scope.row)">编辑</el-button>
            <span v-if="canEditDelete(scope.row)" style="color:#dcdfe6">|</span>
            <el-button v-if="canEditDelete(scope.row)" type="text" size="small" style="color:#F56C6C" @click="deleteNotice(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 公告编辑对话框 -->
    <el-dialog :title="isEdit ? '编辑公告' : '新增公告'" :visible.sync="dialogVisible" width="600px">
      <el-form :model="currentNotice" :rules="rules" ref="noticeForm">
        <el-form-item label="标题" prop="title">
          <el-input v-model="currentNotice.title" placeholder="请输入公告标题"></el-input>
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            type="textarea"
            v-model="currentNotice.content"
            :rows="6"
            placeholder="请输入公告内容"
          ></el-input>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveNotice">保存</el-button>
      </span>
    </el-dialog>

    <!-- 公告查看弹窗（只读） -->
    <el-dialog title="公告详情" :visible.sync="viewVisible" width="600px" top="5vh">
      <div style="border-bottom:1px solid #ebeef5;padding-bottom:12px;margin-bottom:12px">
        <h3 style="margin:0;color:#303133">{{ viewingNotice.title }}</h3>
      </div>
      <div style="white-space:pre-wrap;line-height:1.8;font-size:14px;color:#333;max-height:60vh;overflow-y:auto">
        {{ viewingNotice.content }}
      </div>
      <div style="color:#999;font-size:12px;margin-top:16px;text-align:right">
        发布者：{{ viewingNotice.author || '—' }} &emsp; 发布时间：{{ viewingNotice.createTime }}
      </div>
    </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'NoticeManage',
  data() {
    return {
      notices: [],
      loading: false,
      dialogVisible: false,
      viewVisible: false,
      viewingNotice: { title: '', content: '', createTime: '' },
      isEdit: false,
      currentNotice: { id: null, title: '', content: '' },
      userInfo: {},
      rules: {
        title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
        content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
      }
    }
  },
  created() {
    const info = sessionStorage.getItem('userInfo')
    if (info) { try { this.userInfo = JSON.parse(info) } catch (e) { this.userInfo = {} } }
    this.loadNotices()
  },
  methods: {
    canEditDelete(notice) {
      // 管理员能编辑所有，教师只能编辑自己的
      if (this.userInfo.role === 'ADMIN') return true
      if (!this.userInfo.realName) return false
      return notice.author === this.userInfo.realName
    },
    loadNotices() {
      this.loading = true
      request.get('/api/admin/notices').then(res => {
        this.notices = res.data
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    viewNotice(row) {
      this.viewingNotice = { ...row }
      this.viewVisible = true
    },
    openDialog(row) {
      if (row) {
        this.isEdit = true
        this.currentNotice = { ...row }
      } else {
        this.isEdit = false
        this.currentNotice = { id: null, title: '', content: '' }
      }
      this.dialogVisible = true
      this.$nextTick(() => {
        if (this.$refs.noticeForm) this.$refs.noticeForm.clearValidate()
      })
    },
    saveNotice() {
      this.$refs.noticeForm.validate(valid => {
        if (!valid) return

        if (this.isEdit) {
          request.put('/api/admin/notice/update', this.currentNotice).then(() => {
            this.$message.success('更新成功')
            this.dialogVisible = false
            this.loadNotices()
          })
        } else {
          request.post('/api/admin/notice/add', this.currentNotice).then(() => {
            this.$message.success('添加成功')
            this.dialogVisible = false
            this.loadNotices()
          })
        }
      })
    },
    deleteNotice(id) {
      this.$confirm('确认删除该公告吗？', '提示', { type: 'warning' }).then(() => {
        request.delete(`/api/admin/notice/delete/${id}`).then(() => {
          this.$message.success('删除成功')
          this.loadNotices()
        })
      }).catch(() => {})
    }
  }
}
</script>
