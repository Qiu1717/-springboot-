<template>
  <div>
    <el-card>
      <div slot="header">
        <span style="font-size:16px;font-weight:bold">系统公告</span>
      </div>
      <el-timeline v-if="notices.length > 0">
        <el-timeline-item
          v-for="notice in notices"
          :key="notice.id"
          :timestamp="notice.createTime"
          placement="top"
        >
          <el-card shadow="hover" class="notice-card">
            <h4 class="notice-title" @click="showDetail(notice)">{{ notice.title }}</h4>
            <p style="font-size:12px;color:#999;margin:4px 0">{{ notice.author || '—' }}</p>
            <p class="notice-brief" @click="showDetail(notice)">
              {{ cutContent(notice.content) }}
              <span v-if="notice.content.length > 80" style="color:#409EFF;cursor:pointer">…查看详情</span>
            </p>
          </el-card>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-if="notices.length === 0" description="暂无公告"></el-empty>
    </el-card>

    <!-- 公告详情弹窗 -->
    <el-dialog :title="currentNotice.title" :visible.sync="dialogVisible" width="600px" top="5vh">
      <div style="white-space:pre-wrap;line-height:1.8;font-size:14px;color:#333;max-height:60vh;overflow-y:auto">
        {{ currentNotice.content }}
      </div>
      <div style="color:#999;font-size:12px;margin-top:16px;text-align:right">
        发布者：{{ currentNotice.author || '—' }} &emsp; 发布时间：{{ currentNotice.createTime }}
      </div>
    </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'StudentNotices',
  data() {
    return {
      notices: [],
      dialogVisible: false,
      currentNotice: { title: '', content: '', createTime: '' }
    }
  },
  created() {
    this.loadNotices()
  },
  methods: {
    loadNotices() {
      request.get('/api/student/notices').then(res => {
        this.notices = res.data
      })
    },
    cutContent(text) {
      if (!text) return ''
      return text.length > 80 ? text.slice(0, 80) : text
    },
    showDetail(notice) {
      this.currentNotice = notice
      this.dialogVisible = true
    }
  }
}
</script>

<style scoped>
.notice-title {
  color: #409EFF;
  cursor: pointer;
  margin: 0 0 8px 0;
}
.notice-title:hover { color: #66b1ff; }
.notice-brief {
  color: #666;
  margin: 0;
  cursor: pointer;
  line-height: 1.6;
}
</style>
