package com.systems.kafka;

import com.google.gson.JsonObject;
import com.systems.config.Config;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

public class MessageProducer {
    private final KafkaProducer<String, String> producer;

    public MessageProducer() {
        Properties props = new Properties();
        props.put("bootstrap.servers", Config.KAFKA_BOOTSTRAP_SERVERS);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("acks", "1");
        this.producer = new KafkaProducer<>(props);
    }

    public void send(String toSystem, String content) {
        String topic = "system-" + toSystem.toLowerCase();

        JsonObject json = new JsonObject();
        json.addProperty("from", Config.SYSTEM_NAME);
        json.addProperty("content", content);
        json.addProperty("timestamp", System.currentTimeMillis());

        ProducerRecord<String, String> record = new ProducerRecord<>(topic, json.toString());
        producer.send(record, (metadata, ex) -> {
            if (ex != null) {
                System.err.println("Failed to send to " + toSystem + ": " + ex.getMessage());
            } else {
                System.out.println("[System " + Config.SYSTEM_NAME + "] Sent to " + toSystem + ": " + content);
            }
        });
    }

    public void close() {
        producer.close();
    }
}
