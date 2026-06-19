const mongoose = require('mongoose');

const notificationSchema = new mongoose.Schema({
    recipientId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'User',
        required: true,
        index: true
    },
    senderId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'User',
        required: true
    },
    type: {
        type: String,
        enum: ['like', 'comment', 'follow'],
        required: true
    },
    postId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'Post',
        default: null
    },
    commentId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'Comment',
        default: null
    },
    message: {
        type: String,
        required: true
    },
    read: {
        type: Boolean,
        default: false
    },
    createdAt: {
        type: Date,
        default: Date.now
    }
});

// Index for fetching user notifications
notificationSchema.index({ recipientId: 1, createdAt: -1 });

module.exports = mongoose.model('Notification', notificationSchema);