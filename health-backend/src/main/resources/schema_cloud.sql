CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  nickname VARCHAR(50),
  avatar_url VARCHAR(255),
  email VARCHAR(100),
  phone VARCHAR(30),
  role VARCHAR(20) NOT NULL DEFAULT 'USER',
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS health_profiles (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL UNIQUE,
  gender VARCHAR(20) NOT NULL,
  age INT NOT NULL,
  height_cm DECIMAL(6,2) NOT NULL,
  weight_kg DECIMAL(6,2) NOT NULL,
  target_weight_kg DECIMAL(6,2),
  activity_level VARCHAR(30) NOT NULL,
  goal VARCHAR(30) NOT NULL,
  bmi DECIMAL(6,2),
  bmr DECIMAL(8,2),
  recommended_calories DECIMAL(8,2),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_health_profiles_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS weight_records (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  weight_kg DECIMAL(6,2) NOT NULL,
  record_date DATE NOT NULL,
  remark VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_weight_user_date (user_id, record_date),
  CONSTRAINT fk_weight_records_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS exercise_records (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  exercise_type VARCHAR(50) NOT NULL,
  record_date DATE NOT NULL,
  duration_minutes INT NOT NULL,
  intensity VARCHAR(30),
  calories_burned DECIMAL(8,2) NOT NULL,
  image_url VARCHAR(255),
  remark VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_exercise_records_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS diet_records (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  record_date DATE NOT NULL,
  meal_type VARCHAR(30) NOT NULL,
  food_name VARCHAR(100) NOT NULL,
  amount VARCHAR(50),
  calories DECIMAL(8,2) NOT NULL,
  image_url VARCHAR(255),
  remark VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_diet_records_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS food_calories (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(80) NOT NULL,
  category VARCHAR(40) NOT NULL DEFAULT '',
  calories_per_unit DECIMAL(8,2) NOT NULL COMMENT '参考热量 kcal',
  unit_label VARCHAR(30) NOT NULL COMMENT '计量基准，如 100g、1个',
  sort_order INT NOT NULL DEFAULT 100,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_food_name_unit (name, unit_label),
  KEY idx_food_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO food_calories (name, category, calories_per_unit, unit_label, sort_order) VALUES
('米饭(熟)', '谷薯', 116.00, '100g', 10),
('白粥', '谷薯', 46.00, '100g', 11),
('馒头', '谷薯', 223.00, '100g', 12),
('包子(肉馅)', '谷薯', 230.00, '100g', 13),
('面条(熟)', '谷薯', 109.00, '100g', 14),
('水饺', '谷薯', 250.00, '100g', 15),
('燕麦片(干)', '谷薯', 389.00, '100g', 16),
('红薯', '谷薯', 86.00, '100g', 17),
('玉米(鲜)', '谷薯', 112.00, '100g', 18),
('全麦面包', '谷薯', 246.00, '100g', 19),
('鸡蛋', '肉蛋奶', 144.00, '100g', 20),
('鸡蛋', '肉蛋奶', 72.00, '1个(约50g)', 21),
('牛奶', '肉蛋奶', 54.00, '100ml', 22),
('无糖酸奶', '肉蛋奶', 62.00, '100g', 23),
('鸡胸肉', '肉蛋奶', 133.00, '100g', 24),
('猪瘦肉', '肉蛋奶', 143.00, '100g', 25),
('酱牛肉', '肉蛋奶', 250.00, '100g', 26),
('三文鱼', '肉蛋奶', 208.00, '100g', 27),
('北豆腐', '肉蛋奶', 81.00, '100g', 28),
('西红柿', '蔬果', 18.00, '100g', 30),
('黄瓜', '蔬果', 16.00, '100g', 31),
('西兰花(汆烫)', '蔬果', 35.00, '100g', 32),
('苹果', '蔬果', 52.00, '100g', 33),
('香蕉', '蔬果', 89.00, '100g', 34),
('橙子', '蔬果', 47.00, '100g', 35),
('草莓', '蔬果', 32.00, '100g', 36),
('白菜(炖)', '蔬果', 17.00, '100g', 37),
('可乐', '饮料', 43.00, '100ml', 40),
('橙汁', '饮料', 45.00, '100ml', 41),
('啤酒', '饮料', 43.00, '100ml', 42),
('奶茶(含糖,参考)', '饮料', 55.00, '100ml', 43),
('薯片(油炸)', '零食', 536.00, '100g', 50),
('苏打饼干', '零食', 433.00, '100g', 51),
('黑巧克力', '零食', 589.00, '100g', 52),
('油条', '油脂', 388.00, '100g', 60),
('花生油(参考)', '油脂', 899.00, '100g', 61);

CREATE TABLE IF NOT EXISTS posts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(120) NOT NULL,
  content TEXT NOT NULL,
  category VARCHAR(40) NOT NULL,
  tags VARCHAR(255),
  image_url VARCHAR(255),
  like_count INT NOT NULL DEFAULT 0,
  favorite_count INT NOT NULL DEFAULT 0,
  comment_count INT NOT NULL DEFAULT 0,
  view_count INT NOT NULL DEFAULT 0,
  audit_status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_posts_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS comments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  post_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  parent_id BIGINT,
  content TEXT NOT NULL,
  like_count INT NOT NULL DEFAULT 0,
  audit_status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts(id),
  CONSTRAINT fk_comments_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS post_likes (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  post_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_post_user (post_id, user_id),
  CONSTRAINT fk_post_likes_post FOREIGN KEY (post_id) REFERENCES posts(id),
  CONSTRAINT fk_post_likes_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS reports (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  reporter_id BIGINT NOT NULL,
  target_type VARCHAR(30) NOT NULL,
  target_id BIGINT NOT NULL,
  reason VARCHAR(255) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  handled_by BIGINT,
  result VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  handled_at DATETIME,
  CONSTRAINT fk_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users(id)
);
