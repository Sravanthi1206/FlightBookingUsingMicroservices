package com.flightapp.config;

import com.flightapp.dto.BookingMessage;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import org.springframework.kafka.listener.DefaultErrorHandler;

import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    // Use the shared DTO type in the generics
    @Bean
    public ConsumerFactory<String, BookingMessage> consumerFactory() {

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // Wrap the JsonDeserializer with ErrorHandlingDeserializer
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

        // IMPORTANT: target type must match your shared DTO package
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.flightapp.dto.BookingMessage");

        // Restrict trusted packages to your DTO package (don't use "*")
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.flightapp.dto");

        // If producer does not send type headers and you're relying on default type above:
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        // Group id can be set here or via properties
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "email-group");

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, BookingMessage> kafkaListenerContainerFactory(
            ConsumerFactory<String, BookingMessage> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, BookingMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        // Simple error handler (adjust backoff/DLQ as needed)
        factory.setCommonErrorHandler(new DefaultErrorHandler());

        return factory;
    }
}
