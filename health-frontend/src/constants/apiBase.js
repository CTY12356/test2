/**
 * 前端访问后端的 API 前缀。
 * 本地开发默认 `/api`，由 Vite 代理到后端。
 * Zeabur 等多域名部署时，在项目的环境变量中设置 VITE_API_BASE 为后端完整前缀，
 * 例如：https://你的后端服务域名.zeabur.app/api
 */
export const API_BASE = (import.meta.env.VITE_API_BASE || '').trim() || '/api'
