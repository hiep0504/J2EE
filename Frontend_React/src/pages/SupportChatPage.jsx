import { useEffect, useMemo, useRef, useState } from 'react'
import {
  getConversationForUser,
  getMyActiveConversation,
  sendUserMessage,
  startConversation,
} from '../services/chatService'
import {
  connectChatRealtime,
  sendMessageOverWs,
  subscribeToConversation,
} from '../services/chatRealtimeService'
import './SupportChatPage.css'

function SupportChatPage() {
  const [conversationId, setConversationId] = useState(null)
  const [details, setDetails] = useState(null)
  const [text, setText] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const convoSubRef = useRef(null)
  const bottomRef = useRef(null)

  const messages = useMemo(() => (Array.isArray(details?.messages) ? details.messages : []), [details])

  function scrollToBottom() {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' })
  }

  async function loadConversation(id) {
    if (!id) {
      setDetails(null)
      return
    }
    const res = await getConversationForUser(id)
    setDetails(res.data)
  }

  useEffect(() => {
    let mounted = true

    async function boot() {
      setBusy(true)
      setError('')
      try {
        let id = null
        try {
          const activeRes = await getMyActiveConversation()
          id = activeRes?.data?.conversation?.id || activeRes?.data?.id || null
        } catch {
          id = null
        }

        if (!id) {
          const startRes = await startConversation({})
          id = startRes?.data?.conversation?.id || startRes?.data?.id || null
        }

        if (!mounted || !id) {
          return
        }

        setConversationId(id)
        await loadConversation(id)
      } catch {
        if (mounted) {
          setError('Không thể khởi tạo cuộc trò chuyện hỗ trợ. Vui lòng thử lại.')
        }
      } finally {
        if (mounted) {
          setBusy(false)
        }
      }
    }

    boot()

    return () => {
      mounted = false
      if (convoSubRef.current) {
        convoSubRef.current.unsubscribe()
      }
    }
  }, [])

  useEffect(() => {
    if (!conversationId) {
      return
    }

    const disconnect = connectChatRealtime(() => {
      if (convoSubRef.current) {
        convoSubRef.current.unsubscribe()
      }
      convoSubRef.current = subscribeToConversation(conversationId, (message) => {
        setDetails((prev) => {
          if (!prev?.conversation || prev.conversation.id !== conversationId) {
            return prev
          }
          const currentMessages = Array.isArray(prev.messages) ? prev.messages : []
          return {
            ...prev,
            messages: [...currentMessages, message],
          }
        })
        setTimeout(() => scrollToBottom(), 0)
      })
    })

    return () => {
      if (convoSubRef.current) {
        convoSubRef.current.unsubscribe()
      }
      disconnect?.()
    }
  }, [conversationId])

  useEffect(() => {
    const timer = setTimeout(() => {
      scrollToBottom()
    }, 0)
    return () => clearTimeout(timer)
  }, [messages.length, conversationId])

  async function handleSend() {
    if (!conversationId || !text.trim()) {
      return
    }

    const content = text.trim()
    setText('')
    setError('')

    const sentByWs = sendMessageOverWs(conversationId, content)
    if (sentByWs) {
      setTimeout(() => scrollToBottom(), 50)
      return
    }

    try {
      await sendUserMessage(conversationId, { content })
      setTimeout(() => scrollToBottom(), 50)
    } catch {
      setError('Không gửi được tin nhắn. Vui lòng thử lại.')
    }
  }

  return (
    <div className="support-chat-page">
      <section className="support-chat-card">
        <header className="support-chat-header">
          <h2>Hỗ trợ trực tuyến</h2>
          <p>Đội ngũ ShopApp sẽ phản hồi ngay khi admin nhận cuộc trò chuyện của bạn.</p>
        </header>

        <div className="support-chat-messages">
          {messages.map((item) => (
            <article
              key={item.id}
              className={`support-chat-bubble ${item.senderType === 'USER' ? 'support-chat-bubble--self' : ''}`}
            >
              <div className="support-chat-bubble__meta">
                {item.senderName}
              </div>
              <div>{item.content}</div>
            </article>
          ))}
          {messages.length === 0 && !busy && (
            <p className="support-chat-empty">Bạn chưa có tin nhắn nào. Hãy gửi câu hỏi để bắt đầu.</p>
          )}
          <div ref={bottomRef} />
        </div>

        {error && <p className="support-chat-status">{error}</p>}

        <footer className="support-chat-compose">
          <input
            value={text}
            onChange={(event) => setText(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === 'Enter') {
                handleSend()
              }
            }}
            placeholder="Nhập câu hỏi của bạn..."
            disabled={!conversationId || busy}
          />
          <button type="button" onClick={handleSend} disabled={!conversationId || busy}>
            Gửi
          </button>
        </footer>
      </section>
    </div>
  )
}

export default SupportChatPage
