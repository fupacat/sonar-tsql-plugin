package io.github.fupacat.sonar.tsql.checks;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SelfAssignmentCheckTest {

    private final SelfAssignmentCheck check = new SelfAssignmentCheck();

    @Test
    void testFlagsSelfAssignment() {
        String sql = "DECLARE @x INT; SET @x = @x;";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0)).contains("self-assignment");
    }

    @Test
    void testAllowsRealAssignment() {
        String sql = "DECLARE @x INT; SET @x = 7;";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).isEmpty();
    }
}
