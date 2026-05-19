const express = require('express');
const auth = require('../middleware/auth');
const User = require('../models/User');
const { upload, uploadDir } = require('../config/localStorage');

const router = express.Router();

// 静态文件服务 - 头像和图片
const path = require('path');
router.use('/uploads', express.static(path.join(__dirname, '../uploads')));

// Get base URL from request
const getBaseUrl = (req) => {
    const protocol = req.protocol;
    const host = req.get('host');
    return `${protocol}://${host}`;
};

// Update user profile (nickname + avatar)
router.put('/profile', auth, upload.single('avatar'), async (req, res) => {
    try {
        const { nickname, avatar, bio } = req.body;
        const updates = {};

        if (nickname && nickname.trim().length > 0) updates.nickname = nickname.trim();
        if (bio !== undefined) updates.bio = bio.trim().substring(0, 200);

        // Handle avatar upload - local storage
        if (req.file) {
            // Return full URL path
            const baseUrl = getBaseUrl(req);
            const avatarUrl = `${baseUrl}/uploads/avatars/${path.basename(req.file.path)}`;
            updates.avatar = avatarUrl;
        }
        // Allow preset avatars
        else if (avatar && !avatar.startsWith('http') && avatar.startsWith('preset_')) {
            updates.avatar = avatar;
        }

        if (Object.keys(updates).length === 0) {
            return res.status(400).json({ message: '没有要更新的字段' });
        }

        const user = await User.findByIdAndUpdate(
            req.userId,
            updates,
            { new: true }
        ).select('-password');

        res.json({ user });
    } catch (error) {
        console.error('Update profile error:', error);
        res.status(500).json({ message: '服务器错误' });
    }
});

// Upload avatar only
router.post('/avatar', auth, upload.single('avatar'), async (req, res) => {
    try {
        if (!req.file) {
            return res.status(400).json({ message: '请选择图片' });
        }

        // Return full URL path
        const baseUrl = getBaseUrl(req);
        const avatarUrl = `${baseUrl}/uploads/avatars/${path.basename(req.file.path)}`;

        const user = await User.findByIdAndUpdate(
            req.userId,
            { avatar: avatarUrl },
            { new: true }
        ).select('-password');

        res.json({
            user,
            avatarUrl: avatarUrl
        });
    } catch (error) {
        console.error('Upload avatar error:', error);
        res.status(500).json({ message: '服务器错误' });
    }
});

// Discover users for explore globe
router.get('/discover', auth, async (req, res) => {
    try {
        const limit = Math.min(Math.max(parseInt(req.query.limit, 10) || 60, 1), 120);
        const users = await User.find({ _id: { $ne: req.userId } })
            .select('nickname avatar bio isOnline lastOnlineAt createdAt')
            .sort({ isOnline: -1, lastOnlineAt: -1, createdAt: -1 })
            .limit(limit)
            .lean();

        res.json({
            users: users.map(u => ({
                id: u._id.toString(),
                nickname: (u.nickname || '').trim(),
                avatar: u.avatar || '',
                bio: (u.bio || '').trim(),
                isOnline: !!u.isOnline,
                lastOnlineAt: u.lastOnlineAt || null
            }))
        });
    } catch (error) {
        console.error('Discover users error:', error);
        res.status(500).json({ message: '获取遇见列表失败' });
    }
});

// Get user profile by ID
router.get('/:id', auth, async (req, res) => {
    try {
        const user = await User.findById(req.params.id).select('nickname avatar bio createdAt following followers').lean();

        if (!user) {
            return res.status(404).json({ message: '用户不存在' });
        }

        // Get user's posts count
        const Post = require('../models/Post');
        const Comment = require('../models/Comment');
        const postsCount = await Post.countDocuments({ userId: req.params.id });
        const commentsCount = await Comment.countDocuments({ userId: req.params.id });

        // Check if current user is following this user
        const currentUser = await User.findById(req.userId).select('following').lean();
        const isFollowing = currentUser?.following?.some(id => id.toString() === req.params.id) || false;

        res.json({
            id: user._id,
            nickname: user.nickname,
            avatar: user.avatar,
            bio: user.bio || '',
            createdAt: user.createdAt,
            postsCount: postsCount,
            commentsCount: commentsCount,
            isFollowing: isFollowing,
            followersCount: user.followers?.length || 0,
            followingCount: user.following?.length || 0
        });
    } catch (error) {
        console.error('Get user error:', error);
        res.status(500).json({ message: '获取用户信息失败' });
    }
});

module.exports = router;
