const express = require('express');
const auth = require('../middleware/auth');
const { PartyRoom, PartyMessage } = require('../models/Party');

const router = express.Router();

// Get all party rooms
router.get('/rooms', auth, async (req, res) => {
    try {
        const rooms = await PartyRoom.find()
            .sort({ heat: -1, lastMessageAt: -1 })
            .limit(50)
            .select('_id name subtitle participants maxParticipants onlineCount heat messageCount lastMessage lastMessageAt')
            .lean();

        res.json({
            rooms: rooms.map(room => ({
                id: room._id.toString(),
                name: room.name,
                subtitle: room.subtitle || '',
                participantCount: room.participants?.length || 0,
                maxParticipants: room.maxParticipants || 6,
                onlineCount: room.onlineCount || 0,
                heat: room.heat || 0,
                messageCount: room.messageCount || 0,
                lastMessage: room.lastMessage || '',
                lastMessageAt: room.lastMessageAt ? room.lastMessageAt.toISOString() : null
            }))
        });
    } catch (error) {
        console.error('Get party rooms error:', error);
        res.status(500).json({ message: '服务器错误' });
    }
});

// Create a new party room
router.post('/rooms', auth, async (req, res) => {
    try {
        const { name, subtitle } = req.body;

        if (!name || name.trim().length === 0) {
            return res.status(400).json({ message: '聊天室名称不能为空' });
        }

        if (name.length > 50) {
            return res.status(400).json({ message: '聊天室名称不能超过50字' });
        }

        const User = require('../models/User');
        const creator = await User.findById(req.userId).select('nickname').lean();
        const creatorName = creator?.nickname || '匿名用户';

        const room = new PartyRoom({
            name: name.trim(),
            subtitle: subtitle?.trim() || '',
            creatorId: req.userId,
            participants: [req.userId]  // Creator automatically joins
        });

        await room.save();

        // Add system message for room creation
        const systemMessage = new PartyMessage({
            roomId: room._id,
            senderId: 'system',
            senderNickname: '系统',
            content: `${creatorName} 创建了聊天室`,
            isSystemMessage: true
        });
        await systemMessage.save();

        res.status(201).json({
            message: '聊天室创建成功',
            room: {
                id: room._id.toString(),
                name: room.name,
                subtitle: room.subtitle || '',
                participantCount: room.participants.length,
                maxParticipants: room.maxParticipants,
                onlineCount: room.onlineCount || 0,
                heat: room.heat || 0,
                messageCount: room.messageCount || 0,
                lastMessage: room.lastMessage || '',
                lastMessageAt: room.lastMessageAt ? room.lastMessageAt.toISOString() : null
            }
        });
    } catch (error) {
        console.error('Create party room error:', error);
        res.status(500).json({ message: '服务器错误' });
    }
});

// Join a party room
router.post('/rooms/:roomId/join', auth, async (req, res) => {
    try {
        const { roomId } = req.params;
        const room = await PartyRoom.findById(roomId);

        if (!room) {
            return res.status(404).json({ message: '聊天室不存在' });
        }

        // Check if already a participant (compare both as strings)
        if (room.participants.some(p => p.toString() === req.userId)) {
            return res.json({
                message: '已经在聊天室中',
                room: {
                    id: room._id.toString(),
                    name: room.name,
                    subtitle: room.subtitle || '',
                    participantCount: room.participants.length,
                    maxParticipants: room.maxParticipants
                }
            });
        }

        // Check if room is full
        if (room.participants.length >= room.maxParticipants) {
            return res.status(400).json({ message: '聊天室已满（最多6人）' });
        }

        // Get user nickname
        const User = require('../models/User');
        const user = await User.findById(req.userId).select('nickname').lean();
        const userName = user?.nickname || '匿名用户';

        room.participants.push(req.userId);
        room.onlineCount = room.participants.length;
        await room.save();

        // Add system message for join
        const joinMessage = new PartyMessage({
            roomId: room._id,
            senderId: 'system',
            senderNickname: '系统',
            content: `${userName} 加入了聊天室`,
            isSystemMessage: true
        });
        await joinMessage.save();

        res.json({
            message: '加入成功',
            room: {
                id: room._id.toString(),
                name: room.name,
                subtitle: room.subtitle || '',
                participantCount: room.participants.length,
                maxParticipants: room.maxParticipants
            }
        });
    } catch (error) {
        console.error('Join party room error:', error);
        res.status(500).json({ message: '服务器错误' });
    }
});

