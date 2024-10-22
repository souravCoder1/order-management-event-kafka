package org.producer.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.producer.domain.OrderEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@Slf4j
public class OrderEventProducer {

    @Autowired
    KafkaTemplate<Integer, String> kafkaTemplate;

    String topic = "order-events"; // Updated topic name
    @Autowired
    ObjectMapper objectMapper;

    public void sendOrderEvent(OrderEvent orderEvent) throws JsonProcessingException {
        Integer key = orderEvent.getOrderEventId();
        String value = objectMapper.writeValueAsString(orderEvent);

        CompletableFuture<SendResult<Integer, String>> sendResultCompletableFuture
                = kafkaTemplate.sendDefault(key, value);

        sendResultCompletableFuture.whenComplete((result, ex) -> {
            if (ex != null) {
                handleFailure(key, value, ex);
            } else {
                handleSuccess(key, value, result);
            }
        });
    }

    public CompletableFuture<SendResult<Integer, String>> sendOrderEvent_Approach2(OrderEvent orderEvent) throws JsonProcessingException {
        Integer key = orderEvent.getOrderEventId();
        String value = objectMapper.writeValueAsString(orderEvent);

        ProducerRecord<Integer, String> producerRecord = buildProducerRecord(key, value, topic);

        CompletableFuture<SendResult<Integer, String>> sendResultCompletableFuture = kafkaTemplate.send(producerRecord);

        // Replacing listenableFuture.addCallback with whenComplete
        sendResultCompletableFuture.whenComplete((result, ex) -> {
            if (ex != null) {
                handleFailure(key, value, ex);
            } else {
                handleSuccess(key, value, result);
            }
        });

        return sendResultCompletableFuture;
    }

    private ProducerRecord<Integer, String> buildProducerRecord(Integer key, String value, String topic) {
        List<Header> recordHeaders = List.of(new RecordHeader("event-source", "scanner".getBytes()));
        return new ProducerRecord<>(topic, null, key, value, recordHeaders);
    }

    public SendResult<Integer, String> sendOrderEventSynchronous(OrderEvent orderEvent) throws JsonProcessingException, ExecutionException, InterruptedException, TimeoutException {
        Integer key = orderEvent.getOrderEventId();
        String value = objectMapper.writeValueAsString(orderEvent);
        SendResult<Integer, String> sendResult = null;

        try {
            sendResult = kafkaTemplate.sendDefault(key, value).get(1, TimeUnit.SECONDS);
        } catch (ExecutionException | InterruptedException e) {
            log.error("ExecutionException/InterruptedException Sending the Message and the exception is {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Exception Sending the Message and the exception is {}", e.getMessage());
            throw e;
        }

        return sendResult;
    }

    private void handleFailure(Integer key, String value, Throwable ex) {
        log.error("Error Sending the Message, key: {}, value: {}. Exception: {}", key, value, ex.getMessage());
        // Consider adding retry logic or alerting here if needed
    }

    private void handleSuccess(Integer key, String value, SendResult<Integer, String> result) {
        log.info("Message Sent Successfully for key: {}. Value: {}, Partition: {}", key, value, result.getRecordMetadata().partition());
    }
}
