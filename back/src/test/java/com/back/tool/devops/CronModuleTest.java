package com.back.tool.devops;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolResult;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CronModuleTest {

    private static String[] lines(ToolResult result) {
        return result.textResult().lines().filter(l -> !l.isBlank()).toArray(String[]::new);
    }

    @Test
    void dailyMidnightCronProducesFiveDistinctMidnightTimes() {
        CronModule module = new CronModule();
        ToolResult result = module.process(new ToolInput(
                List.of(), Map.of("expression", "0 0 * * *")
        ));

        assertThat(result.isFile()).isFalse();
        String[] lines = lines(result);
        // 기본 count=5 정확히. "0 0 * * *"는 매일 자정이므로 now()와 무관하게 시각은 00:00:00이어야 한다.
        assertThat(lines).hasSize(5);
        assertThat(lines).allMatch(l -> l.contains(" 00:00:00 "));
        // 같은 시각을 반복 출력하는 뮤턴트 방지 — 5개가 모두 다른 날짜여야 한다.
        assertThat(Arrays.stream(lines).distinct().count()).isEqualTo(5);
    }

    @Test
    void parsesMinuteHourAndDayOfWeekFields() {
        CronModule module = new CronModule();
        ToolResult result = module.process(new ToolInput(
                List.of(), Map.of("expression", "30 14 * * 1", "count", "3")
        ));

        String[] lines = lines(result);
        assertThat(lines).hasSize(3);
        // "30 14 * * 1" = 매주 월요일 14:30. 분/시/요일 필드를 실제로 읽는지 검증
        // (필드 인덱스를 뒤바꾸는 뮤턴트를 잡는다).
        for (String line : lines) {
            assertThat(line).contains(" 14:30:00 ");
            LocalDate date = LocalDate.parse(line.substring(0, 10));
            assertThat(date.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        }
    }

    @Test
    void respectsCountParam() {
        CronModule module = new CronModule();
        ToolResult result = module.process(new ToolInput(
                List.of(), Map.of("expression", "0 * * * *", "count", "3")
        ));

        long count = result.textResult().lines().filter(l -> !l.isBlank()).count();
        assertThat(count).isEqualTo(3);
    }

    @Test
    void moduleMetadata() {
        CronModule module = new CronModule();
        assertThat(module.getId()).isEqualTo("cron");
        assertThat(module.isHeavy()).isFalse();
        assertThat(module.getCategory()).isEqualTo("devops");
    }
}
