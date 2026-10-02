package com.winten.greenlight.core.support.redis;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RedisFailOpenGateTest {
    @Test
    void opensAfterThreeConsecutiveFailuresAndClosesOnSuccess() {
        var gate = new RedisFailOpenGate();
        gate.onFailure();
        gate.onFailure();
        assertThat(gate.isOpen()).isFalse();
        gate.onFailure();
        assertThat(gate.isOpen()).isTrue();
        gate.onSuccess();
        assertThat(gate.isOpen()).isFalse();
    }
}
