const mongoose = require('mongoose');

const chatRoomSchema = new mongoose.Schema({
    participants: [{ type: mongoose.Schema.Types.ObjectId, ref: 'User' }],
    // For quick lookup: sorted array of participant IDs
    participantIds: [{ type: String }],
    lastMessage: { type: String, default: '' },
    lastMessageAt: { type: Date, default: null },
    // Unread counts for each participant: { userId: count }
    unreadCounts: { type: Map, of: Number, default: {} },
    // Message limit: if other person hasn't followed, only 1 message allowed
    messageLimitReached: { type: Map, of: Boolean, default: {} },
    // For message limit feature: who has followed whom
    followingStatus: { type: Map, of: Boolean, default: {} },
    isActive: { type: Boolean, default: true }
}, { timestamps: true });

// Index for efficient participant lookup
chatRoomSchema.index({ participantIds: 1 });
chatRoomSchema.index({ lastMessageAt: -1 });

module.exports = mongoose.model('ChatRoom', chatRoomSchema);
