import axios from 'axios'

const apiClient = axios.create({
  baseURL: 'https://busticket.ink/api',
  withCredentials: true,
})

export default apiClient
