import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'

export interface AnalysisProgressMessage {
  taskNo: string
  status: string
  progress: number
  message: string
  reportId?: number
  errorMessage?: string
  time?: string
}

export function createAnalysisSocket(
  taskNo: string,
  onMessage: (message: AnalysisProgressMessage) => void,
  onError?: (error: any) => void
) {
  const socketUrl = `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/ws/analysis`

  const client = new Client({
    webSocketFactory: () => new SockJS(socketUrl),
    reconnectDelay: 5000,
    onConnect: () => {
      client.subscribe(`/topic/analysis/${taskNo}`, (frame) => {
        const body = JSON.parse(frame.body)
        onMessage(body)
      })
    },
    onStompError: (frame) => {
      onError?.(frame)
    },
    onWebSocketError: (event) => {
      onError?.(event)
    }
  })

  client.activate()

  return client
}