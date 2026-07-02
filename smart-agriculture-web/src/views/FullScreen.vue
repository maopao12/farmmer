<template>
  <div class="fullscreen-page">
    <div class="screen-header">
      <el-button text @click="$router.push('/dashboard')" style="color:#fff">
        <el-icon><ArrowLeft /></el-icon> 退出大屏
      </el-button>
      <span class="screen-logo">智慧农业数据大屏</span>
      <span class="screen-time">{{ now }}</span>
    </div>
    <el-row :gutter="12" style="padding:12px">
      <el-col :span="6" v-for="s in stats" :key="s.label">
        <div class="screen-stat">
          <div class="s-label">{{ s.label }}</div>
          <div class="s-value">{{ s.value }}<span class="s-unit"> {{ s.unit }}</span></div>
        </div>
      </el-col>
    </el-row>
    <el-row :gutter="12" style="padding:0 12px">
      <el-col :span="16">
        <div class="screen-card">
          <div class="screen-card-title">温湿度趋势（近24小时）</div>
          <div ref="trendRef" style="height:320px"></div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="screen-card">
          <div class="screen-card-title">设备状态分布（实时）</div>
          <div ref="pieRef" style="height:320px"></div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useEcharts } from '@/utils/useEcharts'
import { deviceApi, sensorApi } from '@/api'
import { ArrowLeft } from '@element-plus/icons-vue'

const now = ref(new Date().toLocaleString())
setInterval(() => { now.value = new Date().toLocaleString() }, 1000)

const stats = ref([
  { label: '平均温度', value: '--', unit: '°C' },
  { label: '平均湿度', value: '--', unit: '%' },
  { label: '在线设备', value: '--', unit: '台' },
  { label: '未读告警', value: '--', unit: '条' }
])

const { chartRef: trendRef, setOption: setTrendOption } = useEcharts({ toolbox: true })
const { chartRef: pieRef, setOption: setPieOption } = useEcharts({ toolbox: true })

onMounted(async () => {
  // ========== 1. 获取设备状态（真实数据） ==========
  try {
    const allPlots = await import('@/api').then(m => m.plotApi.all())
    const plots = Array.isArray(allPlots) ? allPlots : []
    let totalOnline = 0
    let totalOffline = 0

    for (const plot of plots) {
      try {
        const devices = await deviceApi.list(plot.id)
        for (const d of devices) {
          if (d.status === 'ONLINE') totalOnline++
          else totalOffline++
        }
      } catch {}
    }

    // 饼图：设备状态
    setPieOption({
      tooltip: { trigger: 'item' },
      toolbox: { right: 10, feature: { saveAsImage: { title: '保存', pixelRatio: 2, backgroundColor: '#0d1b3e' } } },
      series: [{
        type: 'pie', radius: ['55%', '78%'], center: ['50%', '55%'],
        label: { color: '#aaa', fontSize: 11 },
        data: [
          { value: totalOnline, name: '在线', itemStyle: { color: '#34a853' } },
          { value: totalOffline, name: '离线', itemStyle: { color: '#e81123' } }
        ]
      }]
    })

    stats.value[2].value = totalOnline
    stats.value[3].value = totalOffline
  } catch {}

  // ========== 2. 获取传感器历史数据（真实数据） ==========
  try {
    // 从数据库查地块A的温湿度传感器
    const tempData = await sensorApi.history({ deviceId: 1, dataType: 'TEMPERATURE', days: 1 })
    const humData = await sensorApi.history({ deviceId: 1, dataType: 'HUMIDITY', days: 1 })

    const tempValues = (tempData || []).map(p => p.value)
    const humValues = (humData || []).map(p => p.value)
    const timeLabels = (tempData || []).map(p => (p.time || '').substring(11, 16))

    // 计算平均值
    if (tempValues.length > 0) {
      const avgTemp = (tempValues.reduce((a, b) => a + b, 0) / tempValues.length).toFixed(1)
      stats.value[0].value = avgTemp
    }
    if (humValues.length > 0) {
      const avgHum = (humValues.reduce((a, b) => a + b, 0) / humValues.length).toFixed(1)
      stats.value[1].value = avgHum
    }

    // 趋势图：用真实数据，数据为空时用空数组
    setTrendOption({
      tooltip: { trigger: 'axis' },
      toolbox: { right: 10, feature: { saveAsImage: { title: '保存', pixelRatio: 2, backgroundColor: '#0d1b3e' }, restore: { title: '还原' } } },
      legend: { data: ['温度', '湿度'], textStyle: { color: '#fff' }, top: 40 },
      grid: { top: 80, right: 30, bottom: 30, left: 60 },
      xAxis: { type: 'category', data: timeLabels.length > 0 ? timeLabels : ['暂无数据'], axisLabel: { color: '#aaa', fontSize: 10 } },
      yAxis: [
        { type: 'value', name: '°C', nameTextStyle: { color: '#aaa' }, axisLabel: { color: '#aaa' } },
        { type: 'value', name: '%', nameTextStyle: { color: '#aaa' }, axisLabel: { color: '#aaa' } }
      ],
      series: [
        { name: '温度', type: 'line', smooth: true, yAxisIndex: 0,
          data: tempValues.length > 0 ? tempValues : [],
          lineStyle: { color: '#e81123' }, itemStyle: { color: '#e81123' } },
        { name: '湿度', type: 'line', smooth: true, yAxisIndex: 1,
          data: humValues.length > 0 ? humValues : [],
          lineStyle: { color: '#1a73e8' }, itemStyle: { color: '#1a73e8' } }
      ]
    })
  } catch {}
})
</script>

<style scoped>
.fullscreen-page { min-height: 100vh; background: #0d1b3e; color: #fff; }
.screen-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 24px; background: rgba(0,0,0,0.3);
}
.screen-logo { font-size: 20px; font-weight: 600; }
.screen-time { font-size: 13px; color: #aaa; }
.screen-stat { background: rgba(255,255,255,0.08); border-radius: 8px; padding: 16px; text-align: center; margin: 6px 0; }
.s-label { font-size: 11px; color: #aaa; text-transform: uppercase; }
.s-value { font-size: 28px; font-weight: 700; margin-top: 4px; color: #4fc3f7; }
.s-unit { font-size: 13px; color: #aaa; }
.screen-card { background: rgba(255,255,255,0.06); border-radius: 8px; padding: 16px; margin: 6px 0; }
.screen-card-title { font-size: 14px; font-weight: 600; margin-bottom: 8px; color: #e0e0e0; }
</style>
