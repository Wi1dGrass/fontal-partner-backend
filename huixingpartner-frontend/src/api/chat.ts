import request from '@/utils/request'
import type { Message, SendMessageRequest, SendMessageResponse } from '@/types/chat'

/**
 * 获取聊天历史记录
 * @param chatId 聊天ID（user_xxx 或 team_xxx）
 * @param page 页码
 * @param pageSize 每页数量
 */
export const getChatHistory = (chatId: string, page: number = 1, pageSize: number = 20) => {
  return request.get<Message[]>(
    `/chat/history?chatId=${encodeURIComponent(chatId)}&page=${page}&pageSize=${pageSize}`
  )
}

/**
 * 发送消息
 * @param data 消息数据
 */
export const sendMessage = (data: SendMessageRequest) => {
  return request.post<SendMessageResponse>('/chat/send', data)
}

/**
 * WebSocket连接地址
 * @param chatId 聊天ID
 */
export const getWebSocketUrl = (chatId: string) => {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const host = import.meta.env.VITE_API_BASE_URL || window.location.host
  return `${protocol}//${host}/ws/chat/${encodeURIComponent(chatId)}`
}

/**
 * 创建WebSocket连接
 * @param chatId 聊天ID
 * @param onMessage 消息回调
 * @param onError 错误回调
 * @param onClose 关闭回调
 */
export const connectWebSocket = (
  chatId: string,
  onMessage: (message: Message) => void,
  onError?: (error: Event) => void,
  onClose?: () => void
) => {
  const ws = new WebSocket(getWebSocketUrl(chatId))

  ws.onopen = () => {
    console.log('WebSocket连接成功')
  }

  ws.onmessage = (event) => {
    try {
      const message: Message = JSON.parse(event.data)
      onMessage(message)
    } catch (error) {
      console.error('解析消息失败:', error)
    }
  }

  ws.onerror = (error) => {
    console.error('WebSocket错误:', error)
    onError?.(error)
  }

  ws.onclose = () => {
    console.log('WebSocket连接关闭')
    onClose?.()
  }

  return ws
}
