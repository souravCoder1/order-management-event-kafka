package org.consumer.service;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.consumer.entity.FailureRecord;
import org.consumer.repo.FailureRecordRepository;
import org.springframework.stereotype.Service;

@Service
public class FailureService {

    private final FailureRecordRepository failureRecordRepository;

    public FailureService(FailureRecordRepository failureRecordRepository) {
        this.failureRecordRepository = failureRecordRepository;
    }

    public void saveFailedRecord(ConsumerRecord<Integer, String> record, Exception exception, String recordStatus) {
        var failureRecord = new FailureRecord(
                null,
                record.topic(),
                record.key(),
                record.value(),
                record.partition(),
                record.offset(),
                exception.getCause().getMessage(),
                recordStatus
        );

        failureRecordRepository.save(failureRecord);
    }
}
