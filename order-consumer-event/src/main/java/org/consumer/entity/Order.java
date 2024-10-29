package org.consumer.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Entity
public class Order {
    @Id
    private Integer id; // Renamed from bookId to id for clarity

    private String productName; // Changed from bookName to productName for order context
    private String customerName; // Changed from bookAuthor to customerName to reflect the ordering entity context

    @OneToOne
    @JoinColumn(name = "order_event_id") // Updated to relate to an order event instead of a library event
    private OrderEvent orderEvent; // Changed from LibraryEvent to OrderEvent to reflect the new context
}
