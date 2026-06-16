# Log Aggregation & Alerting System

A real-time log aggregation and alerting system built with **Apache Kafka**, **Spring Boot**, and **PostgreSQL**.

Services emit log events → Kafka distributes them → Processor aggregates error counts → Alerts are raised when error rate crosses a threshold → REST API serves logs and alerts.

---

## Architecture

```
┌─────────────────┐        ┌───────────────┐        ┌──────────────────┐
│   log-producer  │──────▶ │     Kafka     │──────▶ │  log-processor   │
│  (REST API)     │        │  (log-events) │        │ (Kafka Consumer) │
│  Port: 8081     │        │  Port: 9092   │        │  + Alert Logic   │
└─────────────────┘        └───────────────┘        └────────┬─────────┘
                                                             │
                                                             │ saves logs + alerts
                                                             ▼
                                                    ┌─────────────────┐
                                                    │   PostgreSQL    │
                                                    │  log_events     │
                                                    │  alerts         │
                                                    └────────┬────────┘
                                                             │
                                                             │ reads
                                                             ▼
                                                    ┌─────────────────┐
                                                    │   alert-api     │
                                                    │  (REST API)     │
                                                    │  Port: 8083     │
                                                    └─────────────────┘
```

---

## How It Works

1. **log-producer** exposes a REST API. Any service can POST a log event to it.
2. The log event is published to the Kafka topic `log-events`, using the **service name as the partition key** — so all logs from the same service go to the same partition (ordered delivery).
3. **log-processor** consumes from Kafka and saves every log event to PostgreSQL.
4. A **scheduler runs every 60 seconds** and counts ERROR logs per service in the last 1-minute window. If count exceeds threshold (3), an alert is saved.
5. **alert-api** provides REST endpoints to query all logs and alerts from PostgreSQL.

---

## Tech Stack

| Component     | Technology                        |
|---------------|-----------------------------------|
| Language      | Java 17                           |
| Framework     | Spring Boot 3.2                   |
| Message Broker| Apache Kafka + ZooKeeper          |
| Database      | PostgreSQL 15                     |
| Containers    | Docker, Docker Compose            |
| Build Tool    | Maven                             |

---

## Project Structure

```
log-aggregation-alerting-system/
├── docker-compose.yml          # runs all services + infra
├── docker/
│   └── init.sql                # creates PostgreSQL tables
├── log-producer/               # publishes log events to Kafka
├── log-processor/              # consumes from Kafka, stores logs, raises alerts
└── alert-api/                  # REST API to query logs and alerts
```

---

## Running the Project

**Prerequisites:** Docker and Docker Compose installed.

```bash
# Clone the repo
git clone https://github.com/ayeshagagguturi22/log-aggregation-alerting-system.git
cd log-aggregation-alerting-system

# Start everything
docker-compose up --build
```

This starts:
- ZooKeeper on port 2181
- Kafka on port 9092
- PostgreSQL on port 5432
- log-producer on port 8081
- log-processor (background, no port exposed)
- alert-api on port 8083

---

## API Reference

### log-producer (port 8081)

**Publish a single log event**
```
POST http://localhost:8081/api/logs
Content-Type: application/json

{
  "service": "payment-service",
  "level": "ERROR",
  "message": "Payment gateway timeout",
  "timestamp": 1718000000000
}
```

**Simulate 5 ERROR logs for a service**
```
POST http://localhost:8081/api/logs/simulate/payment-service
```

---

### alert-api (port 8083)

| Method | Endpoint                        | Description                        |
|--------|---------------------------------|------------------------------------|
| GET    | /api/alerts                     | Get all alerts                     |
| GET    | /api/alerts/{service}           | Get alerts for a specific service  |
| GET    | /api/logs                       | Get all log events                 |
| GET    | /api/logs/service/{service}     | Get logs for a specific service    |
| GET    | /api/logs/level/{level}         | Get logs by level (ERROR/WARN/INFO)|

---

## Testing the Flow

```bash
# 1. Send 5 ERROR logs for payment-service (triggers alert)
curl -X POST http://localhost:8081/api/logs/simulate/payment-service

# 2. Wait up to 60 seconds for the scheduler to evaluate

# 3. Check if alert was raised
curl http://localhost:8083/api/alerts/payment-service

# 4. View all stored logs
curl http://localhost:8083/api/logs/service/payment-service
```

---

## Alert Logic

The processor checks every 60 seconds:
- **Window:** last 1 minute
- **Threshold:** more than 3 ERROR logs from a single service
- **Action:** saves an alert to PostgreSQL with service name, error count, and window timestamps
