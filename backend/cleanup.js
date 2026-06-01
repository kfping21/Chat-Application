// Quick cleanup script - run with: node cleanup.js

require('dotenv').config();
const mongoose = require('mongoose');

// Prefer MONGO_URI, fall back to DATABASE_URL (Railway) or local
const MONGO_URI =
    process.env.MONGO_URI ||
    process.env.DATABASE_URL ||
    'mongodb://localhost:27017/treehole';

async function cleanup() {
    try {
        console.log('Connecting to MongoDB...');
        console.log('URI:', MONGO_URI.replace(/\/\/.*@/, '//***:***@'));
        await mongoose.connect(MONGO_URI);
        console.log('Connected!');

        const db = mongoose.connection.db;

        // 1. Clear all notifications for user panjiawei
        console.log('\n1. Finding panjiawei user...');
        const user = await db.collection('users').findOne({ username: 'panjiawei' });
        if (user) {
            console.log(`Found user: ${user._id}`);

            // Delete all notifications for this user
            const notifResult = await db.collection('notifications').deleteMany({
                recipientId: user._id
            });
            console.log(`Deleted ${notifResult.deletedCount} notifications for panjiawei`);

            // Also delete notifications sent BY this user
            const sentResult = await db.collection('notifications').deleteMany({
                senderId: user._id
            });
            console.log(`Deleted ${sentResult.deletedCount} notifications from panjiawei`);
        }

        // 2. Fix all post commentCounts
        console.log('\n2. Fixing post comment counts...');
        const posts = await db.collection('posts').find({}).toArray();
        for (const post of posts) {
            const actualCount = await db.collection('comments').countDocuments({ postId: post._id });
            await db.collection('posts').updateOne(
                { _id: post._id },
                { $set: { commentCount: actualCount } }
            );
            if (post.commentCount !== actualCount) {
                console.log(`Post ${post._id}: ${post.commentCount} -> ${actualCount}`);
            }
        }
        console.log('Comment counts fixed!');

        // 3. Remove duplicate like notifications
        console.log('\n3. Removing duplicate like notifications...');
        const likeNotifs = await db.collection('notifications').find({ type: 'like' }).sort({ _id: 1 }).toArray();
        const seen = new Map();
        let deletedCount = 0;

        for (const notif of likeNotifs) {
            const key = `${notif.recipientId?.toString()}-${notif.senderId?.toString()}-${notif.postId?.toString()}`;
            if (seen.has(key)) {
                await db.collection('notifications').deleteOne({ _id: notif._id });
                deletedCount++;
            } else {
                seen.set(key, notif._id);
            }
        }
        console.log(`Deleted ${deletedCount} duplicate like notifications`);

        // 4. Remove duplicate comment notifications
        console.log('\n4. Removing duplicate comment notifications...');
        const commentNotifs = await db.collection('notifications').find({ type: 'comment' }).sort({ _id: 1 }).toArray();
        const seenComments = new Map();
        let deletedCommentCount = 0;

        for (const notif of commentNotifs) {
            const key = `${notif.recipientId?.toString()}-${notif.senderId?.toString()}-${notif.postId?.toString()}`;
            if (seenComments.has(key)) {
                await db.collection('notifications').deleteOne({ _id: notif._id });
                deletedCommentCount++;
            } else {
                seenComments.set(key, notif._id);
            }
        }
        console.log(`Deleted ${deletedCommentCount} duplicate comment notifications`);

        console.log('\n✅ Cleanup complete!');
        await mongoose.disconnect();
        process.exit(0);
    } catch (error) {
        console.error('Error:', error);
        process.exit(1);
    }
}

cleanup();
