const express = require('express');
const auth = require('../middleware/auth');
const PartyRoom = require('../models/PartyRoom');
const PartyMessage = require('../models/PartyMessage');
const User = require('../models/User');

const router = express.Router();

router.post('/rooms', auth, async (req, res) => {
    try {
        const name = (req.body?.name || '').trim();
        const subtitle = (req.body?.subtitle || '').trim();

        if (!name) {
            return res.status(400).json({ message: '聊天室名称不能为空' });
        }
        if (name.length > 60) {
            return res.status(400).json({ message: '聊天室名称不能超过60个字符' });
        }
        if (subtitle.length > 120) {
            return res.status(400).json({ message: '简介不能超过120个字符' });
        }

        const existing = await PartyRoom.findOne({ name }).lean();
        if (existing) {
            return res.status(400).json({ message: '聊天室名称已存在' });
        }

        const room = await PartyRoom.create({
            name,
            subtitle,
            onlineCount: 0,
            heat: 0
        });

        res.status(201).json({
            message: '创建成功',
            room: {
                id: room._id.toString(),
                name: room.name,
                subtitle: room.subtitle || '',
                onlineCount: 0,
                heat: 0,
                messageCount: 0,
                lastMessage: '',
                lastMessageAt: null
            }
        });
    } catch (error) {
        console.error('Create party room error:', error);
        res.status(500).json({ message: '创建聊天室失败' });
    }
});

router.get('/rooms', auth, async (req, res) => {
    try {
        const rooms = await PartyRoom.find().sort({ createdAt: -1 }).lean();
        const roomIds = rooms.map(r => r._id);

        if (roomIds.length === 0) {
            return res.json({ rooms: [] });
        }

        const latest = await PartyMessage.aggregate([
            { $match: { roomId: { $in: roomIds } } },
            { $sort: { createdAt: -1 } },
            { $group: { _id: '$roomId', message: { $first: '$content' }, createdAt: { $first: '$createdAt' } } }
        ]);
        const latestMap = new Map(latest.map(m => [m._id.toString(), m]));

        const counts = await PartyMessage.aggregate([
            { $match: { roomId: { $in: roomIds } } },
            { $group: { _id: '$roomId', count: { $sum: 1 } } }
        ]);
        const countMap = new Map(counts.map(c => [c._id.toString(), c.count]));

        // 在线人数：最近5分钟内在该房间发过言的去重用户数（真实活跃人数）
        const activeSince = new Date(Date.now() - 5 * 60 * 1000);
        const activeUsers = await PartyMessage.aggregate([
            { $match: { roomId: { $in: roomIds }, createdAt: { $gte: activeSince } } },
            { $group: { _id: { roomId: '$roomId', senderId: '$senderId' } } },
            { $group: { _id: '$_id.roomId', count: { $sum: 1 } } }
        ]);
        const activeMap = new Map(activeUsers.map(a => [a._id.toString(), a.count]));

        res.json({
            rooms: rooms.map(r => {
                const roomId = r._id.toString();
                const messageCount = countMap.get(roomId) || 0;
                return {
                id: r._id.toString(),
                name: r.name,
                subtitle: r.subtitle || '',
                onlineCount: activeMap.get(roomId) || 0,
                heat: messageCount,
                messageCount,
                lastMessage: latestMap.get(roomId)?.message || '',
                lastMessageAt: latestMap.get(roomId)?.createdAt || null
            };
            })
        });
    } catch (error) {
        console.error('Get party rooms error:', error);
        res.status(500).json({ message: '获取聊天室失败' });
    }
});

router.get('/messages/:roomId', auth, async (req, res) => {
    try {
        const { roomId } = req.params;
        const limit = Math.min(Math.max(parseInt(req.query.limit, 10) || 80, 1), 200);

        const room = await PartyRoom.findById(roomId);
        if (!room) {
            return res.status(404).json({ message: '聊天室不存在' });
        }

        const messages = await PartyMessage.find({ roomId })
            .sort({ createdAt: -1 })
            .limit(limit)
            .lean();

        res.json({
            room: {
                id: room._id.toString(),
                name: room.name,
                subtitle: room.subtitle || ''
            },
            messages: messages.reverse().map(m => ({
                id: m._id.toString(),
                roomId: m.roomId.toString(),
                senderId: m.senderId.toString(),
                senderNickname: m.senderNickname,
                content: m.content,
                createdAt: m.createdAt
            }))
        });
    } catch (error) {
        console.error('Get party messages error:', error);
        res.status(500).json({ message: '获取聊天记录失败' });
    }
});

router.post('/messages/:roomId', auth, async (req, res) => {
    try {
        const { roomId } = req.params;
        const { content } = req.body;
        const trimmed = (content || '').trim();
        if (!trimmed) {
            return res.status(400).json({ message: '消息不能为空' });
        }

        const room = await PartyRoom.findById(roomId);
        if (!room) {
            return res.status(404).json({ message: '聊天室不存在' });
        }

        const sender = await User.findById(req.userId).select('nickname').lean();
        if (!sender) {
            return res.status(404).json({ message: '用户不存在' });
        }

        const message = await PartyMessage.create({
            roomId,
            senderId: req.userId,
            senderNickname: (sender.nickname || req.username || '匿名用户').trim(),
            content: trimmed
        });

        res.status(201).json({
            message: {
                id: message._id.toString(),
                roomId: message.roomId.toString(),
                senderId: message.senderId.toString(),
                senderNickname: message.senderNickname,
                content: message.content,
                createdAt: message.createdAt
            }
        });
    } catch (error) {
        console.error('Send party message error:', error);
        res.status(500).json({ message: '发送消息失败' });
    }
});

module.exports = router;
