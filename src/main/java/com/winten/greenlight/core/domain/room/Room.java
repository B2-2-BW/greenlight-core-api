package com.winten.greenlight.core.domain.room;

import com.winten.greenlight.core.domain.action.DefaultRuleType;
import com.winten.greenlight.core.support.dto.AuditDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class Room extends AuditDto {
    private String roomId;
    private String siteId;
    private String name;
    private String description;
    private Integer maxTrafficPerSecond;
    private Integer capacity;
    private Boolean enabled;
    private DefaultRuleType defaultRuleType;
    private List<RoomRule> roomRules;
}