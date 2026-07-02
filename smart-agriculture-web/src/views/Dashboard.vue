<template>
  <div class="dashboard">
    <div class="welcome">
      <h2>{{ greeting }}，{{ store.user?.realName }}</h2>
      <p>当前管理 <b>{{ store.plots.length }}个地块</b> · {{ onlineDevices }}台设备在线 · {{ unreadAlerts }}条未读告警</p>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="12" class="stats-row">
      <el-col :span="6" v-for="s in stats" :key="s.label">
        <div class="card stat-card" @click="$router.push(s.link)">
          <div class="label">{{ s.label }}</div>
          <div class="value">{{ s.value }}<span class="unit" v-if="s.unit"> {{ s.unit }}</span></div>
          <div class="trend" :style="{ color: s.trendColor }">{{ s.trend }}</div>
        </div>
      </el-col>
    </el-row>

    <!-- 主内容区 -->
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="16">
        <!-- 趋势图 -->
        <div class="card" style="margin-bottom:16px">
          <div class="card-header">
            <span class="card-title"><el-icon><TrendCharts /></el-icon> 温湿度趋势</span>
            <el-link type="primary" @click="$router.push('/trends')">查看详情</el-link>
          </div>
          <div ref="trendChart" style="height:260px"></div>
        </div>

        <!-- 我的地块 -->
        <div class="card">
          <div class="card-header">
            <span class="card-title"><el-icon><Grid /></el-icon> 我的地块</span>
            <el-button v-if="store.isAdmin" type="primary" size="small" @click="showPlotDialog = true">
              <el-icon><Plus /></el-icon>添加地块
            </el-button>
          </div>
          <el-row :gutter="12">
            <el-col :span="8" v-for="plot in store.plots" :key="plot.id">
              <div class="plot-card" @click="$router.push(`/monitor/${plot.id}`)">
                <div class="plot-name">{{ plot.name }}</div>
                <div class="plot-info">面积: {{ plot.area }}亩 | 作物: {{ plot.cropType || '未设置' }}</div>
                <div class="plot-tags">
                  <el-tag size="small" type="success">{{ plot.deviceCount || 0 }}台设备</el-tag>
                  <el-tag size="small" :type="plot.offlineCount ? 'danger' : 'success'">
                    {{ plot.offlineCount ? plot.offlineCount + '台离线' : '正常' }}
                  </el-tag>
                </div>
              </div>
            </el-col>
            <el-col :span="8" v-if="store.plots.length === 0">
              <div class="empty-state">
                <el-icon :size="48"><FolderOpened /></el-icon>
                <p style="margin-top:12px">暂无地块数据</p>
              </div>
            </el-col>
          </el-row>
        </div>
      </el-col>

      <el-col :span="8">
        <!-- 快捷操作 -->
        <div class="card" style="margin-bottom:16px">
          <div class="card-title" style="margin-bottom:12px"><el-icon><Aim /></el-icon> 快捷操作</div>
          <el-row :gutter="8">
            <el-col :span="8" v-for="act in quickActions" :key="act.label">
              <div class="quick-action" :style="{background: act.bg}" @click="$router.push(act.link)">
                <el-icon :size="20"><component :is="act.icon" /></el-icon>
                <div style="margin-top:6px">{{ act.label }}</div>
              </div>
            </el-col>
          </el-row>
        </div>

        <!-- 最近告警 -->
        <div class="card">
          <div class="card-header">
            <span class="card-title"><el-icon><Bell /></el-icon> 最近告警</span>
            <el-link type="primary" @click="$router.push('/alerts')">全部</el-link>
          </div>
          <div v-if="recentAlerts.length > 0">
            <div v-for="a in recentAlerts" :key="a.id" class="alert-item" :class="{ warning: a.alertLevel === 'WARNING' }">
              <b>{{ a.alertMsg?.substring(0, 30) }}</b>
              <div style="color:#5f6368;margin-top:2px">{{ formatTime(a.triggerTime) }}</div>
            </div>
          </div>
          <div v-else class="empty-state" style="padding:30px">
            <el-icon :size="32"><CircleCheck /></el-icon>
            <p style="margin-top:8px;font-size:12px">暂无告警</p>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { alertApi, sensorApi } from '@/api'
import { subscribe, unsubscribe } from '@/utils/websocket'
import { useEcharts } from '@/utils/useEcharts'
import { TrendCharts, Grid, Plus, FolderOpened, Bell, CircleCheck, Aim } from '@element-plus/icons-vue'

