<template>
  <div class="layout">
    <!-- WebSocket 状态指示条 -->
    <div v-if="store.wsStatus !== 'connected'" class="ws-status-bar" :class="store.wsStatus">
      <span class="ws-dot"></span>
      <span v-if="store.wsStatus === 'reconnecting'">实时连接重连中，数据更新可能延迟...</span>
      <span v-else-if="store.wsStatus === 'error'">实时连接异常，请刷新页面</span>
      <span v-else>实时连接已断开</span>
    </div>

    <header class="top-bar">
      <div class="top-left">
        <el-button v-if="$route.path !== '/dashboard'" text size="small"
                   @click="$router.push('/dashboard')" style="margin-right:8px">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <el-icon :size="22" color="#1a73e8" style="cursor:pointer" @click="$router.push('/dashboard')"><Monitor /></el-icon>
        <span class="logo" style="cursor:pointer" @click="$router.push('/dashboard')">SmartFarm</span>
        <!-- WS 连接正常时的小指示灯 -->
        <span v-if="store.wsStatus === 'connected'" class="ws-indicator-online" title="实时连接正常">
          <el-icon :size="12"><CircleCheckFilled /></el-icon>
        </span>
      </div>
      <div class="top-center">
        <el-input v-model="searchText" placeholder="搜索地块、设备、告警..." :prefix-icon="Search"
                  class="search-input" clearable />
      </div>
      <div class="top-right">
        <el-badge :value="unreadAlerts" :hidden="!unreadAlerts" class="alert-badge">
          <el-icon :size="20" @click="$router.push('/alerts')" style="cursor:pointer"><Bell /></el-icon>
        </el-badge>
        <el-dropdown @command="handleCommand">
          <span class="user-info">
            <el-icon :size="18"><UserFilled /></el-icon>
            {{ store.user?.realName || '用户' }}
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人信息</el-dropdown-item>
              <el-dropdown-item v-if="store.isAdmin" command="settings">系统设置</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>
    <div class="layout-body">
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { Search, Bell, CircleCheckFilled, ArrowLeft } from '@element-plus/icons-vue'
import { subscribe, unsubscribeAll } from '@/utils/websocket'

const router = useRouter()
const store = useAppStore()
const searchText = ref('')
const unreadAlerts = ref(0)

function handleCommand(cmd) {
  if (cmd === 'logout') {
    store.logout()
    router.push('/login')
  } else if (cmd === 'settings') {
    router.push('/settings')
  } else if (cmd === 'profile') {
    router.push('/settings')
  }
}

onMounted(async () => {
  store.initWsListener()
  await store.fetchUser()
  await store.fetchPlots()
  // 订阅告警通知
  if (store.currentPlotId) {
    subscribe(`/topic/plot/${store.currentPlotId}/alerts`, (data) => {
      unreadAlerts.value++
    })
  }
})
</script>

<style scoped>
.layout { min-height: 100vh; background: var(--bg-page); }
.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 56px;
  background: #fff;
  border-bottom: 1px solid var(--border);
  position: sticky;
  top: 0;
  z-index: 100;
}
.top-left { display: flex; align-items: center; gap: 8px; }
.logo { font-weight: 700; font-size: 18px; color: var(--primary); }
.top-center { flex: 1; max-width: 500px; margin: 0 24px; }
.search-input { --el-input-bg-color: #f1f3f4; --el-border-radius: 20px; }
.top-right { display: flex; align-items: center; gap: 20px; }
.user-info {
  display: flex; align-items: center; gap: 6px;
  background: var(--primary-light); color: var(--primary);
  padding: 6px 14px; border-radius: 20px; cursor: pointer; font-size: 13px;
}
.alert-badge { cursor: pointer; }
.ws-status-bar {
  display: flex; align-items: center; justify-content: center; gap: 8px;
  padding: 6px; font-size: 12px; color: #fff; transition: all 0.3s;
}
.ws-status-bar.reconnecting { background: #ea8600; }
.ws-status-bar.error { background: #e81123; }
.ws-status-bar.disconnected { background: #5f6368; }
.ws-dot { width: 6px; height: 6px; border-radius: 50%; background: #fff; animation: pulse 1.5s infinite; }
@keyframes pulse { 0%,100%{opacity:1} 50%{opacity:0.3} }
.ws-indicator-online { color: #34a853; margin-left: 4px; display: inline-flex; align-items: center; }
.layout-body { padding: 24px; max-width: 1400px; margin: 0 auto; }
</style>
