import api from './client'

export const getUsers = () => api.get('/admin/users')
export const banUser = (id) => api.patch(`/admin/users/${id}/ban`)
export const unbanUser = (id) => api.patch(`/admin/users/${id}/unban`)

export const getAllPosts = () => api.get('/admin/posts')
// 用 POST 审核：避免网关/代理对 PUT、PATCH 返回 405；后端同路径仍支持 PUT/PATCH
export const auditPost = (id, status) => api.post(`/admin/posts/${id}/audit`, null, { params: { status } })
export const deletePost = (id) => api.delete(`/admin/posts/${id}`)

export const auditComment = (id, status) => api.post(`/admin/comments/${id}/audit`, null, { params: { status } })
export const deleteComment = (id) => api.delete(`/admin/comments/${id}`)
export const getCommentsByPost = (postId) => api.get(`/forum/posts/${postId}/comments`)

export const getReports = () => api.get('/admin/reports')
export const handleReport = (id, action, result) => api.patch(`/admin/reports/${id}/handle`, null, { params: { action, result: result || '' } })
