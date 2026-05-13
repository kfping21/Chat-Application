// Migration script to fix comment counts and clean up duplicate like notifications
// Run with: node fixCounts.js

const mongoose = require('mongoose');
require('dotenv').config();
const { FIXED_MONGO_URI } = require('./mongoUri');

const Post = require('./models/Post');
const Comment = require('./models/Comment');
const Notification = require('./models/Notification');

async function migrate() {
    try {
        await mongoose.connect(FIXED_MONGO_URI);
        console.log('Connected to MongoDB');

        // 1. Fix comment counts for all posts
        console.log('\n--- Fixing comment counts ---');
        const posts = await Post.find({});
        for (const post of posts) {
            const actualCount = await Comment.countDocuments({ postId: post._id });
            if (post.commentCount !== actualCount) {
                console.log(`Post ${post._id}: commentCount ${post.commentCount} -> ${actualCount}`);
                post.commentCount = actualCount;
                await post.save();
            }
        }
        console.log('Comment counts fixed!');

        // 2. Clean up duplicate like notifications (keep only one per user per post for likes)
        console.log('\n--- Cleaning up duplicate like notifications ---');
        const likeNotifications = await Notification.find({ type: 'like' }).lean();
        const seen = new Map();

        for (const notif of likeNotifications) {
            const key = `${notif.recipientId.toString()}-${notif.senderId.toString()}-${notif.postId?.toString()}`;
            if (seen.has(key)) {
                // Duplicate - delete this one
                console.log(`Deleting duplicate like notification: ${notif._id}`);
                await Notification.deleteOne({ _id: notif._id });
            } else {
                seen.set(key, notif._id);
            }
        }
        console.log('Like notifications cleanup done!');

        // 3. Clean up duplicate comment notifications similarly
        console.log('\n--- Cleaning up duplicate comment notifications ---');
        const commentNotifications = await Notification.find({ type: 'comment' }).lean();
        const seenComments = new Map();

        for (const notif of commentNotifications) {
            const key = `${notif.recipientId.toString()}-${notif.senderId.toString()}-${notif.postId?.toString()}`;
            if (seenComments.has(key)) {
                console.log(`Deleting duplicate comment notification: ${notif._id}`);
                await Notification.deleteOne({ _id: notif._id });
            } else {
                seenComments.set(key, notif._id);
            }
        }
        console.log('Comment notifications cleanup done!');

        console.log('\n--- Migration complete! ---');
        process.exit(0);
    } catch (error) {
        console.error('Migration error:', error);
        process.exit(1);
    }
}

migrate();
