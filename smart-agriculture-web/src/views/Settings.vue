<template>
  <div class="settings-page" v-if="store.isAdmin">
    <h3><el-icon><Setting /></el-icon> 系统设置</h3>
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="12">
        <div class="card">
          <div class="card-title">个人信息</div>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="用户名">{{ store.user?.username }}</el-descriptions-item>
            <el-descriptions-item label="姓名">{{ store.user?.realName }}</el-descriptions-item>
            <el-descriptions-item label="角色">
              <el-tag size="small">{{ roleText }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="card">
          <div class="card-title">系统可调参数</div>
          <el-alert type="info" :closable="false" style="margin-bottom:12px;font-size:12px">
            以下参数从 application.yml 读取。修改后需重启后端生效，或通过环境变量覆盖。
          </el-alert>
          <el-form label-width="130px" size="small">
            <el-form-item label="数据采集间隔(秒)">
              <el-input-number v-model="config.dataInterval" :min="1" :max="60" disabled />
              <span style="margin-left:8px;font-size:11px;color:#5f6368">环境变量: DATA_INTERVAL</span>
            </el-form-item>
            <el-form-item label="心跳超时(分钟)">
              <el-input-number v-model="config.heartbeatTimeout" :min="1" :max="30" disabled />
              <span style="margin-left:8px;font-size:11px;color:#5f6368">环境变量: HEARTBEAT_TIMEOUT</span>
            </el-form-item>
            <el-form-item label="指令超时(秒)">
              <el-input-number v-model="config.commandTimeout" :min="5" :max="60" disabled />
              <span style="margin-left:8px;font-size:11px;color:#5f6368">环境变量: CMD_TIMEOUT</span>
            </el-form-item>
          </el-form>
          <el-divider />
          <div style="font-size:12px;color:#5f6368">
            <p>虚拟机部署时通过环境变量覆盖：</p>
            <code style="background:#f5f5f5;padding:4px 8px;display:block;margin-top:4px">
              export DATA_INTERVAL=10<br/>
              export HEARTBEAT_TIMEOUT=5<br/>
              export CMD_TIMEOUT=15
            </code>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
  <div v-else class="empty-state">
    <el-icon :size="48"><Lock /></el-icon>
    <p style="margin-top:12px">仅管理员可访问系统设置</p>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAppStore } from '@/stores/app'
import { systemApi } from '@/api'
import { Setting, Lock } from '@element-plus/icons-vue'

const store = useAppStore()
const config = ref({ dataInterval: 5, heartbeatTimeout: 3, commandTimeout: 10 })

const roleText = computed(() => {
  const map = { ADMIN: '管理员', SUPER_ADMIN: '超级管理员', FARMER: '农户' }
  return map[store.user?.role] || store.user?.role
})

onMounted(async () => {
  if (!store.isAdmin) return
  try {
    const data = await systemApi.config()
    if (data) config.value = data
  } catch {}
})
</script>
