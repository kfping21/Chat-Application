CREATE DATABASE IF NOT EXISTS treehole
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE treehole;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  anonymous_name VARCHAR(50) NOT NULL,
  avatar_color VARCHAR(20) NOT NULL DEFAULT 'yellow',
  joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  status TINYINT NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS emotions (
  id TINYINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(30) NOT NULL UNIQUE,
  display_name VARCHAR(30) NOT NULL
);

CREATE TABLE IF NOT EXISTS topics (
  id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL UNIQUE,
  is_hot TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS posts (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  content VARCHAR(500) NOT NULL,
  emotion_id TINYINT UNSIGNED NOT NULL,
  allow_comments TINYINT NOT NULL DEFAULT 1,
  is_public TINYINT NOT NULL DEFAULT 1,
  likes_count INT UNSIGNED NOT NULL DEFAULT 0,
  comments_count INT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_posts_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_posts_emotion FOREIGN KEY (emotion_id) REFERENCES emotions(id),
  INDEX idx_posts_created_at (created_at),
  INDEX idx_posts_emotion_created (emotion_id, created_at)
);

CREATE TABLE IF NOT EXISTS post_topics (
  post_id BIGINT UNSIGNED NOT NULL,
  topic_id INT UNSIGNED NOT NULL,
  PRIMARY KEY (post_id, topic_id),
  CONSTRAINT fk_post_topics_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
  CONSTRAINT fk_post_topics_topic FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS comments (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  post_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  parent_comment_id BIGINT UNSIGNED NULL,
  floor_no INT UNSIGNED NOT NULL,
  content VARCHAR(300) NOT NULL,
  likes_count INT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
  CONSTRAINT fk_comments_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_comments_parent FOREIGN KEY (parent_comment_id) REFERENCES comments(id) ON DELETE SET NULL,
  INDEX idx_comments_post_created (post_id, created_at),
  INDEX idx_comments_parent (parent_comment_id)
);

CREATE TABLE IF NOT EXISTS post_likes (
  post_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (post_id, user_id),
  CONSTRAINT fk_post_likes_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
  CONSTRAINT fk_post_likes_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS comment_likes (
  comment_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (comment_id, user_id),
  CONSTRAINT fk_comment_likes_comment FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE,
  CONSTRAINT fk_comment_likes_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS encounters (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  target_user_id BIGINT UNSIGNED NOT NULL,
  met_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_encounters_pair (user_id, target_user_id),
  CONSTRAINT fk_encounters_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_encounters_target FOREIGN KEY (target_user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS notifications (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  type VARCHAR(20) NOT NULL,
  ref_id BIGINT UNSIGNED NULL,
  payload JSON NULL,
  is_read TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_notifications_user_read (user_id, is_read, created_at)
);

CREATE TABLE IF NOT EXISTS private_messages (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  sender_user_id BIGINT UNSIGNED NOT NULL,
  receiver_user_id BIGINT UNSIGNED NOT NULL,
  content VARCHAR(500) NOT NULL,
  is_read TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_private_messages_sender FOREIGN KEY (sender_user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_private_messages_receiver FOREIGN KEY (receiver_user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_private_messages_pair_time (sender_user_id, receiver_user_id, created_at),
  INDEX idx_private_messages_receiver_read (receiver_user_id, is_read, created_at)
);

INSERT INTO emotions (code, display_name) VALUES
  ('lonely', '孤独'),
  ('happy', '开心'),
  ('regret', '后悔'),
  ('anxious', '焦虑'),
  ('moved', '感动'),
  ('calm', '平静')
ON DUPLICATE KEY UPDATE display_name = VALUES(display_name);

INSERT INTO topics (name, is_hot) VALUES
  ('深夜话题', 1),
  ('分享喜悦', 1),
  ('倾诉烦恼', 1),
  ('人生困惑', 1),
  ('温暖瞬间', 1),
  ('失眠夜晚', 1),
  ('工作压力', 1),
  ('感情故事', 1)
ON DUPLICATE KEY UPDATE is_hot = VALUES(is_hot);

INSERT INTO users (id, anonymous_name, avatar_color)
VALUES (1, '匿名的灵魂', 'yellow')
ON DUPLICATE KEY UPDATE anonymous_name = VALUES(anonymous_name), avatar_color = VALUES(avatar_color);

INSERT INTO users (id, anonymous_name, avatar_color)
VALUES
  (2, '温柔的回声', 'blue'),
  (3, '夜航星', 'purple')
ON DUPLICATE KEY UPDATE anonymous_name = VALUES(anonymous_name), avatar_color = VALUES(avatar_color);

INSERT INTO posts (id, user_id, content, emotion_id, allow_comments, is_public, likes_count, comments_count, created_at)
VALUES
  (
    1,
    1,
    '我在公司装了三年的开心，每天早上戴上笑容，可内心早已空空如也。我已经不知道自己是谁了。',
    (SELECT id FROM emotions WHERE code = 'lonely'),
    1,
    1,
    234,
    3,
    DATE_SUB(NOW(), INTERVAL 2 HOUR)
  ),
  (
    2,
    1,
    '今天我终于对父母说了我爱他们。那一刻，感觉像是回家了。',
    (SELECT id FROM emotions WHERE code = 'moved'),
    1,
    1,
    892,
    127,
    DATE_SUB(NOW(), INTERVAL 5 HOUR)
  ),
  (
    3,
    1,
    '今天傍晚坐在湖边看日落，突然觉得一切都会好起来的。这是几个月来第一次感到平静。',
    (SELECT id FROM emotions WHERE code = 'calm'),
    1,
    1,
    1234,
    203,
    DATE_SUB(NOW(), INTERVAL 1 DAY)
  )
ON DUPLICATE KEY UPDATE content = VALUES(content);

INSERT INTO comments (id, post_id, user_id, parent_comment_id, floor_no, content, likes_count, created_at)
VALUES
  (1, 1, 1, NULL, 1, '我也有同样的感受。每天都在演戏，真的好累。但你能说出来就已经很勇敢了。', 23, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
  (2, 1, 1, NULL, 2, '抱抱你，愿你慢慢找回自己。', 11, DATE_SUB(NOW(), INTERVAL 45 MINUTE)),
  (3, 1, 1, NULL, 3, '先好好睡一觉，明天会不一样。', 8, DATE_SUB(NOW(), INTERVAL 30 MINUTE))
ON DUPLICATE KEY UPDATE content = VALUES(content);

INSERT INTO notifications (user_id, type, ref_id, payload, is_read, created_at)
VALUES
  (1, 'post_like', 3, JSON_OBJECT('text', '有人共鸣了你关于找到平静的秘密', 'count', 1), 0, DATE_SUB(NOW(), INTERVAL 5 MINUTE)),
  (1, 'comment', 1, JSON_OBJECT('text', '新评论：「我也有同样的感受，你不是一个人。」'), 0, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
  (1, 'comment_reply', 1, JSON_OBJECT('text', '有人回复了你的评论'), 1, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
  (1, 'post_like', 3, JSON_OBJECT('text', '23个人与你的故事产生了共鸣', 'count', 23), 1, DATE_SUB(NOW(), INTERVAL 5 HOUR))
ON DUPLICATE KEY UPDATE payload = VALUES(payload);

INSERT INTO private_messages (sender_user_id, receiver_user_id, content, is_read, created_at)
VALUES
  (2, 1, '在吗？我看到你发的秘密了，想跟你说你并不孤单。', 0, DATE_SUB(NOW(), INTERVAL 10 MINUTE)),
  (3, 1, '晚安，愿你今晚睡个好觉。', 0, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
  (1, 2, '谢谢你，我收到你的温暖了。', 1, DATE_SUB(NOW(), INTERVAL 3 MINUTE));
