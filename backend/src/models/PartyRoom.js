const mongoose = require('mongoose');

const partyRoomSchema = new mongoose.Schema({
    name: {
        type: String,
        required: true,
        trim: true,
        maxlength: 60
    },
    subtitle: {
        type: String,
        default: '',
        trim: true,
        maxlength: 120
    },
    onlineCount: {
        type: Number,
        default: 0
    },
    heat: {
        type: Number,
        default: 0
    },
    createdAt: {
        type: Date,
        default: Date.now
    }
});

partyRoomSchema.index({ createdAt: -1 });

module.exports = mongoose.model('PartyRoom', partyRoomSchema);
