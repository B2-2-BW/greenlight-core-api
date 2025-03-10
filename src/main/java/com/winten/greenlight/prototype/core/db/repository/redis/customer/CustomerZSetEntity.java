package com.winten.greenlight.prototype.core.db.repository.redis.customer;

import com.winten.greenlight.prototype.core.domain.customer.Customer;
import com.winten.greenlight.prototype.core.domain.customer.WaitingPhase;
import lombok.Data;

@Data
public class CustomerZSetEntity {
    private String customerId;
    private double score;
    private Long queueSize;
    private WaitingPhase waitingPhase;

    public String key() {
        return waitingPhase.queueName();
    }

    public String value() {
        return customerId;
    }

    public double score() {
    return score;
}

    public static CustomerZSetEntity of(Customer customer) {
        var entity = new CustomerZSetEntity();
        entity.setCustomerId(customer.getCustomerId());
        entity.setScore(customer.getScore());
        entity.setWaitingPhase(customer.getWaitingPhase());
        return entity;
    }

    public Customer toCustomer() {
        var Customer = new Customer();
        Customer.setCustomerId(customerId);
        Customer.setScore(score);
        Customer.setWaitingPhase(waitingPhase);
        return Customer;
    }
}
