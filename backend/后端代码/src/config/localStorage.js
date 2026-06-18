const multer = require('multer');
const path = require('path');
const fs = require('fs');

// 确保 uploads 目录存在 - 使用绝对路径
// __dirname is src/config, so go up 2 levels to backend/
const backendDir = path.join(__dirname, '../..');
const uploadDir = path.join(backendDir, 'uploads');
if (!fs.existsSync(uploadDir)) {
    fs.mkdirSync(uploadDir, { recursive: true });
}

// 本地存储配置 - 头像
const avatarStorage = multer.diskStorage({
    destination: (req, file, cb) => {
        cb(null, uploadDir + '/avatars');
    },
    filename: (req, file, cb) => {
        const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
        const ext = path.extname(file.originalname);
        cb(null, `avatar-${uniqueSuffix}${ext}`);
    }
});

// 本地存储配置 - 帖子图片
const postStorage = multer.diskStorage({
    destination: (req, file, cb) => {
        cb(null, uploadDir + '/posts');
    },
    filename: (req, file, cb) => {
        const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
        const ext = path.extname(file.originalname);
        cb(null, `post-${uniqueSuffix}${ext}`);
    }
});

// 确保子目录存在 - 使用绝对路径
const avatarsDir = path.join(uploadDir, 'avatars');
const postsDir = path.join(uploadDir, 'posts');
console.log('[LocalStorage] Upload directory:', uploadDir);
console.log('[LocalStorage] Avatars directory:', avatarsDir);
if (!fs.existsSync(avatarsDir)) {
    fs.mkdirSync(avatarsDir, { recursive: true });
}
if (!fs.existsSync(postsDir)) {
    fs.mkdirSync(postsDir, { recursive: true });
}

// 文件过滤器 - 只允许图片
const imageFilter = (req, file, cb) => {
    if (file.mimetype.startsWith('image/')) {
        cb(null, true);
    } else {
        cb(new Error('只允许上传图片文件'), false);
    }
};

// 限制文件大小 5MB
const upload = multer({
    storage: avatarStorage,
    fileFilter: imageFilter,
    limits: { fileSize: 5 * 1024 * 1024 }
});

const uploadPost = multer({
    storage: postStorage,
    fileFilter: imageFilter,
    limits: { fileSize: 5 * 1024 * 1024 }
});

module.exports = { upload, uploadPost, uploadDir };
