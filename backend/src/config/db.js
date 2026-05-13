const mongoose = require('mongoose');
const { FIXED_MONGO_URI } = require('./mongoUri');

// 固定连接到本地同一个库，避免因环境变量变化连到其他库
const MONGO_URI = FIXED_MONGO_URI;

const connectDB = async () => {
    try {
        await mongoose.connect(MONGO_URI);
        console.log(`✅ MongoDB 连接成功: ${mongoose.connection.name}`);
    } catch (error) {
        console.error('❌ MongoDB 连接失败:', error.message);
        process.exit(1);
    }
};

module.exports = connectDB;
