# Panduan Menjalankan Aplikasi RabbitMQ Consumer

## Prasyarat
1. **RabbitMQ** harus berjalan di localhost:5672
2. **Java 17+** sudah terinstall
3. **Maven** atau Maven Wrapper

## Option 1: Menggunakan Docker Compose (Rekomendasi)

Buat file `docker-compose.yml` di root project:

```yaml
version: '3.8'
services:
  rabbitmq:
    image: rabbitmq:3.13-management
    container_name: rabbitmq_consumer
    ports:
      - "5672:5672"      # AMQP port
      - "15672:15672"    # Management UI
    environment:
      RABBITMQ_DEFAULT_USER: user
      RABBITMQ_DEFAULT_PASS: password
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    restart: unless-stopped

volumes:
  rabbitmq_data:
```

Jalankan RabbitMQ:
```bash
docker-compose up -d
```

Check RabbitMQ Management: http://localhost:15672 (user: user, password: password)

---

## Option 2: Menggunakan RabbitMQ Local Installation

Jika RabbitMQ sudah terinstall di sistem, jalankan service RabbitMQ.

---

## Menjalankan Aplikasi Consumer

Setelah RabbitMQ berjalan, jalankan aplikasi:

```bash
cd c:\Users\Ifan Anhar\Downloads\cunsumer\ (1)\cunsumer

# Run aplikasi
.\mvnw spring-boot:run

# ATAU jalankan JAR langsung
java -jar target/cunsumer-0.0.1-SNAPSHOT.jar
```

---

## Expected Output

```
========================================
✓ Aplikasi dimulai
========================================

✓ Pesan terkirim: Halo dari Consumer App 1
✓ Pesan diterima: Halo dari Consumer App 1
Waktu: 2026-04-12T12:12:15.123

✓ Pesan terkirim: Halo dari Consumer App 2
✓ Pesan diterima: Halo dari Consumer App 2
Waktu: 2026-04-12T12:12:16.456

✓ Pesan terkirim: Halo dari Consumer App 3
✓ Pesan diterima: Halo dari Consumer App 3
Waktu: 2026-04-12T12:12:17.789

========================================
✓ Aplikasi siap mendengarkan pesan
========================================
```

---

## Struktur Alur Program

1. **AppStarter** - Memulai aplikasi dan mengirim pesan demo
2. **MessageProducer** - Mengirim pesan ke queue RabbitMQ
3. **MessageListener** - Mendengarkan dan menerima pesan dari queue
4. **RabbitMQConfig** - Konfigurasi queue, exchange, dan binding

---

## Testing Aplikasi

### Menggunakan RabbitMQ Management UI
1. Buka http://localhost:15672
2. Login: user / password
3. Ke tab "Queues and Streams"
4. Lihat queue "myQueue" dengan pesan

### Menggunakan amqplib CLI (jika ada)
```bash
rabbitmq-plugins enable rabbitmq_management
```

---

## Stop Aplikasi

```bash
# Ctrl + C di terminal aplikasi

# Jika menggunakan Docker
docker-compose down
```
