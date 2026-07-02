import { ref } from 'vue'

/**
 * 打字机逐字显示效果 composable
 *
 * 用法:
 *   const { displayText, isTyping, startTyping } = useTypewriter()
 *   startTyping(fullText, { speed: 30 })
 *   <div>{{ displayText }}<span v-if="isTyping" class="cursor">|</span></div>
 */
export function useTypewriter() {
  const displayText = ref('')
  const isTyping = ref(false)
  let timer = null

  function startTyping(text, opts = {}) {
    const { speed = 30, onComplete } = opts

    // 清除上一次的定时器
    stopTyping()
    displayText.value = ''
    isTyping.value = true

    let index = 0
    const chars = [...text]  // 正确处理中文等多字节字符

    timer = setInterval(() => {
      if (index < chars.length) {
        displayText.value += chars[index]
        index++
      } else {
        stopTyping()
        if (typeof onComplete === 'function') onComplete()
      }
    }, speed)
  }

  function stopTyping() {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
    isTyping.value = false
  }

  function finishImmediately(text) {
    stopTyping()
    displayText.value = text
  }

  return { displayText, isTyping, startTyping, stopTyping, finishImmediately }
}
