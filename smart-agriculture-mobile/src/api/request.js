// 虚拟机部署时修改此地址为后端实际IP:端口
// 例如: 'http://192.168.56.101:8080/api/v1'
const BASE_URL = (typeof __API_BASE_URL__ !== 'undefined' ? __API_BASE_URL__ : 'http://localhost:8080') + '/api/v1'

function request(url, options = {}) {
  const app = getApp()
  const token = app?.globalData?.token || uni.getStorageSync('token')

  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + url,
      method: options.method || 'GET',
      data: options.data,
      header: {
        'Authorization': token ? `Bearer ${token}` : '',
        'Content-Type': 'application/json'
      },
      success: (res) => {
        const data = res.data
        if (data.code === 200) {
          resolve(data.data)
        } else if (res.statusCode === 401) {
          uni.removeStorageSync('token')
          uni.reLaunch({ url: '/pages/login/login' })
          reject(new Error('登录已过期'))
        } else {
          uni.showToast({ title: data.message || '请求失败', icon: 'none' })
          reject(new Error(data.message))
        }
      },
      fail: () => {
        uni.showToast({ title: '网络连接失败', icon: 'none' })
        reject(new Error('网络连接失败'))
      }
    })
  })
}

export default {
  get: (url, params) => request(url, { method: 'GET', data: params }),
  post: (url, data) => request(url, { method: 'POST', data }),
  put: (url, data) => request(url, { method: 'PUT', data }),
  delete: (url) => request(url, { method: 'DELETE' })
}
