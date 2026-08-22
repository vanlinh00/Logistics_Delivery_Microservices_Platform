# AI Studio Project Instructions: Frontend & Spring Boot Backend Integration

Tài liệu này chứa toàn bộ quy tắc, cấu trúc API, và hướng dẫn tích hợp giữa Frontend (React / Vite / TypeScript) và Backend (Spring Boot 3.4). Khi bạn import file `AGENTS.md` này vào project Frontend, AI Studio sẽ tự động đọc và thực hiện kết nối API chính xác 100%.

---

## 1. Cấu Hình Biến Môi Trường (Environment Variables)

Frontend sử dụng `Vite`, khai báo trong file `.env` hoặc `.env.local`:

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_WS_BASE_URL=http://localhost:8080/ws-chat
```

---

## 2. Cấu Trúc Khung Phản Hồi Từ Backend (Standard Generic Response)

Tất cả các API RESTful của Spring Boot Backend đều trả về một wrapper chuẩn `GenericResponse<T>` theo định dạng JSON:

```typescript
export interface GenericResponse<T> {
  success: boolean;
  code: string;       // "200" cho thành công, hoặc mã lỗi (vd: "400", "401", "403")
  message: string;    // Thông báo lỗi hoặc kết quả
  data: T;            // Dữ liệu thực tế trả về
  details: string[];  // Chi tiết lỗi (nếu có)
}
```

> ⚠️ **LƯU Ý QUAN TRỌNG CHO AI CODING AGENT:**
> Khi gọi API bằng `axios` hay `fetch`, dữ liệu nằm trong `response.data.data` (vì `response.data` là `GenericResponse`).

---

## 3. Quản Lý Định Danh & Authentication (JWT Token)

Backend sử dụng **JWT (JSON Web Token)** để xác thực.

### Header Xác Thực
Mọi request cần truyền Header:
```http
Authorization: Bearer <access_token>
```

---

## 4. Danh Sách API Endpoints & Interfaces

### 4.1. Authentication (`/api/auth`)

#### 1. Đăng nhập (`POST /api/auth/login`)
- **Request Body**:
```typescript
export interface LoginRequest {
  usernameOrEmail: string;
  password: string;
}
```
- **Response Data (`AuthResponse`)**:
```typescript
export interface AuthResponse {
  token: string;
  refreshToken: string;
  tokenType: string; // "Bearer"
  id: number;
  username: string;
  email: string;
  fullName?: string;
  avatarUrl?: string;
  coupleId?: number;
}
```

#### 2. Đăng ký (`POST /api/auth/register`)
- **Request Body**:
```typescript
export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  fullName?: string;
  gender?: 'MALE' | 'FEMALE' | string;
}
```

#### 3. Làm mới Token (`POST /api/auth/refresh`)
- **Request Body**:
```typescript
export interface RefreshTokenRequest {
  refreshToken: string;
}
```
- **Response Data**: Trả về `AuthResponse` mới.

#### 4. Lấy thông tin user hiện tại (`GET /api/auth/me`)
- **Header**: `Authorization: Bearer <token>`
- **Response Data**: Thông tin chi tiết của User.

---

### 4.2. Quản Lý Đôi Lứa / Couple (`/api/couple`)

#### 1. Tạo Mã Kết Đôi (`POST /api/couple/create-code`)
- **Response Data**: Trả về object `Couple` có chứa `pairingCode`.

#### 2. Ghép Đôi Với Người Yêu (`POST /api/couple/pair`)
- **Request Body**:
```typescript
export interface PairRequest {
  pairingCode: string;
}
```

#### 3. Lấy Thông Tin Cặp Đôi Của Tôi (`GET /api/couple/my-couple`)
- **Response Data (`CoupleResponse`)**:
```typescript
export interface CoupleResponse {
  id: number;
  pairingCode: string;
  user1Id: number;
  user1Name: string;
  user1Avatar?: string;
  user2Id?: number;
  user2Name?: string;
  user2Avatar?: string;
  startDate: string; // "YYYY-MM-DD"
  daysTogether: number; // Số ngày bên nhau tự động tính từ backend
  loveTitle?: string;
  statusMessage?: string;
  coverImageUrl?: string;
}
```

#### 4. Cập Nhật Thông Tin Cặp Đôi (`PUT /api/couple/update`)
- **Request Body**:
```typescript
export interface UpdateCoupleRequest {
  startDate?: string; // "YYYY-MM-DD"
  loveTitle?: string;
  statusMessage?: string;
  coverImageUrl?: string;
}
```

---

### 4.3. Kỷ Niệm / Memories (`/api/memories`)

#### 1. Lấy Danh Sách Kỷ Niệm (`GET /api/memories`)
- **Response Data**: `Memory[]`

#### 2. Tạo Kỷ Niệm Mới (`POST /api/memories/create`)
- **Request Body (`MemoryDTO`)**:
```typescript
export interface MemoryDTO {
  id?: number;
  coupleId?: number;
  title: string;
  description?: string;
  memoryDate: string; // "YYYY-MM-DD"
  imageUrl?: string;
  location?: string;
}
```

#### 3. Xóa Kỷ Niệm (`DELETE /api/memories/{id}`)

---

### 4.4. Trò Chuyện & Message Chat (`/api/messages` & WebSocket)

#### 1. Lấy Lịch Sử Tin Nhắn (`GET /api/messages/{coupleId}`)
- **Response Data**: `ChatMessageDTO[]`
```typescript
export interface ChatMessageDTO {
  id?: number;
  coupleId: number;
  senderId: number;
  senderName?: string;
  senderAvatar?: string;
  content: string;
  msgType?: 'TEXT' | 'IMAGE' | 'HEART' | 'STICKER';
  createdAt?: string;
}
```

#### 2. Kết Nối Realtime Chat qua WebSocket (STOMP / SockJS)
- **Endpoint**: `http://localhost:8080/ws-chat`
- **Subscribe Destination**: `/topic/couple/{coupleId}`
- **Send Message Destination**: `/app/chat.sendMessage/{coupleId}`
- **Payload**:
```json
{
  "senderId": 1,
  "content": "Anh yêu em ❤️",
  "msgType": "TEXT"
}
```

---

## 5. Mẫu Client Axios Cho Frontend (React + TypeScript)

AI Coding Agent nên tạo hoặc cập nhật file `/src/api/client.ts` như sau:

```typescript
import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Interceptor tự động gán Bearer Token
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('access_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Helper bóc tách GenericResponse<T>
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

---

## 6. Yêu Cầu Thực Thi Cho AI Studio Khi Code Frontend

1. Tất cả các service gọi API (VD: `authService`, `coupleService`, `memoryService`, `chatService`) phải gọi thông qua `apiClient` hoặc `fetchApi`.
2. Lưu giữ `access_token` và `refresh_token` vào `localStorage`.
3. Xử lý trạng thái Loading & Error trực quan cho người dùng.
4. Tuân thủ chính xác tên trường thuộc tính (casing camelCase) giống với định dạng các interface ở trên.
