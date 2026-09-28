package dev.coreystevens.titlerate;

import dev.coreystevens.titlerate.support.PostgresTestcontainer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@Import(PostgresTestcontainer.class)
@SpringBootTest
class TitlerateJavaApplicationTests {

	@Test
	void contextLoads() {
	}

}
