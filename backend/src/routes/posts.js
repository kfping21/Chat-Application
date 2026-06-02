const express = require('express');
const auth = require('../middleware/auth');
const Post = require('../models/Post');
const Comment = require('../models/Comment');
const User = require('../models/User');
const Notification = require('../models/Notification');

const router = express.Router();

// Create a new post
router.post('/', auth, async (req, res) => {
    try {
        const { content, mood } = req.body;

        if (!content || content.trim().length === 0) {
            return res.status(400).json({ message: '内容不能为空' });
        }

        if (content.length > 2000) {
            return res.status(400).json({ message: '内容不能超过2000字' });
        }

        const post = new Post({
            userId: req.userId,
            content: content.trim(),
            mood: mood || '平静'
        });

        await post.save();
        res.status(201).json({ message: '发布成功', post });
    } catch (error) {
        console.error('Create post error:', error);
        res.status(500).json({ message: '服务器错误' });
    }
});

// Get feed (all posts, newest first) with pagination
router.get('/feed', auth, async (req, res) => {
    try {
        const page = parseInt(req.query.page) || 1;
        const limit = parseInt(req.query.limit) || 10;
        const skip = (page - 1) * limit;
        const type = req.query.type || 'all';

        // Get current user's following list if type is 'following'
        let followingIds = [];
        if (type === 'following') {
            const currentUser = await User.findById(req.userId).select('following').lean();
            followingIds = currentUser?.following || [];
        }

        // Build query
        const query = {};
        if (type === 'following' && followingIds.length > 0) {
            query.userId = { $in: followingIds };
        } else if (type === 'following' && followingIds.length === 0) {
            // No following, return empty feed
            return res.json({
                posts: [],
                pagination: { page, limit, total: 0, totalPages: 0, hasMore: false }
            });
        }

        // Get total count for pagination info
        const total = await Post.countDocuments(query);

        const posts = await Post.find(query)
            .sort({ createdAt: -1 })
            .skip(skip)
            .limit(limit)
            .select('_id content mood likes createdAt userId likedBy')
            .lean();

        // Fetch users in batch
        const userIds = [...new Set(posts.map(p => p.userId.toString()))];
        const users = await User.find({ _id: { $in: userIds } }).select('nickname avatar').lean();
        const userMap = new Map(users.map(u => [u._id.toString(), u]));

        // Get comment counts for all posts in batch
        const postIds = posts.map(p => p._id.toString());
        const commentCounts = await Comment.aggregate([
            { $match: { postId: { $in: posts.map(p => p._id) } } },
            { $group: { _id: '$postId', count: { $sum: 1 } } }
        ]);
        const commentCountMap = new Map(commentCounts.map(c => [c._id.toString(), c.count]));

        const result = posts.map(post => {
            const user = userMap.get(post.userId.toString());
            const commentCount = commentCountMap.get(post._id.toString()) || 0;
            return {
                id: post._id.toString(),
                content: post.content,
                mood: post.mood,
                likes: post.likes,
                commentCount: commentCount,
                createdAt: post.createdAt,
                user: user ? {
                    id: user._id.toString(),
                    nickname: user.nickname,
                    avatar: user.avatar
                } : null,
                isLiked: post.likedBy.some(id => id.toString() === req.userId)
            };
        });

        res.json({
            posts: result,
            pagination: {
                page,
                limit,
                total,
                totalPages: Math.ceil(total / limit),
                hasMore: skip + posts.length < total
            }
        });
    } catch (error) {
        console.error('Get feed error:', error);
        res.status(500).json({ message: '服务器错误' });
    }
});

// Get user's own posts
router.get('/my', auth, async (req, res) => {
    try {
        const posts = await Post.find({ userId: req.userId })
            .sort({ createdAt: -1 })
            .limit(10)
            .select('_id content mood likes createdAt')
            .lean();

        // Get comment counts for all posts in batch
        const commentCounts = await Comment.aggregate([
            { $match: { postId: { $in: posts.map(p => p._id) } } },
            { $group: { _id: '$postId', count: { $sum: 1 } } }
        ]);
        const commentCountMap = new Map(commentCounts.map(c => [c._id.toString(), c.count]));

        res.json({
            posts: posts.map(post => ({
                id: post._id.toString(),
                content: post.content,
                mood: post.mood,
                likes: post.likes,
                commentCount: commentCountMap.get(post._id.toString()) || 0,
                createdAt: post.createdAt
            }))
        });
    } catch (error) {
        console.error('Get my posts error:', error);
        res.status(500).json({ message: '服务器错误' });
    }
});

