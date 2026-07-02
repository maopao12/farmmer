<template>
  <div class="alerts-page">
    <h3><el-icon><Bell /></el-icon> 告警中心</h3>
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="16">
        <div class="card">
          <el-tabs v-model="activeTab">
            <el-tab-pane label="告警列表" name="list">
              <el-table :data="alerts" v-loading="loading">
                <el-table-column prop="alertLevel" label="级别" width="80">
                  <template #default="{ row }">
                    <el-tag :type="levelType(row.alertLevel)" size="small" effect="dark">
                      {{ row.alertLevel }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="alertMsg" label="告警内容" show-overflow-tooltip />
                <el-table-column prop="triggerTime" label="触发时间" width="160" />
                <el-table-column label="状态" width="80">
                  <template #default="{ row }">
                    <el-tag :type="row.isRead ? 'info' : 'danger'" size="small">
                      {{ row.isRead ? '已读' : '未读' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="80">
                  <template #default="{ row }">
                    <el-button v-if="!row.isRead" size="small" @click="markRead(row.id)">标记已读</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <el-pagination v-model:current-page="page" :total="total" :page-size="10"
                             layout="prev, pager, next" @current-change="fetchAlerts" style="margin-top:16px" />
            </el-tab-pane>
            <el-tab-pane label="告警规则" name="rules" v-if="store.isAdmin">
              <el-form :model="ruleForm" label-width="100px" size="small">
                <el-form-item label="地块">
                  <el-select v-model="ruleForm.plotId">
                    <el-option v-for="p in store.plots" :key="p.id" :label="p.name" :value="p.id" />
                  </el-select>
                </el-form-item>
                <el-form-item label="指标类型">
                  <el-select v-model="ruleForm.metricType">
                    <el-option label="温度" value="TEMPERATURE" />
                    <el-option label="湿度" value="HUMIDITY" />
                    <el-option label="土壤湿度" value="SOIL_MOISTURE" />
                  </el-select>
                </el-form-item>
                <el-form-item label="下限阈值">
                  <el-input-number v-model="ruleForm.minThreshold" :min="0" :max="100" />
                </el-form-item>
                <el-form-item label="告警级别">
                  <el-select v-model="ruleForm.alertLevel">
                    <el-option label="严重" value="CRITICAL" />
                    <el-option label="警告" value="WARNING" />
                    <el-option label="提示" value="INFO" />
                  </el-select>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="saveRule">保存规则</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>
          </el-tabs>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAppStore } from '@/stores/app'
import { alertApi } from '@/api'
import { Bell } from '@element-plus/icons-vue'

const store = useAppStore()
const alerts = ref([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)
const activeTab = ref('list')
const ruleForm = ref({ plotId: null, metricType: 'HUMIDITY', minThreshold: 30, alertLevel: 'CRITICAL' })

function levelType(level) {
  return level === 'CRITICAL' ? 'danger' : level === 'WARNING' ? 'warning' : 'info'
}

async function fetchAlerts() {
  loading.value = true
  try {
    const res = await alertApi.logs({ page: page.value, size: 10 })
    alerts.value = res?.records || []
    total.value = res?.total || 0
  } finally { loading.value = false }
}

async function markRead(id) {
  await alertApi.markRead(id)
  fetchAlerts()
}

async function saveRule() {
  await alertApi.saveRule(ruleForm.value)
  ElMessage.success('规则已保存')
}

onMounted(fetchAlerts)
</script>
