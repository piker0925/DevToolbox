package com.back;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Testcontainers 기반 테스트는 JobRepositoryTest 사용")
class BackApplicationTests {

	@Test
	void contextLoads() {
	}

}
