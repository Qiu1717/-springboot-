<template>
  <div>
    <el-card>
      <div slot="header">
        <span style="font-size:16px;font-weight:bold">我的消息</span>
        <span v-if="messages.length > 0" style="color:#999;font-size:13px;margin-left:8px">
          共 {{ messages.length }} 条，未读 {{ unreadCount }} 条
        </span>
      </div>

      <el-timeline v-if="messages.length > 0">
        <el-timeline-item
          v-for="msg in messages"
          :key="msg.id"
          :timestamp="msg.createTime"
          placement="top"
        >
          <el-card
            shadow="hover"
            :class="['msg-card', { 'msg-unread': msg.isRead === 0 }]"
            @click.native="readMessage(msg)"
          >
            <h4 :style="{color: msg.isRead === 0 ? '#303133' : '#999'}">
              <span v-if="msg.isRead === 0" style="display:inline-block;width:8px;height:8px;background:#F56C6C;border-radius:50%;margin-right:6px;vertical-align:middle"></span>
              {{ msg.title }}
            </h4>
            <p style="font-size:13px;color:#666;line-height:1.7;white-space:pre-wrap;margin:8px 0 0">{{ msg.content }}</p>
          </el-card>
        </el-timeline-item>
      </el-timeline>

      <el-empty v-if="messages.length === 0" description="暂无消息"></el-empty>
    </el-card>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'Messages',
  data() {
    return {
      messages: [],
      unreadCount: 0
    }
  },
  created() {
    this.loadMessages()
  },
  methods: {
    loadMessages() {
      request.get('/api/messages').then(res => {
        this.messages = res.data || []
        this.unreadCount = this.messages.filter(m => m.isRead === 0).length
      })
    },
    readMessage(msg) {
      if (msg.isRead === 1) return  // 已读不重复请求
      request.put(`/api/messages/${msg.id}/read`).then(() => {
        msg.isRead = 1
        this.unreadCount--
      })
    }
  }
}
</script>

<style scoped>
.msg-card { cursor: pointer; transition: all 0.2s; }
.msg-card:hover { border-color: #409EFF; }
.msg-unread { border-left: 3px solid #409EFF; }
</style>
