<template>
  <div class="trends-page">
    <h3><el-icon><TrendCharts /></el-icon> 历史趋势分析</h3>
    <div class="card" style="margin-top:16px">
      <el-form :inline="true" size="small">
        <el-form-item label="地块">
          <el-select v-model="selectedPlotId" style="width:180px">
            <el-option v-for="p in store.plots" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="设备">
          <el-select v-model="selectedDeviceId" style="width:200px">
            <el-option v-for="d in sensors" :key="d.id" :label="d.deviceName" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据类型">
          <el-select v-model="dataType" style="width:120px">
            <el-option label="温度" value="TEMPERATURE" />
            <el-option label="湿度" value="HUMIDITY" />
            <el-option label="土壤湿度" value="SOIL_MOISTURE" />
            <el-option label="光照" value="LIGHT_INTENSITY" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-select v-model="days" style="width:100px">
            <el-option label="24小时" :value="1" />
            <el-option label="7天" :value="7" />
            <el-option label="30天" :value="30" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchHistory">查询</el-button>
          <el-button @click="saveAsImage('历史趋势.png')">
            <el-icon><Download /></el-icon> 导出图片
          </el-button>
        </el-form-item>
      </el-form>
      <div ref="chartRef" style="height:400px"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { useAppStore } from '@/stores/app'
import { deviceApi, sensorApi } from '@/api'
import { useEcharts } from '@/utils/useEcharts'
import { TrendCharts, Download } from '@element-plus/icons-vue'

const store = useAppStore()
const selectedPlotId = ref(null)
const selectedDeviceId = ref(null)
const dataType = ref('TEMPERATURE')
const days = ref(7)
const sensors = ref([])

const { chartRef, setOption, saveAsImage } = useEcharts({ toolbox: true })

watch(selectedPlotId, async (plotId) => {
  if (!plotId) return
  const devices = await deviceApi.list(plotId)
  sensors.value = devices.filter(d => d.deviceCategory === 'SENSOR')
  selectedDeviceId.value = sensors.value[0]?.id || null
})

async function fetchHistory() {
  if (!selectedDeviceId.value) return
  const data = await sensorApi.history({ deviceId: selectedDeviceId.value, dataType: dataType.value, days: days.value })
  renderChart(data || [])
}

function renderChart(points) {
  const times = points.map(p => p.time?.substring(5, 16) || '')
  const values = points.map(p => p.value)
  const unit = points[0]?.unit || ''
  setOption({
    tooltip: { trigger: 'axis' },
    toolbox: {
      right: 20,
      feature: {
        saveAsImage: { title: '保存图片', pixelRatio: 2, backgroundColor: '#fff' },
        dataZoom: { title: { zoom: '区域缩放', back: '还原' } },
        restore: { title: '还原' }
      }
    },
    grid: { top: 60, right: 30, bottom: 30, left: 60 },
    xAxis: { type: 'category', data: times, axisLabel: { fontSize: 10, rotate: 30 } },
    yAxis: { type: 'value', name: unit, axisLabel: { fontSize: 10 } },
    series: [{
      type: 'line', smooth: true, data: values,
      lineStyle: { color: '#1a73e8' },
      itemStyle: { color: '#1a73e8' },
      areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [
          { offset: 0, color: 'rgba(26,115,232,0.25)' },
          { offset: 1, color: 'rgba(26,115,232,0)' }
        ]}
      }
    }]
  })
}

// 首次加载默认地块
watch(() => store.plots, (plots) => {
  if (plots.length > 0 && !selectedPlotId.value) {
    selectedPlotId.value = plots[0].id
  }
}, { immediate: true })
</script>
