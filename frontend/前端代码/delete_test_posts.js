const mongoose = require('mongoose');
const Post = require('./backend/src/models/Post');
const User = require('./backend/src/models/User');
const Comment = require('./backend/src/models/Comment');

mongoose.connect('mongodb://127.0.0.1:27017/treehole', {
    useNewUrlParser: true,
    useUnifiedTopology: true
}).then(async () => {
    console.log('Connected to MongoDB');
    
    // Find the user "某工商学生"
    const user = await User.findOne({ nickname: '某工商学生' });
    if (!user) {
        console.log('User not found');
        process.exit(0);
    }
    
    console.log(`Found user: ${user._id}`);
    
    // Find all posts by this user
    const posts = await Post.find({ userId: user._id });
    console.log(`Found ${posts.length} posts to delete.`);
    
    // Delete all posts
    const deleteResult = await Post.deleteMany({ userId: user._id });
    console.log(`Deleted ${deleteResult.deletedCount} posts.`);
    
    // Delete all comments belonging to these posts
    for (const post of posts) {
        await Comment.deleteMany({ postId: post._id });
    }
    
    console.log('Done cleaning up.');
    process.exit(0);
}).catch(err => {
    console.error(err);
    process.exit(1);
});
