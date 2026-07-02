<template>
  <div class="devices-page">
    <h3><el-icon><Cpu /></el-icon> 设备管理</h3>
    <el-tabs v-model="activeTab" style="margin-top:16px">
      <el-tab-pane label="已绑定设备" name="bound">
        <el-select v-model="selectedPlotId" placeholder="选择地块" @change="fetchDevices" style="width:240px;margin-bottom:16px">
          <el-option v-for="p in store.plots" :key="p.id" :label="p.name" :value="p.id" />
        </el-select>
        <el-table :data="devices" v-loading="loading">
          <el-table-column prop="deviceCode" label="设备编码" />
          <el-table-column prop="deviceName" label="名称" />
          <el-table-column prop="deviceType" label="类型" />
          <el-table-column prop="deviceCategory" label="类别" width="100">
            <template #default="{ row }">
              <el-tag :type="row.deviceCategory === 'CONTROLLER' ? 'warning' : 'info'" size="small">
                {{ row.deviceCategory === 'CONTROLLER' ? '控制器' : '传感器' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ONLINE' ? 'success' : 'danger'" size="small" effect="dark">
                {{ row.status === 'ONLINE' ? '在线' : '离线' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" v-if="store.isAdmin">
            <template #default="{ row }">
              <el-button type="danger" size="small" @click="handleUnbind(row.id)">解绑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="仓库设备" name="unbound" v-if="store.isAdmin">
        <el-table :data="unboundDevices">
          <el-table-column prop="deviceCode" label="设备编码" />
          <el-table-column prop="deviceName" label="名称" />
          <el-table-column prop="deviceType" label="类型" />
          <el-table-column label="操作" width="200">
            <template #default="{ row }">
              <el-select v-model="bindPlotId" placeholder="选择地块" size="small" style="width:120px">
                <el-option v-for="p in store.plots" :key="p.id" :label="p.name" :value="p.id" />
              </el-select>
              <el-button type="primary" size="small" @click="handleBind(row.id)" style="margin-left:8px">绑定</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAppStore } from '@/stores/app'
import { deviceApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Cpu } from '@element-plus/icons-vue'

const store = useAppStore()
const activeTab = ref('bound')
const selectedPlotId = ref(null)
const devices = ref([])
const unboundDevices = ref([])
const bindPlotId = ref(null)
const loading = ref(false)

async function fetchDevices() {
  if (!selectedPlotId.value) return
  loading.value = true
  try { devices.value = await deviceApi.list(selectedPlotId.value) }
  finally { loading.value = false }
}

async function fetchUnbound() {
  unboundDevices.value = await deviceApi.unbound()
}

async function handleBind(deviceId) {
  if (!bindPlotId.value) { ElMessage.warning('请选择目标地块'); return }
  await deviceApi.bind({ deviceId, plotId: bindPlotId.value })
  ElMessage.success('绑定成功')
  fetchUnbound()
}

async function handleUnbind(deviceId) {
  await ElMessageBox.confirm('确认解绑此设备？', '提示', { type: 'warning' })
  await deviceApi.unbind(deviceId)
  ElMessage.success('解绑成功')
  fetchDevices()
}

onMounted(() => { if (store.isAdmin) fetchUnbound() })
</script>
