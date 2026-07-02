<template>
  <view class="login-page">
    <view class="login-card">
      <view class="login-logo">
        <image src="/static/icons/logo.png" mode="aspectFit" style="width:64px;height:64px" />
      </view>
      <text class="login-title">智慧农业</text>
      <text class="login-sub">Smart Agriculture</text>

      <input v-model="username" placeholder="用户名" class="input-field" />
      <input v-model="password" type="password" placeholder="密码" class="input-field" />

      <button :loading="loading" @tap="handleLogin" class="login-btn">登 录</button>

      <text class="login-hint">演示: admin / admin123 或 farmer1 / farmer123</text>
    </view>
  </view>
</template>

<script>
import request from '@/api/request.js'

export default {
  data() {
    return { username: '', password: '', loading: false }
  },
  methods: {
    async handleLogin() {
      if (!this.username || !this.password) {
        uni.showToast({ title: '请输入用户名和密码', icon: 'none' })
        return
      }
      this.loading = true
      try {
        const res = await request.post('/auth/login', {
          username: this.username,
          password: this.password
        })
        getApp().globalData.token = res.token
        getApp().globalData.user = { id: res.userId, username: res.username, realName: res.realName, role: res.role }
        uni.setStorageSync('token', res.token)
        uni.switchTab({ url: '/pages/index/index' })
      } catch { } finally { this.loading = false }
    }
  }
}
</script>

<style scoped>
.login-page { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #e8f0fe, #f8f9fa, #e8f5e9); }
.login-card { width: 85%; max-width: 360px; background: #fff; border-radius: 16px; padding: 40px 28px; box-shadow: 0 8px 32px rgba(0,0,0,0.08); }
.login-logo { text-align: center; margin-bottom: 12px; }
.login-title { display: block; text-align: center; font-size: 22px; font-weight: 700; color: #202124; }
.login-sub { display: block; text-align: center; font-size: 13px; color: #5f6368; margin: 4px 0 24px; }
.input-field { width: 100%; height: 44px; border: 1px solid #e0e0e0; border-radius: 8px; padding: 0 12px; margin: 10px 0; font-size: 14px; box-sizing: border-box; }
.login-btn { width: 100%; height: 44px; background: #1a73e8; color: #fff; border: none; border-radius: 8px; font-size: 16px; margin-top: 8px; }
.login-hint { display: block; text-align: center; font-size: 11px; color: #9e9e9e; margin-top: 16px; }
</style>
