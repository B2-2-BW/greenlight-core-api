package com.winten.greenlight.prototype.core.support.dto;

import java.time.LocalDateTime;

public abstract class AuditDto {
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}