package org.consumer.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.consumer.config.OrderEventsConsumerConfig;
import org.consumer.entity.FailureRecord;
import org.consumer.repo.FailureRecordRepository;
import org.consumer.service.OrderEventsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RetryScheduler {

    @Autowired
    private OrderEventsService orderEventsService;

    @Autowired
    private FailureRecordRepository failureRecordRepository;

    @Scheduled(fixedRate = 10000)
    public void retryFailedRecords() {
        log.info("Retrying Failed Records Started!");
        var status = OrderEventsConsumerConfig.RETRY;
        
        failureRecordRepository.findAllByStatus(status)
                .forEach(failureRecord -> {
                    try {
                        var consumerRecord = buildConsumerRecord(failureRecord);
                        orderEventsService.processOrderEvent(consumerRecord);
                        failureRecord.setStatus(OrderEventsConsumerConfig.SUCCESS);
                    } catch (Exception e) {
                        log.error("Exception in retryFailedRecords: ", e);
                    }
                });
    }

    private ConsumerRecord<Integer, String> buildConsumerRecord(FailureRecord failureRecord) {
        return new ConsumerRecord<>(
                failureRecord.getTopic(),
                failureRecord.getPartition(),
                failureRecord.getOffsetValue(),
                failureRecord.getKey(),
                failureRecord.getErrorRecord()
        );
    }
}
