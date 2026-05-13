require('dotenv').config();

const mongoose = require('mongoose');
const connectDB = require('../config/db');
const User = require('../models/User');
const Post = require('../models/Post');
const Comment = require('../models/Comment');
const Notification = require('../models/Notification');

async function upsertUser(data) {
    let user = await User.findOne({ username: data.username });

    if (!user) {
        user = new User(data);
    } else {
        user.nickname = data.nickname;
        user.bio = data.bio;
        user.avatar = data.avatar;
        user.password = data.password;
    }

    await user.save();
    return user;
}

async function seedUsers() {
    const users = await Promise.all([
        upsertUser({
            username: 'xiaoyu',
            password: '123456',
            nickname: '小雨',
            bio: '今天也要好好生活',
            avatar: ''
        }),
        upsertUser({
            username: 'ashan',
            password: '123456',
            nickname: '阿山',
            bio: '会慢慢变好的',
            avatar: ''
        }),
        upsertUser({
            username: 'momo',
            password: '123456',
            nickname: '默默',
            bio: '树洞常驻听众',
            avatar: ''
        })
    ]);

    const [xiaoyu, ashan, momo] = users;

    xiaoyu.following = [ashan._id, momo._id];
    xiaoyu.followers = [momo._id];
    ashan.following = [xiaoyu._id];
    ashan.followers = [xiaoyu._id];
    momo.following = [xiaoyu._id];
    momo.followers = [xiaoyu._id];

    await Promise.all([xiaoyu.save(), ashan.save(), momo.save()]);
    return { xiaoyu, ashan, momo };
}

async function seedPostsAndInteractions(seedUsersMap) {
    const postCount = await Post.countDocuments();
    if (postCount > 0) {
        console.log('ℹ️ 检测到已有帖子，跳过帖子/评论/通知初始化。');
        return;
    }

    const now = Date.now();
    const posts = await Post.insertMany([
        {
            userId: seedUsersMap.xiaoyu._id,
            content: '今天心情有点低落，但还是给自己做了顿热饭。',
            mood: '低落',
            createdAt: new Date(now - 4 * 60 * 60 * 1000)
        },
        {
            userId: seedUsersMap.ashan._id,
            content: '第一次在树洞发言，希望能遇到温柔的人。',
            mood: '期待',
            createdAt: new Date(now - 3 * 60 * 60 * 1000)
        },
        {
            userId: seedUsersMap.momo._id,
            content: '有人愿意分享一个最近让你觉得温暖的小事吗？',
            mood: '平静',
            createdAt: new Date(now - 2 * 60 * 60 * 1000)
        },
        {
            userId: seedUsersMap.xiaoyu._id,
            content: '睡前记录：今天比昨天多笑了一次。',
            mood: '开心',
            createdAt: new Date(now - 1 * 60 * 60 * 1000)
        }
    ]);

    const comments = await Comment.insertMany([
        {
            postId: posts[0]._id,
            userId: seedUsersMap.ashan._id,
            content: '抱抱你，照顾好自己已经很棒了。'
        },
        {
            postId: posts[0]._id,
            userId: seedUsersMap.momo._id,
            content: '热饭真的很治愈，辛苦啦。'
        },
        {
            postId: posts[1]._id,
            userId: seedUsersMap.xiaoyu._id,
            content: '欢迎你，一起慢慢说。'
        }
    ]);

    posts[0].likedBy = [seedUsersMap.ashan._id, seedUsersMap.momo._id];
    posts[0].likes = 2;
    posts[0].commentCount = 2;
    posts[1].likedBy = [seedUsersMap.xiaoyu._id];
    posts[1].likes = 1;
    posts[1].commentCount = 1;
    posts[2].likedBy = [seedUsersMap.xiaoyu._id];
    posts[2].likes = 1;
    posts[2].commentCount = 0;
    posts[3].likes = 0;
    posts[3].commentCount = 0;

    await Promise.all(posts.map(post => post.save()));

    await Notification.insertMany([
        {
            recipientId: seedUsersMap.xiaoyu._id,
            senderId: seedUsersMap.ashan._id,
            type: 'comment',
            postId: posts[0]._id,
            commentId: comments[0]._id,
            message: `${seedUsersMap.ashan.nickname} 回复了你的秘密`
        },
        {
            recipientId: seedUsersMap.xiaoyu._id,
            senderId: seedUsersMap.momo._id,
            type: 'like',
            postId: posts[0]._id,
            message: `${seedUsersMap.momo.nickname} 赞了你的秘密`
        },
        {
            recipientId: seedUsersMap.xiaoyu._id,
            senderId: seedUsersMap.ashan._id,
            type: 'follow',
            message: `${seedUsersMap.ashan.nickname} 关注了你`
        }
    ]);
}

async function main() {
    try {
        await connectDB();
        const seedUsersMap = await seedUsers();
        await seedPostsAndInteractions(seedUsersMap);

        console.log('✅ 初始数据准备完成');
        console.log('测试账号:');
        console.log('- xiaoyu / 123456');
        console.log('- ashan / 123456');
        console.log('- momo / 123456');
    } catch (error) {
        console.error('❌ 初始化失败:', error);
        process.exitCode = 1;
    } finally {
        await mongoose.connection.close();
    }
}

main();
