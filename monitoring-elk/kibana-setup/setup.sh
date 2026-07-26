#!/bin/sh
# ============================================================
# Kibana Setup Script
# Membuat index patterns dan saved objects untuk 4 microservices
# ============================================================

set -e

KIBANA_URL="${KIBANA_HOST:-http://kibana:5601}"
ES_URL="${ELASTICSEARCH_HOST:-http://elasticsearch:9200}"

echo "[setup] Menunggu Kibana siap..."
until curl -sf "${KIBANA_URL}/api/status" | grep -q '"level":"available"'; do
  echo "[setup] Kibana belum siap, tunggu 10 detik..."
  sleep 10
done
echo "[setup] Kibana siap!"

# ── Buat index patterns untuk setiap service ────────────────
SERVICES="auth-service-main order-main email-main product-main"

for SERVICE in $SERVICES; do
  echo "[setup] Membuat index pattern untuk: ${SERVICE}"
  curl -sf -X POST "${KIBANA_URL}/api/saved_objects/index-pattern/microservices-${SERVICE}" \
    -H "kbn-xsrf: true" \
    -H "Content-Type: application/json" \
    -d "{
      \"attributes\": {
        \"title\": \"microservices-${SERVICE}-*\",
        \"timeFieldName\": \"@timestamp\"
      }
    }" || echo "[setup] Index pattern ${SERVICE} sudah ada atau gagal dibuat"
done

# ── Buat index pattern gabungan semua service ────────────────
echo "[setup] Membuat index pattern gabungan semua microservices"
curl -sf -X POST "${KIBANA_URL}/api/saved_objects/index-pattern/microservices-all" \
  -H "kbn-xsrf: true" \
  -H "Content-Type: application/json" \
  -d '{
    "attributes": {
      "title": "microservices-*",
      "timeFieldName": "@timestamp"
    }
  }' || echo "[setup] Index pattern gabungan sudah ada"

# ── Set default index pattern ────────────────────────────────
echo "[setup] Set default index pattern ke microservices-all"
curl -sf -X POST "${KIBANA_URL}/api/kibana/settings" \
  -H "kbn-xsrf: true" \
  -H "Content-Type: application/json" \
  -d '{
    "changes": {
      "defaultIndex": "microservices-all"
    }
  }' || echo "[setup] Gagal set default index"

# ── Buat ILM policy untuk manajemen lifecycle index ──────────
echo "[setup] Membuat ILM policy untuk microservices logs"
curl -sf -X PUT "${ES_URL}/_ilm/policy/microservices-logs-policy" \
  -H "Content-Type: application/json" \
  -d '{
    "policy": {
      "phases": {
        "hot": {
          "actions": {
            "rollover": {
              "max_age": "7d",
              "max_size": "5gb"
            }
          }
        },
        "delete": {
          "min_age": "30d",
          "actions": {
            "delete": {}
          }
        }
      }
    }
  }' || echo "[setup] ILM policy sudah ada"

echo "[setup] ✅ Setup selesai!"
echo "[setup] Buka Kibana di: ${KIBANA_URL}"
echo "[setup] Pergi ke: Discover → pilih index pattern 'microservices-*'"
