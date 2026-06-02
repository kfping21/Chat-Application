const mongoose = require('mongoose');

// 本地 MongoDB 配置
const MONGO_URI = process.env.MONGO_URI || 'mongodb://localhost:27017/treehole';

const connectDB = async () => {
    try {
        await mongoose.connect(MONGO_URI);
        console.log('✅ MongoDB 连接成功 (本地)');
    } catch (error) {
        console.error('❌ MongoDB 连接失败:', error.message);
        process.exit(1);
    }
};

module.exports = connectDB;