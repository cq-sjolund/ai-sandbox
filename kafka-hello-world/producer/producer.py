import os
import time
from kafka import KafkaProducer
from kafka.errors import NoBrokersAvailable

bootstrap_servers = os.environ.get("KAFKA_BOOTSTRAP_SERVERS")
print("Bootstrap servers:", bootstrap_servers)

producer = None

# Retry loop (proper way)
while producer is None:
    try:
        print("Connecting to Kafka...")
        producer = KafkaProducer(
            bootstrap_servers=bootstrap_servers,
            value_serializer=lambda v: v.encode("utf-8")
        )
    except NoBrokersAvailable:
        print("Kafka not ready, retrying in 3 seconds...")
        time.sleep(3)

print("Connected!")

message = "Hello World from Dockerized Producer 🚀"
producer.send("hello-topic", message)
producer.flush()

print("Message sent:", message)