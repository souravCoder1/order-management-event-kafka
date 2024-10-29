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
public class OrderEventsConsumer {

    @Autowired
    private OrderEventsService orderEventsService;

    @KafkaListener(
            topics = {"order-events"},
            autoStartup = "${orderListener.startup:true}",
            groupId = "order-events-listener-group")
    public void onMessage(ConsumerRecord<Integer, String> consumerRecord) throws JsonProcessingException {

        log.info("ConsumerRecord : {} ", consumerRecord);
        orderEventsService.processOrderEvent(consumerRecord);

    }
}
