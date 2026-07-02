<template>
  <view class="container">
    <!-- 用户信息 -->
    <view class="profile-card">
      <view class="avatar">{{ (user?.realName || '用户')[0] }}</view>
      <view>
        <text class="profile-name">{{ user?.realName || '未登录' }}</text>
        <text class="profile-role">{{ roleText }}</text>
      </view>
    </view>

    <!-- 功能菜单 -->
    <view class="card">
      <view class="menu-item" @tap="goAlerts">
        <text>告警记录</text>
        <text class="menu-badge" v-if="unreadCount > 0">{{ unreadCount }}</text>
      </view>
      <view class="menu-item" @tap="goDevices">
        <text>设备管理</text>
        <text class="menu-arrow">></text>
      </view>
      <view class="menu-item" @tap="goThreshold">
        <text>阈值设置</text>
        <text class="menu-arrow">></text>
      </view>
    </view>

    <view class="card">
      <view class="menu-item" @tap="goKnowledge">
        <text>知识库</text>
        <text class="menu-arrow">></text>
      </view>
      <view class="menu-item" @tap="goSettings">
        <text>设置</text>
        <text class="menu-arrow">></text>
      </view>
    </view>

    <button class="logout-btn" @tap="handleLogout">退出登录</button>
  </view>
</template>

<script>
export default {
  data() {
    return { unreadCount: 3 }
  },
  computed: {
    user() { return getApp().globalData.user },
    roleText() {
      const role = this.user?.role
      return role === 'ADMIN' || role === 'SUPER_ADMIN' ? '管理员' : role === 'FARMER' ? '农户' : ''
    }
  },
  methods: {
    goAlerts() { uni.showToast({ title: '告警记录（演示）', icon: 'none' }) },
    goDevices() { uni.showToast({ title: '设备管理（演示）', icon: 'none' }) },
    goThreshold() { uni.showToast({ title: '阈值设置（演示）', icon: 'none' }) },
    goKnowledge() { uni.showToast({ title: '知识库（演示）', icon: 'none' }) },
    goSettings() { uni.showToast({ title: '设置（演示）', icon: 'none' }) },
    handleLogout() {
      getApp().globalData.token = ''
      getApp().globalData.user = null
      uni.removeStorageSync('token')
      uni.reLaunch({ url: '/pages/login/login' })
    }
  }
}
</script>

<style scoped>
.profile-card { display: flex; align-items: center; gap: 14px; background: #fff; border-radius: 12px; padding: 20px; margin-bottom: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); }
.avatar { width: 48px; height: 48px; border-radius: 50%; background: #1a73e8; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 20px; font-weight: 600; }
.profile-name { font-size: 16px; font-weight: 600; display: block; }
.profile-role { font-size: 12px; color: #5f6368; }
.menu-item { display: flex; justify-content: space-between; align-items: center; padding: 14px 0; border-bottom: 1px solid #f5f5f5; font-size: 14px; }
.menu-badge { background: #e81123; color: #fff; border-radius: 10px; padding: 2px 8px; font-size: 11px; }
.menu-arrow { color: #ccc; }
.logout-btn { margin-top: 24px; background: #f5f5f5; color: #e81123; border: none; font-size: 14px; }
</style>
