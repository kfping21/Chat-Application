// Health check endpoint
app.get('/health', (req, res) => {
    const memUsage = process.memoryUsage();
    res.json({
        status: 'healthy',
        timestamp: new Date().toISOString(),
        version: '1.0.0',
        uptime: process.uptime().toFixed(0) + 's',
        memory: {
            rss: (memUsage.rss / 1024 / 1024).toFixed(2) + 'MB',
            heapUsed: (memUsage.heapUsed / 1024 / 1024).toFixed(2) + 'MB'
        }
    });
});