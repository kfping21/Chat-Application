const mongoose = require('mongoose');

const partyMessageSchema = new mongoose.Schema({
    roomId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'PartyRoom',
        required: true,
        index: true
    },
    senderId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'User',
        required: true
    },
    senderNickname: {
        type: String,
        required: true,
        trim: true,
        maxlength: 50
    },
    content: {
        type: String,
        required: true,
        trim: true,
        maxlength: 500
    },
    createdAt: {
        type: Date,
        default: Date.now,
        index: true
    }
});

partyMessageSchema.index({ roomId: 1, createdAt: -1 });

module.exports = mongoose.model('PartyMessage', partyMessageSchema);
