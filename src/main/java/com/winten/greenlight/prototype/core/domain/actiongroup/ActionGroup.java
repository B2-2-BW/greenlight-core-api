package com.winten.greenlight.prototype.core.domain.actiongroup;

import com.winten.greenlight.prototype.core.domain.action.Action;
import com.winten.greenlight.prototype.core.support.dto.AuditDto;
import com.winten.greenlight.prototype.core.support.dto.Hashable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;


/**
 * 여러 {@link Action}을 묶어 공통된 자원 및 정책을 관리하는 그룹입니다.
 * 주로 성격이 유사한 Action들을 그룹으로 관리하여,
 * 그룹 단위로 동시 활성 사용자 수를 제어하는 데 사용됩니다.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ActionGroup extends AuditDto implements Hashable {

    /**
     * ActionGroup의 고유 식별자(ID)입니다.
     */
    private Long id;

    /**
     * 이 Action Group을 소유하고 관리하는 관리자의 고유 ID입니다.
     * 이 필드는 특정 관리자가 자신이 소유한 Action Group만 조회하거나 수정할 수 있도록
     * 권한을 제어하는 데 핵심적인 역할을 합니다.
     *
     */
    private String ownerId;

    /**
     * ActionGroup을 식별하기 위한 사용자 친화적인 이름입니다. (예: "상품 관련 액션 그룹", "티켓 예매 그룹")
     */
    private String name;

    /**
     * ActionGroup의 역할이나 목적에 대한 상세 설명입니다.
     */
    private String description;

    /**
     * 이 그룹에 속한 모든 Action에 걸쳐 허용되는 최대 동시 활성 사용자(또는 세션)의 수입니다.
     * 이 값을 초과하는 요청은 대기열로 보내집니다.
     */
    private Integer maxActiveCustomers;

    /**
     * 그룹 전체의 활성화 여부를 나타내는 플래그입니다.
     * {@code false}일 경우, 이 그룹에 속한 모든 Action의 대기열 기능이 비활성화됩니다.
     */
    private Boolean enabled;
}