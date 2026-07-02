<template>
  <div class="logs-page">
    <h3><el-icon><Document /></el-icon> 操作日志</h3>
    <div class="card" style="margin-top:16px">
      <el-table :data="logs" v-loading="loading">
        <el-table-column prop="deviceId" label="设备ID" width="80" />
        <el-table-column prop="command" label="指令" width="80">
          <template #default="{ row }">
            <el-tag :type="row.command === 'ON' ? 'success' : 'danger'" size="small">{{ row.command }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="commandStatus" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.commandStatus)" size="small" effect="dark">
              {{ row.commandStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="resultMsg" label="结果" show-overflow-tooltip />
        <el-table-column prop="sendTime" label="发送时间" width="160" />
        <el-table-column prop="responseTime" label="响应时间" width="160" />
      </el-table>
      <el-pagination v-model:current-page="page" :total="total" :page-size="10"
                     layout="prev, pager, next" @current-change="fetchLogs" style="margin-top:16px" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { controlApi } from '@/api'
import { Document } from '@element-plus/icons-vue'

const logs = ref([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)

function statusType(s) {
  return s === 'SUCCESS' ? 'success' : s === 'TIMEOUT' ? 'warning' : s === 'FAILED' ? 'danger' : 'info'
}

async function fetchLogs() {
  loading.value = true
  try {
    const res = await controlApi.log({ page: page.value, size: 10 })
    logs.value = res?.records || []
    total.value = res?.total || 0
  } finally { loading.value = false }
}

onMounted(fetchLogs)
</script>
