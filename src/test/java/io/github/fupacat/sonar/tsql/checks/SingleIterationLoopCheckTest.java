package io.github.fupacat.sonar.tsql.checks;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SingleIterationLoopCheckTest {

    private final SingleIterationLoopCheck check = new SingleIterationLoopCheck();

    @Test
    void testFlagsSingleIterationLoop() {
        String sql = "WHILE 1 = 1 BEGIN SELECT 1; BREAK; END;";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0)).contains("one iteration");
    }

    @Test
    void testAllowsMultiIterationLoop() {
        String sql = "WHILE @i < 10 BEGIN SET @i = @i + 1; END;";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).isEmpty();
    }
}
