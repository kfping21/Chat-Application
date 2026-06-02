const mongoose = require('mongoose');

const chatMessageSchema = new mongoose.Schema({
    chatId: { type: String, required: true, index: true },
    senderId: { type: String, required: true },
    senderNickname: { type: String, default: '' },
    content: { type: String, required: true },
    timestamp: { type: Date, default: Date.now, index: true }
}, { timestamps: true });

// Index for efficient querying of chat history
chatMessageSchema.index({ chatId: 1, timestamp: -1 });

module.exports = mongoose.model('ChatMessage', chatMessageSchema);