// Get user's liked posts
router.get('/liked', auth, async (req, res) => {
    try {
        const posts = await Post.find({ likedBy: req.userId })
            .sort({ createdAt: -1 })
            .limit(10)
            .select('_id content mood likes createdAt userId likedBy')
            .lean();

        // Fetch users in batch
        const userIds = [...new Set(posts.map(p => p.userId.toString()))];
        const users = await User.find({ _id: { $in: userIds } }).select('nickname avatar').lean();
        const userMap = new Map(users.map(u => [u._id.toString(), u]));

        // Get comment counts for all posts in batch
        const commentCounts = await Comment.aggregate([
            { $match: { postId: { $in: posts.map(p => p._id) } } },
            { $group: { _id: '$postId', count: { $sum: 1 } } }
        ]);
        const commentCountMap = new Map(commentCounts.map(c => [c._id.toString(), c.count]));

        const result = posts.map(post => {
            const user = userMap.get(post.userId.toString());
            return {
                id: post._id.toString(),
                content: post.content,
                mood: post.mood,
                likes: post.likes,
                commentCount: commentCountMap.get(post._id.toString()) || 0,
                createdAt: post.createdAt,
                user: user ? {
                    id: user._id.toString(),
                    nickname: user.nickname,
                    avatar: user.avatar
                } : null,
                isLiked: true
            };
        });

        res.json({ posts: result });
    } catch (error) {
        console.error('Get liked posts error:', error);
        res.status(500).json({ message: '服务器错误' });
    }
});

// Get posts by user ID
router.get('/user/:userId', auth, async (req, res) => {
    try {
        const posts = await Post.find({ userId: req.params.userId })
            .sort({ createdAt: -1 })
            .limit(10)
            .select('_id content mood likes createdAt userId likedBy')
            .lean();

        // Get user info
        const user = await User.findById(req.params.userId).select('nickname avatar').lean();

        // Get comment counts for all posts in batch
        const commentCounts = await Comment.aggregate([
            { $match: { postId: { $in: posts.map(p => p._id) } } },
            { $group: { _id: '$postId', count: { $sum: 1 } } }
        ]);
        const commentCountMap = new Map(commentCounts.map(c => [c._id.toString(), c.count]));

        res.json({
            posts: posts.map(post => ({
                id: post._id.toString(),
                content: post.content,
                mood: post.mood,
                likes: post.likes,
                commentCount: commentCountMap.get(post._id.toString()) || 0,
                createdAt: post.createdAt,
                user: user ? {
                    id: user._id.toString(),
                    nickname: user.nickname,
                    avatar: user.avatar
                } : null,
                isLiked: post.likedBy.some(id => id.toString() === req.userId)
            }))
        });
    } catch (error) {
        console.error('Get user posts error:', error);
        res.status(500).json({ message: '服务器错误' });
    }
});

// Get single post with comments
router.get('/:id', auth, async (req, res) => {
    try {
        const post = await Post.findById(req.params.id)
            .select('_id content mood likes createdAt userId likedBy')
            .lean();

        if (!post) {
            return res.status(404).json({ message: '帖子不存在' });
        }

        // Get user info
        const user = await User.findById(post.userId).select('nickname avatar').lean();

        // Get comments
        const comments = await Comment.find({ postId: post._id })
            .sort({ createdAt: 1 })
            .select('_id content createdAt userId')
            .lean();

        // Get comment users
        const commentUserIds = [...new Set(comments.map(c => c.userId.toString()))];
        const commentUsers = await User.find({ _id: { $in: commentUserIds } }).select('nickname avatar').lean();
        const commentUserMap = new Map(commentUsers.map(u => [u._id.toString(), u]));

        res.json({
            post: {
                id: post._id.toString(),
                content: post.content,
                mood: post.mood,
                likes: post.likes,
                createdAt: post.createdAt,
                user: user ? {
                    id: user._id.toString(),
                    nickname: user.nickname,
                    avatar: user.avatar
                } : null,
                isLiked: post.likedBy.some(id => id.toString() === req.userId)
            },
            comments: comments.map((c, i) => {
                const cUser = commentUserMap.get(c.userId.toString());
                return {
                    id: c._id.toString(),
                    content: c.content,
                    createdAt: c.createdAt,
                    user: cUser ? {
                        id: cUser._id.toString(),
                        nickname: cUser.nickname,
                        avatar: cUser.avatar
                    } : null
                };
            })
        });
    } catch (error) {
        console.error('Get post error:', error);
        res.status(500).json({ message: '服务器错误' });
    }
});

