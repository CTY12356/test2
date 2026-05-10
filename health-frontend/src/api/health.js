import api from './client'

export const getProfile = () => api.get('/profiles/me')
export const saveProfile = (data) => api.post('/profiles/me', data)
export const getWeights = () => api.get('/profiles/weights')
export const addWeight = (data) => api.post('/profiles/weights', data)
/** @param {{ keyword?: string, limit?: number }} params */
export const searchFoodCalories = (params) => api.get('/food-calories/search', { params })
/** 餐图识别：云侧多模态较慢，单独放宽超时（默认 axios 为 8s） */
export const recognizeFoodFromImage = (imageUrl) =>
  api.post('/food-calories/recognize', { imageUrl }, { timeout: 120000 })

export const getDietRecords = (date) => api.get('/diet-records', { params: date ? { date } : {} })
export const createDietRecord = (data) => api.post('/diet-records', data)
export const updateDietRecord = (id, data) => api.put(`/diet-records/${id}`, data)
export const deleteDietRecord = (id) => api.delete(`/diet-records/${id}`)

export const getExerciseRecords = (date) => api.get('/exercise-records', { params: date ? { date } : {} })
export const createExerciseRecord = (data) => api.post('/exercise-records', data)
export const updateExerciseRecord = (id, data) => api.put(`/exercise-records/${id}`, data)
export const deleteExerciseRecord = (id) => api.delete(`/exercise-records/${id}`)

export const getDailySummary = (date) => api.get('/summary/daily', { params: date ? { date } : {} })
export const getWeeklyTrend = () => api.get('/summary/trend')
