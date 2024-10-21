package org.producer.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class Order {

    @NotNull
    private Integer orderId;

    @NotBlank
    private String productName;

    @NotNull
    private BigDecimal productPrice;

    @NotNull
    private Integer quantity;

    @NotBlank
    private String customerName;
}
