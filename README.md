# 健康管理网站

本项目采用前后端分离结构：

- `health-frontend`：Vue 3 + Vite + Element Plus + Axios + ECharts
- `health-backend`：Spring Boot + MyBatis-Plus + JWT + MySQL + Knife4j

## 数据库

本地开发默认库名：`health_management`（见 `schema.sql` 中的 `CREATE DATABASE`）。

后端支持通过环境变量连接任意 MySQL（Zeabur、Docker 等）：`MYSQL_HOST`、`MYSQL_PORT`、`MYSQL_USERNAME`、`MYSQL_PASSWORD`、`MYSQL_DATABASE`。未设置时仍使用 `application.properties` 中的本地默认值。

首次运行前：

- **本地**：在 MySQL 中执行  
  `source health-backend/src/main/resources/schema.sql;`
- **Zeabur / 云库已有固定库名（如 `zeabur`）**：连接到该库后执行  
  `health-backend/src/main/resources/schema_cloud.sql`  
  （不含建库语句，避免覆盖错误库名）

环境变量模板见仓库根目录 `zeabur.env.example`。

## 启动后端

```bash
cd health-backend
./mvnw spring-boot:run
```

Windows PowerShell 可使用：

```powershell
cd health-backend
.\mvnw.cmd spring-boot:run
```

接口文档地址：

```text
http://localhost:8080/api/doc.html
```

## 启动前端

```bash
cd health-frontend
npm install
npm run dev
```

前端默认访问：

```text
http://localhost:5173
```

## Zeabur 部署要点

1. **MySQL**：在 Zeabur 创建 MySQL，将服务与 **后端** 绑定（或手动配置 `MYSQL_*` 环境变量）。库名为控制台给出的名称（常见为 `zeabur`）时，须设置 `MYSQL_DATABASE=zeabur`。
2. **初始化表**：用 Zeabur 提供的连接方式连上后，在对应库中执行 `schema_cloud.sql`（不要用本地那份带 `CREATE DATABASE health_management` 的脚本）。
3. **后端**：部署目录选 `health-backend`；设置强随机 **`JWT_SECRET`**；Zeabur 会注入 **`PORT`**，项目已使用 `${PORT:8080}`。
4. **前端**：单独服务部署目录选 `health-frontend`；构建前设置 **`VITE_API_BASE`** 为后端公网地址加 `/api`，例如 `https://<你的后端>.zeabur.app/api`，再 `npm run build`。

## 已包含功能

- 用户注册、登录和 JWT 鉴权
- 健康档案创建与 BMI/BMR/TDEE 计算
- 饮食记录和运动记录
- 今日摄入、消耗和热量差值统计
- 论坛发帖、评论和举报接口
- 管理员审核接口
- 图片上传和本地静态访问
