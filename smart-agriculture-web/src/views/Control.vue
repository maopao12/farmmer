<template>
  <div class="control-page">
    <h3><el-icon><Switch /></el-icon> 设备控制</h3>

    <!-- 步骤1：选择地块和设备 -->
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="8">
        <div class="card">
          <div class="card-title">选择地块</div>
          <el-select v-model="selectedPlotId" placeholder="请选择地块" @change="onPlotChange" style="width:100%">
            <el-option v-for="p in store.plots" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="card">
          <div class="card-title">选择控制设备</div>
          <el-select v-model="selectedDeviceId" placeholder="请选择设备" @change="onDeviceChange" style="width:100%">
            <el-option v-for="d in controllers" :key="d.id"
                       :label="`${d.deviceName} (${d.installLocation || '未指定位置'})`"
                       :value="d.id"
                       :disabled="d.status !== 'ONLINE'" />
          </el-select>
        </div>
      </el-col>
    </el-row>

    <!-- 步骤2：执行控制 - 仅在有CONTROLLER时显示 -->
    <div v-if="controllers.length > 0 && selectedDevice" class="card" style="margin-top:16px;max-width:500px">
      <div class="card-title">{{ selectedDevice.deviceName }}</div>
      <div style="font-size:11px;color:#5f6368;margin-bottom:16px">
        安装位置: {{ selectedDevice.installLocation || '未指定' }} |
        状态:
        <el-tag :type="selectedDevice.status === 'ONLINE' ? 'success' : 'danger'" size="small" effect="dark">
          {{ selectedDevice.status === 'ONLINE' ? '在线' : '离线' }}
        </el-tag>
      </div>

      <div style="text-align:center">
        <div class="switch-indicator" :class="{ active: isOn }">
          {{ isOn ? 'ON' : 'OFF' }}
        </div>
        <div style="margin-top:16px;display:flex;gap:12px;justify-content:center">
          <el-button type="success" size="large" :loading="loading === 'ON'"
                     :disabled="selectedDevice.status !== 'ONLINE'"
                     @click="executeCommand('ON')">
            <el-icon><VideoPlay /></el-icon> 开启灌溉
          </el-button>
          <el-button type="danger" size="large" :loading="loading === 'OFF'"
                     :disabled="selectedDevice.status !== 'ONLINE'"
                     @click="executeCommand('OFF')">
            <el-icon><VideoPause /></el-icon> 关闭灌溉
          </el-button>
        </div>
        <div style="margin-top:12px;font-size:12px;color:#5f6368">
          <el-input-number v-model="duration" :min="1" :max="120" size="small" />
          分钟定时（可选）
        </div>
      </div>

      <!-- 执行结果反馈 -->
      <el-alert v-if="resultMsg" :title="resultMsg" :type="resultType" show-icon
                style="margin-top:16px" closable @close="resultMsg = ''" />
    </div>

    <!-- 空状态：无CONTROLLER -->
    <div v-else-if="controllers.length === 0 && selectedPlotId" class="empty-state" style="margin-top:24px">
      <el-icon :size="64"><WarningFilled /></el-icon>
      <p style="font-size:15px;margin-top:16px;color:#5f6368">当前农田未配置控制设备，无法进行远程操作</p>
      <p style="font-size:12px;color:#9e9e9e">请联系管理员为地块绑定灌溉或通风控制器</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { deviceApi, controlApi } from '@/api'
import { subscribe, unsubscribe } from '@/utils/websocket'
import { ElMessage } from 'element-plus'
import { Switch, VideoPlay, VideoPause, WarningFilled } from '@element-plus/icons-vue'

const route = useRoute()
const store = useAppStore()
const selectedPlotId = ref(null)
const selectedDeviceId = ref(null)
const controllers = ref([])
const duration = ref(30)
const loading = ref(null)
const resultMsg = ref('')
const resultType = ref('success')
const isOn = ref(false)

const selectedDevice = computed(() => controllers.value.find(d => d.id === selectedDeviceId.value))

async function onPlotChange(plotId) {
  selectedDeviceId.value = null
  controllers.value = []
  const devices = await deviceApi.list(plotId)
  controllers.value = devices.filter(d => d.deviceCategory === 'CONTROLLER')
}

function onDeviceChange(deviceId) {
  resultMsg.value = ''
}

async function executeCommand(command) {
  if (!selectedDeviceId.value) return
  loading.value = command
  resultMsg.value = ''

  try {
    const result = await controlApi.irrigation({
      deviceId: selectedDeviceId.value,
      command,
      duration: duration.value
    })
    if (result.commandStatus === 'SUCCESS') {
      isOn.value = command === 'ON'
      resultType.value = 'success'
      resultMsg.value = `指令下发成功 - ${command === 'ON' ? '已开启灌溉' : '已关闭灌溉'}`
    }
  } catch (e) {
    resultType.value = 'error'
    resultMsg.value = e.message || '指令下发失败'
  } finally {
    loading.value = null
  }
}
</script>

<style scoped>
.control-page { max-width: 800px; }
h3 { display: flex; align-items: center; gap: 8px; font-weight: 500; font-size: 16px; }
.switch-indicator {
  width: 80px; height: 80px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  margin: 0 auto; font-size: 20px; font-weight: 700;
  background: #f5f5f5; border: 3px solid #e0e0e0; color: #9e9e9e;
  transition: all 0.3s;
}
.switch-indicator.active { background: #e8f5e9; border-color: #34a853; color: #34a853; }
</style>
