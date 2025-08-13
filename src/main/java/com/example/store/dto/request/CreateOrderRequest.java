package com.example.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateOrderRequest(
        @NotBlank String description,
        @NotNull Long customerId,
        @NotNull @NotEmpty List<Long> productIds
) {
}
