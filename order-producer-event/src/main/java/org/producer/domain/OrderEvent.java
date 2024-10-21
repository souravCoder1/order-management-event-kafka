package org.producer.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class OrderEvent {

    private Integer orderEventId;
    private OrderEventType orderEventType;

    @NotNull
    @Valid
    private Order order;

}
