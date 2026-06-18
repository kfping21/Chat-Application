const fs = require('fs');
const path = require('path');
const mongoose = require('mongoose');
const User = require('../models/User');

const SOURCE_DIR = 'C:\\Users\\panjiawei\\Desktop\\头像';
const TARGET_DIR = path.join(__dirname, '../../uploads/avatars');

async function main() {
    try {
        // Connect to MongoDB
        const MONGO_URI = 'mongodb://127.0.0.1:27017/treehole';
        await mongoose.connect(MONGO_URI);
        console.log('Connected to MongoDB');

        // Ensure target directory exists
        if (!fs.existsSync(TARGET_DIR)) {
            fs.mkdirSync(TARGET_DIR, { recursive: true });
        }

        // Read all images from source
        const files = fs.readdirSync(SOURCE_DIR).filter(file => 
            file.endsWith('.jpg') || file.endsWith('.jpeg') || file.endsWith('.png') || file.endsWith('.webp')
        );

        if (files.length === 0) {
            console.log('No images found in source directory.');
            process.exit(0);
        }

        console.log(`Found ${files.length} images.`);

        const imageUrls = [];
        // Copy files and get their relative URLs
        for (const file of files) {
            const sourcePath = path.join(SOURCE_DIR, file);
            const ext = path.extname(file);
            const uniqueName = `avatar-${Date.now()}-${Math.round(Math.random() * 1E9)}${ext}`;
            const targetPath = path.join(TARGET_DIR, uniqueName);
            
            fs.copyFileSync(sourcePath, targetPath);
            // Write absolute URL to DB to avoid any client parsing issues
            imageUrls.push(`http://10.17.27.114:3001/uploads/avatars/${uniqueName}`);
        }

        console.log('Images copied to uploads/avatars.');

        // Get all users
        const users = await User.find({});
        console.log(`Found ${users.length} users in database.`);

        let updatedCount = 0;
        for (const user of users) {
            // Skip 潘嘉伟 and 平恺飞 if they exist
            if (user.nickname === '潘嘉伟' || user.nickname === '平恺飞' || 
                user.username === '潘嘉伟' || user.username === '平恺飞') {
                continue;
            }

            // Assign random avatar
            const randomAvatar = imageUrls[Math.floor(Math.random() * imageUrls.length)];
            user.avatar = randomAvatar;
            await user.save();
            updatedCount++;
        }

        console.log(`Successfully updated avatars for ${updatedCount} users.`);

    } catch (err) {
        console.error('Error:', err);
    } finally {
        await mongoose.disconnect();
    }
}

main();
