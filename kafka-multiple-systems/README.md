# Kafka Multi-System Messaging

A Dockerized demo of four independent systems communicating over a shared Apache Kafka cluster. Each system exposes a web UI where you can send messages to any of the other systems and see incoming messages in real time.

## Architecture

```
                        ┌─────────────────────┐
                        │   Kafka Cluster      │
                        │   (Confluent, 1 broker) │
                        │                      │
                        │  topic: system-a     │
                        │  topic: system-b     │
                        │  topic: system-c     │
                        │  topic: system-d     │
                        └─────────────────────┘
                           ▲  ▼   ▲  ▼   ▲  ▼   ▲  ▼
                        ┌──┘  └─┐ ...
              ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐
              │ System A │  │ System B │  │ System C │  │ System D │
              │ :8001    │  │ :8002    │  │ :8003    │  │ :8004    │
              └──────────┘  └──────────┘  └──────────┘  └──────────┘
```

Each system is an identical Java application, differentiated only by its `SYSTEM_NAME` environment variable. It runs:

- A **Kafka producer** — sends messages to the target system's topic
- A **Kafka consumer** — subscribes to its own topic (`system-a`, `system-b`, etc.)
- A **JDK HTTP server** — serves the web UI and a small REST API
- A **plain HTML/JS frontend** — polls for new messages every second

## Prerequisites

- [Docker](https://www.docker.com/) and Docker Compose v2

## Getting Started

```bash
docker compose up --build
```

Kafka starts first. The four systems wait for it to become healthy before connecting.

| System   | URL                   |
|----------|-----------------------|
| System A | http://localhost:8001 |
| System B | http://localhost:8002 |
| System C | http://localhost:8003 |
| System D | http://localhost:8004 |

Open two browser tabs on different systems, pick a receiver in the dropdown, type a message, and click **Send Message** (or press `Ctrl+Enter`). The message appears on the receiver's page within one second.

## Project Structure

```
kafka-multiple-systems/
├── docker-compose.yml
└── system-app/                    # Single image used by all four systems
    ├── Dockerfile
    ├── pom.xml
    └── src/main/
        ├── java/com/systems/
        │   ├── Main.java
        │   ├── config/Config.java           # Reads env vars
        │   ├── model/Message.java
        │   ├── store/MessageStore.java      # Thread-safe, keeps last 100 messages
        │   ├── kafka/
        │   │   ├── MessageConsumer.java     # Polls own topic
        │   │   └── MessageProducer.java     # Publishes to target topic
        │   └── http/ApiServer.java          # JDK HttpServer, no Spring Boot
        └── resources/static/index.html      # Frontend
```

## API Endpoints

Each system exposes the following on its port:

| Method | Path        | Description                                      |
|--------|-------------|--------------------------------------------------|
| GET    | `/`         | Serves the web UI                                |
| GET    | `/config`   | Returns `systemName` and list of all systems     |
| GET    | `/messages` | Returns received messages as a JSON array        |
| POST   | `/send`     | Sends a message — body: `{"to":"B","message":"…"}` |

## Tech Stack

| Layer     | Technology                              |
|-----------|-----------------------------------------|
| Broker    | Apache Kafka 7.5.0 (confluentinc, Zookeeper mode) |
| Backend   | Java 17, kafka-clients, Gson            |
| HTTP      | `com.sun.net.httpserver.HttpServer`     |
| Frontend  | Plain HTML / CSS / JavaScript           |
| Container | Docker Compose v2                       |

## Stopping

```bash
docker compose down
```

To also remove the Kafka data volume:

```bash
docker compose down -v
```
