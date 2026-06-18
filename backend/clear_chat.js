const mongoose = require('mongoose');

async function main() {
    try {
        await mongoose.connect('mongodb://127.0.0.1:27017/treehole');
        const User = require('./src/models/User');
        const ChatRoom = require('./src/models/ChatRoom');
        const ChatMessage = require('./src/models/ChatMessage');

        const users = await User.find({
            $or: [
                { nickname: '潘嘉伟' },
                { nickname: '平恺飞' },
                { username: 'panjiawei' },
                { username: 'pingkaifei' }
            ]
        });

        if (users.length < 2) {
            console.log('Could not find both users');
            console.log(users.map(u => u.nickname));
            return;
        }

        const u1 = users.find(u => u.nickname === '潘嘉伟' || u.username === 'panjiawei');
        const u2 = users.find(u => u.nickname === '平恺飞' || u.username === 'pingkaifei');

        if (!u1 || !u2) {
            console.log('One or both users not found', !!u1, !!u2);
            return;
        }

        const pIds = [u1._id.toString(), u2._id.toString()].sort().join('_');
        console.log('Participant IDs:', pIds);

        const room = await ChatRoom.findOne({ participantIds: pIds });
        if (room) {
            const resMsg = await ChatMessage.deleteMany({ chatId: room._id });
            const resRoom = await ChatRoom.deleteOne({ _id: room._id });
            console.log(`Deleted chat room (${resRoom.deletedCount}) and messages (${resMsg.deletedCount})`);
        } else {
            console.log('Room not found between', u1.nickname, 'and', u2.nickname);
            
            // Wait, what if participantIds is not perfectly sorted or they don't use this format?
            // Let's try finding the room by participants array
            const room2 = await ChatRoom.findOne({
                participants: { $all: [u1._id, u2._id] }
            });
            if (room2) {
                const resMsg = await ChatMessage.deleteMany({ chatId: room2._id });
                const resRoom = await ChatRoom.deleteOne({ _id: room2._id });
                console.log(`Deleted chat room 2 (${resRoom.deletedCount}) and messages (${resMsg.deletedCount})`);
            } else {
                console.log('No chat room found using array query either.');
            }
        }
    } catch (e) {
        console.error(e);
    } finally {
        await mongoose.disconnect();
    }
}

main();
