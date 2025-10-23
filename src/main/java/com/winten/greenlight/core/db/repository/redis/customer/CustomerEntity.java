package com.winten.greenlight.core.db.repository.redis.customer;

import com.winten.greenlight.core.domain.customer.WaitStatus;
import lombok.Data;

@Data
public class CustomerEntity {
    private String customerId;
    private double score;
    private Long queueSize;
    private WaitStatus waitStatus;
}