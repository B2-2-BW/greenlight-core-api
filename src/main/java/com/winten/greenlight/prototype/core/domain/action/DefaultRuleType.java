package com.winten.greenlight.prototype.core.domain.action;

/**
 * Action에 연결된 규칙({@code ActionRule})들을 어떤 방식으로 평가할지 결정하는 전략입니다.
 * 규칙이 없는 경우 또는 여러 규칙이 있을 때의 기본 동작을 정의합니다.
 */
public enum DefaultRuleType {

    /**
     * 모든 요청에 대기열을 적용합니다.
     * 연결된 {@code ActionRule}이 있더라도 무시하고 항상 대기열을 활성화합니다.
     * 가장 간단하고 포괄적인 적용 방식입니다.
     */
    ALL,

    /**
     * 요청이 연결된 {@code ActionRule} 중 하나라도 일치할 경우에만 대기열을 적용합니다. (Inclusion/Allow-list 방식)
     * 기본적으로는 대기열을 적용하지 않으며, 특정 조건의 요청만 선별하여 대기시키고 싶을 때 사용합니다.
     * 예: 특정 상품 ID를 포함한 요청에만 대기열 적용
     */
    INCLUDE,

    /**
     * 요청이 연결된 {@code ActionRule} 중 하나라도 일치할 경우 대기열을 적용하지 않습니다. (Exclusion/Deny-list 방식)
     * 기본적으로 모든 요청에 대기열을 적용하지만, 특정 조건의 요청은 예외적으로 통과시키고 싶을 때 사용합니다.
     * 예: 내부 테스트용 파라미터가 포함된 요청은 대기열 미적용
     */
    EXCLUDE
}