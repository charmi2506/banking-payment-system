package com.banking.payment.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${app.kafka.topics.transaction-events}")
    private String transactionEventsTopic;

    @Value("${app.kafka.topics.audit-logs}")
    private String auditLogsTopic;

    @Value("${app.kafka.topics.notifications}")
    private String notificationsTopic;

    @Bean
    public NewTopic transactionEventsTopic() {
        return TopicBuilder.name(transactionEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic auditLogsTopic() {
        return TopicBuilder.name(auditLogsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic notificationsTopic() {
        return TopicBuilder.name(notificationsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
