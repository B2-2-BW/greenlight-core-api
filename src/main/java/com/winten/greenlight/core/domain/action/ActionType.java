package com.winten.greenlight.core.domain.action;

/**
 * Action의 동작 방식을 정의하는 타입입니다.
 * 사용자가 어떤 경로로 대기열 시스템을 마주하게 되는지를 결정합니다.
 * @version 1.0
 */
public enum ActionType {

    /**
     * 기존 웹사이트의 특정 페이지/기능에 직접 대기열을 적용하는 방식입니다.
     * 사용자가 보호된 URL({@code actionUrl})로 접근을 시도할 때 대기열이 트리거됩니다.
     * Backend(서버 리다이렉트) 방식과 Frontend(클라이언트 모달) 방식 모두에 해당합니다.
     */
    DIRECT,

    /**
     * 별도로 격리된 전용 랜딩 페이지를 통해 대기열에 진입하는 방식입니다.
     * 사용자는 사전에 공유된 랜딩 페이지 URL({@code landingId} 포함)로 접속하여 대기 후,
     * 설정된 최종 목적지({@code landingDestinationUrl})로 이동하게 됩니다.
     * 대규모 이벤트나 사전 예약 등 특정 목적을 위해 사용됩니다.
     */
    LANDING
}