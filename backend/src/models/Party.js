const mongoose = require('mongoose');

const partyMessageSchema = new mongoose.Schema({
    roomId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'PartyRoom',
        required: true,
        index: true
    },
    senderId: {
        type: String,
        required: true
    },
    senderNickname: {
        type: String,
        default: '匿名用户'
    },
    content: {
        type: String,
        required: true,
        trim: true,
        maxlength: 500
    },
    isSystemMessage: {
        type: Boolean,
        default: false
    },
    createdAt: {
        type: Date,
        default: Date.now,
        index: true
    }
});

const partyRoomSchema = new mongoose.Schema({
    name: {
        type: String,
        required: true,
        trim: true,
        maxlength: 50
    },
    subtitle: {
        type: String,
        default: '',
        maxlength: 100
    },
    creatorId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'User',
        required: true
    },
    participants: [{
        type: mongoose.Schema.Types.ObjectId,
        ref: 'User'
    }],
    maxParticipants: {
        type: Number,
        default: 6
    },
    onlineCount: {
        type: Number,
        default: 0
    },
    heat: {
        type: Number,
        default: 0
    },
    messageCount: {
        type: Number,
        default: 0
    },
    lastMessage: {
        type: String,
        default: ''
    },
    lastMessageAt: {
        type: Date,
        default: null
    },
    createdAt: {
        type: Date,
        default: Date.now
    }
});

// Index for listing rooms
partyRoomSchema.index({ heat: -1 });
partyRoomSchema.index({ lastMessageAt: -1 });

const PartyMessage = mongoose.model('PartyMessage', partyMessageSchema);
const PartyRoom = mongoose.model('PartyRoom', partyRoomSchema);

module.exports = { PartyRoom, PartyMessage };
