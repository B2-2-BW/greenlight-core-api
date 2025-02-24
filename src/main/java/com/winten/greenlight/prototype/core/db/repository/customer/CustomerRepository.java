package com.winten.greenlight.prototype.core.db.repository.customer;

import com.winten.greenlight.prototype.core.domain.customer.Customer;
import org.springframework.stereotype.Repository;

@Repository
public class CustomerRepository {
    public Customer createQueueMember() {
        return new Customer();
    }

}