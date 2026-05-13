const express = require('express');
const router = express.Router();
const ChatMessage = require('../models/ChatMessage');
const ChatRoom = require('../models/ChatRoom');
const User = require('../models/User');
const auth = require('../middleware/auth');

// Helper to get sorted participant IDs
function getParticipantIds(userId1, userId2) {
    return [userId1, userId2].sort();
}

// Get or create chat room between two users
async function getOrCreateChatRoom(userId1, userId2) {
    const participantIds = getParticipantIds(userId1, userId2);
    let room = await ChatRoom.findOne({ participantIds });

    if (!room) {
        room = new ChatRoom({
            participants: [userId1, userId2],
            participantIds
        });
        await room.save();
    }
    return room;
}

// Get all chat rooms for current user
router.get('/rooms', auth, async (req, res) => {
    try {
        const rooms = await ChatRoom.find({
            participantIds: req.userId
        })
        .sort({ lastMessageAt: -1 })
        .populate('participants', 'nickname avatar');

        const roomsData = rooms.map(room => {
            const otherParticipant = room.participants.find(
                p => p._id.toString() !== req.userId
            );

            return {
                id: room._id,
                participantId: otherParticipant._id,
                nickname: otherParticipant.nickname,
                avatar: otherParticipant.avatar,
                lastMessage: room.lastMessage,
                lastMessageAt: room.lastMessageAt,
                unreadCount: room.unreadCounts.get(req.userId) || 0,
                messageLimitReached: room.messageLimitReached.get(req.userId) || false
            };
        });

        res.json({ rooms: roomsData });
    } catch (error) {
        console.error('Get chat rooms error:', error);
        res.status(500).json({ message: '服务器错误' });
    }
});

// Get chat history
router.get('/history/:roomId', auth, async (req, res) => {
    try {
        const { roomId } = req.params;
        const { limit = 50, before } = req.query;

        console.log(`[GET /history/${roomId}] userId=${req.userId}, auth verified`);

        // Verify user is in this room
        const room = await ChatRoom.findById(roomId);
        if (!room) {
            console.log(`[GET /history] Room not found: ${roomId}`);
            return res.status(404).json({ message: '聊天室不存在' });
        }
        if (!room.participantIds.includes(req.userId)) {
            console.log(`[GET /history] User ${req.userId} not in room ${roomId}`);
            return res.status(403).json({ message: '无权限访问' });
        }

        let query = { chatId: roomId };
        if (before) {
            query.timestamp = { $lt: new Date(before) };
        }

        const messages = await ChatMessage.find(query)
            .sort({ timestamp: -1 })
            .limit(parseInt(limit))
            .lean();

        console.log(`[GET /history] Found ${messages.length} messages for room ${roomId}`);

        // Transform _id to id for client compatibility
        const transformedMessages = messages.reverse().map(msg => ({
            id: msg._id.toString(),
            chatId: msg.chatId,
            from: msg.senderId,
            fromNickname: msg.senderNickname,
            content: msg.content,
            timestamp: msg.timestamp
        }));

        const response = { messages: transformedMessages };
        console.log(`[GET /history] Sending response:`, JSON.stringify(response).substring(0, 200));
        res.json(response);
    } catch (error) {
        console.error('Get chat history error:', error);
        res.status(500).json({ message: '服务器错误' });
    }
});

