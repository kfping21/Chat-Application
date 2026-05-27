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

// Discover users for soul sphere (遇见)
router.get('/discover', auth, async (req, res) => {
    try {
        const limit = parseInt(req.query.limit) || 60;
        const currentUserId = req.userId;

        // Get users with recent activity, excluding current user
        const users = await User.aggregate([
            { $match: { _id: { $ne: require('mongoose').Types.ObjectId.createFromString(currentUserId) } } },
            { $addFields: { followersCount: { $size: { $ifNull: ['$followers', []] } } } },
            { $sort: { followersCount: -1, createdAt: -1 } },
            { $limit: limit },
            { $project: {
                _id: 1,
                nickname: 1,
                avatar: 1,
                bio: 1,
                isOnline: 1,
                lastOnlineAt: 1
            }}
        ]);

        res.json({
            users: users.map(user => ({
                id: user._id.toString(),
                nickname: user.nickname || '匿名用户',
                avatar: user.avatar || '',
                bio: user.bio || '',
                isOnline: user.isOnline || false,
                lastOnlineAt: user.lastOnlineAt ? user.lastOnlineAt.toISOString() : null
            }))
        });
    } catch (error) {
        console.error('Discover users error:', error);
        res.status(500).json({ message: '服务器错误' });
    }
});

// Get user profile by ID
router.get('/:id', auth, async (req, res) => {
    try {
        const user = await User.findById(req.params.id).select('nickname avatar bio createdAt following followers isOnline lastOnlineAt').lean();

        if (!user) {
            return res.status(404).json({ message: '用户不存在' });
        }

        // Get user's posts count
        const Post = require('../models/Post');
        const Comment = require('../models/Comment');
        const ChatRoom = require('../models/ChatRoom');
        const postsCount = await Post.countDocuments({ userId: req.params.id });
        // 回响: get user's posts, then count comments on those posts
        const userPosts = await Post.find({ userId: req.params.id }).select('_id');
        const userPostIds = userPosts.map(p => p._id);
        const commentsCount = userPostIds.length > 0
            ? await Comment.countDocuments({ postId: { $in: userPostIds } })
            : 0;
        // 遇见的灵魂: count of chat rooms this user participates in
        const chatRoomsCount = await ChatRoom.countDocuments({ participantIds: req.params.id });

        // Check if current user is following this user
        const isFollowing = user.following?.some(id => id.toString() === req.userId) || false;

        console.log(`[GetUserProfile] userId=${req.params.id}, isOnline=${user.isOnline}, posts=${postsCount}, comments=${commentsCount}, chatRooms=${chatRoomsCount}`);

        res.json({
            id: user._id,
            nickname: user.nickname,
            avatar: user.avatar,
            bio: user.bio || '',
            createdAt: user.createdAt,
            postsCount: postsCount,
            commentsCount: commentsCount,
            chatRoomsCount: chatRoomsCount,
            isFollowing: isFollowing,
            isOnline: user.isOnline === true,
            followersCount: user.followers?.length || 0,
            followingCount: user.following?.length || 0
        });
    } catch (error) {
        console.error('Get user error:', error);
        res.status(500).json({ message: '获取用户信息失败' });
    }
});

// Get users that this user follows
router.get('/:id/following', auth, async (req, res) => {
    try {
        const user = await User.findById(req.params.id).select('following').lean();
        if (!user) {
            return res.status(404).json({ message: '用户不存在' });
        }

        const followingUsers = await User.find({ _id: { $in: user.following } })
            .select('nickname avatar bio isOnline')
            .lean();

        const currentUser = await User.findById(req.userId).select('following').lean();
        const currentFollowing = currentUser?.following || [];

        res.json({
            users: followingUsers.map(u => ({
                id: u._id.toString(),
                nickname: u.nickname || '匿名灵魂',
                avatar: u.avatar || '',
                bio: u.bio || '',
                isOnline: u.isOnline || false,
                isFollowing: currentFollowing.some(id => id.toString() === u._id.toString())
            }))
        });
    } catch (error) {
        console.error('Get following error:', error);
        res.status(500).json({ message: '获取关注列表失败' });
    }
});

// Get users that follow this user
router.get('/:id/followers', auth, async (req, res) => {
    try {
        const user = await User.findById(req.params.id).select('followers').lean();
        if (!user) {
            return res.status(404).json({ message: '用户不存在' });
        }

        const followerUsers = await User.find({ _id: { $in: user.followers } })
            .select('nickname avatar bio isOnline')
            .lean();

        const currentUser = await User.findById(req.userId).select('following').lean();
        const currentFollowing = currentUser?.following || [];

        res.json({
            users: followerUsers.map(u => ({
                id: u._id.toString(),
                nickname: u.nickname || '匿名灵魂',
                avatar: u.avatar || '',
                bio: u.bio || '',
                isOnline: u.isOnline || false,
                isFollowing: currentFollowing.some(id => id.toString() === u._id.toString())
            }))
        });
    } catch (error) {
        console.error('Get followers error:', error);
        res.status(500).json({ message: '获取粉丝列表失败' });
    }
});

module.exports = router;
