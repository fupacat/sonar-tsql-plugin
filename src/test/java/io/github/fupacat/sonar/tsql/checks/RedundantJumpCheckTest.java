package io.github.fupacat.sonar.tsql.checks;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RedundantJumpCheckTest {

    private final RedundantJumpCheck check = new RedundantJumpCheck();

    @Test
    void testFlagsRedundantReturnAtProcedureEnd() {
        String sql = "CREATE PROCEDURE dbo.p AS BEGIN SELECT 1; RETURN; END;";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0)).contains("redundant RETURN");
    }

    @Test
    void testAllowsProcedureWithoutTrailingReturn() {
        String sql = "CREATE PROCEDURE dbo.p AS BEGIN SELECT 1; END;";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).isEmpty();
    }
}
