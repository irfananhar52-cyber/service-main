/**
 * metrics.js — Prometheus metrics middleware untuk Node.js / Express
 *
 * Install dependency:
 *   npm install prom-client
 *
 * Cara pakai di app.js / server.js:
 *   const { metricsMiddleware, metricsEndpoint } = require('./middleware/metrics');
 *   app.use(metricsMiddleware);
 *   app.get('/metrics', metricsEndpoint);
 */

'use strict';

const client = require('prom-client');

// ─────────────────────────────────────────────────────────────────────────────
// Registry
// ─────────────────────────────────────────────────────────────────────────────
const register = new client.Registry();

// Default Node.js metrics (heap, event loop lag, GC, dll.)
client.collectDefaultMetrics({ register, prefix: 'nodejs_' });

// ─────────────────────────────────────────────────────────────────────────────
// Custom Metrics
// ─────────────────────────────────────────────────────────────────────────────

/** Total HTTP requests */
const httpRequestsTotal = new client.Counter({
  name: 'http_requests_total',
  help: 'Total number of HTTP requests',
  labelNames: ['method', 'route', 'status_code'],
  registers: [register],
});

/** HTTP request duration histogram */
const httpRequestDurationSeconds = new client.Histogram({
  name: 'http_request_duration_seconds',
  help: 'Duration of HTTP requests in seconds',
  labelNames: ['method', 'route', 'status_code'],
  buckets: [0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1, 2.5, 5, 10],
  registers: [register],
});

/** Active HTTP connections */
const httpActiveConnections = new client.Gauge({
  name: 'http_active_connections',
  help: 'Number of active HTTP connections',
  registers: [register],
});

/** Total HTTP errors */
const httpErrorsTotal = new client.Counter({
  name: 'http_errors_total',
  help: 'Total number of HTTP errors (4xx and 5xx)',
  labelNames: ['method', 'route', 'status_code'],
  registers: [register],
});

// ─────────────────────────────────────────────────────────────────────────────
// Middleware: catat setiap request
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Normalisasi route agar path param /:id tidak menghasilkan cardinality explosion.
 * Contoh: /users/123 => /users/:id
 */
function normalizeRoute(req) {
  // Gunakan express matched route jika tersedia
  if (req.route && req.route.path) {
    const basePath = req.baseUrl || '';
    return basePath + req.route.path;
  }
  // Fallback: ganti angka dengan :id
  return req.path.replace(/\/\d+/g, '/:id');
}

function metricsMiddleware(req, res, next) {
  // Skip endpoint /metrics itu sendiri
  if (req.path === '/metrics') return next();

  httpActiveConnections.inc();
  const end = httpRequestDurationSeconds.startTimer();

  res.on('finish', () => {
    const route = normalizeRoute(req);
    const labels = {
      method: req.method,
      route,
      status_code: res.statusCode,
    };

    httpRequestsTotal.inc(labels);
    end(labels);
    httpActiveConnections.dec();

    if (res.statusCode >= 400) {
      httpErrorsTotal.inc(labels);
    }
  });

  next();
}

// ─────────────────────────────────────────────────────────────────────────────
// Endpoint: GET /metrics
// ─────────────────────────────────────────────────────────────────────────────
async function metricsEndpoint(req, res) {
  try {
    res.set('Content-Type', register.contentType);
    res.end(await register.metrics());
  } catch (err) {
    res.status(500).end(err.message);
  }
}

module.exports = { metricsMiddleware, metricsEndpoint, register };
