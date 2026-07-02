<template>
  <div class="monitor-page">
    <el-page-header @back="$router.push('/dashboard')">
      <template #content>
        <span class="page-title">{{ overview?.plot?.name || '地块监测' }}</span>
      </template>
    </el-page-header>

    <el-row :gutter="16" style="margin-top:20px">
      <!-- 传感器数据卡片 -->
      <el-col :span="6" v-for="s in sensorCards" :key="s.type">
        <div class="card stat-card">
          <div class="label"><el-icon :size="14"><component :is="s.icon" /></el-icon> {{ s.label }}</div>
          <div class="value">{{ s.value }}<span class="unit"> {{ s.unit }}</span></div>
          <el-tag :type="s.statusType" size="small">{{ s.statusText }}</el-tag>
        </div>
      </el-col>
    </el-row>

    <!-- 传感器设备列表 -->
    <div class="card" style="margin-top:16px">
      <div class="card-title"><el-icon><Cpu /></el-icon> 传感器设备</div>
      <el-table :data="overview?.sensors || []" v-if="overview?.sensors?.length" style="margin-top:12px">
        <el-table-column prop="deviceName" label="设备名称" />
        <el-table-column prop="deviceType" label="类型" />
        <el-table-column prop="installLocation" label="安装位置" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ONLINE' ? 'success' : 'danger'" size="small" effect="dark">
              {{ row.status === 'ONLINE' ? '在线' : '离线' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div v-else class="empty-state">
        <el-icon :size="48"><FolderOpened /></el-icon>
        <p>该地块暂无绑定传感器设备</p>
      </div>
    </div>

    <!-- 控制设备列表 - 含空状态降级 -->
    <div class="card" style="margin-top:16px">
      <div class="card-title"><el-icon><Switch /></el-icon> 控制设备</div>
      <template v-if="overview?.controllers?.length">
        <el-table :data="overview.controllers" style="margin-top:12px">
          <el-table-column prop="deviceName" label="设备名称" />
          <el-table-column prop="installLocation" label="安装位置" />
          <el-table-column prop="status" label="状态">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ONLINE' ? 'success' : 'danger'" size="small" effect="dark">
                {{ row.status === 'ONLINE' ? '在线' : '离线' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作">
            <template #default="{ row }">
              <el-button type="primary" size="small" :disabled="row.status !== 'ONLINE'"
                         @click="$router.push(`/control?deviceId=${row.id}`)">
                控制
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
      <div v-else class="empty-state">
        <el-icon :size="48"><WarningFilled /></el-icon>
        <p style="margin-top:12px;font-size:14px;color:#5f6368">当前农田未配置控制设备，无法进行远程操作</p>
        <p style="font-size:12px;color:#9e9e9e">请联系管理员为地块绑定灌溉或通风控制器</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { plotApi } from '@/api'
import { FolderOpened, WarningFilled, Cpu, Switch } from '@element-plus/icons-vue'

const route = useRoute()
const overview = ref(null)

const sensorCards = computed(() => {
  if (!overview.value?.latestSensorData) return []
  const cards = []
  const data = overview.value.latestSensorData
  const config = {
    TEMPERATURE: { label: '温度', unit: '°C', icon: 'Sunny', statusType: 'success', statusText: '正常' },
    HUMIDITY: { label: '湿度', unit: '%', icon: 'Umbrella', statusType: '', statusText: '正常' },
    SOIL_MOISTURE: { label: '土壤湿度', unit: '%', icon: 'Grid', statusType: 'warning', statusText: '接近下限' },
    LIGHT_INTENSITY: { label: '光照', unit: 'lux', icon: 'Sunrise', statusType: 'success', statusText: '充足' }
  }
  for (const [deviceId, sensorData] of Object.entries(data)) {
    const c = config[sensorData.dataType] || { label: sensorData.dataType, unit: sensorData.unit, icon: 'DataLine' }
    cards.push({
      type: sensorData.dataType,
      label: c.label,
      value: sensorData.dataValue,
      unit: c.unit,
      icon: c.icon,
      statusType: c.statusType || '',
      statusText: c.statusText || ''
    })
  }
  return cards.slice(0, 4)
})

onMounted(async () => {
  const plotId = route.params.plotId
  if (plotId) {
    overview.value = await plotApi.overview(plotId)
  }
})
</script>

<style scoped>
.monitor-page { max-width: 1000px; }
.page-title { font-size: 18px; font-weight: 600; }
</style>
