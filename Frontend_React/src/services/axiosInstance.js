import axios from 'axios';

const axiosInstance = axios.create({
  baseURL: 'http://localhost:8080/api',
});

axiosInstance.interceptors.response.use((response) => {
  const apiResponse = response.data;

  if (apiResponse.success === false) {
    return Promise.reject(new Error(apiResponse.message));
  }

  return apiResponse.data;
});

export default axiosInstance;
