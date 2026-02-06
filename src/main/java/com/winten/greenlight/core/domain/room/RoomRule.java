package com.winten.greenlight.core.domain.room;

import com.winten.greenlight.core.domain.action.MatchOperator;
import com.winten.greenlight.core.support.dto.AuditDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class RoomRule extends AuditDto {
    private String roomId;
    private Long ruleSeq;
    private String siteId;
    private String value;
    private MatchOperator matchOperator;
    private String description;
}