package com.winten.greenlight.core.support.publisher;

import com.winten.greenlight.core.domain.customer.WaitStatus;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ActionEvent {
    private WaitStatus waitStatus;
    private Long actionGroupId;
    private Long actionId;
    private String customerId;
    private String recordId;
    private Long timestamp;
}