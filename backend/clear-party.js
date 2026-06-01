// Clear all party chat rooms and messages
require('dotenv').config();
const mongoose = require('mongoose');
const { PartyRoom, PartyMessage } = require('./src/models/Party');

const MONGO_URI =
    process.env.MONGO_URI ||
    process.env.DATABASE_URL ||
    'mongodb://localhost:27017/treehole';

async function clearPartyData() {
    try {
        await mongoose.connect(MONGO_URI);
        const messages = await PartyMessage.deleteMany({});
        const rooms = await PartyRoom.deleteMany({});

        console.log(`✅ Deleted party messages: ${messages.deletedCount}`);
        console.log(`✅ Deleted party rooms: ${rooms.deletedCount}`);

        await mongoose.disconnect();
        process.exit(0);
    } catch (error) {
        console.error('❌ Failed to clear party data:', error);
        process.exit(1);
    }
}

clearPartyData();
