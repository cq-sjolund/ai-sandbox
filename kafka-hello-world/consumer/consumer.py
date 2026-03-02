import os
import time
import sys
from kafka import KafkaConsumer
from kafka.errors import NoBrokersAvailable

bootstrap_servers = os.environ.get("KAFKA_BOOTSTRAP_SERVERS")
print("Bootstrap servers:", bootstrap_servers, flush=True)

consumer = None

while consumer is None:
    try:
        print("Connecting to Kafka...", flush=True)
        consumer = KafkaConsumer(
            "hello-topic",
            bootstrap_servers=bootstrap_servers,
            auto_offset_reset="earliest",
            group_id="hello-group",
            value_deserializer=lambda v: v.decode("utf-8")
        )
    except NoBrokersAvailable:
        print("Kafka not ready, retrying...", flush=True)
        time.sleep(3)

print("Connected. Waiting for messages...", flush=True)

for message in consumer:
    print(f"Received message: {message.value}", flush=True)