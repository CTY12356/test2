-- v4: weight_records 全局去重 — 同一 user_id + record_date 只保留 id 最大的一条（通常为最后录入）
-- 建议先备份。执行后可加唯一索引，与 schema.sql 一致。
--
-- MySQL Workbench「安全更新模式」：即便是 WHERE id IN (子查询)，也可能报 Error 1175。
-- 请在本文件中从 SET SQL_SAFE_UPDATES 到 SET ... = 1 一次性选中执行（同一连接、同一批次）。

USE health_management;

-- 可选：查看是否仍有重复
-- SELECT user_id, record_date, COUNT(*) AS n
-- FROM weight_records
-- GROUP BY user_id, record_date
-- HAVING n > 1;

SET SQL_SAFE_UPDATES = 0;

DELETE wr FROM weight_records wr
INNER JOIN weight_records wr2
  ON wr.user_id = wr2.user_id
  AND wr.record_date = wr2.record_date
  AND wr.id < wr2.id;

SET SQL_SAFE_UPDATES = 1;

-- 若尚未有 uk_weight_user_date，执行下面一行；若报 Duplicate key name 说明已存在，可忽略
ALTER TABLE weight_records ADD UNIQUE KEY uk_weight_user_date (user_id, record_date);
