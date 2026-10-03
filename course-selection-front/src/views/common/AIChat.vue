<template>
  <div class="ai-assistant">
    <!-- ===== 收缩状态：浮动气泡按钮 ===== -->
    <div v-if="!visible" class="ai-bubble" @click="visible = true">
      <div class="bubble-inner">
        <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M12 2a10 10 0 0 1 6.5 17.5l2.5 2.5H3l2.5-2.5A10 10 0 0 1 12 2z"/>
          <circle cx="8.5" cy="11" r="1.2" fill="currentColor"/>
          <circle cx="12" cy="11" r="1.2" fill="currentColor"/>
          <circle cx="15.5" cy="11" r="1.2" fill="currentColor"/>
        </svg>
      </div>
      <div class="bubble-pulse"></div>
      <span class="bubble-tip">AI 助手</span>
    </div>

    <!-- ===== 展开状态：聊天窗口 ===== -->
    <transition name="slide-up">
      <div v-if="visible" class="ai-panel">
        <!-- 头部 -->
        <div class="panel-header">
          <div class="header-left">
            <div class="ai-avatar">✨</div>
            <div>
              <div class="ai-title">AI 助手</div>
              <div class="ai-subtitle">{{ sceneLabel }}</div>
            </div>
          </div>
          <div class="header-right">
            <i class="el-icon-minus" @click="visible = false" title="收起"></i>
          </div>
        </div>

        <!-- 消息列表 -->
        <div class="panel-messages" ref="msgBox">
          <!-- 欢迎消息 -->
          <div v-if="messages.length === 0" class="welcome">
            <div class="welcome-icon">🤖</div>
            <p>你好！我是选课系统的 AI 助手</p>
            <p class="welcome-hint">{{ sceneHint }}</p>
            <div class="quick-btns">
              <span v-for="q in quickQuestions" :key="q" @click="send(q)">{{ q }}</span>
            </div>
          </div>

          <!-- 历史消息 -->
          <div v-for="(msg, i) in messages" :key="i" :class="['msg-row', msg.role]">
            <div class="msg-avatar">{{ msg.role === 'ai' ? '✨' : '👤' }}</div>
            <div class="msg-bubble">{{ msg.content }}</div>
          </div>

          <!-- 加载中 -->
          <div v-if="loading" class="msg-row ai">
            <div class="msg-avatar">✨</div>
            <div class="msg-bubble typing">
              <span></span><span></span><span></span>
            </div>
          </div>
        </div>

        <!-- 输入区 -->
        <div class="panel-input">
          <input
            v-model="input"
            placeholder="输入问题，按回车发送..."
            @keyup.enter="send()"
            :disabled="loading"
          />
          <button @click="send()" :disabled="loading || !input.trim()">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
              <path d="M2 21l21-9L2 3v7l15 2-15 2v7z"/>
            </svg>
          </button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'AIChat',
  data() {
    return {
      visible: false,
      input: '',
      messages: [],
      loading: false
    }
  },
  computed: {
    currentPage() {
      return this.$route.path  // 如 /student/courses
    },
    sceneLabel() {
      const map = {
        '/student/courses': '选课推荐',
        '/student/my-selections': '成绩分析',
        '/teacher/score': '成绩录入',
        '/teacher/score/': '成绩录入',
        '/admin/notices': '公告助手',
        '/admin/courses': '课程管理',
        '/student/files': '课程问答',
        '/teacher/files': '课程问答',
        '/admin/files': '课程问答'
      }
      for (const [key, val] of Object.entries(map)) {
        if (this.currentPage.startsWith(key)) return val
      }
      return '通用助手'
    },
    sceneHint() {
      const map = {
        '选课推荐': '我可以帮你推荐适合的课程，分析课程难度～',
        '成绩分析': '我可以帮你分析成绩趋势，给出学习建议～',
        '成绩录入': '我可以帮你分析班级整体成绩分布～',
        '公告助手': '我可以帮你撰写或润色公告文案～',
        '课程管理': '我可以帮你规划课程安排～',
        '课程问答': '我可以回答与课程相关的问题～'
      }
      return map[this.sceneLabel] || '有什么可以帮你的？'
    },
    quickQuestions() {
      const map = {
        '选课推荐': ['帮我推荐一些课程', '哪些课比较热门？', '怎么选课比较合理？'],
        '成绩分析': ['分析一下我的成绩', '怎么提高绩点？', '我这学期表现如何？'],
        '成绩录入': ['分析班级成绩分布', '如何设置合理的评分标准？'],
        '公告助手': ['帮我写一个考试通知', '润色这段公告文案'],
        '课程管理': ['如何合理排课？', '课程容量怎么设置？'],
        '课程问答': ['介绍一下这门课', '这门课的先修课是什么？']
      }
      return map[this.sceneLabel] || ['你好，请介绍一下自己', '你能帮我做什么？']
    }
  },
  methods: {
    send(msg) {
      const text = (msg || this.input).trim()
      if (!text || this.loading) return

      this.messages.push({ role: 'user', content: text })
      this.input = ''
      this.loading = true
      this.$nextTick(() => this.scrollBottom())

      request.post('/api/ai/chat', {
        message: text,
        currentPage: this.currentPage
      }).then(res => {
        this.messages.push({ role: 'ai', content: res.data })
        this.loading = false
        this.$nextTick(() => this.scrollBottom())
      }).catch(() => {
        this.messages.push({ role: 'ai', content: '抱歉，AI 服务暂时不可用。' })
        this.loading = false
      })
    },
    scrollBottom() {
      const el = this.$refs.msgBox
      if (el) el.scrollTop = el.scrollHeight
    }
  }
}
</script>

