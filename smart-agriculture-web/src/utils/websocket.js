import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

let stompClient = null
const subscribers = new Map()
let statusCallback = null

/**
 * 注册 WebSocket 状态变化回调
 * @param {function} cb - cb(status)  status: 'connected' | 'disconnected' | 'reconnecting' | 'error'
 */
export function onStatusChange(cb) {
  statusCallback = cb
}

function notifyStatus(status) {
  if (typeof statusCallback === 'function') {
    statusCallback(status)
  }
}

export function connectWebSocket() {
  const token = localStorage.getItem('token')
  if (!token) return

  if (stompClient) {
    stompClient.deactivate()
  }

  stompClient = new Client({
    // 开发模式通过Vite代理，生产模式使用绝对路径
    webSocketFactory: () => {
      const baseUrl = window.__API_BASE_URL__ || ''
      return new SockJS(baseUrl + '/ws')
    },
    connectHeaders: { Authorization: `Bearer ${token}` },
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onConnect: () => {
      console.log('[WS] 已连接')
      notifyStatus('connected')
    },
    onDisconnect: () => {
      console.log('[WS] 已断开')
      notifyStatus('disconnected')
    },
    onStompError: (frame) => {
      console.error('[WS] STOMP错误:', frame.headers['message'])
      notifyStatus('error')
    },
    onWebSocketClose: () => {
      notifyStatus('reconnecting')
    }
  })
  stompClient.activate()
}

export function disconnectWebSocket() {
  if (stompClient) {
    stompClient.deactivate()
    stompClient = null
  }
  notifyStatus('disconnected')
}

export function subscribe(topic, callback) {
  if (!stompClient || !stompClient.connected) return null
  const sub = stompClient.subscribe(topic, (message) => {
    try {
      const data = JSON.parse(message.body)
      callback(data)
    } catch {
      callback(message.body)
    }
  })
  subscribers.set(topic, sub)
  return sub
}

export function unsubscribe(topic) {
  const sub = subscribers.get(topic)
  if (sub) {
    sub.unsubscribe()
    subscribers.delete(topic)
  }
}

export function unsubscribeAll() {
  subscribers.forEach((sub) => sub.unsubscribe())
  subscribers.clear()
}
