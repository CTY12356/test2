# 健康管理网站

本项目采用前后端分离结构：

- `health-frontend`：Vue 3 + Vite + Element Plus + Axios + ECharts
- `health-backend`：Spring Boot + MyBatis-Plus + JWT + MySQL + Knife4j

## 数据库

数据库名：`health_management`

后端已配置本地 MySQL：

```properties
spring.datasource.username=root
spring.datasource.password=050626
```

首次运行前，请在 MySQL 中执行：

```sql
source health-backend/src/main/resources/schema.sql;
```

也可以手动复制 `schema.sql` 中的建表语句执行。

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

## 已包含功能

- 用户注册、登录和 JWT 鉴权
- 健康档案创建与 BMI/BMR/TDEE 计算
- 饮食记录和运动记录
- 今日摄入、消耗和热量差值统计
- 论坛发帖、评论和举报接口
- 管理员审核接口
- 图片上传和本地静态访问
