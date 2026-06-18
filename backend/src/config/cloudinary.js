const cloudinary = require('cloudinary').v2;
const { CloudinaryStorage } = require('multer-storage-cloudinary');

cloudinary.config({
    cloud_name: process.env.CLOUDINARY_CLOUD_NAME,
    api_key: process.env.CLOUDINARY_API_KEY,
    api_secret: process.env.CLOUDINARY_API_SECRET
});

const storage = new CloudinaryStorage({
    cloudinary: cloudinary,
    params: {
        folder: 'treehole/avatars',
        // Optimize images for web: auto format, quality, and size
        format: 'auto',
        transformation: [
            { width: 200, height: 200, crop: 'fill', radius: 'max' },
            { quality: 'auto:good' },
            { fetch_format: 'auto' },
            { dpr: 'auto' }
        ]
    }
});

module.exports = { cloudinary, storage };
