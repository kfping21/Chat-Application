const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const cors = require('cors');
const compression = require('compression');
const path = require('path');
require('dotenv').config();
const connectDB = require('./config/db');
const authRoutes = require('./routes/auth');
const userRoutes = require('./routes/user');
const postsRoutes = require('./routes/posts');
const whisperRoutes = require('./routes/whisper');
const notificationRoutes = require('./routes/notifications');
const partyRoutes = require('./routes/party');
const ChatMessage = require('./models/ChatMessage');
const ChatRoom = require('./models/ChatRoom');
const User = require('./models/User');

const app = express();
const server = http.createServer(app);

// Enable gzip compression
app.use(compression());

// Socket.IO setup for real-time whisper chat
const io = new Server(server, {
    cors: {
        origin: '*',
        methods: ['GET', 'POST']
    },
    pingTimeout: 60000,
    pingInterval: 25000
});

// Middleware
app.use(cors());
app.use(express.json());

// 静态文件服务 - 本地存储的图片（极快）
app.use('/uploads', express.static(path.join(__dirname, '../uploads')));

// Connect to MongoDB
connectDB();

// Routes
app.use('/api/auth', authRoutes);
app.use('/api/user', userRoutes);
app.use('/api/posts', postsRoutes);
app.use('/api/whisper', whisperRoutes);
app.use('/api/notifications', notificationRoutes);
app.use('/api/party', partyRoutes);

