package com.systems;

import com.systems.config.Config;
import com.systems.http.ApiServer;
import com.systems.kafka.MessageConsumer;
import com.systems.kafka.MessageProducer;
import com.systems.store.MessageStore;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("Starting System " + Config.SYSTEM_NAME);

        MessageStore store = new MessageStore();
        MessageProducer producer = new MessageProducer();
        MessageConsumer consumer = new MessageConsumer(store);

        Thread consumerThread = new Thread(consumer, "kafka-consumer");
        consumerThread.setDaemon(true);
        consumerThread.start();

        ApiServer server = new ApiServer(store, producer);
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down System " + Config.SYSTEM_NAME);
            consumer.stop();
            producer.close();
            server.stop();
        }, "shutdown-hook"));
    }
}
