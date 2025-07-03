package com.winten.greenlight.prototype.core.db.repository.redis.customer;

import com.winten.greenlight.prototype.core.domain.customer.WaitStatus;
import lombok.Data;

@Data
public class CustomerEntity {
    private String customerId;
    private double score;
    private Long queueSize;
    private WaitStatus waitStatus;
}