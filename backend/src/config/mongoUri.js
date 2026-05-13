const FIXED_MONGO_URI = 'mongodb://127.0.0.1:27017/treehole';

function maskMongoUri(uri) {
    return uri.replace(/\/\/.*@/, '//***:***@');
}

module.exports = {
    FIXED_MONGO_URI,
    maskMongoUri
};
