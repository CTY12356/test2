import api from './client'

export const getPosts = (params) => api.get('/forum/posts', { params })
export const createPost = (data) => api.post('/forum/posts', data)
export const getComments = (postId) => api.get(`/forum/posts/${postId}/comments`)
export const likePost = (postId) => api.post(`/forum/posts/${postId}/like`)
export const createComment = (data) => api.post('/forum/comments', data)
export const reportContent = (data) => api.post('/forum/reports', data)
