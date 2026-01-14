/**
 * 简单的事件总线，用于组件间通信
 */
import { ref } from 'vue'

const listeners = ref(new Map())

export const eventBus = {
  /**
   * 监听事件
   */
  on(event, callback) {
    if (!listeners.value.has(event)) {
      listeners.value.set(event, [])
    }
    listeners.value.get(event).push(callback)
  },
  
  /**
   * 取消监听
   */
  off(event, callback) {
    if (!listeners.value.has(event)) return
    const cbs = listeners.value.get(event)
    const index = cbs.indexOf(callback)
    if (index > -1) {
      cbs.splice(index, 1)
    }
  },
  
  /**
   * 触发事件
   */
  emit(event, data) {
    if (!listeners.value.has(event)) return
    listeners.value.get(event).forEach(cb => cb(data))
  }
}

// 预定义事件名
export const Events = {
  UNREAD_COUNT_CHANGED: 'unread_count_changed',
  NEW_MESSAGE: 'new_message',
  FRIEND_REQUEST_CHANGED: 'friend_request_changed'
}
