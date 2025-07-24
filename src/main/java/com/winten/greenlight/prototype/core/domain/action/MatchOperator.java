package com.winten.greenlight.prototype.core.domain.action;

/**
 * {@code ActionRule}에서 요청 파라미터와 설정된 값을 비교할 때 사용할 연산자입니다.
 * 규칙의 매칭 논리를 정의합니다.
 */
public enum MatchOperator {

    /**
     * 파라미터 값과 규칙의 값이 정확히 일치하는지 확인합니다. (Exact Match)
     * 예: "productId"가 "special-item-123"과 정확히 같을 때 매칭
     */
    EQUAL,

    /**
     * 파라미터 값이 규칙의 값을 부분 문자열로 포함하는지 확인합니다.
     * 예: slitmCd에 "item"이 포함되어 있을 때 매칭
     */
    CONTAINS,

    /**
     * 파라미터 값이 규칙의 값으로 시작하는지 확인합니다.
     * 예: "couponCode"가 "SUMMER-"로 시작할 때 매칭
     */
    STARTSWITH,

    /**
     * 파라미터 값이 규칙의 값으로 끝나는지 확인합니다.
     * 예: "fileName"이 ".zip"으로 끝날 때 매칭
     */
    ENDSWITH
}