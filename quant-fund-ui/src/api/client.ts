import axios from 'axios'

const apiClient = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' }
})

// 响应拦截器
apiClient.interceptors.response.use(
  res => res,
  error => {
    console.error('API Error:', error.message)
    return Promise.reject(error)
  }
)

export default apiClient
