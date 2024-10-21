package org.producer.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.producer.domain.OrderEvent;
import org.producer.domain.OrderEventType;
import org.producer.event.OrderEventProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.support.SendResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ExecutionException;

@RestController
@Slf4j
public class OrderEventsController {

    @Autowired
    OrderEventProducer orderEventProducer;

    @PostMapping("/v1/orderevent")
    public ResponseEntity<OrderEvent> postOrderEvent(@RequestBody @Valid OrderEvent orderEvent) throws JsonProcessingException {

        //invoke kafka producer
        orderEvent.setOrderEventType(OrderEventType.NEW);
        orderEventProducer.sendOrderEvent_Approach2(orderEvent);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderEvent);
    }

    //PUT
    @PutMapping("/v1/orderevent")
    public ResponseEntity<?> putOrderEvent(@RequestBody @Valid OrderEvent orderEvent) throws JsonProcessingException, ExecutionException, InterruptedException {

        log.info("OrderEvent : {} ", orderEvent);
        if(orderEvent.getOrderEventId() == null){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please pass the OrderEventId");
        }

        orderEvent.setOrderEventType(OrderEventType.UPDATE);
        orderEventProducer.sendOrderEvent_Approach2(orderEvent);
        return ResponseEntity.status(HttpStatus.OK).body(orderEvent);
    }
}
