package org.consumer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.consumer.entity.Order;
import org.consumer.entity.OrderEvent;
import org.consumer.repo.OrderEventsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class OrderEventsService {

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    KafkaTemplate<Integer, String> kafkaTemplate;

    @Autowired
    private OrderEventsRepository orderEventsRepository;

    public void processOrderEvent(ConsumerRecord<Integer, String> consumerRecord) throws JsonProcessingException {
        OrderEvent orderEvent = objectMapper.readValue(consumerRecord.value(), OrderEvent.class);
        log.info("OrderEvent : {} ", orderEvent);

        if (orderEvent.getOrderEventId() != null && (orderEvent.getOrderEventId() == 999)) {
            throw new RecoverableDataAccessException("Temporary Network Issue");
        }

        switch (orderEvent.getOrderEventType()) {
            case NEW:
                save(orderEvent);
                break;
            case UPDATE:
                // Validate the order event
                validate(orderEvent);
                save(orderEvent);
                break;
            default:
                log.info("Invalid Order Event Type");
        }
    }

    private void validate(OrderEvent orderEvent) {
        if (orderEvent.getOrderEventId() == null) {
            throw new IllegalArgumentException("Order Event Id is missing");
        }

        Optional<OrderEvent> orderEventOptional = orderEventsRepository.findById(orderEvent.getOrderEventId());
        if (!orderEventOptional.isPresent()) {
            throw new IllegalArgumentException("Not a valid Order Event");
        }
        log.info("Validation is successful for the order Event : {} ", orderEventOptional.get());
    }

    private void save(OrderEvent orderEvent) {
        orderEvent.getOrder().setOrderEvent(orderEvent);
        orderEventsRepository.save(orderEvent);
        log.info("Successfully Persisted the Order Event {} ", orderEvent);
    }

    public void handleRecovery(ConsumerRecord<Integer, String> record) {
        Integer key = record.key();
        String message = record.value();

        CompletableFuture<SendResult<Integer, String>> completableFuture = kafkaTemplate.sendDefault(key, message);
        completableFuture.whenComplete((result, ex) -> {
            if (ex != null) {
                handleFailure(key, message, ex);
            }

            else {
                handleSuccess(key, message, result);
            }
        });
    }

    private void handleFailure(Integer key, String value, Throwable ex) {
        log.error("Error Sending the Message and the exception is {}", ex.getMessage());
        try {
            throw ex;
        } catch (Throwable throwable) {
            log.error("Error in OnFailure: {}", throwable.getMessage());
        }
    }

    private void handleSuccess(Integer key, String value, SendResult<Integer, String> result) {
        log.info("Message Sent Successfully for the key : {} and the value is {} , partition is {}", key, value, result.getRecordMetadata().partition());
    }
}
