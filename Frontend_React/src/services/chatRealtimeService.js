import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client/dist/sockjs'

let client = null
const connectListeners = new Set()

function ensureClient() {
  if (client) {
    return client
  }

  client = new Client({
    webSocketFactory: () => new SockJS('https://busticket.ink/ws-chat'),
    reconnectDelay: 3000,
    heartbeatIncoming: 15000,
    heartbeatOutgoing: 15000,
    debug: () => {},
  })

  client.onConnect = () => {
    connectListeners.forEach((listener) => {
      listener?.(client)
    })
  }

  return client
}

export function connectChatRealtime(onConnect) {
  const stompClient = ensureClient()
  if (onConnect) {
    connectListeners.add(onConnect)
  }

  if (stompClient.connected) {
    onConnect?.(stompClient)
    return () => {
      if (onConnect) {
        connectListeners.delete(onConnect)
      }
    }
  }

  stompClient.activate()
  return () => {
    if (onConnect) {
      connectListeners.delete(onConnect)
    }
  }
}

export function disconnectChatRealtime() {
  if (!client) {
    return
  }
  connectListeners.clear()
  client.deactivate()
  client = null
}

export function subscribeToConversation(conversationId, onMessage) {
  const stompClient = ensureClient()
  if (!stompClient.connected) {
    return null
  }

  return stompClient.subscribe(`/topic/conversation/${conversationId}`, (message) => {
    if (!message?.body) {
      return
    }
    onMessage(JSON.parse(message.body))
  })
}

export function subscribeToAdminConversationEvents(onEvent) {
  const stompClient = ensureClient()
  if (!stompClient.connected) {
    return null
  }

  return stompClient.subscribe('/topic/admin/conversations', (message) => {
    if (!message?.body) {
      return
    }
    onEvent(JSON.parse(message.body))
  })
}

export function sendMessageOverWs(conversationId, content) {
  const stompClient = ensureClient()
  if (!stompClient.connected) {
    return false
  }

  stompClient.publish({
    destination: '/app/chat.send',
    body: JSON.stringify({ conversationId, content }),
  })
  return true
}

export function isRealtimeConnected() {
  return !!client?.connected
}
