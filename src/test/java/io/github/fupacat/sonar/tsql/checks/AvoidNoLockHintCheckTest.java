package io.github.fupacat.sonar.tsql.checks;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AvoidNoLockHintCheckTest {

    private final AvoidNoLockHintCheck check = new AvoidNoLockHintCheck();

    // TRACEABILITY_MAPPING: REQ-009
    @Test
    void testFlagsNoLockHint() {
        String sql = "SELECT OrderId FROM Orders WITH (NOLOCK);";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0)).contains("NOLOCK");
    }

    // TRACEABILITY_MAPPING: REQ-009
    @Test
    void testFlagsReadUncommittedHint() {
        String sql = "SELECT OrderId FROM Orders WITH (READUNCOMMITTED);";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0)).contains("READUNCOMMITTED");
    }

    // TRACEABILITY_MAPPING: REQ-009
    @Test
    void testCompliantQueryWithoutHint() {
        String sql = "SELECT OrderId FROM Orders;";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).isEmpty();
    }
}
