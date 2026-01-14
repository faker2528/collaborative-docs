/**
 * 消息WebSocket服务
 */
import { eventBus, Events } from './eventBus'

class MessageWebSocket {
  constructor() {
    this.ws = null
    this.reconnectTimer = null
    this.heartbeatTimer = null
    this.isConnected = false
    this.userId = null
  }
  
  /**
   * 连接WebSocket
   */
  connect(userId) {
    if (this.ws && this.isConnected) {
      return
    }
    
    this.userId = userId
    
    // 构建WebSocket URL
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.host
    const wsUrl = `${protocol}//${host}/ws/message?userId=${userId}`
    
    console.log('正在连接消息WebSocket:', wsUrl)
    
    try {
      this.ws = new WebSocket(wsUrl)
      
      this.ws.onopen = () => {
        console.log('消息WebSocket已连接')
        this.isConnected = true
        this.startHeartbeat()
      }
      
      this.ws.onmessage = (event) => {
        this.handleMessage(event.data)
      }
      
      this.ws.onclose = () => {
        console.log('消息WebSocket已断开')
        this.isConnected = false
        this.stopHeartbeat()
        this.scheduleReconnect()
      }
      
      this.ws.onerror = (error) => {
        console.error('消息WebSocket错误:', error)
        this.isConnected = false
      }
    } catch (e) {
      console.error('创建WebSocket失败:', e)
    }
  }
  
  /**
   * 处理收到的消息
   */
  handleMessage(data) {
    try {
      if (data === 'pong') {
        return // 心跳响应
      }
      
      const message = JSON.parse(data)
      console.log('收到WebSocket消息:', message)
      
      switch (message.type) {
        case 'NEW_MESSAGE':
          // 收到新消息
          eventBus.emit(Events.NEW_MESSAGE, message.data)
          eventBus.emit(Events.UNREAD_COUNT_CHANGED)
          break
        case 'UNREAD_COUNT':
          // 未读数更新
          eventBus.emit(Events.UNREAD_COUNT_CHANGED, message.data)
          break
        case 'FRIEND_REQUEST':
          // 好友请求
          eventBus.emit(Events.FRIEND_REQUEST_CHANGED, message.data)
          break
        default:
          console.log('未知消息类型:', message.type)
      }
    } catch (e) {
      console.error('解析WebSocket消息失败:', e)
    }
  }
  
  /**
   * 开始心跳
   */
  startHeartbeat() {
    this.heartbeatTimer = setInterval(() => {
      if (this.ws && this.isConnected) {
        this.ws.send('ping')
      }
    }, 30000) // 30秒发一次心跳
  }
  
  /**
   * 停止心跳
   */
  stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }
  
  /**
   * 计划重连
   */
  scheduleReconnect() {
    if (this.reconnectTimer) {
      return
    }
    
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null
      if (this.userId) {
        console.log('尝试重新连接WebSocket...')
        this.connect(this.userId)
      }
    }, 5000) // 5秒后重连
  }
  
  /**
   * 断开连接
   */
  disconnect() {
    this.stopHeartbeat()
    
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
    
    this.isConnected = false
    this.userId = null
  }
}

// 导出单例
export const messageWs = new MessageWebSocket()
