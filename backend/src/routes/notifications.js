const express = require('express');
const auth = require('../middleware/auth');
const Notification = require('../models/Notification');
const Post = require('../models/Post');
const User = require('../models/User');

const router = express.Router();

// Get user's notifications (likes and comments on own posts)
router.get('/', auth, async (req, res) => {
    try {
        const notifications = await Notification.find({ recipientId: req.userId })
            .sort({ createdAt: -1 })
            .limit(50)
            .populate('senderId', 'nickname avatar')
            .populate('postId', 'content')
            .lean();

        const result = notifications.map(n => ({
            id: n._id.toString(),
            type: n.type,
            message: n.message,
            read: n.read,
            createdAt: n.createdAt,
            sender: n.senderId ? {
                id: n.senderId._id.toString(),
                nickname: n.senderId.nickname || '',
                avatar: n.senderId.avatar || ''
            } : null,
            postId: n.postId ? n.postId._id.toString() : null,
            postContent: n.postId ? (n.postId.content?.substring(0, 50) || '') : ''
        }));

        res.json({ notifications: result });
    } catch (error) {
        console.error('Get notifications error:', error);
        res.status(500).json({ message: '服务器错误' });
    }
});

// Mark notification as read
router.put('/:id/read', auth, async (req, res) => {
    try {
        const notification = await Notification.findOneAndUpdate(
            { _id: req.params.id, recipientId: req.userId },
            { read: true },
            { new: true }
        );

        if (!notification) {
            return res.status(404).json({ message: '通知不存在' });
        }

        res.json({ success: true });
    } catch (error) {
        console.error('Mark notification read error:', error);
        res.status(500).json({ message: '服务器错误' });
    }
});

// Mark all notifications as read
router.put('/read-all', auth, async (req, res) => {
    try {
        await Notification.updateMany(
            { recipientId: req.userId, read: false },
            { read: true }
        );

        res.json({ success: true });
    } catch (error) {
        console.error('Mark all notifications read error:', error);
        res.status(500).json({ message: '服务器错误' });
    }
});

// Get unread count
router.get('/unread-count', auth, async (req, res) => {
    try {
        const count = await Notification.countDocuments({ recipientId: req.userId, read: false });
        res.json({ count });
    } catch (error) {
        console.error('Get unread count error:', error);
        res.status(500).json({ message: '服务器错误' });
    }
});

// Cleanup duplicate notifications (admin function)
router.post('/cleanup', auth, async (req, res) => {
    try {
        // Get all like notifications
        const likeNotifications = await Notification.find({ type: 'like' }).lean();
        const seenLikes = new Map();
        let deletedLikes = 0;

        for (const notif of likeNotifications) {
            const key = `${notif.recipientId.toString()}-${notif.senderId.toString()}-${notif.postId?.toString()}`;
            if (seenLikes.has(key)) {
                await Notification.deleteOne({ _id: notif._id });
                deletedLikes++;
            } else {
                seenLikes.set(key, notif._id);
            }
        }

        // Get all comment notifications
        const commentNotifications = await Notification.find({ type: 'comment' }).lean();
        const seenComments = new Map();
        let deletedComments = 0;

        for (const notif of commentNotifications) {
            const key = `${notif.recipientId.toString()}-${notif.senderId.toString()}-${notif.postId?.toString()}`;
            if (seenComments.has(key)) {
                await Notification.deleteOne({ _id: notif._id });
                deletedComments++;
            } else {
                seenComments.set(key, notif._id);
            }
        }

        res.json({
            success: true,
            message: `清理完成：删除了 ${deletedLikes} 条重复点赞通知，${deletedComments} 条重复评论通知`
        });
    } catch (error) {
        console.error('Cleanup error:', error);
        res.status(500).json({ message: '服务器错误' });
    }
});

// Fix comment counts for all posts
router.post('/fix-comment-counts', auth, async (req, res) => {
    try {
        const Post = require('../models/Post');
        const Comment = require('../models/Comment');

        const posts = await Post.find({});
        let fixed = 0;

        for (const post of posts) {
            const actualCount = await Comment.countDocuments({ postId: post._id });
            if (post.commentCount !== actualCount) {
                post.commentCount = actualCount;
                await post.save();
                fixed++;
            }
        }

        res.json({
            success: true,
            message: `修复完成：修正了 ${fixed} 个帖子的评论数`
        });
    } catch (error) {
        console.error('Fix comment counts error:', error);
        res.status(500).json({ message: '服务器错误' });
    }
});

// Delete all notifications for a user (for testing)
router.delete('/clear-all', auth, async (req, res) => {
    try {
        const result = await Notification.deleteMany({
            $or: [
                { recipientId: req.userId },
                { senderId: req.userId }
            ]
        });

        res.json({
            success: true,
            message: `删除了 ${result.deletedCount} 条通知`
        });
    } catch (error) {
        console.error('Clear notifications error:', error);
        res.status(500).json({ message: '服务器错误' });
    }
});

module.exports = router;