package com.winten.greenlight.prototype.core.support.publisher;

import com.winten.greenlight.prototype.core.domain.customer.WaitStatus;
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