// Like/unlike a post
router.post('/:id/like', auth, async (req, res) => {
    try {
        const post = await Post.findById(req.params.id);

        if (!post) {
            return res.status(404).json({ message: '帖子不存在' });
        }

        const userIdStr = req.userId;
        const isLiked = post.likedBy.some(id => id.toString() === userIdStr);

        if (isLiked) {
            post.likedBy = post.likedBy.filter(id => id.toString() !== userIdStr);
            post.likes = Math.max(0, post.likes - 1);

            // Remove like notification when unliking
            await Notification.deleteOne({
                recipientId: post.userId,
                senderId: userIdStr,
                type: 'like',
                postId: post._id
            });
        } else {
            post.likedBy.push(req.userId);
            post.likes += 1;

            // Create notification for post author (if not self)
            if (post.userId.toString() !== userIdStr) {
                const liker = await User.findById(userIdStr).select('nickname').lean();
                const notification = new Notification({
                    recipientId: post.userId,
                    senderId: userIdStr,
                    type: 'like',
                    postId: post._id,
                    message: liker ? `${liker.nickname || '匿名用户'} 赞了你的秘密` : '有人赞了你的秘密'
                });
                await notification.save();
            }
        }

        await post.save();

        res.json({ likes: post.likes, isLiked: !isLiked });
    } catch (error) {
        console.error('Like error:', error);
        res.status(500).json({ message: '服务器错误' });
    }
});

// Add comment to post
router.post('/:id/comment', auth, async (req, res) => {
    try {
        const { content } = req.body;

        if (!content || content.trim().length === 0) {
            return res.status(400).json({ message: '评论内容不能为空' });
        }

        const post = await Post.findById(req.params.id);

        if (!post) {
            return res.status(404).json({ message: '帖子不存在' });
        }

        const comment = new Comment({
            postId: post._id,
            userId: req.userId,
            content: content.trim()
        });

        await comment.save();

        // Update post comment count
        await Post.findByIdAndUpdate(post._id, { $inc: { commentCount: 1 } });

        // Get user info
        const user = await User.findById(req.userId).select('nickname avatar').lean();

        // Create notification for post author (if not self)
        if (post.userId.toString() !== req.userId) {
            const notification = new Notification({
                recipientId: post.userId,
                senderId: req.userId,
                type: 'comment',
                postId: post._id,
                commentId: comment._id,
                message: user ? `${user.nickname || '匿名用户'} 回复了你的秘密` : '有人回复了你的秘密'
            });
            await notification.save();
        }

        res.status(201).json({
            comment: {
                id: comment._id.toString(),
                content: comment.content,
                createdAt: comment.createdAt,
                user: user ? {
                    id: user._id.toString(),
                    nickname: user.nickname,
                    avatar: user.avatar
                } : null
            }
        });
    } catch (error) {
        console.error('Add comment error:', error);
        res.status(500).json({ message: '服务器错误' });
    }
});

// Delete a post
router.delete('/:id', auth, async (req, res) => {
    try {
        const post = await Post.findById(req.params.id);

        if (!post) {
            return res.status(404).json({ message: '帖子不存在' });
        }

        // Check ownership
        if (post.userId.toString() !== req.userId) {
            return res.status(403).json({ message: '无权删除此帖子' });
        }

        // Delete the post
        await Post.deleteOne({ _id: post._id });

        // Delete associated comments
        await Comment.deleteMany({ postId: post._id });

        // Delete associated notifications
        await Notification.deleteMany({ postId: post._id });

        res.json({ message: '删除成功' });
    } catch (error) {
        console.error('Delete post error:', error);
        res.status(500).json({ message: '服务器错误' });
    }
});

// Get user's comments
router.get('/comments/my', auth, async (req, res) => {
    try {
        const comments = await Comment.find({ userId: req.userId })
            .sort({ createdAt: -1 })
            .limit(10)
            .select('_id content createdAt postId')
            .lean();

        // Get post info for each comment
        const postIds = [...new Set(comments.map(c => c.postId.toString()))];
        const posts = await Post.find({ _id: { $in: postIds } }).select('_id content mood').lean();
        const postMap = new Map(posts.map(p => [p._id.toString(), p]));

        const result = comments.map(comment => {
            const post = postMap.get(comment.postId.toString());
            return {
                id: comment._id.toString(),
                content: comment.content,
                createdAt: comment.createdAt,
                postId: comment.postId.toString(),
                post: post ? {
                    id: post._id.toString(),
                    content: post.content,
                    mood: post.mood
                } : null
            };
        });

        res.json({ comments: result });
    } catch (error) {
        console.error('Get my comments error:', error);
        res.status(500).json({ message: '服务器错误' });
    }
});

module.exports = router;