package com.systems.config;

public class Config {
    public static final String SYSTEM_NAME =
            System.getenv().getOrDefault("SYSTEM_NAME", "A").toUpperCase();
    public static final String KAFKA_BOOTSTRAP_SERVERS =
            System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
    public static final int HTTP_PORT =
            Integer.parseInt(System.getenv().getOrDefault("HTTP_PORT", "8080"));

    public static final String TOPIC = "system-" + SYSTEM_NAME.toLowerCase();
    public static final String GROUP_ID = "consumer-group-" + SYSTEM_NAME.toLowerCase();
    public static final String[] ALL_SYSTEMS = {"A", "B", "C", "D"};
}
