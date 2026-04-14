const { withTransaction } = require("../../../db");
const { queryOne } = require("./commonStore");

async function findEmotionByCode(emotionCode) {
  return queryOne("SELECT id FROM emotions WHERE code = ? LIMIT 1", [emotionCode]);
}

async function createPostWithTopics(userId, { content, emotionId, allowComments, topicIds }) {
  return withTransaction(async (conn) => {
    const [inserted] = await conn.query(
      `INSERT INTO posts (user_id, content, emotion_id, allow_comments, is_public)
       VALUES (?, ?, ?, ?, 1)`,
      [userId, content, emotionId, allowComments]
    );
    for (const topicId of topicIds) {
      await conn.query("INSERT IGNORE INTO post_topics (post_id, topic_id) VALUES (?, ?)", [inserted.insertId, topicId]);
    }
    return inserted.insertId;
  });
}

module.exports = {
  findEmotionByCode,
  createPostWithTopics
};
