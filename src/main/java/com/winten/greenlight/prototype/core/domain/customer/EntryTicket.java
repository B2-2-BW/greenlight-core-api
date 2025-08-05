package com.winten.greenlight.prototype.core.domain.customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 사용자 대기 시 사용되는 대기표입니다.
 * @version 1.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EntryTicket {
    private Long actionId;
    private String customerId;
    private String destinationUrl;
    private Long timestamp;
    private WaitStatus waitStatus;
    private String jwtToken; 
}