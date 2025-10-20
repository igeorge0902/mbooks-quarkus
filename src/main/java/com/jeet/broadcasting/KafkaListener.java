package com.jeet.broadcasting;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

@ApplicationScoped
@Startup
public class KafkaListener {
    private KafkaConsumer<String, String> consumer;
    private Thread consumerThread;
    private volatile boolean running = true;

    @PostConstruct
    public void startConsumer() {
        String bootstrapUrl = System.getenv().getOrDefault("BOOTSTRAP_URL", "localhost:9092");

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapUrl);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "ios-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");

        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("ios-movies-notifications"));

        consumerThread = new Thread(() -> {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, String> record : records) {
                    WebSocketServer.broadcastMessage(record.value());
                    System.out.println("Sent to WebSocket: " + record.value());
                }
            }
        });
        consumerThread.start();
    }

    @PreDestroy
    void stopConsumer() {
        running = false;
        try {
            // Wait for the consumer thread to exit gracefully
            consumerThread.join(2000);
        } catch (InterruptedException ignored) {
        }
    }
}
