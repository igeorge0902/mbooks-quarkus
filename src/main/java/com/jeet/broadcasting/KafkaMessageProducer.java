package com.jeet.broadcasting;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

public class KafkaMessageProducer {
    private static final String TOPIC = "ios-movies-notifications2";
    private KafkaProducer<String, String> producer;

    public KafkaMessageProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "mbook-kafka:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

        producer = new KafkaProducer<>(props);
    }

    public void sendMessage(String message) {
        ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, message);
        producer.send(record, (metadata, exception) -> {
            if (exception == null) {
                System.out.println("Message sent to topic " + TOPIC + " | Offset: " + metadata.offset());
            } else {
                exception.printStackTrace();
            }
        });
    }

    public void close() {
        producer.close();
    }
}
