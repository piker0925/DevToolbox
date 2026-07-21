package com.back.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * RTR(회전) 유예 초과 재사용 발동 기록(057). RefreshTokenService.reuseWithinGraceOrRevoke가
 * 해당 유저의 refresh token을 전부 폐기할 때 함께 남긴다 — 개별 이벤트만으로 "탈취 확정"은
 * 판단할 수 없어(같은 기기 멀티탭 레이스와 구분 불가) 관리자 화면에서는 유저별 발동 빈도로만 쓴다.
 */
@Entity
@Table(name = "refresh_token_theft_event")
@Getter
@NoArgsConstructor
public class RefreshTokenTheftEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;

    @Column(nullable = false, length = 45)
    private String ip;

    public RefreshTokenTheftEvent(Long userId, LocalDateTime detectedAt, String ip) {
        this.userId = userId;
        this.detectedAt = detectedAt;
        this.ip = ip;
    }
}
