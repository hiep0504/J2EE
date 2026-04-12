import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { askRag } from '../services/ragChatService'
import { toMediaUrl } from '../utils/mediaUrl'
import './AiProductChatPage.css'

function toVnd(value) {
  return Number(value || 0).toLocaleString('vi-VN') + 'đ'
}

function makeBotMessage(text, products = []) {
  return {
    id: `bot-${Date.now()}-${Math.random()}`,
    role: 'bot',
    text,
    products,
  }
}

function buildHistoryWindow(messages, maxTurns = 8) {
  return messages
    .filter((item) => item && item.text && (item.role === 'user' || item.role === 'bot'))
    .map((item) => ({
      role: item.role === 'bot' ? 'assistant' : 'user',
      content: item.text,
    }))
    .slice(-Math.max(0, maxTurns))
}

function AiProductChatPage() {
  const [messages, setMessages] = useState([
    makeBotMessage('Xin chao! Minh co the tu van giay da bong theo gia, loai san, size va thuong hieu.'),
  ])
  const [text, setText] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const bottomRef = useRef(null)

  function scrollToBottom() {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' })
  }

  useEffect(() => {
    setTimeout(() => scrollToBottom(), 0)
  }, [messages.length])

  async function handleSend() {
    if (!text.trim() || busy) {
      return
    }

    const question = text.trim()
    const history = buildHistoryWindow(messages, 8)
    const userMessage = {
      id: `user-${Date.now()}`,
      role: 'user',
      text: question,
      products: [],
    }

    setMessages((prev) => [...prev, userMessage])
    setText('')
    setError('')
    setBusy(true)

    try {
      const res = await askRag(question, history)
      const answer = res?.data?.answer || 'Minh chua the tra loi luc nay. Ban thu lai nhe.'
      const products = Array.isArray(res?.data?.products) ? res.data.products : []
      setMessages((prev) => [...prev, makeBotMessage(answer, products)])
    } catch (err) {
      const apiMessage = err?.response?.data?.message
      const displayMessage = apiMessage || 'Khong the ket noi chatbot. Vui long thu lai.'
      setError(displayMessage)
      setMessages((prev) => [...prev, makeBotMessage(displayMessage)])
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="ai-chat-page">
      <section className="ai-chat-card">
        <header className="ai-chat-header">
          <h2>AI tư vấn sản phẩm</h2>
          <p>Hỏi về mức giá, loại sân, size hoặc so sánh sản phẩm.</p>
        </header>

        <div className="ai-chat-messages">
          {messages.map((item) => (
            <div key={item.id} className="ai-chat-block">
              <article
                className={`ai-chat-bubble ${item.role === 'user' ? 'ai-chat-bubble--self' : ''}`}
              >
                <div className="ai-chat-bubble__meta">{item.role === 'user' ? 'Bạn' : 'AI'}</div>
                <div>{item.text}</div>
              </article>

              {item.role === 'bot' && item.products.length > 0 && (
                <div className="ai-chat-products">
                  {item.products.map((product) => (
                    <Link
                      key={`${item.id}-${product.id}`}
                      to={`/products/${product.id}`}
                      className="ai-chat-product-card"
                    >
                      <img
                        src={toMediaUrl(product.image || 'https://placehold.co/320x220?text=No+Image')}
                        alt={product.name}
                      />
                      <div className="ai-chat-product-card__content">
                        <h4>{product.name}</h4>
                        <p className="ai-chat-product-card__price">{toVnd(product.price)}</p>
                        <p className="ai-chat-product-card__meta">
                          {product.category || 'Sản phẩm'}
                          {Array.isArray(product.sizes) && product.sizes.length > 0
                            ? ` • Size: ${product.sizes.join(', ')}`
                            : ''}
                        </p>
                      </div>
                    </Link>
                  ))}
                </div>
              )}
            </div>
          ))}

          {busy && <p className="ai-chat-empty">AI đang tìm sản phẩm phù hợp...</p>}
          <div ref={bottomRef} />
        </div>

        {error && <p className="ai-chat-status">{error}</p>}

        <footer className="ai-chat-compose">
          <input
            value={text}
            onChange={(event) => setText(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === 'Enter') {
                handleSend()
              }
            }}
            placeholder="VD: giày futsal dưới 2 triệu size 42"
            disabled={busy}
          />
          <button type="button" onClick={handleSend} disabled={busy || !text.trim()}>
            {busy ? 'Đang trả lời...' : 'Gửi'}
          </button>
        </footer>
      </section>
    </div>
  )
}

export default AiProductChatPage
