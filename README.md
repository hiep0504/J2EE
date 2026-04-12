## Tổng quan dự án (Project Overview)

Dự án là một ứng dụng web J2EE được phát triển bằng Spring Boot cho phần Backend và React cho phần Frontend.
Hệ thống được xây dựng theo kiến trúc MVC (Model – View – Controller) ở phía backend và sử dụng RESTful API để giao tiếp với frontend.

Backend chịu trách nhiệm:
xử lý logic nghiệp vụ
quản lý dữ liệu
cung cấp API

Frontend chịu trách nhiệm:
hiển thị giao diện người dùng
gửi request đến backend
xử lý tương tác của người dùng

Ứng dụng sử dụng MySQL làm cơ sở dữ liệu để lưu trữ thông tin người dùng, sản phẩm và đơn hàng.

### Công nghệ sử dụng (Technologies Used)

**Backend**
-Spring Boot 4
-Spring Web (Spring MVC)
-Spring Data JPA (Hibernate) – ORM để thao tác với database
-MySQL – hệ quản trị cơ sở dữ liệu
-Lombok – giảm code boilerplate
-Bean Validation (Hibernate Validator) – kiểm tra dữ liệu đầu vào
-Spring Boot DevTools – hỗ trợ phát triển

**Frontend**
-React
-React Router – quản lý routing
-Axios / Fetch API – gọi REST API
-CSS / Bootstrap / Tailwind – thiết kế giao diện

### Kiến trúc hệ thống
React (Frontend)
       │
       │ HTTP Request (REST API)
       ▼
Spring Boot (Backend)
       │
       ▼
MySQL Database

**Luồng hoạt động:**
Người dùng → React UI → gọi API → Spring Boot xử lý → MySQL
                                     ↓
                                trả JSON
                                     ↓
                               React hiển thị

### Requirements
- Java 21
- Maven
- MySQL
- React

## Chatbot tu van san pham (RAG + Vector DB)

He thong da bo sung chatbot tu van san pham cho khach hang tai trang `/support-chat`.

### Kien truc

React Chat UI
-> Spring Boot API (`/api/chat/rag/ask`)
-> Embedding (OpenAI hoac fallback local)
-> Vector DB MySQL (`rag_product_vectors`)
-> Retrieval top-k
-> LLM sinh cau tra loi

### Vector DB

Bang duoc tao tu dong boi JPA:

- `rag_product_vectors`
       - `product_id` (unique)
       - `document_hash`
       - `embedding_data`
       - `updated_at`

### Cac API chinh

- `POST /api/chat/rag/ask`
- `POST /api/chat/rag/index/rebuild`

Request mau cho ask:

```json
{
       "question": "giay futsal duoi 2 trieu size 42"
}
```

### Cau hinh

Trong `Backend_J2EE/src/main/resources/application.properties`:

- `app.rag.retrieval.top-k=5`
- `app.rag.embedding.provider=openai`
- `app.rag.chat.provider=openai`
- `app.rag.openai.api-key=${OPENAI_API_KEY:}`

Neu khong co `OPENAI_API_KEY`, he thong van hoat dong bang local fallback embedding + fallback answer.

