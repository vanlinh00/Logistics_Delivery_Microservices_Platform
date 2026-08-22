# Hướng Dẫn Kết Nối API Backend Spring Boot Cho Frontend React / Vite

Tài liệu này tổng hợp toàn bộ danh sách API, DTOs, cấu trúc response và mẫu code TypeScript client để kết nối Frontend với Backend Spring Boot.

---

## 1. Biến Môi Trường (.env)
```env
VITE_API_BASE_URL=http://localhost:8080
VITE_WS_BASE_URL=http://localhost:8080/ws-chat
```

---

## 2. Dạng Phản Hồi Chuẩn (GenericResponse<T>)
Mọi endpoint REST của Backend trả về JSON dạng:
```typescript
export interface GenericResponse<T> {
  success: boolean;
  code: string;       // "200" hoặc mã lỗi
  message: string;    // Thông báo
  data: T;            // Dữ liệu chính
  details: string[];  // Chi tiết lỗi
}
```

---

## 3. Các Endpoint & DTO

### A. Auth (`/api/auth`)
- `POST /api/auth/login`: `{ usernameOrEmail, password }` -> Trả về `AuthResponse` `{ token, refreshToken, id, username, email, fullName, avatarUrl, coupleId }`
- `POST /api/auth/register`: `{ username, email, password, fullName, gender }`
- `POST /api/auth/refresh`: `{ refreshToken }` -> Trả về `AuthResponse`
- `GET /api/auth/me`: Lấy thông tin user hiện tại (Header `Authorization: Bearer <token>`)

### B. Couple (`/api/couple`)
- `POST /api/couple/create-code`: Tạo mã ghép đôi `pairingCode`
- `POST /api/couple/pair`: `{ pairingCode }` -> Ghép đôi
- `GET /api/couple/my-couple`: Lấy thông tin cặp đôi `{ id, pairingCode, user1Name, user2Name, startDate, daysTogether, loveTitle, statusMessage, coverImageUrl }`
- `PUT /api/couple/update`: `{ startDate, loveTitle, statusMessage, coverImageUrl }`

### C. Memories (`/api/memories`)
- `GET /api/memories`: Lấy danh sách kỷ niệm
- `POST /api/memories/create`: `{ title, description, memoryDate, imageUrl, location }`
- `DELETE /api/memories/{id}`: Xóa kỷ niệm

### D. Chat (`/api/messages` & WebSocket)
- `GET /api/messages/{coupleId}`: Lấy lịch sử tin nhắn
- WebSocket Endpoint: `ws://localhost:8080/ws-chat`
- Subscribe Destination: `/topic/couple/{coupleId}`
- Send Message Destination: `/app/chat.sendMessage/{coupleId}`

---

## 4. Code Mẫu `src/api/client.ts`

```typescript
import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('access_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export async function fetchApi<T>(requestPromise: Promise<any>): Promise<T> {
  const response = await requestPromise;
  if (response.data && response.data.success !== undefined) {
    if (!response.data.success) {
      throw new Error(response.data.message || 'API Error');
    }
    return response.data.data as T;
  }
  return response.data as T;
}
```
