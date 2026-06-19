const bcrypt = require('bcryptjs');
const mongoose = require('mongoose');
const User = require('../models/User');

// 10 users with cartoon and scenery preset avatars
const users = [
  { username: 'xiaoyu2024', password: '123456', nickname: '小鱼的秘密', avatar: 'avatar_cartoon_1' },
  { username: 'nightowl99', password: '123456', nickname: '熬夜小能手', avatar: 'avatar_cartoon_2' },
  { username: 'moonwalker', password: '123456', nickname: '月亮漫步者', avatar: 'avatar_scenery_1' },
  { username: 'cloudnine', password: '123456', nickname: '九号云朵', avatar: 'avatar_cartoon_3' },
  { username: 'sunshinekid', password: '123456', nickname: '向日葵少年', avatar: 'avatar_scenery_2' },
  { username: 'forestelf', password: '123456', nickname: '森林精灵', avatar: 'avatar_cartoon_4' },
  { username: 'starcatcher', password: '123456', nickname: '抓星星的人', avatar: 'avatar_scenery_3' },
  { username: 'coffeelover', password: '123456', nickname: '咖啡续命中', avatar: 'avatar_cartoon_5' },
  { username: 'dreammaker', password: '123456', nickname: '造梦工程师', avatar: 'avatar_scenery_4' },
  { username: 'seawanderer', password: '123456', nickname: '海边流浪者', avatar: 'avatar_scenery_5' }
];

async function createUser(user) {
  try {
    // Check if user already exists
    const existing = await User.findOne({ username: user.username });
    if (existing) {
      console.log(`✗ Already exists: ${user.username}`);
      return null;
    }

    const hashedPassword = await bcrypt.hash(user.password, 10);
    const newUser = new User({
      username: user.username,
      password: hashedPassword,
      nickname: user.nickname,
      avatar: user.avatar
    });

    await newUser.save();
    console.log(`✓ Created: ${user.nickname} (${user.username}) - ${user.avatar}`);
    return newUser;
  } catch (error) {
    console.error(`✗ Error creating ${user.username}: ${error.message}`);
    return null;
  }
}

async function main() {
  // Connect to MongoDB
  const MONGO_URI = process.env.MONGODB_URI || 'mongodb://localhost:27017/treehole';

  try {
    await mongoose.connect(MONGO_URI);
    console.log('Connected to MongoDB\n');

    console.log('Creating 10 test users with preset avatars...\n');

    for (const user of users) {
      await createUser(user);
    }

    console.log('\n========== Created Users ==========');
    console.log('Password for all: 123456\n');
    users.forEach(u => console.log(`  ${u.username} - ${u.nickname} (${u.avatar})`));
    console.log('\n===================================');

  } catch (error) {
    console.error('MongoDB connection error:', error.message);
    process.exit(1);
  } finally {
    await mongoose.disconnect();
  }
}

main();