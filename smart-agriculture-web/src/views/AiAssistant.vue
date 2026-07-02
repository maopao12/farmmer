<template>
  <div class="ai-page">
    <h3><el-icon><ChatDotRound /></el-icon> AI 农事助手</h3>
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="14">
        <div class="card chat-container">
          <div class="chat-messages" ref="chatBox">
            <div v-if="messages.length === 0" class="empty-state">
              <el-icon :size="48"><ChatLineSquare /></el-icon>
              <p style="margin-top:12px">向AI助手提问，获取基于实时数据的农事建议</p>
              <div class="quick-questions">
                <el-tag v-for="q in quickQuestions" :key="q" @click="sendMessage(q)"
                        style="cursor:pointer;margin:4px" effect="plain">
                  {{ q }}
                </el-tag>
              </div>
            </div>
            <div v-for="(msg, i) in messages" :key="i" :class="['msg', msg.role]">
              <div class="msg-content">
                {{ msg.content }}<span v-if="msg._typing" class="typing-cursor">|</span>
              </div>
              <div v-if="msg.references?.length && !msg._typing" class="msg-refs">
                参考知识：
                <el-tag v-for="r in msg.references" :key="r.question" size="small" effect="plain" style="margin:2px">
                  {{ r.category }}
                </el-tag>
              </div>
            </div>
            <div v-if="thinking" class="msg assistant">
              <div class="msg-content"><el-icon class="is-loading"><Loading /></el-icon> 思考中...</div>
            </div>
          </div>
          <div class="chat-input">
            <el-input v-model="question" placeholder="输入农事问题，如：当前土壤湿度合适吗？"
                      @keyup.enter="sendMessage()" :disabled="thinking">
              <template #append>
                <el-button @click="sendMessage()" :disabled="!question.trim() || thinking">
                  <el-icon><Promotion /></el-icon>
                </el-button>
              </template>
            </el-input>
          </div>
        </div>
      </el-col>
      <el-col :span="10">
        <div class="card">
          <div class="card-title">当前地块环境</div>
          <div v-if="envContext" style="font-size:13px;line-height:1.8;color:#5f6368">
            {{ envContext }}
          </div>
          <div v-else class="empty-state" style="padding:20px">
            <p style="font-size:12px">选择地块后显示实时环境参数</p>
          </div>
          <el-divider />
          <div class="card-title">知识库分类</div>
          <el-tag v-for="cat in categories" :key="cat" style="margin:4px;cursor:pointer"
                  @click="selectedCategory = cat" effect="plain">
            {{ cat }}
          </el-tag>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, nextTick, watch } from 'vue'
import { useAppStore } from '@/stores/app'
import { aiApi } from '@/api'
import { useTypewriter } from '@/utils/useTypewriter'
import { ChatDotRound, ChatLineSquare, Loading, Promotion } from '@element-plus/icons-vue'

const store = useAppStore()
const messages = ref([])
const question = ref('')
const thinking = ref(false)
const envContext = ref('')
const selectedCategory = ref('')
const chatBox = ref(null)
const categories = ['IRRIGATION', 'FERTILIZER', 'PEST', 'DISEASE', 'GENERAL']
const quickQuestions = ['当前土壤湿度合适吗？需要浇水吗？', '番茄苗期湿度多少合适？',
                        '温度过高如何处理？', '如何判断作物是否缺水？']

const { displayText: typingText, isTyping: isTypingText, startTyping, finishImmediately } = useTypewriter()
let currentTypingMsgIndex = -1

// 监听打字机文本变化，更新对应消息
watch(typingText, (val) => {
  if (currentTypingMsgIndex >= 0 && messages.value[currentTypingMsgIndex]) {
    messages.value[currentTypingMsgIndex].content = val
    messages.value[currentTypingMsgIndex]._typing = isTypingText.value
    // 滚动到底部
    nextTick(() => {
      chatBox.value?.scrollTo({ top: chatBox.value.scrollHeight, behavior: 'smooth' })
    })
  }
})

async function sendMessage(msg) {
  const text = msg || question.value.trim()
  if (!text || thinking.value) return
  messages.value.push({ role: 'user', content: text })
  question.value = ''
  thinking.value = true
  await nextTick()
  chatBox.value?.scrollTo({ top: chatBox.value.scrollHeight, behavior: 'smooth' })

  try {
    const res = await aiApi.chat({ question: text, plotId: store.currentPlotId })
    const fullAnswer = res.answer
    // 先添加空消息占位，再用打字机填充
    messages.value.push({
      role: 'assistant',
      content: '',
      references: res.references,
      _typing: true
    })
    currentTypingMsgIndex = messages.value.length - 1
    envContext.value = res.environmentContext
    // 启动打字机效果
    startTyping(fullAnswer, {
      speed: 28,
      onComplete: () => {
        if (messages.value[currentTypingMsgIndex]) {
          messages.value[currentTypingMsgIndex]._typing = false
        }
      }
    })
  } catch {
    messages.value.push({ role: 'assistant', content: '抱歉，AI服务暂时不可用。请稍后重试。', _typing: false })
  } finally {
    thinking.value = false
  }
}
</script>

<style scoped>
.ai-page { max-width: 1000px; }
h3 { display: flex; align-items: center; gap: 8px; font-weight: 500; font-size: 16px; }
.chat-container { display: flex; flex-direction: column; height: 500px; }
.chat-messages { flex: 1; overflow-y: auto; padding: 8px; }
.msg { margin: 8px 0; display: flex; }
.msg.user { justify-content: flex-end; }
.msg.user .msg-content { background: var(--primary-light); border-radius: 12px 12px 0 12px; max-width: 70%; }
.msg.assistant .msg-content { background: #f5f5f5; border-radius: 12px 12px 12px 0; max-width: 80%; }
.msg-content { padding: 10px 14px; font-size: 13px; line-height: 1.6; }
.msg-refs { font-size: 11px; color: #5f6368; padding: 4px 14px; }
.chat-input { padding: 8px; border-top: 1px solid var(--border); }
.quick-questions { margin-top: 12px; }
</style>
