const mongoose = require('mongoose');
const Post = require('./src/models/Post');
const User = require('./src/models/User');
const Comment = require('./src/models/Comment');

mongoose.connect('mongodb://127.0.0.1:27017/treehole', {
    useNewUrlParser: true,
    useUnifiedTopology: true
}).then(async () => {
    console.log('Connected to MongoDB');
    
    const user = await User.findOne({ nickname: '某工商学生' });
    if (!user) {
        console.log('User not found');
        process.exit(0);
    }
    
    const posts = await Post.find({ userId: user._id });
    console.log(`Found ${posts.length} posts to delete.`);
    
    const deleteResult = await Post.deleteMany({ userId: user._id });
    console.log(`Deleted ${deleteResult.deletedCount} posts.`);
    
    for (const post of posts) {
        await Comment.deleteMany({ postId: post._id });
    }
    
    console.log('Done cleaning up.');
    process.exit(0);
}).catch(err => {
    console.error(err);
    process.exit(1);
});