<style scoped>
/* ===== 浮动气泡 ===== */
.ai-bubble {
  position: fixed;
  bottom: 28px;
  right: 28px;
  z-index: 9999;
  cursor: pointer;
}
.bubble-inner {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  box-shadow: 0 6px 24px rgba(102, 126, 234, 0.45);
  position: relative;
  z-index: 2;
  transition: transform 0.3s;
}
.bubble-inner:hover { transform: scale(1.08); }
.bubble-pulse {
  position: absolute;
  top: -4px; left: -4px;
  width: 64px; height: 64px;
  border-radius: 50%;
  background: rgba(102, 126, 234, 0.25);
  animation: pulse 2s infinite;
  z-index: 1;
}
@keyframes pulse {
  0% { transform: scale(1); opacity: 0.8; }
  100% { transform: scale(1.6); opacity: 0; }
}
.bubble-tip {
  position: absolute;
  top: 10px;
  right: 70px;
  background: rgba(0,0,0,0.75);
  color: #fff;
  font-size: 12px;
  padding: 5px 12px;
  border-radius: 14px;
  white-space: nowrap;
  opacity: 0;
  transition: opacity 0.3s;
  pointer-events: none;
}
.ai-bubble:hover .bubble-tip { opacity: 1; }

/* ===== 聊天面板 ===== */
.ai-panel {
  position: fixed;
  bottom: 28px;
  right: 28px;
  z-index: 9999;
  width: 390px;
  height: 540px;
  background: rgba(255,255,255,0.92);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 18px;
  box-shadow: 0 12px 48px rgba(0,0,0,0.15), 0 0 0 1px rgba(255,255,255,0.6) inset;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 头部 */
.panel-header {
  padding: 16px 18px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.header-left { display: flex; align-items: center; gap: 10px; }
.ai-avatar { font-size: 24px; }
.ai-title { font-size: 15px; font-weight: 600; }
.ai-subtitle { font-size: 11px; opacity: 0.75; margin-top: 2px; }
.header-right i {
  font-size: 16px;
  cursor: pointer;
  opacity: 0.8;
  padding: 4px;
}
.header-right i:hover { opacity: 1; }

/* 消息区 */
.panel-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.panel-messages::-webkit-scrollbar { width: 4px; }
.panel-messages::-webkit-scrollbar-thumb { background: #d0d5e0; border-radius: 2px; }

/* 欢迎区 */
.welcome { text-align: center; padding: 30px 10px; }
.welcome-icon { font-size: 48px; margin-bottom: 12px; }
.welcome p { color: #444; font-size: 15px; margin: 4px 0; }
.welcome-hint { color: #999; font-size: 13px !important; }
.quick-btns {
  margin-top: 18px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}
.quick-btns span {
  font-size: 12px;
  padding: 6px 14px;
  background: linear-gradient(135deg, rgba(102,126,234,0.08), rgba(118,75,162,0.08));
  border: 1px solid rgba(102,126,234,0.2);
  border-radius: 20px;
  color: #667eea;
  cursor: pointer;
  transition: all 0.2s;
}
.quick-btns span:hover { background: rgba(102,126,234,0.15); border-color: #667eea; }

/* 消息行 */
.msg-row { display: flex; gap: 8px; align-items: flex-start; }
.msg-row.user { flex-direction: row-reverse; }
.msg-avatar {
  width: 30px; height: 30px;
  border-radius: 50%;
  background: #f0f2ff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  flex-shrink: 0;
}
.msg-row.user .msg-avatar { background: #e8ecff; }
.msg-bubble {
  max-width: 75%;
  padding: 10px 14px;
  border-radius: 16px;
  font-size: 13px;
  line-height: 1.6;
  word-break: break-word;
}
.msg-row.ai .msg-bubble {
  background: #f0f2ff;
  color: #333;
  border-bottom-left-radius: 4px;
}
.msg-row.user .msg-bubble {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  border-bottom-right-radius: 4px;
}

/* 打字动画 */
.typing { display: flex; gap: 4px; padding: 14px 18px !important; }
.typing span {
  width: 7px; height: 7px;
  background: #b0b8d0;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out;
}
.typing span:nth-child(2) { animation-delay: 0.2s; }
.typing span:nth-child(3) { animation-delay: 0.4s; }
@keyframes bounce {
  0%,60%,100% { transform: translateY(0); }
  30% { transform: translateY(-6px); }
}

/* 输入区 */
.panel-input {
  padding: 12px 16px;
  border-top: 1px solid rgba(0,0,0,0.06);
  display: flex;
  gap: 8px;
  align-items: center;
}
.panel-input input {
  flex: 1;
  border: 1px solid #e4e8f0;
  border-radius: 22px;
  padding: 10px 16px;
  font-size: 13px;
  outline: none;
  background: #f8f9fc;
  transition: border-color 0.2s;
}
.panel-input input:focus { border-color: #667eea; }
.panel-input button {
  width: 38px; height: 38px;
  border-radius: 50%;
  border: none;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: opacity 0.2s;
  flex-shrink: 0;
}
.panel-input button:disabled { opacity: 0.4; cursor: not-allowed; }
.panel-input button:not(:disabled):hover { opacity: 0.85; }

/* 动画 */
.slide-up-enter-active { transition: all 0.3s ease-out; }
.slide-up-leave-active { transition: all 0.2s ease-in; }
.slide-up-enter { opacity: 0; transform: translateY(30px) scale(0.95); }
.slide-up-leave-to { opacity: 0; transform: translateY(20px) scale(0.95); }

/* 移动端适配 */
@media (max-width: 768px) {
  .ai-panel {
    width: calc(100vw - 16px);
    height: calc(100vh - 80px);
    bottom: 8px;
    right: 8px;
    border-radius: 14px;
  }
  .ai-bubble { bottom: 16px; right: 16px; }
}
</style>
