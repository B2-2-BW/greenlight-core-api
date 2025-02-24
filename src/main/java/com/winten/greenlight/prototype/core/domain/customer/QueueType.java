package com.winten.greenlight.prototype.core.domain.customer;

public enum QueueType {
    WAITING("proto-queue:waiting"),
    READY("proto-queue:ready");

    private final String queueName;

    QueueType(String queueName) {
        this.queueName = queueName;
    }

    public String queueName() {
        return this.queueName;
    }
}