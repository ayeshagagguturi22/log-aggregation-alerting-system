package com.logagg.processor.config;

import com.logagg.processor.model.LogEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, LogEvent> consumerFactory() {
        JsonDeserializer<LogEvent> deserializer = new JsonDeserializer<>(LogEvent.class);
        deserializer.setRemoveTypeHeaders(false);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeMapperForKey(true);

        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "log-processor-group");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, deserializer);

        // Disable auto commit — we will manually commit offset ONLY after
        // successful DB save. This prevents message loss on crash.
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        // How many messages Kafka sends to consumer in one poll.
        // We collect these into a batch and insert all at once into PostgreSQL.
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);

        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, LogEvent> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, LogEvent> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());

        // MANUAL_IMMEDIATE: offset is committed only when we call acknowledgment.acknowledge()
        // This gives us full control — offset moves forward only after DB save succeeds.
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // Enable batch listener — consumer method receives List<LogEvent>
        // instead of one LogEvent at a time.
        factory.setBatchListener(true);

        return factory;
    }
}
