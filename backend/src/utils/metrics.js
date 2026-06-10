class MetricsCollector {
    constructor() {
        this.metrics = {
            requestCount: 0,
            errorCount: 0,
            totalResponseTime: 0,
            activeUsers: new Set(),
            endpoints: {}
        };
    }

    recordRequest(endpoint, method, responseTime, statusCode) {
        this.metrics.requestCount++;
        this.metrics.totalResponseTime += responseTime;

        const key = `${method}:${endpoint}`;
        if (!this.metrics.endpoints[key]) {
            this.metrics.endpoints[key] = { count: 0, totalTime: 0, errors: 0 };
        }
        this.metrics.endpoints[key].count++;
        this.metrics.endpoints[key].totalTime += responseTime;

        if (statusCode >= 400) {
            this.metrics.errorCount++;
            this.metrics.endpoints[key].errors++;
        }
    }

    recordActiveUser(userId) {
        if (userId) this.metrics.activeUsers.add(userId);
    }

    removeActiveUser(userId) {
        if (userId) this.metrics.activeUsers.delete(userId);
    }

    getMetrics() {
        return {
            requestCount: this.metrics.requestCount,
            errorCount: this.metrics.errorCount,
            errorRate: this.metrics.requestCount > 0
                ? (this.metrics.errorCount / this.metrics.requestCount * 100).toFixed(2) + '%'
                : '0%',
            avgResponseTime: this.metrics.requestCount > 0
                ? (this.metrics.totalResponseTime / this.metrics.requestCount).toFixed(2) + 'ms'
                : '0ms',
            activeUsers: this.metrics.activeUsers.size,
            endpoints: Object.fromEntries(
                Object.entries(this.metrics.endpoints).map(([key, val]) => [
                    key,
                    {
                        count: val.count,
                        avgTime: (val.totalTime / val.count).toFixed(2) + 'ms',
                        errorRate: (val.errors / val.count * 100).toFixed(2) + '%'
                    }
                ])
            )
        };
    }

    reset() {
        this.metrics.requestCount = 0;
        this.metrics.errorCount = 0;
        this.metrics.totalResponseTime = 0;
    }
}

module.exports = new MetricsCollector();