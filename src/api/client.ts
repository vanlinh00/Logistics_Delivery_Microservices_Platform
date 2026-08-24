import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8000';

export interface GenericResponse<T> {
  success: boolean;
  code: string;
  message: string;
  data: T;
  details?: string[];
  timestamp?: string;
}

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

// Helper bóc tách GenericResponse<T> và xử lý Exception tập trung
export async function fetchApi<T>(requestPromise: Promise<any>): Promise<T> {
  try {
    const response = await requestPromise;
    if (response.data && response.data.success !== undefined) {
      if (!response.data.success) {
        throw new Error(response.data.message || 'API Error');
      }
      return response.data.data as T;
    }
    return response.data as T;
  } catch (error: any) {
    if (error.response?.data?.message) {
      const detailsStr = error.response.data.details?.length
        ? ` (${error.response.data.details.join('; ')})`
        : '';
      throw new Error(`[${error.response.data.code || error.response.status}] ${error.response.data.message}${detailsStr}`);
    }
    throw error;
  }
}
