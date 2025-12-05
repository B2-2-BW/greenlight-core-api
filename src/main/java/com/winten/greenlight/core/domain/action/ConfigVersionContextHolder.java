package com.winten.greenlight.core.domain.action;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class ConfigVersionContextHolder {
    private final AtomicReference<String> currentConfigVersion = new AtomicReference<>("initial");

    private ConfigVersionContextHolder() {
    }

    public String get() {
        return currentConfigVersion.get();
    }

    public void set(String newVersion) {
        currentConfigVersion.set(newVersion);
    }

    public boolean isCurrentVersion(String version) {
        if (currentConfigVersion.get() == null) {
            return false;
        }
        return currentConfigVersion.get().equals(version);
    }
}