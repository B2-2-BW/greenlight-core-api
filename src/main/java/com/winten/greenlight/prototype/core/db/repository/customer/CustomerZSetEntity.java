package com.winten.greenlight.prototype.core.db.repository.customer;

import com.winten.greenlight.prototype.core.domain.customer.QueueType;
import lombok.Data;

@Data
public class CustomerZSetEntity {
    private final String customerId;
    private final double score;
    private QueueType queueType;

    public String key() {
        return queueType.queueName();
    }

    public String value() {
        return customerId;
    }

    public double score() {
    return score;
}
}