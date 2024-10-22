package com.producer.controller;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.producer.domain.Order;
import org.producer.domain.OrderEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(topics = {"order-events"}, partitions = 3)
@TestPropertySource(properties = {"spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.admin.properties.bootstrap.servers=${spring.embedded.kafka.brokers}"})
public class OrderEventsControllerIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker;

    private Consumer<Integer, String> consumer;

    @BeforeEach
    void setUp() {
        Map<String, Object> configs = new HashMap<>(KafkaTestUtils.consumerProps("group1", "true", embeddedKafkaBroker));
        configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        consumer = new DefaultKafkaConsumerFactory<>(configs, new IntegerDeserializer(), new StringDeserializer()).createConsumer();
        embeddedKafkaBroker.consumeFromAllEmbeddedTopics(consumer);
    }

    @AfterEach
    void tearDown() {
        consumer.close();
    }

    @Test
    @Timeout(5)
    void postOrderEvent() throws InterruptedException {
        // given
        Order order = Order.builder()
                .orderId(123)
                .productName("Laptop")
                .productPrice(new BigDecimal("1200.00"))
                .quantity(2)
                .customerName("John Doe")
                .build();

        OrderEvent orderEvent = OrderEvent.builder()
                .orderEventId(null)
                .order(order)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.set("content-type", MediaType.APPLICATION_JSON.toString());
        HttpEntity<OrderEvent> request = new HttpEntity<>(orderEvent, headers);

        // when
        ResponseEntity<OrderEvent> responseEntity = restTemplate.exchange("/v1/orderEvent", HttpMethod.POST, request, OrderEvent.class);

        // then
        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());

        ConsumerRecords<Integer, String> consumerRecords = KafkaTestUtils.getRecords(consumer);
        assert consumerRecords.count() == 1;
        consumerRecords.forEach(record -> {
            String expectedRecord = "{\"orderEventId\":null,\"orderEventType\":\"NEW\",\"order\":{\"orderId\":123,\"productName\":\"Laptop\",\"productPrice\":1200.00,\"quantity\":2,\"customerName\":\"John Doe\"}}";
            String value = record.value();
            assertEquals(expectedRecord, value);
        });
    }

    @Test
    @Timeout(5)
    void putOrderEvent() throws InterruptedException {
        // given
        Order order = Order.builder()
                .orderId(456)
                .productName("Smartphone")
                .productPrice(new BigDecimal("800.00"))
                .quantity(1)
                .customerName("Jane Smith")
                .build();

        OrderEvent orderEvent = OrderEvent.builder()
                .orderEventId(123)
                .order(order)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.set("content-type", MediaType.APPLICATION_JSON.toString());
        HttpEntity<OrderEvent> request = new HttpEntity<>(orderEvent, headers);

        // when
        ResponseEntity<OrderEvent> responseEntity = restTemplate.exchange("/v1/orderEvent", HttpMethod.PUT, request, OrderEvent.class);

        // then
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        ConsumerRecords<Integer, String> consumerRecords = KafkaTestUtils.getRecords(consumer);
        assert consumerRecords.count() == 2;
        consumerRecords.forEach(record -> {
            if (record.key() != null) {
                String expectedRecord = "{\"orderEventId\":123,\"orderEventType\":\"UPDATE\",\"order\":{\"orderId\":456,\"productName\":\"Smartphone\",\"productPrice\":800.00,\"quantity\":1,\"customerName\":\"Jane Smith\"}}";
                String value = record.value();
                assertEquals(expectedRecord, value);
            }
        });
    }
}
