<template>
  <view class="container">
    <view class="chat-box">
      <view v-if="messages.length === 0" class="empty-state">
        <text style="font-size:15px">向AI助手提问，获取基于实时数据的农事建议</text>
        <view class="quick-tags">
          <text v-for="q in quickQuestions" :key="q" class="quick-tag" @tap="send(q)">{{ q }}</text>
        </view>
      </view>

      <view v-for="(msg, i) in messages" :key="i" :class="['msg', msg.role]">
        <view class="msg-bubble">{{ msg.content }}
          <text v-if="msg._typing" class="typing-cursor">|</text>
        </view>
      </view>

      <view v-if="thinking" class="msg assistant">
        <view class="msg-bubble thinking">思考中...</view>
      </view>
    </view>

    <view class="input-bar">
      <input v-model="question" placeholder="输入农事问题..." class="chat-input"
             :disabled="thinking" @confirm="send()" />
      <button :disabled="!question.trim() || thinking" @tap="send()"
              :class="['send-btn', { disabled: !question.trim() || thinking }]">发送</button>
    </view>
  </view>
</template>

<script>
import request from '@/api/request.js'

export default {
  data() {
    return {
      messages: [],
      question: '',
      thinking: false,
      quickQuestions: ['需要浇水吗？', '番茄苗期湿度多少合适？', '温度过高如何处理？']
    }
  },
  methods: {
    async send(msg) {
      const text = msg || this.question.trim()
      if (!text || this.thinking) return
      this.messages.push({ role: 'user', content: text })
      this.question = ''
      this.thinking = true

      try {
        const app = getApp()
        const plotId = app.globalData.currentPlotId || 1
        const res = await request.post('/ai/chat', { question: text, plotId })

        // 打字机效果
        const fullText = res.answer
        const msgObj = { role: 'assistant', content: '', _typing: true }
        this.messages.push(msgObj)
        const idx = this.messages.length - 1

        let pos = 0
        const timer = setInterval(() => {
          if (pos < fullText.length) {
            this.messages[idx].content += fullText[pos]
            pos++
          } else {
            clearInterval(timer)
            this.messages[idx]._typing = false
          }
        }, 25)
      } catch {
        this.messages.push({ role: 'assistant', content: '抱歉，AI服务暂时不可用。' })
      } finally { this.thinking = false }
    }
  }
}
</script>

<style scoped>
.chat-box { flex: 1; overflow-y: auto; padding-bottom: 80px; }
.msg { margin: 8px 0; display: flex; }
.msg.user { justify-content: flex-end; }
.msg-bubble { max-width: 80%; padding: 10px 14px; border-radius: 14px; font-size: 13px; line-height: 1.6; }
.msg.user .msg-bubble { background: #e8f0fe; border-bottom-right-radius: 4px; }
.msg.assistant .msg-bubble { background: #f5f5f5; border-bottom-left-radius: 4px; }
.thinking { color: #9e9e9e; }
.typing-cursor { animation: blink 1s infinite; }
@keyframes blink { 0%,100%{opacity:1} 50%{opacity:0} }
.quick-tags { margin-top: 12px; display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; }
.quick-tag { background: #e8f0fe; color: #1a73e8; padding: 6px 12px; border-radius: 16px; font-size: 12px; }
.input-bar { position: fixed; bottom: 0; left: 0; right: 0; padding: 10px 16px; background: #fff; border-top: 1px solid #e0e0e0; display: flex; gap: 8px; align-items: center; }
.chat-input { flex: 1; height: 36px; background: #f8f9fa; border-radius: 18px; padding: 0 14px; font-size: 13px; }
.send-btn { background: #1a73e8; color: #fff; border: none; border-radius: 18px; padding: 6px 18px; font-size: 13px; }
.send-btn.disabled { background: #e0e0e0; color: #9e9e9e; }
</style>
