package io.github.fupacat.sonar.tsql.checks;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UnusedLocalVariableCheckTest {

    private final UnusedLocalVariableCheck check = new UnusedLocalVariableCheck();

    // TRACEABILITY_MAPPING: REQ-025
    @Test
    void testFlagsUnusedDeclaredVariable() {
        String sql = "DECLARE @unused INT; SET @unused = 1;";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0)).contains("unused");
    }

    // TRACEABILITY_MAPPING: REQ-025
    @Test
    void testAllowsUsedDeclaredVariable() {
        String sql = "DECLARE @used INT; SET @used = 1; SELECT @used;";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-025
    @Test
    void testSeparatesVariablesAcrossProcedures() {
        String sql = "CREATE PROCEDURE dbo.A AS BEGIN DECLARE @x INT; END; GO "
                + "CREATE PROCEDURE dbo.B AS BEGIN DECLARE @x INT = 1; SELECT @x; END; GO";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0)).contains("@x");
    }

    // TRACEABILITY_MAPPING: REQ-025
    @Test
    void testNestedBlocksShareSameVariableScope() {
        String sql = "BEGIN DECLARE @x INT; BEGIN SELECT @x; END; END;";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-025
    @Test
    void testCountsReadsButNotWritesAsUsage() {
        List<String> writtenOnlyIssues = CheckTestUtils.scan(check, "DECLARE @x INT; SET @x = 5;");
        assertThat(writtenOnlyIssues).hasSize(1);

        List<String> readAndWriteIssues = CheckTestUtils.scan(check, "DECLARE @x INT; SET @x = @x + 1;");
        assertThat(readAndWriteIssues).isEmpty();
    }
}