// Send message in chat room
router.post('/message/:roomId', auth, async (req, res) => {
    try {
        const { roomId } = req.params;
        const { content } = req.body;

        if (!content || content.trim().length === 0) {
            return res.status(400).json({ message: '消息内容不能为空' });
        }

        // Verify user is in this room
        const room = await ChatRoom.findById(roomId);
        if (!room || !room.participantIds.includes(req.userId)) {
            return res.status(403).json({ message: '无权限访问' });
        }

        // Get recipient ID
        const recipientId = room.participantIds.find(id => id !== req.userId);

        // Save message
        const chatMessage = new ChatMessage({
            chatId: roomId,
            senderId: req.userId,
            senderNickname: '',
            content: content.trim(),
            timestamp: new Date()
        });
        await chatMessage.save();

        // Update room
        room.lastMessage = content.trim().substring(0, 50);
        room.lastMessageAt = new Date();

        // Increment unread count for recipient
        const currentUnread = room.unreadCounts.get(recipientId) || 0;
        room.unreadCounts.set(recipientId, currentUnread + 1);

        await room.save();

        // Get sender info for response
        const senderUser = await User.findById(req.userId).select('nickname avatar');

        res.json({
            message: {
                id: chatMessage._id,
                chatId: roomId,
                from: req.userId,
                fromNickname: senderUser.nickname,
                content: chatMessage.content,
                timestamp: chatMessage.timestamp
            },
            messageLimitReached: room.messageLimitReached.get(req.userId) || false
        });
    } catch (error) {
        console.error('Send message error:', error);
        res.status(500).json({ message: '服务器错误' });
    }
});

// Follow/unfollow user (for chat message limit)
router.post('/follow/:userId', auth, async (req, res) => {
    try {
        const { userId } = req.params;
        const targetUser = await User.findById(userId);

        if (!targetUser) {
            return res.status(404).json({ message: '用户不存在' });
        }

        if (userId === req.userId) {
            return res.status(400).json({ message: '不能关注自己' });
        }

        const user = await User.findById(req.userId);
        const isFollowing = user.following.includes(userId);

        if (isFollowing) {
            // Unfollow
            user.following.pull(userId);
            targetUser.followers.pull(req.userId);
        } else {
            // Follow
            user.following.push(userId);
            targetUser.followers.push(req.userId);
        }

        await user.save();
        await targetUser.save();

        // If now following, clear the message limit for this user
        if (!isFollowing) {
            const chatRoom = await ChatRoom.findOne({
                participantIds: { $all: [req.userId, userId] }
            });
            if (chatRoom) {
                chatRoom.messageLimitReached.set(req.userId, false);
                await chatRoom.save();
            }
        }

        res.json({
            isFollowing: !isFollowing,
            message: !isFollowing ? '关注成功' : '取消关注成功'
        });
    } catch (error) {
        console.error('Follow error:', error);
        res.status(500).json({ message: '服务器错误' });
    }
});

// Get user info by ID (for starting chat)
router.get('/user/:userId', auth, async (req, res) => {
    try {
        const user = await User.findById(req.params.userId)
            .select('nickname avatar');

        if (!user) {
            return res.status(404).json({ message: '用户不存在' });
        }

        // Check if current user is following this user
        const currentUser = await User.findById(req.userId);
        const isFollowing = currentUser.following.includes(req.params.userId);

        res.json({
            user: {
                id: user._id,
                nickname: user.nickname,
                avatar: user.avatar,
                isFollowing
            }
        });
    } catch (error) {
        console.error('Get user error:', error);
        res.status(500).json({ message: '服务器错误' });
    }
});

// Start or get chat room with user
router.post('/start/:userId', auth, async (req, res) => {
    try {
        const { userId } = req.params;

        if (userId === req.userId) {
            return res.status(400).json({ message: '不能和自己聊天' });
        }

        const targetUser = await User.findById(userId);
        if (!targetUser) {
            return res.status(404).json({ message: '用户不存在' });
        }

        // Get or create chat room
        const room = await getOrCreateChatRoom(req.userId, userId);

        res.json({
            roomId: room._id,
            participantId: userId,
            nickname: targetUser.nickname,
            avatar: targetUser.avatar
        });
    } catch (error) {
        console.error('Start chat error:', error);
        res.status(500).json({ message: '服务器错误' });
    }
});

// Mark messages as read
router.post('/read/:roomId', auth, async (req, res) => {
    try {
        const { roomId } = req.params;
        const room = await ChatRoom.findById(roomId);

        if (!room || !room.participantIds.includes(req.userId)) {
            return res.status(403).json({ message: '无权限访问' });
        }

        // Reset unread count for current user
        room.unreadCounts.set(req.userId, 0);
        await room.save();

        res.json({ success: true });
    } catch (error) {
        console.error('Mark read error:', error);
        res.status(500).json({ message: '服务器错误' });
    }
});

module.exports = router;
