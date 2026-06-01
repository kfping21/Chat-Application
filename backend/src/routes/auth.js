const express = require('express');
const jwt = require('jsonwebtoken');
const User = require('../models/User');
const auth = require('../middleware/auth');

// JWT密钥从环境变量读取，默认为开发环境密钥
const JWT_SECRET = process.env.JWT_SECRET || 'treehole_dev_secret_key_2024_do_not_use_in_production';

const router = express.Router();

// Register
router.post('/register', async (req, res) => {
    try {
        const { username, password, nickname } = req.body;

        if (!username || !password) {
            return res.status(400).json({ message: '用户名和密码不能为空' });
        }

        if (username.length < 3 || username.length > 20) {
            return res.status(400).json({ message: '用户名长度需在3-20字符之间' });
        }

        if (password.length < 6) {
            return res.status(400).json({ message: '密码长度至少6位' });
        }

        // Check if user exists
        const existingUser = await User.findOne({ username });
        if (existingUser) {
            return res.status(400).json({ message: '用户名已存在' });
        }

        // Create new user
        const user = new User({
            username,
            password,
            nickname: nickname || username
        });

        await user.save();

        // Generate token
        const token = jwt.sign(
            { userId: user._id, username: user.username },
            JWT_SECRET,
            { expiresIn: '7d' }
        );

        res.status(201).json({
            message: '注册成功',
            token,
            user: {
                id: user._id,
                username: user.username,
                nickname: user.nickname,
                avatar: user.avatar
            }
        });
    } catch (error) {
        console.error('Register error:', error);
        res.status(500).json({ message: '服务器错误' });
    }
});

// Login
router.post('/login', async (req, res) => {
    try {
        const { username, password, forceLogin } = req.body;

        if (!username || !password) {
            return res.status(400).json({ message: '用户名和密码不能为空' });
        }

        // Find user
        const user = await User.findOne({ username });
        if (!user) {
            return res.status(400).json({ message: '用户名或密码错误' });
        }

        // Check password
        const isMatch = await user.comparePassword(password);
        if (!isMatch) {
            return res.status(400).json({ message: '用户名或密码错误' });
        }

        // Update online status using findByIdAndUpdate
        await User.findByIdAndUpdate(user._id, {
            isOnline: true,
            lastOnlineAt: new Date()
        });

        // Generate token
        const token = jwt.sign(
            { userId: user._id, username: user.username },
            JWT_SECRET,
            { expiresIn: '7d' }
        );

        res.json({
            message: '登录成功',
            token,
            user: {
                id: user._id,
                username: user.username,
                nickname: user.nickname,
                avatar: user.avatar
            }
        });
    } catch (error) {
        console.error('Login error:', error);
        res.status(500).json({ message: '服务器错误' });
    }
});

// Get current user info
router.get('/me', auth, async (req, res) => {
    try {
        const user = await User.findById(req.userId).select('-password');
        if (!user) {
            return res.status(404).json({ message: '用户不存在' });
        }
        res.json({ user });
    } catch (error) {
        res.status(500).json({ message: '服务器错误' });
    }
});

module.exports = router;