// Leave a party room
router.post('/rooms/:roomId/leave', auth, async (req, res) => {
    try {
        const { roomId } = req.params;
        const room = await PartyRoom.findById(roomId);

        if (!room) {
            return res.status(404).json({ message: '聊天室不存在' });
        }

        // Get user nickname before removing
        const User = require('../models/User');
        const user = await User.findById(req.userId).select('nickname').lean();
        const userName = user?.nickname || '匿名用户';

        room.participants = room.participants.filter(
            p => p.toString() !== req.userId
        );

        // Add system message for leave BEFORE checking if room will be empty
        const leaveMessage = new PartyMessage({
            roomId: room._id,
            senderId: 'system',
            senderNickname: '系统',
            content: `${userName} 退出了聊天室`,
            isSystemMessage: true
        });
        await leaveMessage.save();

        // Check if room should be destroyed (participants < 1)
        if (room.participants.length < 1) {
            // Delete all messages in this room
            await PartyMessage.deleteMany({ roomId: room._id });
            // Delete the room
            await PartyRoom.deleteOne({ _id: room._id });
            return res.json({ message: '已离开聊天室，聊天室已解散' });
        }

        room.onlineCount = room.participants.length;
        await room.save();

        res.json({ message: '已离开聊天室' });
    } catch (error) {
        console.error('Leave party room error:', error);
        res.status(500).json({ message: '服务器错误' });
    }
});

// Get party room details
router.get('/rooms/:roomId', auth, async (req, res) => {
    try {
        const { roomId } = req.params;
        const room = await PartyRoom.findById(roomId)
            .populate('participants', 'nickname avatar')
            .lean();

        if (!room) {
            return res.status(404).json({ message: '聊天室不存在' });
        }

        const User = require('../models/User');
        const currentUser = await User.findById(req.userId).select('nickname avatar').lean();
        const isParticipant = room.participants.some(
            p => p._id.toString() === req.userId
        );

        res.json({
            room: {
                id: room._id.toString(),
                name: room.name,
                subtitle: room.subtitle || '',
                participantCount: room.participants.length,
                maxParticipants: room.maxParticipants,
                participants: room.participants.map(p => ({
                    id: p._id.toString(),
                    nickname: p.nickname,
                    avatar: p.avatar
                })),
                isParticipant: isParticipant,
                isCreator: room.creatorId.toString() === req.userId
            }
        });
    } catch (error) {
        console.error('Get party room error:', error);
        res.status(500).json({ message: '服务器错误' });
    }
});

// Get messages from a party room
router.get('/messages/:roomId', auth, async (req, res) => {
    try {
        const { roomId } = req.params;
        const limit = parseInt(req.query.limit) || 80;

        const room = await PartyRoom.findById(roomId).select('_id name subtitle').lean();

        if (!room) {
            return res.status(404).json({ message: '聊天室不存在' });
        }

        const messages = await PartyMessage.find({ roomId })
            .sort({ createdAt: -1 })
            .limit(limit)
            .select('_id roomId senderId senderNickname content createdAt')
            .lean();

        res.json({
            room: {
                id: room._id.toString(),
                name: room.name,
                subtitle: room.subtitle || ''
            },
            messages: messages.reverse().map(msg => ({
                id: msg._id.toString(),
                roomId: msg.roomId.toString(),
                senderId: msg.senderId.toString(),
                senderNickname: msg.senderNickname,
                content: msg.content,
                createdAt: msg.createdAt.toISOString(),
                isSystemMessage: msg.isSystemMessage || false
            }))
        });
    } catch (error) {
        console.error('Get party messages error:', error);
        res.status(500).json({ message: '服务器错误' });
    }
});

// Send a message to a party room
router.post('/messages/:roomId', auth, async (req, res) => {
    try {
        const { roomId } = req.params;
        const { content } = req.body;

        if (!content || content.trim().length === 0) {
            return res.status(400).json({ message: '消息内容不能为空' });
        }

        if (content.length > 500) {
            return res.status(400).json({ message: '消息内容不能超过500字' });
        }

        const room = await PartyRoom.findById(roomId);

        if (!room) {
            return res.status(404).json({ message: '聊天室不存在' });
        }

        // Get user info
        const User = require('../models/User');
        const user = await User.findById(req.userId).select('nickname').lean();

        const message = new PartyMessage({
            roomId,
            senderId: req.userId,
            senderNickname: user?.nickname || '匿名用户',
            content: content.trim()
        });

        await message.save();

        // Update room stats
        room.messageCount += 1;
        room.heat += 1;
        room.lastMessage = content.substring(0, 50);
        room.lastMessageAt = new Date();
        await room.save();

        res.status(201).json({
            message: {
                id: message._id.toString(),
                roomId: message.roomId.toString(),
                senderId: message.senderId.toString(),
                senderNickname: message.senderNickname,
                content: message.content,
                createdAt: message.createdAt.toISOString()
            }
        });
    } catch (error) {
        console.error('Send party message error:', error);
        res.status(500).json({ message: '服务器错误' });
    }
});

module.exports = router;
