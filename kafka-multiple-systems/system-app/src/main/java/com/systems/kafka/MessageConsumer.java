package com.systems.kafka;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.systems.config.Config;
import com.systems.model.Message;
import com.systems.store.MessageStore;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

public class MessageConsumer implements Runnable {
    private final MessageStore store;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Gson gson = new Gson();
    private volatile KafkaConsumer<String, String> consumer;

    public MessageConsumer(MessageStore store) {
        this.store = store;
    }

    @Override
    public void run() {
        Properties props = new Properties();
        props.put("bootstrap.servers", Config.KAFKA_BOOTSTRAP_SERVERS);
        props.put("group.id", Config.GROUP_ID);
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("auto.offset.reset", "latest");
        props.put("enable.auto.commit", "true");

        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(Config.TOPIC));
        System.out.println("System " + Config.SYSTEM_NAME + " listening on topic: " + Config.TOPIC);

        try {
            while (running.get()) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> record : records) {
                    try {
                        JsonObject json = gson.fromJson(record.value(), JsonObject.class);
                        String from = json.get("from").getAsString();
                        String content = json.get("content").getAsString();
                        long timestamp = json.get("timestamp").getAsLong();
                        store.add(new Message(from, content, timestamp));
                        System.out.println("[System " + Config.SYSTEM_NAME + "] Received from " + from + ": " + content);
                    } catch (Exception e) {
                        System.err.println("Failed to parse message: " + e.getMessage());
                    }
                }
            }
        } catch (WakeupException e) {
            if (running.get()) throw e;
        } finally {
            consumer.close();
        }
    }

    public void stop() {
        running.set(false);
        KafkaConsumer<String, String> c = consumer;
        if (c != null) c.wakeup();
    }
}
