import request from './request'

// ==================== 认证 ====================
export const authApi = {
  login: (data) => request.post('/auth/login', data),
  me: () => request.get('/auth/me')
}

// ==================== 地块 ====================
export const plotApi = {
  list: (params) => request.get('/plot/list', { params }),
  all: () => request.get('/plot/all'),
  overview: (id) => request.get(`/plot/${id}/overview`),
  create: (data) => request.post('/plot', data),
  update: (id, data) => request.put(`/plot/${id}`, data),
  delete: (id) => request.delete(`/plot/${id}`)
}

// ==================== 设备 ====================
export const deviceApi = {
  list: (plotId) => request.get('/device/list', { params: { plotId } }),
  unbound: () => request.get('/device/unbound'),
  bind: (data) => request.post('/device/bind', data),
  unbind: (id) => request.post(`/device/unbind/${id}`),
  status: (id) => request.get(`/device/status/${id}`)
}

// ==================== 传感器数据 ====================
export const sensorApi = {
  realtime: (deviceId) => request.get(`/sensor/realtime/${deviceId}`),
  history: (params) => request.get('/sensor/history', { params })
}

// ==================== 设备控制 ====================
export const controlApi = {
  irrigation: (data) => request.post('/control/irrigation', data),
  log: (params) => request.get('/control/log', { params })
}

// ==================== 告警 ====================
export const alertApi = {
  rules: (plotId) => request.get('/alert/rule', { params: { plotId } }),
  saveRule: (data) => request.post('/alert/rule', data),
  logs: (params) => request.get('/alert/log', { params }),
  markRead: (id) => request.put(`/alert/log/${id}/read`)
}

// ==================== 系统配置 ====================
export const systemApi = {
  config: () => request.get('/system/config')
}

// ==================== AI ====================
export const aiApi = {
  chat: (data) => request.post('/ai/chat', data),
  knowledge: (params) => request.get('/ai/knowledge', { params })
}
