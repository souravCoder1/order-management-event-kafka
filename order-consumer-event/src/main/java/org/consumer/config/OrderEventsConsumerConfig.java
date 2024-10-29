package org.consumer.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.consumer.service.FailureService;
import org.consumer.service.OrderEventsService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.*;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.util.backoff.FixedBackOff;

import java.util.List;

@Configuration
@EnableKafka
@Slf4j
public class OrderEventsConsumerConfig {

    public static final String RETRY = "RETRY";
    public static final String SUCCESS = "SUCCESS";
    public static final String DEAD = "DEAD";

    @Autowired
    OrderEventsService orderEventsService; // Updated to OrderEventsService

    @Autowired
    KafkaProperties kafkaProperties;

    @Autowired
    KafkaTemplate<Integer, String> kafkaTemplate; // Specify types for KafkaTemplate

    @Autowired
    FailureService failureService;

    @Value("${topics.retry:order-events.RETRY}") // Update topic name
    private String retryTopic;

    @Value("${topics.dlt:order-events.DLT}") // Update topic name
    private String deadLetterTopic;

    public DeadLetterPublishingRecoverer publishingRecoverer() {
        return new DeadLetterPublishingRecoverer(kafkaTemplate, (r, e) -> {
            log.error("Exception in publishingRecoverer : {} ", e.getMessage(), e);
            if (e.getCause() instanceof RecoverableDataAccessException) {
                return new TopicPartition(retryTopic, r.partition());
            } else {
                return new TopicPartition(deadLetterTopic, r.partition());
            }
        });
    }

    ConsumerRecordRecoverer consumerRecordRecoverer = (record, exception) -> {
        log.error("Exception is : {} Failed Record : {} ", exception, record);
        if (exception.getCause() instanceof RecoverableDataAccessException) {
            log.info("Inside the recoverable logic");
            // Add any Recovery Code here.
            // failureService.saveFailedRecord((ConsumerRecord<Integer, String>) record, exception, RETRY);
        } else {
            log.info("Inside the non-recoverable logic and skipping the record : {}", record);
        }
    };

    public DefaultErrorHandler errorHandler() {
        var exceptionToIgnoreList = List.of(
                IllegalArgumentException.class
        );

        ExponentialBackOffWithMaxRetries expBackOff = new ExponentialBackOffWithMaxRetries(2);
        expBackOff.setInitialInterval(1_000L);
        expBackOff.setMultiplier(2.0);
        expBackOff.setMaxInterval(2_000L);

        var fixedBackOff = new FixedBackOff(1_000L, 2L);

        var defaultErrorHandler = new DefaultErrorHandler(
                publishingRecoverer(),
                fixedBackOff
        );

        exceptionToIgnoreList.forEach(defaultErrorHandler::addNotRetryableExceptions);

        defaultErrorHandler.setRetryListeners(
                (record, ex, deliveryAttempt) ->
                        log.info("Failed Record in Retry Listener  exception : {} , deliveryAttempt : {} ", ex.getMessage(), deliveryAttempt)
        );

        return defaultErrorHandler;
    }

    @Bean
    @ConditionalOnMissingBean(name = "kafkaListenerContainerFactory")
    ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ObjectProvider<ConsumerFactory<Object, Object>> kafkaConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, kafkaConsumerFactory
                .getIfAvailable(() -> new DefaultKafkaConsumerFactory<>(this.kafkaProperties.buildConsumerProperties())));
        factory.setConcurrency(3);
        factory.setCommonErrorHandler(errorHandler());
        return factory;
    }
}
