<template>
  <view class="container">
    <!-- 地块选择 -->
    <view class="card">
      <view class="card-title">选择地块</view>
      <picker :range="plotNames" @change="onPlotChange">
        <view class="selector-box">{{ plotNames[plotIndex] || '请选择地块' }}</view>
      </picker>
    </view>

    <!-- 设备选择 -->
    <view class="card" v-if="controllers.length > 0">
      <view class="card-title">选择控制设备</view>
      <view v-for="d in controllers" :key="d.id" class="device-option"
            :class="{ selected: d.id === selectedDeviceId, disabled: d.status !== 'ONLINE' }"
            @tap="selectDevice(d)">
        <view>
          <text class="device-name">{{ d.deviceName }}</text>
          <text class="device-loc">{{ d.installLocation || '未指定' }}</text>
        </view>
        <text :class="d.status === 'ONLINE' ? 'tag-online' : 'tag-offline'">
          {{ d.status === 'ONLINE' ? '在线' : '离线' }}
        </text>
      </view>
    </view>

    <!-- 控制面板 - 仅当有CONTROLLER且已选择 -->
    <view class="card" v-if="selectedDevice">
      <view class="card-title">{{ selectedDevice.deviceName }}</view>
      <text class="device-loc">状态: {{ selectedDevice.status === 'ONLINE' ? '在线' : '离线' }}</text>

      <view class="control-center">
        <view class="switch-circle" :class="{ on: isOn }">
          {{ isOn ? 'ON' : 'OFF' }}
        </view>
        <view class="control-buttons">
          <button class="btn-primary" :disabled="selectedDevice.status !== 'ONLINE' || loading"
                  :class="{ 'btn-disabled': selectedDevice.status !== 'ONLINE' }"
                  @tap="execute('ON')">开启灌溉</button>
          <button class="btn-danger" :disabled="selectedDevice.status !== 'ONLINE' || loading"
                  :class="{ 'btn-disabled': selectedDevice.status !== 'ONLINE' }"
                  @tap="execute('OFF')">关闭灌溉</button>
        </view>
        <view class="timer-row">
          <text>定时(分钟): </text>
          <input v-model="duration" type="number" class="timer-input" />
        </view>
      </view>

      <view v-if="resultMsg" class="result-msg" :class="resultOk ? 'success' : 'error'">
        {{ resultMsg }}
      </view>
    </view>

    <!-- 空状态 -->
    <view v-if="controllers.length === 0 && plotIndex >= 0" class="empty-state" style="padding-top:40px">
      <text style="font-size:15px;color:#5f6368">当前农田未配置控制设备，无法进行远程操作</text>
      <text style="font-size:11px;color:#9e9e9e;margin-top:4px">请联系管理员为地块绑定灌溉或通风控制器</text>
    </view>
  </view>
</template>

<script>
import request from '@/api/request.js'

export default {
  data() {
    return {
      plots: [],
      plotIndex: -1,
      plotNames: [],
      devices: [],
      selectedDeviceId: null,
      duration: 30,
      loading: false,
      isOn: false,
      resultMsg: '',
      resultOk: true
    }
  },
  computed: {
    controllers() { return this.devices.filter(d => d.deviceCategory === 'CONTROLLER') },
    selectedDevice() { return this.devices.find(d => d.id === this.selectedDeviceId) }
  },
  async onShow() {
    const res = await request.get('/plot/all')
    this.plots = Array.isArray(res) ? res : []
    this.plotNames = this.plots.map(p => p.name)
  },
  methods: {
    async onPlotChange(e) {
      this.plotIndex = e.detail.value
      this.selectedDeviceId = null
      this.devices = await request.get('/device/list', { plotId: this.plots[this.plotIndex].id })
    },
    selectDevice(d) {
      if (d.status !== 'ONLINE') { uni.showToast({ title: '设备离线，无法操作', icon: 'none' }); return }
      this.selectedDeviceId = d.id
      this.resultMsg = ''
    },
    async execute(cmd) {
      this.loading = true
      this.resultMsg = ''
      try {
        const res = await request.post('/control/irrigation', {
          deviceId: this.selectedDeviceId, command: cmd, duration: this.duration
        })
        if (res.commandStatus === 'SUCCESS') {
          this.isOn = cmd === 'ON'
          this.resultOk = true
          this.resultMsg = cmd === 'ON' ? '灌溉已开启' : '灌溉已关闭'
        } else {
          this.resultOk = false
          this.resultMsg = res.resultMsg || '指令执行失败'
        }
      } catch (e) {
        this.resultOk = false
        this.resultMsg = e.message || '指令下发失败'
      } finally { this.loading = false }
    }
  }
}
</script>

<style scoped>
.selector-box { background: #f8f9fa; border-radius: 8px; padding: 10px 12px; font-size: 14px; }
.device-option { display: flex; justify-content: space-between; align-items: center; padding: 12px; margin: 4px 0; border: 1px solid #e0e0e0; border-radius: 8px; }
.device-option.selected { border-color: #1a73e8; background: #e8f0fe; }
.device-option.disabled { opacity: 0.5; }
.control-center { text-align: center; padding: 20px 0; }
.switch-circle { width: 80px; height: 80px; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin: 0 auto 16px; font-size: 20px; font-weight: 700; background: #f5f5f5; border: 3px solid #e0e0e0; color: #9e9e9e; }
.switch-circle.on { background: #e8f5e9; border-color: #34a853; color: #34a853; }
.control-buttons { display: flex; gap: 12px; justify-content: center; }
.timer-row { margin-top: 12px; font-size: 13px; display: flex; align-items: center; justify-content: center; gap: 6px; }
.timer-input { width: 60px; border: 1px solid #e0e0e0; border-radius: 4px; padding: 4px; text-align: center; }
.result-msg { margin-top: 12px; padding: 10px; border-radius: 8px; font-size: 13px; text-align: center; }
.result-msg.success { background: #e8f5e9; color: #2e7d32; }
.result-msg.error { background: #ffebee; color: #c62828; }
</style>
