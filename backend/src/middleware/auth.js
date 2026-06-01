const jwt = require('jsonwebtoken');
const User = require('../models/User');

// JWT密钥从环境变量读取，默认为开发环境密钥
const JWT_SECRET = process.env.JWT_SECRET || 'treehole_dev_secret_key_2024_do_not_use_in_production';

const auth = (req, res, next) => {
    try {
        const token = req.header('Authorization')?.replace('Bearer ', '');
        console.log(`[Auth] Token: ${token ? token.substring(0, 50) + '...' : 'none'}`);

        if (!token) {
            console.log('[Auth] No token provided');
            return res.status(401).json({ message: '请先登录' });
        }

        const decoded = jwt.verify(token, JWT_SECRET);
        console.log(`[Auth] Decoded userId: ${decoded.userId}`);
        req.userId = decoded.userId;
        req.username = decoded.username;
        req.token = token;
        next();
    } catch (error) {
        console.log('[Auth] Token error:', error.message);
        res.status(401).json({ message: 'Token 无效' });
    }
};

// Logout - mark user as offline
const logout = async (req, res) => {
    try {
        await User.findByIdAndUpdate(req.userId, {
            isOnline: false,
            lastOnlineAt: new Date()
        });
        res.json({ message: '退出成功' });
    } catch (error) {
        res.status(500).json({ message: '退出失败' });
    }
};

module.exports = auth;
module.exports.logout = logout;