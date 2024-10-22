package controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.producer.controller.OrderEventsController;
import org.producer.domain.Order;
import org.producer.domain.OrderEvent;
import org.producer.event.OrderEventProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderEventsController.class)
@AutoConfigureMockMvc
public class OrderEventControllerUnitTest {

    @Autowired
    MockMvc mockMvc;

    ObjectMapper objectMapper = new ObjectMapper();

    @MockBean
    OrderEventProducer orderEventProducer;

    @Test
    void postOrderEvent() throws Exception {
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

        String json = objectMapper.writeValueAsString(orderEvent);
        when(orderEventProducer.sendOrderEvent_Approach2(isA(OrderEvent.class))).thenReturn(null);

        // expect
        mockMvc.perform(post("/v1/orderEvent")
                .content(json)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void postOrderEvent_4xx() throws Exception {
        // given
        Order order = Order.builder()
                .orderId(null)
                .productName(null)
                .productPrice(new BigDecimal("800.00"))
                .quantity(1)
                .customerName("Jane Smith")
                .build();

        OrderEvent orderEvent = OrderEvent.builder()
                .orderEventId(null)
                .order(order)
                .build();

        String json = objectMapper.writeValueAsString(orderEvent);
        when(orderEventProducer.sendOrderEvent_Approach2(isA(OrderEvent.class))).thenReturn(null);

        // expect
        String expectedErrorMessage = "order.productName - must not be blank, order.orderId - must not be null";
        mockMvc.perform(post("/v1/orderEvent")
                .content(json)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string(expectedErrorMessage));
    }

    @Test
    void updateOrderEvent() throws Exception {
        // given
        Order order = Order.builder()
                .orderId(123)
                .productName("Laptop")
                .productPrice(new BigDecimal("1200.00"))
                .quantity(2)
                .customerName("John Doe")
                .build();

        OrderEvent orderEvent = OrderEvent.builder()
                .orderEventId(123)
                .order(order)
                .build();

        String json = objectMapper.writeValueAsString(orderEvent);
        when(orderEventProducer.sendOrderEvent_Approach2(isA(OrderEvent.class))).thenReturn(null);

        // expect
        mockMvc.perform(
                put("/v1/orderEvent")
                        .content(json)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void updateOrderEvent_withNullOrderEventId() throws Exception {
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

        String json = objectMapper.writeValueAsString(orderEvent);
        when(orderEventProducer.sendOrderEvent_Approach2(isA(OrderEvent.class))).thenReturn(null);

        // expect
        mockMvc.perform(
                put("/v1/orderEvent")
                        .content(json)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string("Please pass the OrderEventId"));
    }
}
