package org.consumer.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.consumer.service.OrderEventsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderEventsRetryConsumer {

    @Autowired
    private OrderEventsService orderEventsService;

    @KafkaListener(topics = {"${topics.order.retry}"}
            , autoStartup = "${retryListener.startup:true}"
            , groupId = "order-retry-listener-group")
    public void onMessage(ConsumerRecord<Integer, String> consumerRecord) throws JsonProcessingException {

        log.info("ConsumerRecord in Retry Consumer for Order: {} ", consumerRecord);
        orderEventsService.processOrderEvent(consumerRecord);
    }
}
