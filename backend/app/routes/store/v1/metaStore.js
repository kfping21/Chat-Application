async function listHotTopics(query) {
  return query(
    `SELECT id, name
     FROM topics
     WHERE is_hot = 1
     ORDER BY id ASC
     LIMIT 50`
  );
}

async function listEmotions(query) {
  return query(
    `SELECT id, code, display_name
     FROM emotions
     ORDER BY id ASC`
  );
}

module.exports = {
  listHotTopics,
  listEmotions
};
