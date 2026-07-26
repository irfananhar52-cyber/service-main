# Manual Book — Monitoring Stack Node.js Services

> **Versi Stack:** Prometheus v2.53.0 · Grafana v11.1.0 · Alertmanager (bundled) · node-exporter v1.8.1 · cAdvisor v0.49.1  
> **Target Service:** auth-service-main · order-main · email-main · product-main

---

## Daftar Isi

1. [Pendahuluan](#1-pendahuluan)
2. [Arsitektur Sistem](#2-arsitektur-sistem)
3. [Prasyarat](#3-prasyarat)
4. [Langkah Pembuatan — Dari Nol](#4-langkah-pembuatan--dari-nol)
   - 4.1 [Siapkan Folder Struktur](#41-siapkan-folder-struktur)
   - 4.2 [Buat docker-compose.yml](#42-buat-docker-composeyml)
   - 4.3 [Konfigurasi Prometheus](#43-konfigurasi-prometheus)
   - 4.4 [Buat Alert Rules](#44-buat-alert-rules)
   - 4.5 [Konfigurasi Alertmanager](#45-konfigurasi-alertmanager)
   - 4.6 [Konfigurasi Grafana Provisioning](#46-konfigurasi-grafana-provisioning)
   - 4.7 [Buat Metrics Middleware untuk Node.js](#47-buat-metrics-middleware-untuk-nodejs)
   - 4.8 [Buat Mock Service](#48-buat-mock-service)
5. [Langkah Deployment & Menjalankan Stack](#5-langkah-deployment--menjalankan-stack)
6. [Integrasi ke Service Node.js yang Sudah Ada](#6-integrasi-ke-service-nodejs-yang-sudah-ada)
7. [Panduan Grafana Dashboard](#7-panduan-grafana-dashboard)
8. [Panduan Prometheus](#8-panduan-prometheus)
9. [Alert Rules — Penjelasan Detail](#9-alert-rules--penjelasan-detail)
10. [Konfigurasi Notifikasi Alertmanager](#10-konfigurasi-notifikasi-alertmanager)
11. [Menambah Service Baru](#11-menambah-service-baru)
12. [Troubleshooting](#12-troubleshooting)
13. [Referensi & Penjelasan Metrics](#13-referensi--penjelasan-metrics)

---

## 1. Pendahuluan

Monitoring stack ini dibangun untuk memantau performa dan kesehatan empat microservice Node.js secara real-time. Stack ini menggunakan pola **Pull-based monitoring** — Prometheus secara periodik **menarik (scrape)** data metrics dari endpoint `/metrics` masing-masing service.

### Komponen & Fungsinya

| Komponen | Image | Port | Fungsi |
|---|---|---|---|
| **Prometheus** | `prom/prometheus:v2.53.0` | 9090 | Scrape, simpan, dan evaluasi metrics serta alert rules |
| **Grafana** | `grafana/grafana:11.1.0` | 3001 | Visualisasi dashboard interaktif |
| **Alertmanager** | `prom/alertmanager` | 9093 | Routing & pengiriman notifikasi alert |
| **node-exporter** | `prom/node-exporter:v1.8.1` | 9100 | Metrics host (CPU, RAM, Disk, Network) |
| **cAdvisor** | `gcr.io/cadvisor/cadvisor:v0.49.1` | 8081 | Metrics per container Docker |
| **Mock Services** | Custom build | 3000 | Simulasi 4 service (untuk testing/demo) |

### Alur Data

```
Node.js Service         Prometheus           Grafana
  /metrics  ──scrape──▶  TSDB  ──query──▶  Dashboard
                           │
                     evaluate rules
                           │
                     Alertmanager ──notify──▶ Slack/Email/PagerDuty
```

---

## 2. Arsitektur Sistem

```
┌─────────────────────────────────────────────────────┐
│                  Docker Network: monitoring          │
│                                                     │
│  ┌──────────────┐    ┌─────────────┐               │
│  │  Prometheus  │───▶│   Grafana   │               │
│  │   :9090      │    │   :3001     │               │
│  └──────┬───────┘    └─────────────┘               │
│         │ scrape                                    │
│  ┌──────▼───────┐    ┌──────────────────────────┐  │
│  │ node-exporter│    │     Alertmanager :9093    │  │
│  │   :9100      │    └──────────────────────────┘  │
│  ├──────────────┤                                   │
│  │  cAdvisor    │                                   │
│  │   :8081      │                                   │
│  └──────────────┘                                   │
│                                                     │
└──────────┬──────────────────────────────────────────┘
           │
           │  Docker Network: app-network (shared)
           │
┌──────────▼──────────────────────────────────────────┐
│  auth-service-main :3000   order-main :3000         │
│  email-main :3000          product-main :3000       │
└─────────────────────────────────────────────────────┘
```

**Dua network Docker digunakan:**
- `monitoring` — internal network antar komponen monitoring
- `app-network` — shared network agar Prometheus bisa scrape service aplikasi

---

## 3. Prasyarat

### Software yang Dibutuhkan

| Software | Versi Minimum | Keterangan |
|---|---|---|
| **Docker** | 24.x+ | Runtime container |
| **Docker Compose** | v2.x+ | Orkestrasi multi-container |
| **Node.js** | 18.x+ | Hanya untuk pengembangan service |
| **npm** | 9.x+ | Package manager Node.js |

### Cek Instalasi

```bash
docker --version          # Docker version 24.x.x
docker compose version    # Docker Compose version v2.x.x
node --version            # v18.x.x atau lebih baru
```

### Kebutuhan Sistem

- RAM minimal: **2 GB** (rekomendasi 4 GB untuk semua service berjalan)
- Disk: minimal **5 GB** free space untuk data Prometheus
- OS: Linux, macOS, atau Windows dengan WSL2

---

## 4. Langkah Pembuatan — Dari Nol

### 4.1 Siapkan Folder Struktur

Buat struktur folder berikut dari awal:

```bash
mkdir -p monitoring/prometheus/rules
mkdir -p monitoring/alertmanager
mkdir -p monitoring/grafana/provisioning/datasources
mkdir -p monitoring/grafana/provisioning/dashboards
mkdir -p monitoring/mock-service
mkdir -p monitoring/src/middleware
```

Hasil akhir struktur:

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
├── mock-service/
│   ├── app.js
│   ├── Dockerfile
│   └── package.json
└── src/
    └── middleware/
        └── metrics.js
```

---

### 4.2 Buat docker-compose.yml

File `docker-compose.yml` adalah inti dari seluruh stack. Setiap service didefinisikan di sini.

**Poin penting yang perlu dipahami:**

#### Dua Docker Network

```yaml
networks:
  monitoring:          # Internal network untuk komponen monitoring
    driver: bridge
  app-network:         # External network yang di-share dengan services aplikasi
    external: true
    name: app-network
```

`app-network` harus dibuat **manual** sebelum menjalankan stack karena `external: true`.

#### Persistent Volumes

```yaml
volumes:
  prometheus_data: {}   # Data metrics Prometheus (agar tidak hilang saat restart)
  grafana_data: {}      # Data dashboard, user, konfigurasi Grafana
```

#### Service Prometheus

```yaml
prometheus:
  command:
    - '--storage.tsdb.retention.time=15d'   # Data disimpan 15 hari
    - '--web.enable-lifecycle'               # Izinkan reload config via API
    - '--web.enable-admin-api'               # Izinkan admin API
```

#### Service Grafana

```yaml
grafana:
  environment:
    - GF_SECURITY_ADMIN_USER=admin
    - GF_SECURITY_ADMIN_PASSWORD=admin123   # Ganti di production!
    - GF_USERS_ALLOW_SIGN_UP=false          # Nonaktifkan self-register
    - GF_UNIFIED_ALERTING_ENABLED=true      # Aktifkan alerting baru
```

---

### 4.3 Konfigurasi Prometheus

**File:** `prometheus/prometheus.yml`

Konfigurasi ini mendefinisikan:
- Seberapa sering Prometheus mengambil metrics
- Ke mana Alertmanager alert dikirim
- Daftar target yang di-scrape

```yaml
global:
  scrape_interval: 15s       # Scrape tiap 15 detik
  evaluation_interval: 15s   # Evaluasi alert rules tiap 15 detik
  scrape_timeout: 10s        # Timeout per scrape request

alerting:
  alertmanagers:
    - static_configs:
        - targets: ['alertmanager:9093']   # Alamat Alertmanager

rule_files:
  - '/etc/prometheus/rules/*.yml'          # Load semua file alert rules
```

#### Menambahkan Scrape Config per Service

Setiap service Node.js perlu entri `scrape_configs` seperti ini:

```yaml
- job_name: 'nama-service'
  metrics_path: '/metrics'
  scrape_interval: 10s          # Override global: lebih sering untuk service kritis
  static_configs:
    - targets: ['nama-service:3000']    # hostname:port (nama container Docker)
      labels:
        service: 'nama-service'
        team: 'backend'
  relabel_configs:
    - source_labels: [__address__]
      target_label: instance            # Set label instance = alamat target
    - target_label: job
      replacement: 'nama-service'       # Set label job = nama service
```

> **Penting:** `targets` menggunakan **nama container Docker** sebagai hostname, bukan `localhost`, karena service berjalan di Docker network yang sama.

---

### 4.4 Buat Alert Rules

**File:** `prometheus/rules/alerts.yml`

Alert rules dievaluasi secara periodik oleh Prometheus. Jika kondisi `expr` terpenuhi selama durasi `for`, alert dikirim ke Alertmanager.

#### Struktur Dasar Alert Rule

```yaml
groups:
  - name: nama_group          # Pengelompokan alert
    interval: 30s             # Override evaluation interval (opsional)
    rules:
      - alert: NamaAlert      # Nama alert (tampil di Grafana & notifikasi)
        expr: <PromQL>        # Kondisi yang dievaluasi
        for: 5m               # Berapa lama kondisi harus terpenuhi sebelum alert aktif
        labels:
          severity: critical  # Label tambahan (critical/warning/info)
        annotations:
          summary: "Ringkasan singkat"
          description: "Detail dengan template: {{ $labels.job }} = {{ $value }}"
```

#### Kelompok Alert yang Ada

| Group | Alert | Kondisi |
|---|---|---|
| `service_availability` | `ServiceDown` | `up == 0` selama 1 menit |
| `service_availability` | `ServiceHighRestartRate` | Restart > 2x dalam 5 menit |
| `http_requests` | `HighErrorRate` | 5xx rate > 5% selama 5 menit |
| `http_requests` | `HighRequestLatency` | P95 latency > 2 detik |
| `http_requests` | `VeryHighRequestLatency` | P99 latency > 5 detik |
| `http_requests` | `HighRequestRate` | Request rate > 1000 req/s |
| `memory_alerts` | `HighMemoryUsage` | RSS Memory > 512 MB |
| `memory_alerts` | `CriticalMemoryUsage` | RSS Memory > 1 GB |

---

### 4.5 Konfigurasi Alertmanager

**File:** `alertmanager/alertmanager.yml`

Alertmanager menentukan **ke mana** dan **kapan** notifikasi dikirim.

```yaml
route:
  group_by: ['alertname', 'job']   # Grupkan alert sejenis agar tidak spam
  group_wait: 30s                   # Tunggu 30s sebelum kirim grup baru (kumpulkan alert)
  group_interval: 5m               # Minimum jeda antar notifikasi satu grup
  repeat_interval: 12h             # Kirim ulang jika alert masih aktif setelah 12 jam
  receiver: default

# Override per severity
routes:
  - match:
      severity: critical
    repeat_interval: 1h            # Critical: kirim ulang tiap 1 jam
  - match:
      severity: warning
    repeat_interval: 6h            # Warning: kirim ulang tiap 6 jam

# Jika ada critical alert, suppress warning alert dari service yang sama
inhibit_rules:
  - source_match:
      severity: 'critical'
    target_match:
      severity: 'warning'
    equal: ['job', 'instance']
```

---

### 4.6 Konfigurasi Grafana Provisioning

Provisioning memungkinkan Grafana otomatis memuat datasource dan dashboard tanpa konfigurasi manual via UI.

#### Datasource (`grafana/provisioning/datasources/prometheus.yml`)

```yaml
apiVersion: 1
datasources:
  - name: Prometheus
    type: prometheus
    access: proxy              # Grafana server yang mengakses Prometheus (bukan browser)
    url: http://prometheus:9090
    isDefault: true
    editable: false
    jsonData:
      timeInterval: '15s'      # Sesuai dengan scrape_interval Prometheus
      httpMethod: POST          # POST lebih efisien untuk query panjang
```

#### Dashboard Provider (`grafana/provisioning/dashboards/dashboards.yml`)

```yaml
apiVersion: 1
providers:
  - name: 'default'
    type: file
    disableDeletion: true       # Cegah penghapusan dashboard dari UI
    updateIntervalSeconds: 60   # Cek update file tiap 60 detik
    options:
      path: /etc/grafana/provisioning/dashboards
```

> **Catatan:** File JSON dashboard (`services-overview.json`) diletakkan di folder yang sama dan otomatis dimuat saat Grafana start.

---

### 4.7 Buat Metrics Middleware untuk Node.js

**File:** `src/middleware/metrics.js`

Middleware ini adalah jembatan antara service Node.js dan Prometheus. Pasang middleware ini di **setiap service** yang ingin dimonitor.

#### Metrics yang Dikumpulkan

| Metric | Tipe | Label | Keterangan |
|---|---|---|---|
| `http_requests_total` | Counter | method, route, status_code | Total request sejak service start |
| `http_request_duration_seconds` | Histogram | method, route, status_code | Distribusi latensi request |
| `http_active_connections` | Gauge | — | Jumlah koneksi aktif saat ini |
| `http_errors_total` | Counter | method, route, status_code | Total request error (4xx + 5xx) |
| `nodejs_*` | Default | — | Heap, GC, event loop, file descriptors |

#### Cara Kerja Middleware

```
Request masuk ──▶ increment active_connections
                  start timer
                       │
                  handler berjalan
                       │
Response keluar ──▶ record: requests_total, duration, active_connections--
                    jika status >= 400: record errors_total
```

#### Fungsi `normalizeRoute`

Untuk mencegah *cardinality explosion* (terlalu banyak label unik di Prometheus), path parameter dinormalisasi:

```
/users/123    ──▶  /users/:id
/orders/456   ──▶  /orders/:id
```

Tanpa normalisasi, setiap ID unik akan membuat label baru dan meledakkan ukuran database Prometheus.

---

### 4.8 Buat Mock Service

**File:** `mock-service/app.js`, `mock-service/Dockerfile`, `mock-service/package.json`

Mock service adalah service simulasi yang menghasilkan traffic dan metrics palsu untuk keperluan testing dan demo dashboard.

#### Cara Kerja

- Setiap **500ms**, service mensimulasikan 1–7 request acak
- Distribusi status: ~85% sukses (2xx), ~10% client error (4xx), ~5% server error (5xx)
- Distribusi latency: 5ms–800ms normal, 5% kemungkinan spike 1–4 detik
- Route pool disesuaikan per nama service (via env var `SERVICE_NAME`)

#### Dockerfile

```dockerfile
FROM node:20-alpine    # Base image ringan
WORKDIR /app
COPY package.json .
RUN npm install --omit=dev    # Skip devDependencies
COPY app.js .
EXPOSE 3000
CMD ["node", "app.js"]
```

---

## 5. Langkah Deployment & Menjalankan Stack

### Langkah 1 — Clone / Siapkan Folder

```bash
# Jika menggunakan repo
git clone <repo-url> monitoring
cd monitoring

# Atau langsung masuk ke folder
cd monitoring
```

### Langkah 2 — Buat External Network

```bash
docker network create app-network
```

> Jalankan **hanya sekali**. Jika sudah ada, perintah ini akan error (aman diabaikan).
> Cek apakah sudah ada: `docker network ls | grep app-network`

### Langkah 3 — (Opsional) Sesuaikan Konfigurasi

Sebelum menjalankan, periksa hal-hal berikut:

- **`docker-compose.yml`** → Ganti password Grafana (`GF_SECURITY_ADMIN_PASSWORD`) jika di production
- **`alertmanager/alertmanager.yml`** → Tambahkan konfigurasi notifikasi (Slack/Email)
- **`prometheus/prometheus.yml`** → Tambahkan target service nyata jika ada

### Langkah 4 — Jalankan Stack

```bash
cd monitoring/
docker compose up -d
```

Tunggu ~30 detik agar semua service selesai start. Cek status:

```bash
docker compose ps
```

Output yang diharapkan (semua `running`):

```
NAME                  STATUS
prometheus            running (healthy)
grafana               running (healthy)
alertmanager          running
node-exporter         running
cadvisor              running
auth-service-main     running
order-main            running
email-main            running
product-main          running (jika ada)
```

### Langkah 5 — Akses Dashboard

| Layanan | URL | Kredensial |
|---|---|---|
| **Grafana** | http://localhost:3001 | `admin` / `admin123` |
| **Prometheus** | http://localhost:9090 | — |
| **Alertmanager** | http://localhost:9093 | — |
| **cAdvisor** | http://localhost:8081 | — |
| **node-exporter** | http://localhost:9100/metrics | — |

### Langkah 6 — Verifikasi Scraping Prometheus

1. Buka http://localhost:9090/targets
2. Pastikan semua target berstatus **UP** (hijau)
3. Jika ada yang DOWN, lihat bagian [Troubleshooting](#12-troubleshooting)

### Perintah Manajemen Stack

```bash
# Lihat log semua service
docker compose logs -f

# Lihat log service tertentu
docker compose logs -f prometheus
docker compose logs -f grafana

# Stop stack (data tetap tersimpan di volume)
docker compose down

# Stop dan hapus semua data (volume)
docker compose down -v

# Restart satu service
docker compose restart prometheus

# Reload konfigurasi Prometheus tanpa restart
curl -X POST http://localhost:9090/-/reload

# Update image ke versi terbaru
docker compose pull
docker compose up -d
```

---

## 6. Integrasi ke Service Node.js yang Sudah Ada

Ikuti langkah ini untuk setiap service Node.js yang ingin dimonitor.

### Langkah 1 — Install Dependency

```bash
cd <folder-service>
npm install prom-client
```

### Langkah 2 — Salin Middleware

Salin file `src/middleware/metrics.js` dari folder monitoring ke service kamu:

```bash
# Dari root workspace
cp monitoring/src/middleware/metrics.js <folder-service>/src/middleware/metrics.js
```

### Langkah 3 — Integrasikan ke app.js / server.js

```javascript
const express = require('express');
const { metricsMiddleware, metricsEndpoint } = require('./middleware/metrics');

const app = express();

// Pasang SEBELUM semua route handler
app.use(metricsMiddleware);

// Endpoint untuk Prometheus scrape
app.get('/metrics', metricsEndpoint);

// Route-route aplikasi kamu...
app.get('/api/users', (req, res) => { /* ... */ });
```

> **Urutan penting:** `app.use(metricsMiddleware)` harus dipanggil **sebelum** route handler lainnya agar semua request terekam.

### Langkah 4 — Pastikan Service Bergabung ke app-network

Di `docker-compose.yml` service kamu, tambahkan:

```yaml
services:
  nama-service-kamu:
    # ... konfigurasi lainnya
    networks:
      - app-network          # Tambahkan network ini
      - default              # Pertahankan network yang sudah ada

networks:
  app-network:
    external: true
    name: app-network
```

### Langkah 5 — Tambahkan Scrape Config di Prometheus

Edit `monitoring/prometheus/prometheus.yml`, tambahkan entri baru di bagian `scrape_configs`:

```yaml
- job_name: 'nama-service-kamu'
  metrics_path: '/metrics'
  scrape_interval: 10s
  static_configs:
    - targets: ['nama-container:3000']    # sesuaikan nama container & port
      labels:
        service: 'nama-service-kamu'
        team: 'backend'
```

### Langkah 6 — Reload Prometheus

```bash
curl -X POST http://localhost:9090/-/reload
```

### Langkah 7 — Verifikasi

1. Buka http://localhost:9090/targets
2. Cari `job_name` yang baru ditambahkan
3. Status harus **UP**
4. Buka Grafana → dashboard "Services Overview" → service baru harus muncul

---

## 7. Panduan Grafana Dashboard

### Login dan Navigasi

1. Buka http://localhost:3001
2. Login dengan `admin` / `admin123`
3. Klik **Dashboards** di menu kiri → cari **"Services Overview — Node.js"**

### Panel-Panel Dashboard

#### Service Status (UP/DOWN)

- Menampilkan status tiap service: **hijau = UP**, **merah = DOWN**
- Menggunakan metric: `up{job="nama-service"}`

#### HTTP Request Rate (req/s)

- Grafik laju request per detik per service
- PromQL: `rate(http_requests_total{job="..."}[1m])`
- Berguna untuk melihat traffic pattern dan spike

#### HTTP 5xx Error Rate (%)

- Persentase request yang berakhir dengan error server
- PromQL: `rate(http_requests_total{status_code=~"5.."}[5m]) / rate(http_requests_total[5m]) * 100`
- Alert otomatis jika > 5%

#### Latency P50 / P95 / P99

- Distribusi latensi dalam persentil:
  - **P50**: 50% request selesai di bawah nilai ini (median)
  - **P95**: 95% request selesai di bawah nilai ini
  - **P99**: 99% request selesai di bawah nilai ini
- PromQL: `histogram_quantile(0.99, rate(http_request_duration_seconds_bucket[5m]))`

#### RSS Memory Usage

- Penggunaan RAM aktual (Resident Set Size) dalam MB
- PromQL: `process_resident_memory_bytes / 1024 / 1024`

#### Node.js Heap Used

- Penggunaan heap JavaScript dalam MB
- PromQL: `nodejs_heap_size_used_bytes / 1024 / 1024`

#### Event Loop Lag

- Keterlambatan event loop Node.js (indikator thread blocking)
- Nilai tinggi (> 100ms) menandakan service kewalahan atau ada operasi blocking

#### Active HTTP Connections

- Jumlah koneksi HTTP yang sedang aktif saat ini
- PromQL: `http_active_connections`

### Mengubah Time Range

- Klik selektor waktu di kanan atas (default: **Last 1 hour**)
- Pilih: Last 5m, 15m, 1h, 6h, 24h, 7d, dst.
- Atau set custom range dengan tanggal spesifik

### Auto Refresh

- Klik ikon refresh di kanan atas → pilih interval (5s, 10s, 30s, 1m, dst.)
- Berguna untuk memantau incident secara real-time

---

## 8. Panduan Prometheus

### Membuka Prometheus UI

Buka http://localhost:9090

### Halaman Targets

http://localhost:9090/targets

Menampilkan semua target yang di-scrape beserta:
- **State**: UP (hijau) / DOWN (merah) / UNKNOWN
- **Last Scrape**: Kapan terakhir di-scrape
- **Duration**: Berapa lama scrape terakhir
- **Error**: Pesan error jika ada

### Query Metrics dengan PromQL

Buka http://localhost:9090/graph, contoh query:

```promql
# Cek apakah service UP
up{job="auth-service-main"}

# Request rate 5 menit terakhir
rate(http_requests_total{job="auth-service-main"}[5m])

# Error rate dalam persen
rate(http_requests_total{status_code=~"5.."}[5m])
  /
rate(http_requests_total[5m]) * 100

# Latency P95
histogram_quantile(0.95,
  rate(http_request_duration_seconds_bucket{job="auth-service-main"}[5m])
)

# Memory usage dalam MB
process_resident_memory_bytes{job="auth-service-main"} / 1024 / 1024
```

### Halaman Alerts

http://localhost:9090/alerts

Menampilkan status semua alert rules:
- **Inactive**: Kondisi tidak terpenuhi
- **Pending**: Kondisi terpenuhi, menunggu durasi `for`
- **Firing**: Alert aktif, dikirim ke Alertmanager

---

## 9. Alert Rules — Penjelasan Detail

### ServiceDown

```yaml
expr: up{job=~"auth-service-main|order-main|..."} == 0
for: 1m
severity: critical
```

**Penjelasan:** Metric `up` bernilai `1` jika Prometheus berhasil scrape service, dan `0` jika gagal. Alert aktif jika service tidak bisa di-scrape selama 1 menit. Delay 1 menit mencegah false positive akibat restart singkat.

### HighErrorRate

```yaml
expr: |
  (
    rate(http_requests_total{status_code=~"5.."}[5m])
    /
    rate(http_requests_total[5m])
  ) * 100 > 5
for: 5m
severity: critical
```

**Penjelasan:** Menghitung persentase request 5xx dibanding total request dalam 5 menit terakhir. Alert jika persentase melebihi 5% selama 5 menit berturut-turut.

### HighRequestLatency (P95 > 2s)

```yaml
expr: |
  histogram_quantile(0.95,
    rate(http_request_duration_seconds_bucket[5m])
  ) > 2
for: 5m
severity: warning
```

**Penjelasan:** `histogram_quantile` menghitung nilai persentil dari histogram. P95 > 2s berarti 5% pengguna mengalami latensi > 2 detik.

### HighMemoryUsage (RSS > 512 MB)

```yaml
expr: |
  process_resident_memory_bytes / 1024 / 1024 > 512
for: 5m
severity: warning
```

**Penjelasan:** RSS (Resident Set Size) adalah total RAM yang digunakan proses. Alert warning di 512 MB memberikan waktu untuk investigasi sebelum mencapai batas kritis 1 GB.

---

## 10. Konfigurasi Notifikasi Alertmanager

### Menambahkan Notifikasi Slack

Edit `alertmanager/alertmanager.yml`:

```yaml
receivers:
  - name: default
    slack_configs:
      - api_url: 'https://hooks.slack.com/services/TOKEN/TOKEN/TOKEN'
        channel: '#alerts-production'
        send_resolved: true
        title: '[{{ .Status | toUpper }}] {{ .GroupLabels.alertname }}'
        text: |
          {{ range .Alerts }}
          *Service:* {{ .Labels.job }}
          *Severity:* {{ .Labels.severity }}
          *Detail:* {{ .Annotations.description }}
          {{ end }}
```

### Menambahkan Notifikasi Email

```yaml
receivers:
  - name: default
    email_configs:
      - to: 'team@company.com'
        from: 'alerts@company.com'
        smarthost: 'smtp.gmail.com:587'
        auth_username: 'alerts@company.com'
        auth_password: 'app-password'
        send_resolved: true
        subject: '[{{ .Status | toUpper }}] {{ .GroupLabels.alertname }}'
        body: |
          {{ range .Alerts }}
          Alert: {{ .Annotations.summary }}
          Detail: {{ .Annotations.description }}
          {{ end }}
```

### Menambahkan PagerDuty

```yaml
receivers:
  - name: default
    pagerduty_configs:
      - routing_key: '<integration-key>'
        send_resolved: true
        description: '{{ .Annotations.summary }}'
        details:
          service: '{{ .Labels.job }}'
          severity: '{{ .Labels.severity }}'
```

### Reload Alertmanager Setelah Perubahan

```bash
docker compose restart alertmanager
# atau
curl -X POST http://localhost:9093/-/reload
```

---

## 11. Menambah Service Baru

Misal ingin menambahkan service `payment-main`.

### Langkah 1 — Tambahkan ke docker-compose.yml (jika mock service)

```yaml
payment-main:
  build: ./mock-service
  container_name: payment-main
  restart: unless-stopped
  environment:
    - SERVICE_NAME=payment-main
  networks:
    - monitoring
    - app-network
```

### Langkah 2 — Tambahkan Scrape Config di prometheus.yml

```yaml
- job_name: 'payment-main'
  metrics_path: '/metrics'
  scrape_interval: 10s
  static_configs:
    - targets: ['payment-main:3000']
      labels:
        service: 'payment-main'
        team: 'backend'
  relabel_configs:
    - source_labels: [__address__]
      target_label: instance
    - target_label: job
      replacement: 'payment-main'
```

### Langkah 3 — Tambahkan ke Alert Rules

Edit `prometheus/rules/alerts.yml`, tambahkan `payment-main` ke regex:

```yaml
# Sebelum:
expr: up{job=~"auth-service-main|order-main|email-main|product-main"} == 0
# Sesudah:
expr: up{job=~"auth-service-main|order-main|email-main|product-main|payment-main"} == 0
```

### Langkah 4 — Apply Perubahan

```bash
# Jika menambahkan container baru
docker compose up -d

# Reload konfigurasi Prometheus
curl -X POST http://localhost:9090/-/reload
```

---

## 12. Troubleshooting

### Target Prometheus Berstatus DOWN

**Gejala:** http://localhost:9090/targets menampilkan target dengan status DOWN

**Penyebab & Solusi:**

| Kemungkinan Penyebab | Solusi |
|---|---|
| Service belum start | `docker compose ps` → cek status container |
| Service tidak expose `/metrics` | Pastikan `app.use(metricsMiddleware)` dan `app.get('/metrics', metricsEndpoint)` ada |
| Network tidak terhubung | Cek `docker network inspect app-network` |
| Port salah di scrape config | Sesuaikan port di `prometheus.yml` dengan port service |
| Hostname salah di scrape config | Gunakan nama container Docker, bukan `localhost` |

**Debug cepat:**
```bash
# Masuk ke container Prometheus
docker exec -it prometheus sh

# Test koneksi ke service dari dalam Prometheus
wget -qO- http://auth-service-main:3000/metrics | head -5
```

### Grafana Tidak Menampilkan Data

**Gejala:** Panel dashboard kosong atau error "No data"

**Solusi:**
1. Cek datasource: **Settings (⚙)** → **Data sources** → klik **Prometheus** → **Save & Test**
2. Pastikan time range mencakup waktu saat data ada (kanan atas)
3. Buka Prometheus UI → jalankan query yang sama untuk verifikasi data ada
4. Cek log Grafana: `docker compose logs grafana | grep -i error`

### Alert Tidak Terkirim

**Gejala:** Alert aktif di Prometheus tapi tidak ada notifikasi

**Solusi:**
1. Cek Alertmanager UI: http://localhost:9093 → pastikan alert muncul di sana
2. Cek konfigurasi `alertmanager.yml` → pastikan receiver dikonfigurasi dengan benar
3. Cek log Alertmanager: `docker compose logs alertmanager`
4. Verifikasi Prometheus bisa reach Alertmanager:
   ```bash
   docker exec -it prometheus wget -qO- http://alertmanager:9093/-/healthy
   ```

### Container Tidak Bisa Start

**Gejala:** `docker compose ps` menampilkan status `exited` atau `restarting`

**Solusi:**
```bash
# Lihat log detail
docker compose logs <nama-service>

# Periksa apakah port sudah dipakai proses lain
netstat -ano | findstr :9090    # Windows
lsof -i :9090                   # Linux/macOS
```

**Port yang digunakan stack ini:**

| Port | Service |
|---|---|
| 3001 | Grafana |
| 9090 | Prometheus |
| 9093 | Alertmanager |
| 9100 | node-exporter |
| 8081 | cAdvisor |

### Disk Penuh karena Data Prometheus

```bash
# Cek ukuran volume
docker system df -v | grep prometheus_data

# Kurangi retention time (edit docker-compose.yml)
# '--storage.tsdb.retention.time=7d'  ← dari 15d ke 7d

# Restart Prometheus
docker compose restart prometheus
```

---

## 13. Referensi & Penjelasan Metrics

### Jenis Metric Prometheus

| Tipe | Keterangan | Contoh |
|---|---|---|
| **Counter** | Hanya naik, tidak pernah turun | Total request, total error |
| **Gauge** | Bisa naik dan turun | Active connections, memory usage |
| **Histogram** | Distribusi nilai dalam bucket | Request duration |
| **Summary** | Persentil pre-computed | (jarang digunakan) |

### Kenapa Menggunakan `rate()` untuk Counter?

Counter terus naik sepanjang service hidup. `rate()` mengkonversi ke laju per detik dalam window waktu:

```promql
# Salah: nilai counter mentah
http_requests_total  →  123456 (tidak bermakna)

# Benar: laju request per detik dalam 5 menit terakhir
rate(http_requests_total[5m])  →  45.3 (req/s)
```

### Kenapa Histogram untuk Latensi?

Histogram memungkinkan agregasi latensi dari banyak instance dan penghitungan persentil akurat:

```promql
# P99 latency dari semua instance auth-service
histogram_quantile(0.99,
  sum(rate(http_request_duration_seconds_bucket{job="auth-service-main"}[5m]))
  by (le)
)
```

### Label Cardinality

Label adalah pasangan key-value yang ditambahkan ke setiap metric. Kombinasi unik label = satu time series. **Hindari label dengan nilai tidak terbatas** (user ID, request ID, IP address) karena akan menyebabkan:
- Konsumsi RAM Prometheus membengkak
- Query menjadi lambat
- Disk cepat penuh

Itulah kenapa `normalizeRoute()` di middleware penting: `/users/123` dan `/users/456` dikonversi menjadi satu label `/users/:id`.

---

*Manual ini dibuat berdasarkan konfigurasi aktual monitoring stack di folder ini.*  
*Untuk pertanyaan atau kontribusi, silakan buka issue di repository.*
