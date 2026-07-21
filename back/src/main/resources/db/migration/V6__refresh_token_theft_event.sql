-- V6: RTR 탈취 감지(유예 초과 재사용) 발동 로그 (057, ADR-0024 참고)
-- 오탐(같은 기기 멀티탭 레이스)과 진짜 탈취(다른 IP에서 재사용)를 구분할 근거로 IP를 남긴다.
-- 개별 이벤트 단위로 "탈취 확정"은 못 하고, 유저별 발동 빈도 지표로만 관리자 화면에 노출한다.

create table refresh_token_theft_event (
    id          bigint       not null auto_increment,
    user_id     bigint       not null,
    detected_at datetime(6)  not null,
    ip          varchar(45)  not null,
    primary key (id),
    key idx_refresh_token_theft_event_user_id (user_id)
) engine=InnoDB;
