package com.winten.greenlight.prototype.core.api.controller.customer;

import com.winten.greenlight.prototype.core.domain.customer.Customer;
import com.winten.greenlight.prototype.core.domain.customer.CustomerQueueInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerRegistrationResponseDto {
    private String customerId;
    private double score;
    private CustomerQueueInfo queueInfo;

    public CustomerRegistrationResponseDto(Customer customer, CustomerQueueInfo queueInfo) {
        // TODO
    }
}