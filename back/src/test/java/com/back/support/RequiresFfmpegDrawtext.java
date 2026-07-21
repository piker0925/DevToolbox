package com.back.support;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 텍스트 워터마크가 셸아웃하는 ffmpeg drawtext 필터가 없는 빌드(예: libfreetype/
 * libfontconfig 없이 빌드된 Homebrew 기본 ffmpeg)에서는 테스트를 실패 대신 스킵한다.
 * 배포/CI(apt ffmpeg)에는 항상 있으므로 정상 실행된다(docs/adr/0029 참조).
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(RequiresFfmpegDrawtextCondition.class)
public @interface RequiresFfmpegDrawtext {
}
