package org.consumer.entity;

import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Entity
public class OrderEvent {

    @Id
    @GeneratedValue
    private Integer orderEventId;
    @Enumerated(EnumType.STRING)
    private OrderEventType orderEventType;
    @OneToOne(mappedBy = "orderEvent", cascade = {CascadeType.ALL})
    @ToString.Exclude
    private Order order;

}
