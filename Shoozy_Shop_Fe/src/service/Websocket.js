import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

let stompClient = null
let connected = false

// Danh sách callback cho các loại message khác nhau
const refreshListeners = []
const couponListeners = []
const orderListeners = []

export const connectWebSocket = () => {
  if (connected) return

  const socket = new SockJS('http://localhost:8080/ws')
  stompClient = new Client({
    webSocketFactory: () => socket,
    reconnectDelay: 5000,

    onConnect: () => {
      console.log('🔌 [WS] Connected to WebSocket server')
      connected = true

      // Kênh refresh tổng
      stompClient.subscribe('/topic/refresh', (message) => {
        const data = JSON.parse(message.body)
        console.log('🔄 [WS] Received refresh message:', data)
        refreshListeners.forEach((cb) => cb(data))
      })

      // Kênh cập nhật coupon cho admin
      stompClient.subscribe('/topic/admin/coupon', (message) => {
        const data = JSON.parse(message.body)
        console.log('🎫 [WS] Received coupon update from /topic/admin/coupon:', data)
        couponListeners.forEach((cb) => {
          console.log('🎫 [WS] Calling coupon listener:', cb)
          cb(data)
        })
      })

      // Kênh thông báo đơn hàng có sử dụng coupon
      stompClient.subscribe('/topic/admin/orders', (message) => {
        const data = JSON.parse(message.body)
        console.log('📦 [WS] Received order update from /topic/admin/orders:', data)
        orderListeners.forEach((cb) => {
          console.log('📦 [WS] Calling order listener:', cb)
          cb(data)
        })
      })

      // Kênh trạng thái coupon riêng
      stompClient.subscribe('/topic/coupon/status', (message) => {
        const data = JSON.parse(message.body)
        console.log('📊 [WS] Received coupon status from /topic/coupon/status:', data)
        couponListeners.forEach((cb) => {
          console.log('📊 [WS] Calling coupon status listener:', cb)
          cb(data)
        })
      })

      // Thêm kênh tổng quát cho tất cả messages
      stompClient.subscribe('/topic/notifications', (message) => {
        const data = JSON.parse(message.body)
        console.log('🔔 [WS] Received notification from /topic/notifications:', data)
        // Gửi đến tất cả listeners
        refreshListeners.forEach((cb) => cb(data))
        couponListeners.forEach((cb) => cb(data))
        orderListeners.forEach((cb) => cb(data))
      })
    },

    onDisconnect: () => {
      connected = false
      console.log('[WS] Disconnected')
    },

    onStompError: (err) => {
      console.error('[WS] STOMP error:', err)
    },
  })

  stompClient.activate()
}

export const disconnectWebSocket = () => {
  if (stompClient && connected) {
    stompClient.deactivate()
    connected = false
  }
}

// Đăng ký lắng nghe refresh tổng
export const addMessageListener = (callback) => {
  refreshListeners.push(callback)
}

export const removeMessageListener = (callback) => {
  const index = refreshListeners.indexOf(callback)
  if (index !== -1) refreshListeners.splice(index, 1)
}

// Đăng ký lắng nghe cập nhật coupon
export const addCouponListener = (callback) => {
  console.log('🎫 [WS] Adding coupon listener:', callback)
  couponListeners.push(callback)
  console.log('🎫 [WS] Total coupon listeners:', couponListeners.length)
}

export const removeCouponListener = (callback) => {
  console.log('🎫 [WS] Removing coupon listener:', callback)
  const index = couponListeners.indexOf(callback)
  if (index !== -1) {
    couponListeners.splice(index, 1)
    console.log('🎫 [WS] Removed coupon listener, remaining:', couponListeners.length)
  }
}

// Đăng ký lắng nghe đơn hàng có coupon
export const addOrderListener = (callback) => {
  orderListeners.push(callback)
}

export const removeOrderListener = (callback) => {
  const index = orderListeners.indexOf(callback)
  if (index !== -1) orderListeners.splice(index, 1)
}

/**
 * Subscribe events của coupon theo id hoặc code.
 * Server nên broadcast tới: /topic/coupons/{couponId} (hoặc {code})
 * Trả về handle subscription (gọi .unsubscribe() để huỷ).
 */
export const subscribeCouponEvents = (couponIdOrCode, callback) => {
  if (!connected || !stompClient) {
    console.warn('[WS] Not connected yet, cannot subscribe coupon channel.')
    return null
  }
  const destination = `/topic/coupons/${couponIdOrCode}`
  return stompClient.subscribe(destination, (message) => {
    try {
      const evt = JSON.parse(message.body)
      callback(evt)
    } catch (e) {
      console.error('[WS] parse coupon event error:', e)
    }
  })
}

// Kiểm tra trạng thái kết nối
export const isConnected = () => connected

// Gửi message (nếu cần)
export const sendMessage = (destination, message) => {
  if (connected && stompClient) {
    stompClient.publish({
      destination: destination,
      body: JSON.stringify(message)
    })
  } else {
    console.warn('[WS] Not connected, cannot send message')
  }
}