require('dotenv').config();

const mongoose = require('mongoose');
const connectDB = require('../config/db');
const User = require('../models/User');

const nicknames = [
    '小满', '阿屿', '晚风', '青柠', '星眠', '南枝', '雾里', '初夏', '知秋', '木木',
    '久安', '小鹿', '白露', '简言', '念安', '半夏', '未央', '拾光', '清野', '松月',
    '北海', '一橙', '米粒', '小舟', '羽生', '林深', '向晚', '可可', '阿宁', '予安',
    '听雨', '晴川', '漫漫', '朝颜', '拾一', '六月', '南风', '之遥', '阿星', '乐乐',
    '迟迟', '元气', '晚晴', '阿暖', '夜阑', '如初', '微甜', '流光', '半糖', '远山',
    '见山', '亦欢', '如歌', '阿湫', '沐言', '柚子', '安和', '禾木', '若水', '森屿'
];

const bios = [
    '今晚有点失眠，想找个能聊得来的人。',
    '白天很坚强，夜里还是会脆弱。',
    '最近压力有点大，想听听你的故事。',
    '一个人在外打拼，偶尔会想家。',
    '希望在这里遇见温柔的人。',
    '想认真认识一些同频的灵魂。',
    '今天有小确幸，也有一点点难过。',
    '想把心事说给陌生但善良的你。',
    '慢慢来，生活总会亮起来。',
    '最近有点迷茫，想找人聊聊。'
];

async function upsertExploreUsers() {
    let created = 0;
    let updated = 0;

    for (let i = 0; i < nicknames.length; i++) {
        const index = i + 1;
        const username = `soul_${String(index).padStart(3, '0')}`;
        const nickname = nicknames[i];
        const bio = bios[i % bios.length];
        const avatar = `preset_${(index % 5) + 1}`;
        const isOnline = index % 3 !== 0;
        const lastOnlineAt = new Date(Date.now() - (index % 24) * 60 * 60 * 1000);

        const existing = await User.findOne({ username });
        if (!existing) {
            const user = new User({
                username,
                password: '123456',
                nickname,
                bio,
                avatar,
                isOnline,
                lastOnlineAt
            });
            await user.save();
            created++;
        } else {
            existing.nickname = nickname;
            existing.bio = bio;
            existing.avatar = avatar;
            existing.isOnline = isOnline;
            existing.lastOnlineAt = lastOnlineAt;
            await existing.save();
            updated++;
        }
    }

    console.log(`✅ Explore users seeded. created=${created}, updated=${updated}`);
}

async function main() {
    try {
        await connectDB();
        await upsertExploreUsers();
    } catch (error) {
        console.error('❌ Seed explore users failed:', error);
        process.exitCode = 1;
    } finally {
        await mongoose.connection.close();
    }
}

main();
