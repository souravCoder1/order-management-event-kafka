package producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.producer.domain.Order;
import org.producer.domain.OrderEvent;
import org.producer.event.OrderEventProducer;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrderEventProducerUnitTest {

    @Mock
    KafkaTemplate<Integer, String> kafkaTemplate;

    @Spy
    ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    OrderEventProducer eventProducer;

    @Test
    void sendOrderEvent_Approach2_failure() throws JsonProcessingException, ExecutionException, InterruptedException {
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

        CompletableFuture<SendResult<Integer, String>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Exception Calling Kafka"));

        when(kafkaTemplate.send(isA(ProducerRecord.class))).thenReturn(future);

        // when
        assertThrows(Exception.class, () -> eventProducer.sendOrderEvent_Approach2(orderEvent).get());
    }

    @Test
    void sendOrderEvent_Approach2_success() throws JsonProcessingException, ExecutionException, InterruptedException {
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

        String record = objectMapper.writeValueAsString(orderEvent);

        ProducerRecord<Integer, String> producerRecord = new ProducerRecord<>("order-events", orderEvent.getOrderEventId(), record);
        RecordMetadata recordMetadata = new RecordMetadata(new TopicPartition("order-events", 1),
                1, 1, System.currentTimeMillis(), 1, 2);
        SendResult<Integer, String> sendResult = new SendResult<>(producerRecord, recordMetadata);

        CompletableFuture<SendResult<Integer, String>> future = CompletableFuture.completedFuture(sendResult);

        when(kafkaTemplate.send(isA(ProducerRecord.class))).thenReturn(future);

        // when
        CompletableFuture<SendResult<Integer, String>> listenableFuture = eventProducer.sendOrderEvent_Approach2(orderEvent);

        // then
        SendResult<Integer, String> sendResult1 = listenableFuture.get();
        assert sendResult1.getRecordMetadata().partition() == 1;
    }
}
