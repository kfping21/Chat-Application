const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const cors = require('cors');
const compression = require('compression');
const path = require('path');
const responseTime = require('response-time');
require('dotenv').config();
const connectDB = require('./config/db');
const logger = require('./utils/logger');
const metrics = require('./utils/metrics');
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

// Metrics collection middleware
app.use(responseTime((req, res, time) => {
    const endpoint = req.route ? req.route.path : req.path;
    metrics.recordRequest(endpoint, req.method, time, res.statusCode);
    logger.info('request', {
        method: req.method,
        url: req.originalUrl,
        statusCode: res.statusCode,
        responseTime: time.toFixed(2) + 'ms'
    });
}));

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
    const memUsage = process.memoryUsage();
    res.json({
        status: 'healthy',
        timestamp: new Date().toISOString(),
        version: '1.0.0',
        uptime: process.uptime().toFixed(0) + 's',
        memory: {
            rss: (memUsage.rss / 1024 / 1024).toFixed(2) + 'MB',
            heapUsed: (memUsage.heapUsed / 1024 / 1024).toFixed(2) + 'MB'
        }
    });
});

// Metrics endpoint
app.get('/metrics', (req, res) => {
    res.json(metrics.getMetrics());
});

// Get online users (for whisper chat online status) - no auth needed
app.get('/api/online-users', (req, res) => {
    res.json({ onlineUsers: Array.from(onlineUsers.keys()) });
});

// Make io accessible to routes
app.set('io', io);

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
        metrics.recordActiveUser(userId);
        io.emit('users-online', Array.from(onlineUsers.keys()));
        logger.info('user_online', { userId, socketId: socket.id });
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

    // Soul Match Queue: IP -> { userId, socketId, timestamp }
    const soulMatchQueue = new Map();

    socket.on('soul-match-request', async (data) => {
        const userId = data.userId;
        const ip = socket.handshake.address;
        
        // Clean up expired entries (e.g. 30 seconds)
        const now = Date.now();
        for (const [key, val] of soulMatchQueue.entries()) {
            if (now - val.timestamp > 30000) {
                soulMatchQueue.delete(key);
            }
        }

        let matched = false;
        console.log(`[SoulMatch] Request from ${userId} with IP ${ip}`);

        // Find if anyone is waiting
        for (const [waitingUserId, val] of soulMatchQueue.entries()) {
            if (waitingUserId !== userId) {
                // Match found!
                console.log(`[SoulMatch] Found in queue: ${waitingUserId}`);
                soulMatchQueue.delete(waitingUserId);
                matched = true;
                
                try {
                    // Create chat room
                    const ChatRoom = require('./models/ChatRoom');
                    const participantIds = [userId, waitingUserId].sort().join('_');
                    let room = await ChatRoom.findOne({ participantIds });
                    if (!room) {
                        room = new ChatRoom({
                            participants: [userId, waitingUserId],
                            participantIds,
                            lastMessageAt: new Date()
                        });
                        await room.save();
                    }
                    
                    const User = require('./models/User');
                    const user1 = await User.findById(userId).select('nickname avatar');
                    const user2 = await User.findById(waitingUserId).select('nickname avatar');

                    if (user1 && user2) {
                        // Emit to caller
                        socket.emit('soul-match-found', {
                            roomId: room._id.toString(),
                            participantId: user2._id.toString(),
                            nickname: user2.nickname,
                            avatar: user2.avatar
                        });
                        
                        // Emit to waiting user
                        io.to(val.socketId).emit('soul-match-found', {
                            roomId: room._id.toString(),
                            participantId: user1._id.toString(),
                            nickname: user1.nickname,
                            avatar: user1.avatar
                        });
                    }
                } catch (e) {
                    console.error('Soul match error:', e);
                }
                break;
            }
        }

        // 2. If no one in queue, check if there is an ONLINE user on the SAME IP (same hotspot)
        if (!matched) {
            console.log(`[SoulMatch] No one in queue. Scanning ${onlineUsers.size} online users for IP ${ip}...`);
            for (const [onlineUserId, onlineSocketId] of onlineUsers.entries()) {
                if (onlineUserId !== userId) {
                    const onlineSocket = io.sockets.sockets.get(onlineSocketId);
                    if (onlineSocket) {
                        console.log(`[SoulMatch] Checking user ${onlineUserId} with IP ${onlineSocket.handshake.address}`);
                        // Match any other online user (acts as local test match)
                        console.log(`[SoulMatch] Match found by online status: ${onlineUserId}`);
                        matched = true;
                            
                            try {
                                // Create chat room
                                const ChatRoom = require('./models/ChatRoom');
                                const participantIds = [userId, onlineUserId].sort().join('_');
                                let room = await ChatRoom.findOne({ participantIds });
                                if (!room) {
                                    room = new ChatRoom({
                                        participants: [userId, onlineUserId],
                                        participantIds,
                                        lastMessageAt: new Date()
                                    });
                                    await room.save();
                                }
                                
                                const User = require('./models/User');
                                const user1 = await User.findById(userId).select('nickname avatar');
                                const user2 = await User.findById(onlineUserId).select('nickname avatar');
    
                                if (user1 && user2) {
                                    // Emit to caller
                                    socket.emit('soul-match-found', {
                                        roomId: room._id.toString(),
                                        participantId: user2._id.toString(),
                                        nickname: user2.nickname,
                                        avatar: user2.avatar
                                    });
                                    
                                    // Emit to the other user who didn't even click!
                                    io.to(onlineSocketId).emit('soul-match-found', {
                                        roomId: room._id.toString(),
                                        participantId: user1._id.toString(),
                                        nickname: user1.nickname,
                                        avatar: user1.avatar
                                    });
                                }
                            } catch (e) {
                                console.error('Soul match same-ip error:', e);
                            }
                            break;
                    }
                }
            }
        }

        if (!matched) {
            console.log(`[SoulMatch] No match found. Adding ${userId} to queue.`);
            soulMatchQueue.set(userId, { ip, socketId: socket.id, timestamp: Date.now() });
        }
    });

    socket.on('soul-match-cancel', (data) => {
        // Find user by socket id just to be safe, or just iterate
        for (const [waitingUserId, val] of soulMatchQueue.entries()) {
            if (val.socketId === socket.id) {
                soulMatchQueue.delete(waitingUserId);
                break;
            }
        }
    });

    socket.on('disconnect', async (reason) => {
        logger.info('socket_disconnect', { socketId: socket.id, reason, userId: socket.userId });
        if (socket.userId) {
            onlineUsers.delete(socket.userId);
            metrics.removeActiveUser(socket.userId);
            io.emit('users-online', Array.from(onlineUsers.keys()));
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
    console.log(`📱 手机访问: http://10.17.27.114:${PORT}`);
});
