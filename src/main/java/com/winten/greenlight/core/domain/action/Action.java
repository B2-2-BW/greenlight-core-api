package com.winten.greenlight.core.domain.action;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.winten.greenlight.core.support.dto.AuditDto;
import com.winten.greenlight.core.support.dto.Hashable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 대기열 정책이 적용되는 사용자의 행동 단위를 정의하는 핵심 클래스입니다.
 * 웹사이트의 특정 URL 접근이나 이벤트 참여 등이 하나의 Action이 될 수 있습니다.
 *
 * @see ActionGroup ActionGroup은 이 Action이 속한 그룹을 정의합니다.
 * @see ActionRule ActionRule은 이 Action에 적용될 세부 규칙을 정의합니다.
 * @version 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class Action extends AuditDto implements Hashable {

    /**
     * Action의 고유 식별자(ID)입니다.
     */
    private Long id;

    /**
     * 이 Action이 속한 {@link ActionGroup}의 고유 ID입니다.
     * 하나의 Action은 반드시 하나의 ActionGroup에 속하며, 그룹의 정책(예: 최대 동시 접속자 수)을 따릅니다.
     */
    private Long actionGroupId;

    /**
     * 이 Action을 소유하고 관리하는 관리자의 고유 ID입니다.
     * 이 필드는 특정 관리자가 자신이 소유한 Action Group만 조회하거나 수정할 수 있도록
     * 권한을 제어하는 데 핵심적인 역할을 합니다.
     *
     */
    private String ownerId;

    /**
     * Action을 식별하기 위한 사용자 친화적인 이름입니다. (예: "상품 상세 페이지 보기", "콘서트 티켓 예매")
     */
    private String name;

    /**
     * Action의 활성화 여부입니다. true이면 활성화, false이면 비활성화됩니다.
     */
    private boolean enabled;

    /**
     * 대기열을 트리거할 대상 URL의 경로(Path)입니다.
     * 쿼리 파라미터는 제외하며, 세부 조건은 {@link ActionRule}을 통해 정의됩니다. (예: "/products/detail")
     * {@code actionType}이 {@link ActionType#DIRECT}일 때 사용됩니다.
     */
    private String actionUrl;

    /**
     * Action의 대기열 적용 방식을 결정합니다.
     * @see ActionType
     */
    private ActionType actionType;

    /**
     * 랜딩 페이지 식별을 위한 고유 ID입니다.
     * 이 값은 랜딩 페이지 URL의 일부로 사용되어 사용자를 특정 대기열로 안내합니다. (예: https://wait.example.com/{landingId})
     * {@code actionType}이 {@link ActionType#LANDING}일 때 사용됩니다.
     */
    private String landingId;

    /**
     * 랜딩 페이지에서 대기 가능 기간의 시작 시간입니다.
     * 이 시간 이전에는 랜딩 페이지에 접속해도 대기할 수 없습니다.
     * {@code actionType}이 {@link ActionType#LANDING}일 때 사용됩니다.
     */
    private LocalDateTime landingStartAt;

    /**
     * 랜딩 페이지에서 대기 가능 기간의 종료 시간입니다.
     * 이 시간 이후에는 랜딩 페이지에 접속해도 대기할 수 없습니다.
     * {@code actionType}이 {@link ActionType#LANDING}일 때 사용됩니다.
     */
    private LocalDateTime landingEndAt;

    /**
     * 랜딩 페이지에서 대기 종료 후 사용자를 이동시킬 최종 목적지 URL입니다.
     * (예: "https://www.example-event.com/main")
     * {@code actionType}이 {@link ActionType#LANDING}일 때 사용됩니다.
     */
    private String landingDestinationUrl;

    /**
     * 이 Action에 연결된 {@link ActionRule}들을 평가하는 기본 전략을 정의합니다.
     * @see DefaultRuleType
     */
    private DefaultRuleType defaultRuleType;

    /**
     * 이 Action에 연결된 {@link ActionRule}입니다.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ActionRule> actionRules;
}