# Kafka Docker Hello World

This project demonstrates a minimal Apache Kafka setup using Docker containers, including:

- 🐘 Zookeeper
- 📨 Kafka Broker
- 📤 Python Producer
- 📥 Python Consumer

Everything runs inside Docker using docker compose.

## 📁 Project Structure
```
kafka-docker-hello/
│
├── docker-compose.yml
│
├── producer/
│   ├── Dockerfile
│   └── producer.py
│
└── consumer/
    ├── Dockerfile
    └── consumer.py
```

## ⚙️ Prerequisites

Make sure you have installed:

- Docker
- Docker Compose (v2+)

Verify:
```
docker --version
docker compose version
```

## 🚀 How to Run the Project
1️⃣ Clone or Navigate to the Project
```
cd kafka-docker-hello
```
2️⃣ Build and Start Everything
```
docker compose up --build
```

This will:

- Start Zookeeper

- Start Kafka

- Build the producer image

- Build the consumer image

Start sending and consuming messages

## 3️⃣ Watch Consumer Logs

In another terminal:

docker compose logs -f consumer

You should see something like:

consumer  | Connected. Waiting for messages...
consumer  | Received message: Hello 0
consumer  | Received message: Hello 1
consumer  | Received message: Hello 2
## 🛑 Stop the Project
docker compose down

To remove volumes (clean reset):
```
docker compose down -v
```

## 🧠 How It Works
Producer

Connects to Kafka (kafka:9092)

Sends messages to topic: hello-topic

Consumer

Connects to Kafka

Subscribes to hello-topic

Belongs to consumer group: hello-group

Prints received messages to container logs

## 🔎 Useful Kafka Debug Commands

List topics:
```
docker compose exec kafka kafka-topics \
  --list \
  --bootstrap-server kafka:9092
```

Describe consumer group:
```
docker compose exec kafka kafka-consumer-groups \
  --bootstrap-server kafka:9092 \
  --describe \
  --group hello-group
```
Consume messages manually:
```
docker compose exec kafka kafka-console-consumer \
  --topic hello-topic \
  --bootstrap-server kafka:9092 \
  --from-beginning
```

## 📚 Key Kafka Concepts Demonstrated

Topics

Producers

Consumers

Consumer Groups

Offsets

Lag

Docker container networking

## 🔥 Possible Improvements

Add multiple partitions

Scale multiple consumers

Add Kafka UI dashboard

Switch to KRaft mode (no Zookeeper)

Add healthchecks

Convert producer to REST API (FastAPI)

## 🏁 Summary

This project is a minimal, fully dockerized Kafka setup suitable for:

Learning Kafka fundamentals

Testing local event-driven architectures

Building a base for microservices