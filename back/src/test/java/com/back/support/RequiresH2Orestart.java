package com.back.support;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * HWP/HWPX 임포트에 필요한 LibreOffice H2Orestart(shared) 확장이 없는 환경에서는
 * 테스트를 실패 대신 스킵한다. 배포/CI는 항상 있으므로 정상 실행되고, macOS
 * Homebrew Cask LibreOffice처럼 unopkg --shared 설치가 막힌 로컬 환경만 스킵된다
 * (docs/adr/0029 "macOS 로컬 개발 환경 주의사항" 참조).
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(RequiresH2OrestartCondition.class)
public @interface RequiresH2Orestart {
}
