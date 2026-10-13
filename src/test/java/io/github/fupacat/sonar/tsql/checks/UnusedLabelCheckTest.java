package io.github.fupacat.sonar.tsql.checks;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UnusedLabelCheckTest {

    private final UnusedLabelCheck check = new UnusedLabelCheck();

    @Test
    void testFlagsUnusedLabel() {
        String sql = "label: SELECT 1;";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0)).contains("label");
    }

    @Test
    void testAllowsUsedLabel() {
        String sql = "label: SELECT 1; GOTO label;";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).isEmpty();
    }
}
