package io.github.fupacat.sonar.tsql.checks;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UnusedLocalVariableCheckTest {

    private final UnusedLocalVariableCheck check = new UnusedLocalVariableCheck();

    @Test
    void testFlagsUnusedDeclaredVariable() {
        String sql = "DECLARE @unused INT; SET @unused = 1;";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0)).contains("unused");
    }

    @Test
    void testAllowsUsedDeclaredVariable() {
        String sql = "DECLARE @used INT; SET @used = 1; SELECT @used;";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).isEmpty();
    }
}
