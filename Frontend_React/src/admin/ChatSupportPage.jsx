import { useEffect, useMemo, useRef, useState } from 'react'
import {
  adminCloseConversation,
  adminPickConversation,
  adminSendMessage,
  adminTakeoverConversation,
  getAdminAssignedConversations,
  getAdminConversation,
  getAdminOpenConversations,
} from '../services/chatService'
import {
  connectChatRealtime,
  sendMessageOverWs,
  subscribeToAdminConversationEvents,
  subscribeToConversation,
} from '../services/chatRealtimeService'
import './AdminPages.css'

function ChatSupportPage() {
  const [openItems, setOpenItems] = useState([])
  const [assignedItems, setAssignedItems] = useState([])
  const [selectedId, setSelectedId] = useState(null)
  const [details, setDetails] = useState(null)
  const [text, setText] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  const queueSubRef = useRef(null)
  const convoSubRef = useRef(null)
  const bottomRef = useRef(null)
  const pendingScrollAfterSendRef = useRef(false)
  const pendingScrollToLatestOnOpenRef = useRef(false)

  function scrollToBottom() {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' })
  }

  async function loadQueue() {
    const [openRes, assignedRes] = await Promise.all([
      getAdminOpenConversations(),
      getAdminAssignedConversations(),
    ])
    setOpenItems(Array.isArray(openRes.data) ? openRes.data : [])
    setAssignedItems(Array.isArray(assignedRes.data) ? assignedRes.data : [])
  }

  async function loadConversation(conversationId) {
    if (!conversationId) {
      setDetails(null)
      return
    }
    const res = await getAdminConversation(conversationId)
    setDetails(res.data)
  }

  useEffect(() => {
    let mounted = true

    async function boot() {
      try {
        await loadQueue()
      } catch {
        if (mounted) {
          setError('Không tải được danh sách hội thoại')
        }
      }

      connectChatRealtime(() => {
        if (queueSubRef.current) {
          queueSubRef.current.unsubscribe()
        }
        queueSubRef.current = subscribeToAdminConversationEvents(() => {
          loadQueue().catch(() => {})
        })
      })
    }

    boot()

    return () => {
      mounted = false
      if (queueSubRef.current) {
        queueSubRef.current.unsubscribe()
      }
      if (convoSubRef.current) {
        convoSubRef.current.unsubscribe()
      }
    }
  }, [])

  useEffect(() => {
    if (!selectedId) {
      return
    }

    loadConversation(selectedId).catch(() => {
      setError('Không tải được nội dung hội thoại')
    })

    connectChatRealtime(() => {
      if (convoSubRef.current) {
        convoSubRef.current.unsubscribe()
      }
      convoSubRef.current = subscribeToConversation(selectedId, (message) => {
        setDetails((prev) => {
          if (!prev?.conversation || prev.conversation.id !== selectedId) {
            return prev
          }
          const currentMessages = Array.isArray(prev.messages) ? prev.messages : []
          return {
            ...prev,
            messages: [...currentMessages, message],
          }
        })
      })
    })

    return () => {
      if (convoSubRef.current) {
        convoSubRef.current.unsubscribe()
      }
    }
  }, [selectedId])

  useEffect(() => {
    const timer = setTimeout(() => {
      scrollToBottom()
    }, 0)
    return () => clearTimeout(timer)
  }, [selectedId])

  useEffect(() => {
    if (!pendingScrollToLatestOnOpenRef.current || !details?.conversation?.id) {
      return
    }
    pendingScrollToLatestOnOpenRef.current = false
    const timer = setTimeout(() => {
      scrollToBottom()
    }, 0)
    return () => clearTimeout(timer)
  }, [details?.conversation?.id, details?.messages?.length])

  useEffect(() => {
    if (!pendingScrollAfterSendRef.current) {
      return
    }
    pendingScrollAfterSendRef.current = false
    const timer = setTimeout(() => {
      scrollToBottom()
    }, 0)
    return () => clearTimeout(timer)
  }, [details?.messages?.length])

  const selectedConversation = details?.conversation
  const canReply = selectedConversation?.status === 'ASSIGNED'
  const messages = Array.isArray(details?.messages) ? details.messages : []

  const dateTimeFormatter = useMemo(
    () => new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' }),
    [],
  )

  function formatDateTime(value) {
    if (!value) {
      return 'Chưa có hoạt động'
    }
    const parsed = new Date(value)
    if (Number.isNaN(parsed.getTime())) {
      return 'Chưa có hoạt động'
    }
    return dateTimeFormatter.format(parsed)
  }

  function getInitials(name) {
    if (!name) {
      return 'NA'
    }
    return name
      .trim()
      .split(/\s+/)
      .slice(0, 2)
      .map((part) => part.charAt(0).toUpperCase())
      .join('')
  }

  const mergedQueue = useMemo(() => {
    return [
      ...openItems.map((item) => ({ ...item, scope: 'open' })),
      ...assignedItems.map((item) => ({ ...item, scope: 'assigned' })),
    ]
  }, [openItems, assignedItems])

  const queueSections = useMemo(
    () => [
      {
        key: 'assigned',
        title: 'Đang xử lý',
        subtitle: 'Hội thoại đã có admin phụ trách',
        items: assignedItems,
      },
      {
        key: 'open',
        title: 'Chờ nhận',
        subtitle: 'Khách hàng mới chưa được admin xử lý',
        items: openItems,
      },
    ],
    [openItems, assignedItems],
  )

  async function handlePick(id) {
    setBusy(true)
    setError('')
    pendingScrollToLatestOnOpenRef.current = true
    try {
      await adminPickConversation(id)
      await loadQueue()
      setSelectedId(id)
      await loadConversation(id)
    } catch (err) {
      setError(err?.response?.data?.message || 'Pick thất bại')
    } finally {
      setBusy(false)
    }
  }

  async function handleTakeover(id) {
    setBusy(true)
    setError('')
    try {
      await adminTakeoverConversation(id)
      await loadQueue()
      setSelectedId(id)
      await loadConversation(id)
    } catch (err) {
      setError(err?.response?.data?.message || 'Takeover thất bại')
    } finally {
      setBusy(false)
    }
  }

  async function handleClose(id) {
    setBusy(true)
    setError('')
    try {
      await adminCloseConversation(id)
      await loadQueue()
      await loadConversation(id)
    } catch (err) {
      setError(err?.response?.data?.message || 'Đóng hội thoại thất bại')
    } finally {
      setBusy(false)
    }
  }

  async function sendMessage() {
    if (!selectedId || !text.trim()) {
      return
    }
    const messageContent = text.trim()
    setText('')
    pendingScrollAfterSendRef.current = true
    requestAnimationFrame(() => scrollToBottom())

    const sentByWs = sendMessageOverWs(selectedId, messageContent)
    if (sentByWs) {
      setTimeout(() => scrollToBottom(), 50)
      return
    }

    try {
      await adminSendMessage(selectedId, { content: messageContent })
      setTimeout(() => scrollToBottom(), 50)
    } catch (err) {
      pendingScrollAfterSendRef.current = false
      setError(err?.response?.data?.message || 'Gửi tin nhắn thất bại')
    }
  }

  return (
    <div className="admin-panel admin-chat-shell">
      <div className="admin-panel__header admin-chat-shell__header">
        <div>
          <h2 className="admin-panel__title">Hỗ trợ khách hàng realtime</h2>
          <p className="admin-chat-shell__sub">Theo dõi queue trực tiếp, pick nhanh và phản hồi ngay trong một màn hình.</p>
        </div>
        <div className="admin-chat-shell__actions">
          <button type="button" className="admin-btn admin-btn--ghost" onClick={() => loadQueue()} disabled={busy}>
            Refresh queue
          </button>
        </div>
      </div>

      <div className="admin-chat-overview">
        <article className="admin-chat-overview__card">
          <span>Tổng hội thoại</span>
          <strong>{mergedQueue.length}</strong>
        </article>
        <article className="admin-chat-overview__card admin-chat-overview__card--open">
          <span>Đang chờ</span>
          <strong>{openItems.length}</strong>
        </article>
        <article className="admin-chat-overview__card admin-chat-overview__card--assigned">
          <span>Đang xử lý</span>
          <strong>{assignedItems.length}</strong>
        </article>
      </div>

      {error && <div className="admin-empty admin-empty--error">{error}</div>}

      <div className="admin-chat-layout admin-chat-layout--revamp">
        <aside className="admin-chat-queue">
          {queueSections.map((section) => (
            <section key={section.key} className="admin-chat-queue__section">
              <header className="admin-chat-queue__section-header">
                <div>
                  <h3>{section.title}</h3>
                  <p>{section.subtitle}</p>
                </div>
                <span className="admin-chat-queue__count">{section.items.length}</span>
              </header>

              <div className="admin-chat-list admin-chat-list--revamp">
                {section.items.map((item) => (
                  <article
                    key={`${section.key}-${item.id}`}
                    className={`admin-chat-item admin-chat-item--revamp ${selectedId === item.id ? 'active' : ''}`}
                    onClick={() => {
                      pendingScrollToLatestOnOpenRef.current = true
                      setSelectedId(item.id)
                    }}
                    onKeyDown={(event) => {
                      if (event.key === 'Enter') {
                        pendingScrollToLatestOnOpenRef.current = true
                        setSelectedId(item.id)
                      }
                    }}
                    role="button"
                    tabIndex={0}
                  >
                    <div className="admin-chat-item__head">
                      <div className="admin-chat-item__identity">
                        <span className="admin-chat-avatar">{getInitials(item.username)}</span>
                        <div>
                          <strong>#{item.id} {item.username}</strong>
                          <div className="admin-chat-item__meta">{formatDateTime(item.lastMessageAt)}</div>
                        </div>
                      </div>
                      <span className={`admin-tag ${item.status === 'OPEN' ? 'admin-tag--pending' : 'admin-tag--confirmed'}`}>{item.status}</span>
                    </div>

                    <div className="admin-chat-item__meta">Admin: {item.adminUsername || 'Chưa có'}</div>

                    <div className="admin-chat-item__actions">
                      {item.status === 'OPEN' && (
                        <button
                          type="button"
                          className="admin-btn admin-btn--primary"
                          onClick={(event) => {
                            event.stopPropagation()
                            handlePick(item.id)
                          }}
                          disabled={busy}
                        >
                          Pick
                        </button>
                      )}
                      {item.status === 'ASSIGNED' && (
                        <button
                          type="button"
                          className="admin-btn admin-btn--ghost"
                          onClick={(event) => {
                            event.stopPropagation()
                            handleTakeover(item.id)
                          }}
                          disabled={busy}
                        >
                          Take over
                        </button>
                      )}
                    </div>
                  </article>
                ))}
                {section.items.length === 0 && <div className="admin-empty">Không có hội thoại trong mục này</div>}
              </div>
            </section>
          ))}
          {mergedQueue.length === 0 && <div className="admin-empty">Queue đang trống</div>}
        </aside>

        <section className="admin-chat-thread admin-chat-thread--revamp">
          {selectedConversation ? (
            <>
              <div className="admin-chat-thread__header">
                <div className="admin-chat-thread__identity">
                  <span className="admin-chat-avatar admin-chat-avatar--thread">{getInitials(selectedConversation.username)}</span>
                  <div>
                    <strong>Conversation #{selectedConversation.id}</strong>
                    <div className="admin-chat-item__meta">Khách: {selectedConversation.username} | Admin: {selectedConversation.adminUsername || 'Chưa có'}</div>
                    <div className="admin-chat-item__meta">Cập nhật: {formatDateTime(selectedConversation.lastMessageAt)}</div>
                  </div>
                </div>
                <div className="admin-chat-thread__actions">
                  <span className={`admin-tag ${selectedConversation.status === 'OPEN' ? 'admin-tag--pending' : 'admin-tag--confirmed'}`}>
                    {selectedConversation.status}
                  </span>
                  <button type="button" className="admin-btn admin-btn--danger" onClick={() => handleClose(selectedConversation.id)} disabled={busy}>
                    Close
                  </button>
                </div>
              </div>

              <div className="admin-chat-messages">
                {messages.map((item) => (
                  <div key={item.id} className={`admin-chat-bubble ${item.senderType === 'ADMIN' ? 'admin-chat-bubble--right' : ''}`}>
                    <div className="admin-chat-bubble__meta">{item.senderName} • {item.senderType} • {formatDateTime(item.createdAt)}</div>
                    <div>{item.content}</div>
                  </div>
                ))}
                {messages.length === 0 && <div className="admin-empty">Chưa có tin nhắn</div>}
                <div ref={bottomRef} />
              </div>

              <div className="admin-chat-compose">
                <textarea
                  className="admin-input admin-textarea"
                  placeholder={canReply ? 'Nhập phản hồi cho khách hàng' : 'Cần được assign mới có thể trả lời'}
                  value={text}
                  onChange={(event) => setText(event.target.value)}
                  disabled={!canReply || busy}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter' && !event.shiftKey) {
                      event.preventDefault()
                      sendMessage()
                    }
                  }}
                />
                <button type="button" className="admin-btn admin-btn--primary" onClick={sendMessage} disabled={!canReply || busy}>Gửi</button>
              </div>
            </>
          ) : (
            <div className="admin-empty admin-chat-empty-state">
              <strong>Chọn một hội thoại để bắt đầu</strong>
              <span>Danh sách bên trái sẽ cập nhật realtime khi khách hàng nhắn tin hoặc có admin pick/take over.</span>
            </div>
          )}
        </section>
      </div>
    </div>
  )
}

export default ChatSupportPage
