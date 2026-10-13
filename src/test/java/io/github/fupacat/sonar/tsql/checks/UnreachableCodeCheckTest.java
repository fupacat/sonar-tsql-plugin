package io.github.fupacat.sonar.tsql.checks;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UnreachableCodeCheckTest {

    private final UnreachableCodeCheck check = new UnreachableCodeCheck();

    @Test
    void testFlagsStatementsAfterReturn() {
        String sql = "BEGIN IF 1 = 1 RETURN; SELECT 1; END;";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0)).contains("unreachable");
    }

    @Test
    void testAllowsReachableCode() {
        String sql = "BEGIN IF 1 = 1 SELECT 1; ELSE SELECT 2; END;";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).isEmpty();
    }
}
