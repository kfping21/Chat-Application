const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');

const userSchema = new mongoose.Schema({
    username: {
        type: String,
        required: true,
        unique: true,
        trim: true,
        minlength: 3,
        maxlength: 20,
        index: true
    },
    password: {
        type: String,
        required: true,
        minlength: 6
    },
    nickname: {
        type: String,
        default: ''
    },
    avatar: {
        type: String,
        default: ''
    },
    bio: {
        type: String,
        default: '',
        maxlength: 200
    },
    // For single device login
    isOnline: { type: Boolean, default: false },
    lastOnlineAt: { type: Date, default: null },
    // Following/followers
    following: [{ type: mongoose.Schema.Types.ObjectId, ref: 'User' }],
    followers: [{ type: mongoose.Schema.Types.ObjectId, ref: 'User' }],
    createdAt: {
        type: Date,
        default: Date.now
    }
});

// Pre-save hook to hash password
userSchema.pre('save', async function (next) {
    if (!this.isModified('password')) return next();
    this.password = await bcrypt.hash(this.password, 10);
    next();
});

// Method to compare password
userSchema.methods.comparePassword = async function (candidatePassword) {
    return bcrypt.compare(candidatePassword, this.password);
};

module.exports = mongoose.model('User', userSchema);