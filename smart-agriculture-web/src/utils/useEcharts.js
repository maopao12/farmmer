import { ref, onMounted, onBeforeUnmount, watch, shallowRef } from 'vue'
import * as echarts from 'echarts'

/**
 * ECharts Composition API 封装
 *
 * 功能：
 *   - ResizeObserver 自适应容器大小变化
 *   - window.resize 兜底监听
 *   - onBeforeUnmount 自动 dispose() 防止内存泄漏
 *   - saveAsImage 工具箱配置（可选）
 *
 * 用法：
 *   const { chartRef, setOption } = useEcharts({ toolbox: true })
 *   <div ref="chartRef" style="height:300px"></div>
 */
export function useEcharts(options = {}) {
  const { toolbox = false } = options

  const chartRef = shallowRef(null)
  let chartInstance = null
  let resizeObserver = null

  /** 设置图表配置 */
  function setOption(option, notMerge = true) {
    if (!chartInstance) return
    chartInstance.setOption(option, notMerge)
  }

  /** 获取图表实例（用于调用 getDataURL 等原生方法） */
  function getInstance() {
    return chartInstance
  }

  /** 导出图片 */
  function saveAsImage(filename = 'chart.png') {
    if (!chartInstance) return
    const url = chartInstance.getDataURL({ type: 'png', pixelRatio: 2, backgroundColor: '#fff' })
    const link = document.createElement('a')
    link.href = url
    link.download = filename
    link.click()
  }

  /** 初始化图表 */
  function initChart() {
    const dom = chartRef.value
    if (!dom) return

    if (chartInstance) {
      chartInstance.dispose()
    }

    chartInstance = echarts.init(dom)

    // 默认工具箱（仅在图表尚未设置 toolbox 时添加）
    if (toolbox) {
      const currentOption = chartInstance.getOption()
      const hasToolbox = currentOption && currentOption.toolbox &&
                         (Array.isArray(currentOption.toolbox) ? currentOption.toolbox.length > 0 : true)
      if (!hasToolbox) {
        chartInstance.setOption({
          toolbox: {
            feature: {
              saveAsImage: { title: '保存为图片', pixelRatio: 2 },
              dataZoom: { title: { zoom: '区域缩放', back: '还原' } },
              restore: { title: '还原' }
            }
          }
        }, false)
      }
    }

    // ResizeObserver —— 优先使用，监听容器尺寸变化
    if (window.ResizeObserver) {
      resizeObserver = new ResizeObserver(() => {
        chartInstance?.resize()
      })
      resizeObserver.observe(dom)
    }

    // window.resize 兜底
    const onWindowResize = () => chartInstance?.resize()
    window.addEventListener('resize', onWindowResize)

    // 保存清理函数引用
    dom._echartsCleanup = () => {
      window.removeEventListener('resize', onWindowResize)
      resizeObserver?.disconnect()
      resizeObserver = null
    }
  }

  onMounted(() => {
    initChart()
  })

  onBeforeUnmount(() => {
    if (chartRef.value?._echartsCleanup) {
      chartRef.value._echartsCleanup()
    }
    if (chartInstance) {
      chartInstance.dispose()
      chartInstance = null
    }
  })

  return { chartRef, setOption, getInstance, saveAsImage, initChart }
}
