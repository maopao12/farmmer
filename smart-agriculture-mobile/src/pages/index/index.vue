<template>
  <view class="container">
    <!-- 地块选择器 -->
    <view class="plot-selector" @tap="showPlotPicker = true">
      <text class="plot-name">{{ currentPlot?.name || '选择地块' }}</text>
      <text class="plot-info" v-if="currentPlot">{{ currentPlot.area }}亩 | {{ currentPlot.cropType || '未设置' }}</text>
      <text class="arrow">&#9662;</text>
    </view>

    <!-- 地块选择弹窗 -->
    <view class="picker-overlay" v-if="showPlotPicker" @tap="showPlotPicker = false">
      <view class="picker-sheet" @tap.stop>
        <text class="picker-title">选择地块</text>
        <view v-for="p in plots" :key="p.id" class="picker-item"
              :class="{ active: p.id === currentPlotId }"
              @tap="selectPlot(p)">
          <text>{{ p.name }}</text>
          <text class="picker-sub">{{ p.area }}亩 · {{ p.cropType || '未设置' }}</text>
        </view>
        <button class="picker-close" @tap="showPlotPicker = false">取消</button>
      </view>
    </view>

    <!-- 传感器数据卡片 -->
    <view class="card" v-if="sensorCards.length > 0">
      <view class="card-title">实时环境数据</view>
      <view class="sensor-row" v-for="s in sensorCards" :key="s.type">
        <text>{{ s.label }}</text>
        <text class="sensor-value" :style="{ color: s.color }">
          {{ s.value }}<text class="sensor-unit"> {{ s.unit }}</text>
        </text>
      </view>
    </view>

    <!-- 在线设备状态 -->
    <view class="card">
      <view class="card-title">设备状态</view>
      <view v-if="devices.length > 0">
        <view class="device-item" v-for="d in devices" :key="d.id">
          <view>
            <text class="device-name">{{ d.deviceName }}</text>
            <text class="device-loc">{{ d.installLocation || '' }}</text>
          </view>
          <text :class="d.status === 'ONLINE' ? 'tag-online' : 'tag-offline'">
            {{ d.status === 'ONLINE' ? '在线' : '离线' }}
          </text>
        </view>
      </view>
      <view v-else class="empty-state">
        <text>该地块暂无绑定设备</text>
      </view>
    </view>

    <!-- 控制设备快捷入口 -->
    <view class="card" v-if="controllers.length > 0">
      <view class="card-title">快捷控制</view>
      <view v-for="c in controllers" :key="c.id" class="device-item" @tap="goControl(c.id)">
        <text>{{ c.deviceName }}</text>
        <text class="tag-online">{{ c.status === 'ONLINE' ? '可控制' : '离线' }}</text>
      </view>
    </view>
    <view v-else class="card">
      <view class="empty-state">
        <text style="font-size:14px;color:#5f6368">当前农田未配置控制设备，无法进行远程操作</text>
        <text style="font-size:11px;color:#9e9e9e;margin-top:4px">请联系管理员绑定灌溉或通风控制器</text>
      </view>
    </view>
  </view>
</template>

<script>
import request from '@/api/request.js'

export default {
  data() {
    return {
      plots: [],
      currentPlotId: null,
      showPlotPicker: false,
      devices: [],
      sensorCards: []
    }
  },
  computed: {
    currentPlot() { return this.plots.find(p => p.id === this.currentPlotId) },
    controllers() { return this.devices.filter(d => d.deviceCategory === 'CONTROLLER') }
  },
  async onShow() {
    await this.fetchPlots()
    if (this.currentPlotId) await this.fetchOverview()
  },
  methods: {
    async fetchPlots() {
      try {
        const res = await request.get('/plot/all')
        this.plots = Array.isArray(res) ? res : []
        if (this.plots.length > 0 && !this.currentPlotId) {
          this.currentPlotId = this.plots[0].id
        }
      } catch { }
    },
    async fetchOverview() {
      try {
        const overview = await request.get(`/plot/${this.currentPlotId}/overview`)
        this.devices = [...(overview.sensors || []), ...(overview.controllers || [])]
        const data = overview.latestSensorData || {}
        const cards = []
        for (const [deviceId, sd] of Object.entries(data)) {
          const config = {
            TEMPERATURE: { label: '温度', unit: '°C', color: '#e65100' },
            HUMIDITY: { label: '空气湿度', unit: '%', color: '#1565c0' },
            SOIL_MOISTURE: { label: '土壤湿度', unit: '%', color: '#2e7d32' },
            LIGHT_INTENSITY: { label: '光照', unit: 'lux', color: '#f9a825' }
          }
          cards.push({ ...config[sd.dataType] || { label: sd.dataType, unit: sd.unit, color: '#333' }, value: sd.dataValue, type: sd.dataType })
        }
        this.sensorCards = cards
      } catch { }
    },
    selectPlot(p) {
      this.currentPlotId = p.id
      this.showPlotPicker = false
      this.fetchOverview()
    },
    goControl(deviceId) {
      uni.navigateTo({ url: `/pages/control/control?deviceId=${deviceId}` })
    }
  }
}
</script>

<style scoped>
.plot-selector { background: #fff; border-radius: 12px; padding: 14px; margin-bottom: 12px; display: flex; flex-wrap: wrap; align-items: center; box-shadow: 0 1px 3px rgba(0,0,0,0.06); }
.plot-name { font-size: 15px; font-weight: 600; width: 100%; }
.plot-info { font-size: 11px; color: #5f6368; }
.arrow { margin-left: auto; color: #5f6368; }
.picker-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.4); z-index: 999; display: flex; align-items: flex-end; }
.picker-sheet { background: #fff; border-radius: 16px 16px 0 0; width: 100%; padding: 20px; }
.picker-title { font-size: 16px; font-weight: 600; display: block; margin-bottom: 12px; text-align: center; }
.picker-item { padding: 14px; border-bottom: 1px solid #f0f0f0; }
.picker-item.active { background: #e8f0fe; border-radius: 8px; }
.picker-sub { font-size: 11px; color: #5f6368; display: block; }
.picker-close { margin-top: 12px; background: #f5f5f5; border: none; }
.device-item { display: flex; justify-content: space-between; align-items: center; padding: 10px 0; border-bottom: 1px solid #f5f5f5; }
.device-name { font-size: 13px; font-weight: 500; }
.device-loc { font-size: 11px; color: #5f6368; display: block; }
</style>
