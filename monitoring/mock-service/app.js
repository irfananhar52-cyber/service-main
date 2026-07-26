'use strict';

const express = require('express');
const client  = require('prom-client');

const app          = express();
const PORT         = 3000;
const SERVICE_NAME = process.env.SERVICE_NAME || 'mock-service';

// ─── Registry ────────────────────────────────────────────────────────────────
const register = new client.Registry();
register.setDefaultLabels({ service: SERVICE_NAME });
client.collectDefaultMetrics({ register });

// ─── Custom Metrics ───────────────────────────────────────────────────────────
const httpRequestsTotal = new client.Counter({
  name: 'http_requests_total',
  help: 'Total HTTP requests',
  labelNames: ['method', 'route', 'status_code'],
  registers: [register],
});

const httpRequestDuration = new client.Histogram({
  name: 'http_request_duration_seconds',
  help: 'HTTP request duration in seconds',
  labelNames: ['method', 'route', 'status_code'],
  buckets: [0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1, 2.5, 5, 10],
  registers: [register],
});

const httpActiveConnections = new client.Gauge({
  name: 'http_active_connections',
  help: 'Active HTTP connections',
  registers: [register],
});

// ─── Route pools per service ──────────────────────────────────────────────────
const routesByService = {
  'auth-service-main': [
    '/api/auth/login', '/api/auth/logout', '/api/auth/refresh',
    '/api/auth/register', '/api/auth/verify', '/api/users/:id',
  ],
  'order-main': [
    '/api/orders', '/api/orders/:id', '/api/orders/:id/status',
    '/api/orders/:id/cancel', '/api/cart', '/api/checkout',
  ],
  'email-main': [
    '/api/emails/send', '/api/emails/template', '/api/emails/queue',
    '/api/emails/status/:id', '/api/emails/retry',
  ],
  'product-main': [
    '/api/products', '/api/products/:id', '/api/products/search',
    '/api/categories', '/api/products/:id/stock', '/api/products/:id/reviews',
  ],
};

const ROUTES  = routesByService[SERVICE_NAME] || ['/api/resource'];
const METHODS = ['GET', 'GET', 'GET', 'POST', 'PUT', 'DELETE'];
// Status weighted: ~85% sukses, ~10% 4xx, ~5% 5xx
const STATUSES = [200,200,200,200,200,201,201,400,404,500,503];

function randomItem(arr) { return arr[Math.floor(Math.random() * arr.length)]; }
function randomBetween(min, max) { return Math.random() * (max - min) + min; }

// ─── Simulasi traffic setiap 500ms ───────────────────────────────────────────
setInterval(() => {
  const batch = Math.floor(randomBetween(1, 8));   // 1-7 req per interval
  for (let i = 0; i < batch; i++) {
    const method  = randomItem(METHODS);
    const route   = randomItem(ROUTES);
    const status  = randomItem(STATUSES);
    // latency: 5ms – 800ms, sesekali spike 1-4s
    const latency = Math.random() < 0.05
      ? randomBetween(1, 4)
      : randomBetween(0.005, 0.8);

    httpRequestsTotal.inc({ method, route, status_code: status });
    httpRequestDuration.observe({ method, route, status_code: status }, latency);
  }
  httpActiveConnections.set(Math.floor(randomBetween(1, 30)));
}, 500);

// ─── Middleware untuk real requests ──────────────────────────────────────────
app.use((req, res, next) => {
  if (req.path === '/metrics') return next();
  httpActiveConnections.inc();
  const end = httpRequestDuration.startTimer();
  res.on('finish', () => {
    const labels = { method: req.method, route: req.path, status_code: res.statusCode };
    httpRequestsTotal.inc(labels);
    end(labels);
    httpActiveConnections.dec();
  });
  next();
});

// ─── Endpoints ───────────────────────────────────────────────────────────────
app.get('/metrics', async (req, res) => {
  res.set('Content-Type', register.contentType);
  res.end(await register.metrics());
});

app.get('/health', (_req, res) =>
  res.json({ status: 'ok', service: SERVICE_NAME, uptime: process.uptime() }));

app.listen(PORT, () =>
  console.log(`[${SERVICE_NAME}] running on port ${PORT} — metrics at /metrics`));
