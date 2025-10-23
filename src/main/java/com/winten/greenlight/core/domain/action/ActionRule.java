package com.winten.greenlight.core.domain.action;

import com.winten.greenlight.core.support.dto.AuditDto;
import com.winten.greenlight.core.support.dto.Hashable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 특정 {@link Action}에 대한 대기열 적용 여부를 결정하는 세부 조건 규칙입니다.
 * 요청의 쿼리 파라미터를 검사하여 대기열을 적용할지(INCLUDE), 또는 제외할지(EXCLUDE)를 판단하는 데 사용됩니다.
 *
 * @see Action#getDefaultRuleType() Action.defaultRuleType은 이 규칙이 어떻게 해석될지 결정합니다.
 * @version 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ActionRule extends AuditDto implements Hashable {

    /**
     * 이 규칙이 적용될 대상 {@link Action}의 고유 ID입니다.
     */
    private Long actionId;


    /**
     * ActionRule의 순번입니다.
     */
    private Long ruleSeq;

    /**
     * 검사할 요청의 쿼리 파라미터 키(key)입니다. (예: "productId", "eventCode")
     */
    private String paramName;

    /**
     * {@code paramName}에 해당하는 쿼리 파라미터의 값을 비교할 대상 값입니다. (예: "special-item-123")
     */
    private String paramValue;

    /**
     * {@code paramName}의 실제 값과 {@code paramValue}를 비교할 때 사용할 매칭 연산자입니다.
     * @see MatchOperator
     */
    private MatchOperator matchOperator;

    /**
     * 이 규칙의 역할이나 목적에 대한 사용자 친화적인 설명입니다. (예: "VIP 고객 대상 대기열 제외 규칙")
     */
    private String description;
}