// Health check
app.get('/health', (req, res) => {
    res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

// Get online users (for whisper chat online status) - no auth needed
app.get('/api/online-users', (req, res) => {
    res.json({ onlineUsers: Array.from(onlineUsers.keys()) });
});

// Secret admin route to clear all data
const Post = require('./models/Post');
const Comment = require('./models/Comment');
const Notification = require('./models/Notification');
const { PartyRoom, PartyMessage } = require('./models/Party');

app.post('/api/admin/clear-all', async (req, res) => {
    try {
        const { secret } = req.body;
        if (secret !== 'treehole_clear_2026') {
            return res.status(401).json({ message: 'Unauthorized' });
        }

        await Promise.all([
            User.deleteMany({}),
            Post.deleteMany({}),
            Comment.deleteMany({}),
            ChatRoom.deleteMany({}),
            ChatMessage.deleteMany({}),
            Notification.deleteMany({}),
            PartyRoom.deleteMany({}),
            PartyMessage.deleteMany({})
        ]);

        // Recreate default user
        await recreateDefaultUser();

        res.json({ message: 'All data cleared and default user created', timestamp: new Date().toISOString() });
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

// Secret admin route to clear party rooms/messages only
app.post('/api/admin/clear-party', async (req, res) => {
    try {
        const { secret } = req.body;
        if (secret !== 'treehole_clear_2026') {
            return res.status(401).json({ message: 'Unauthorized' });
        }

        const [messagesResult, roomsResult] = await Promise.all([
            PartyMessage.deleteMany({}),
            PartyRoom.deleteMany({})
        ]);

        res.json({
            message: 'Party data cleared',
            deletedMessages: messagesResult.deletedCount,
            deletedRooms: roomsResult.deletedCount,
            timestamp: new Date().toISOString()
        });
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

// Recreate default user after clear
async function recreateDefaultUser() {
    try {
        const existing = await User.findOne({ username: 'panjiawei' });
        if (!existing) {
            const defaultUser = new User({
                username: 'panjiawei',
                password: '123456',
                nickname: '潘嘉伟'
            });
            await defaultUser.save();
            console.log('✅ Default user recreated: panjiawei / 123456');
        }
    } catch (e) {
        console.error('Failed to recreate default user:', e);
    }
}

// Socket.IO connection handling for whisper chat
const onlineUsers = new Map();

// Helper to get sorted participant IDs
function getParticipantIds(userId1, userId2) {
    return [userId1, userId2].sort().join('_');
}

io.on('connection', (socket) => {
    console.log('Socket.IO 用户连接:', socket.id);

    // User comes online
    socket.on('user-online', (userId) => {
        onlineUsers.set(userId, socket.id);
        socket.userId = userId;
        io.emit('users-online', Array.from(onlineUsers.keys()));
        console.log(`用户 ${userId} 上线`);
    });

    // Debug: print all events received
    socket.onAny((event, ...args) => {
        console.log(`[Server Received] Event: ${event}, Args:`, args);
    });

    // Join chat room
    socket.on('join-room', async (data) => {
        const roomId = typeof data === 'string' ? data : data.roomId;
        const userId = typeof data === 'string' ? null : data.userId;
        socket.join(roomId);
        socket.currentRoom = roomId;
        if (userId) socket.currentUserId = userId;
        console.log(`用户 ${userId || socket.id} 加入房间 ${roomId}`);
    });

    // Leave chat room
    socket.on('leave-room', (data) => {
        const roomId = typeof data === 'string' ? data : data.roomId;
        socket.leave(roomId);
        console.log(`用户离开房间 ${roomId}`);
    });

    // Send message in chat room
    socket.on('send-message', async (data) => {
        const { roomId, senderId, senderNickname, content } = data;
        console.log(`[send-message] roomId=${roomId}, senderId=${senderId}, content=${content}`);

        try {
            // Verify room exists and user is in it
            const room = await ChatRoom.findById(roomId);
            if (!room) {
                console.log(`[send-message] Room not found: ${roomId}`);
                socket.emit('message-error', { message: '聊天室不存在' });
                return;
            }
            if (!room.participantIds.includes(senderId)) {
                console.log(`[send-message] User not in room: ${senderId}`);
                socket.emit('message-error', { message: '无权限发送' });
                return;
            }

            // Save message to database
            console.log(`[send-message] Saving message to database`);
            const chatMessage = new ChatMessage({
                chatId: roomId,
                senderId: senderId,
                senderNickname: senderNickname || '匿名',
                content: content,
                timestamp: new Date()
            });
            await chatMessage.save();
            console.log(`[send-message] Message saved: ${chatMessage._id}`);

            // Update room
            room.lastMessage = content.substring(0, 50);
            room.lastMessageAt = new Date();

            // Get recipient ID
            const recipientId = room.participantIds.find(id => id !== senderId);

            // Increment unread count for recipient
            const currentUnread = room.unreadCounts.get(recipientId) || 0;
            room.unreadCounts.set(recipientId, currentUnread + 1);

            await room.save();

            // Broadcast to other users in the room (not sender)
            const messageData = {
                id: chatMessage._id,
                chatId: roomId,
                from: senderId,
                fromNickname: senderNickname,
                content: content,
                timestamp: chatMessage.timestamp,
                messageLimitReached: false
            };

            // Broadcast to all users in the room (including sender for confirmation)
            io.to(roomId).emit('new-message', messageData);
            console.log(`[send-message] Broadcasted to room ${roomId}`);

            // If recipient is online, send them a notification
            const recipientSocketId = onlineUsers.get(recipientId);
            if (recipientSocketId) {
                io.to(recipientSocketId).emit('unread-update', {
                    roomId,
                    unreadCount: room.unreadCounts.get(recipientId)
                });
            }
        } catch (error) {
            console.error('发送消息失败:', error);
            socket.emit('message-error', { message: '发送失败' });
        }
    });

    // Mark messages as read
    socket.on('mark-read', async (data) => {
        const { roomId, userId } = data;
        try {
            const room = await ChatRoom.findById(roomId);
            if (room && room.participantIds.includes(userId)) {
                room.unreadCounts.set(userId, 0);
                await room.save();

                // Notify other participant
                socket.to(roomId).emit('read-receipt', { roomId, userId });
            }
        } catch (error) {
            console.error('Mark read error:', error);
        }
    });

    // Follow user
    socket.on('follow-user', async (data) => {
        const { currentUserId, targetUserId } = data;
        try {
            const currentUser = await User.findById(currentUserId);
            const targetUser = await User.findById(targetUserId);

            if (!currentUser || !targetUser) {
                socket.emit('follow-error', { message: '用户不存在' });
                return;
            }

            const isFollowing = currentUser.following.includes(targetUserId);

            if (isFollowing) {
                currentUser.following.pull(targetUserId);
                targetUser.followers.pull(currentUserId);
            } else {
                currentUser.following.push(targetUserId);
                targetUser.followers.push(currentUserId);
            }

            await currentUser.save();
            await targetUser.save();

            // If now following, clear the message limit
            if (!isFollowing) {
                const chatRoom = await ChatRoom.findOne({
                    participantIds: { $all: [currentUserId, targetUserId] }
                });
                if (chatRoom) {
                    chatRoom.messageLimitReached.set(currentUserId, false);
                    await chatRoom.save();

                    // Notify user that message limit is lifted
                    socket.emit('message-limit-lifted', { roomId: chatRoom._id });
                }
            }

            socket.emit('follow-updated', {
                isFollowing: !isFollowing,
                targetUserId
            });
        } catch (error) {
            console.error('Follow error:', error);
            socket.emit('follow-error', { message: '操作失败' });
        }
    });

    socket.on('disconnect', async (reason) => {
        console.log('Socket.IO 用户断开:', socket.id, 'reason:', reason, 'wasOnline:', socket.userId ? 'yes' : 'no');
        if (socket.userId) {
            onlineUsers.delete(socket.userId);
            io.emit('users-online', Array.from(onlineUsers.keys()));
            // Don't set isOnline = false here - user is still logged in, just not in whisper chat
        }
    });

    // Debug: log when handshake is complete
    socket.on('connect', () => {
        console.log('Socket handshake complete for:', socket.id);
    });

    // Client requests online users list
    socket.on('request-online-users', () => {
        socket.emit('users-online', Array.from(onlineUsers.keys()));
    });
});

const PORT = process.env.PORT || 3001;

// Clear all message limits on startup
async function clearMessageLimits() {
    try {
        const result = await ChatRoom.updateMany(
            {},
            { $set: { messageLimitReached: {} } }
        );
        console.log(`🧹 Cleared message limits from ${result.modifiedCount} chat rooms`);
    } catch (e) {
        console.error('Failed to clear message limits:', e);
    }
}

// 监听所有网络接口，这样同一局域网的手机可以访问
server.listen(PORT, '0.0.0.0', async () => {
    await clearMessageLimits();
    await recreateDefaultUser();
    console.log(`🚀 服务器运行在 http://0.0.0.0:${PORT}`);
    console.log(`🔌 WebSocket 悄悄话服务已启用`);
    console.log(`📱 手机访问: http://10.199.113.114:${PORT}`);
});