const router = useRouter()
const store = useAppStore()
const recentAlerts = ref([])
const { chartRef: trendChart, setOption: setTrendOption } = useEcharts()

const greeting = computed(() => {
  const h = new Date().getHours()
  return h < 12 ? '上午好' : h < 18 ? '下午好' : '晚上好'
})

const onlineDevices = ref(0)
const unreadAlerts = ref(0)

const stats = computed(() => [
  { label: '平均温度', value: '26.5', unit: '°C', trend: '+1.2 vs 昨日', trendColor: '#34a853', link: '/trends' },
  { label: '平均土壤湿度', value: '68', unit: '%', trend: '接近下限', trendColor: '#ea8600', link: '/trends' },
  { label: '设备在线率', value: `${onlineDevices.value}/${onlineDevices.value + 2}`, trend: '2台离线', trendColor: '#e81123', link: '/devices' },
  { label: '今日告警', value: unreadAlerts.value, trend: '查看详情', trendColor: '#5f6368', link: '/alerts' }
])

const quickActions = [
  { label: '设备控制', icon: 'Switch', link: '/control', bg: '#e8f0fe' },
  { label: '告警中心', icon: 'Bell', link: '/alerts', bg: '#fce4ec' },
  { label: '操作日志', icon: 'Document', link: '/logs', bg: '#e8f5e9' },
  { label: 'AI助手', icon: 'ChatDotRound', link: '/ai-assistant', bg: '#f3e5f5' },
  { label: '系统设置', icon: 'Setting', link: '/settings', bg: '#fff3e0' },
  { label: '全屏大屏', icon: 'FullScreen', link: '/screen', bg: '#e0f2f1' }
]

function formatTime(t) {
  if (!t) return ''
  const d = new Date(t)
  return `${d.getMonth()+1}/${d.getDate()} ${d.getHours()}:${String(d.getMinutes()).padStart(2,'0')}`
}

onMounted(async () => {
  // 初始化趋势图
  const hours = Array.from({length: 24}, (_, i) => `${i}:00`)
  setTrendOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['温度', '湿度'], bottom: 0 },
    grid: { top: 10, right: 20, bottom: 30, left: 50 },
    xAxis: { type: 'category', data: hours, axisLabel: { fontSize: 10 } },
    yAxis: [
      { type: 'value', name: '°C', axisLabel: { fontSize: 10 } },
      { type: 'value', name: '%', axisLabel: { fontSize: 10 } }
    ],
    series: [
      { name: '温度', type: 'line', smooth: true, yAxisIndex: 0,
        data: [22,22.5,22,21.5,21,22,23,24,25.5,27,28.5,30,31,31.5,31,30,29,28,27,26,25,24,23,22.5],
        lineStyle: { color: '#e81123' }, itemStyle: { color: '#e81123' } },
      { name: '湿度', type: 'line', smooth: true, yAxisIndex: 1,
        data: [75,76,77,78,76,74,72,70,68,66,64,62,61,60,62,64,66,68,70,72,73,74,75,76],
        lineStyle: { color: '#1a73e8' }, itemStyle: { color: '#1a73e8' } }
    ]
  })
  try {
    const alerts = await alertApi.logs({ page: 1, size: 3 })
    recentAlerts.value = alerts?.records || []
  } catch {}
  // WebSocket订阅
  if (store.currentPlotId) {
    subscribe(`/topic/plot/${store.currentPlotId}/sensors`, (data) => {
      console.log('[WS] 传感器数据:', data)
    })
  }
})

onUnmounted(() => {
  // useEcharts 已自动处理 dispose
})
</script>

<style scoped>
.dashboard { max-width: 1200px; }
.welcome { margin-bottom: 20px; }
.welcome h2 { font-weight: 400; color: #202124; margin: 0; }
.welcome p { color: #5f6368; font-size: 13px; margin: 4px 0; }
.stats-row { margin-bottom: 0; }
.card-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 14px;
}
.card-title {
  font-weight: 600; color: #202124; font-size: 14px;
  display: flex; align-items: center; gap: 6px;
}
.plot-name { font-weight: 600; font-size: 14px; }
.plot-info { font-size: 11px; color: #5f6368; margin: 4px 0; }
.plot-tags { display: flex; gap: 6px; margin-top: 8px; }
</style>
