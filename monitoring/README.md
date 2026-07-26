# Monitoring Stack — Services Node.js

Stack monitoring untuk **auth-service-main**, **order-main**, **email-main**, **product-main**.

## Komponen

| Komponen | Port | Fungsi |
|---|---|---|
| **Prometheus** | 9090 | Scrape & store metrics |
| **Grafana** | 3001 | Visualisasi dashboard |
| **Alertmanager** | 9093 | Notifikasi alert |
| **node-exporter** | 9100 | Host CPU/RAM/Disk metrics |
| **cAdvisor** | 8080 | Container metrics |

## Cara Jalankan

### 1. Buat shared network (jika belum ada)
```bash
docker network create app-network
```

### 2. Pastikan semua services bergabung ke `app-network`
Di `docker-compose.yml` masing-masing service, tambahkan:
```yaml
networks:
  - app-network
```

### 3. Integrasikan metrics middleware ke setiap service Node.js
Install dependency:
```bash
npm install prom-client
```
Tambahkan ke `app.js` / `server.js`:
```js
const { metricsMiddleware, metricsEndpoint } = require('./middleware/metrics');
// copy file src/middleware/metrics.js ke project masing-masing service

app.use(metricsMiddleware);
app.get('/metrics', metricsEndpoint);
```

### 4. Jalankan monitoring stack
```bash
cd monitoring/
docker compose up -d
```

### 5. Akses
- **Grafana**: http://localhost:3001 (user: `admin` / pass: `admin123`)
- **Prometheus**: http://localhost:9090
- **Alertmanager**: http://localhost:9093

## Struktur Folder
```
monitoring/
├── docker-compose.yml
├── alertmanager/
│   └── alertmanager.yml
├── prometheus/
│   ├── prometheus.yml
│   └── rules/
│       └── alerts.yml
├── grafana/
│   └── provisioning/
│       ├── datasources/
│       │   └── prometheus.yml
│       └── dashboards/
│           ├── dashboards.yml
│           └── services-overview.json
└── src/
    └── middleware/
        └── metrics.js       ← copy ke masing-masing service
```

## Dashboard

Dashboard **"Services Overview — Node.js"** sudah di-provision otomatis.  
Berisi panel:
- Service status (UP/DOWN) per service
- HTTP Request Rate (req/s)
- HTTP 5xx Error Rate
- Latency P50, P95, P99
- RSS Memory Usage
- CPU Usage
- Node.js Heap Used
- Event Loop Lag
- Active Connections
- Open File Descriptors

## Alert Rules

| Alert | Severity | Kondisi |
|---|---|---|
| ServiceDown | critical | Service down > 1 menit |
| HighErrorRate | critical | Error rate 5xx > 5% |
| HighRequestLatency | warning | P95 latency > 2s |
| VeryHighRequestLatency | critical | P99 latency > 5s |
| HighMemoryUsage | warning | RSS > 512MB |
| CriticalMemoryUsage | critical | RSS > 1GB |
| EventLoopLagHigh | warning | Lag > 500ms |
| HeapUsageHigh | warning | Heap > 85% |

## Konfigurasi Port Services

Default konfigurasi: semua services expose metrics di port **3000**.  
Jika port berbeda, edit `prometheus/prometheus.yml` bagian `targets`:
```yaml
- targets: ['auth-service-main:PORT_ANDA']
```
