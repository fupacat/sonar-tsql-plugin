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

    // TRACEABILITY_MAPPING: REQ-025
    @Test
    void testCoversAlternateProcedureAndFunctionBodies() {
        String sql = "CREATE PROCEDURE dbo.P AS BEGIN DECLARE @p INT; END; GO "
                + "CREATE OR ALTER PROCEDURE dbo.P2 AS BEGIN DECLARE @p INT; SELECT @p; END; GO "
                + "ALTER PROCEDURE dbo.P AS BEGIN DECLARE @p INT; END; GO "
                + "CREATE FUNCTION dbo.F() RETURNS INT AS BEGIN DECLARE @f INT; RETURN @f; END; GO "
                + "CREATE OR ALTER FUNCTION dbo.F2() RETURNS INT AS BEGIN DECLARE @f INT; RETURN @f; END; GO "
                + "ALTER FUNCTION dbo.F() RETURNS INT AS BEGIN DECLARE @f INT; RETURN @f; END; GO";

        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).anyMatch(issue -> issue.contains("@p"));
    }

    // TRACEABILITY_MAPPING: REQ-025
    @Test
    void testTraversesStatementAndAssignmentBranches() {
        List<String> selectIssues = CheckTestUtils.scan(check,
                "DECLARE @target INT; DECLARE @source INT = 1; SELECT @target = @source;");
        assertThat(selectIssues).hasSize(1);
        assertThat(selectIssues.get(0)).contains("@target");

        List<String> updateIssues = CheckTestUtils.scan(check,
                "DECLARE @target INT; DECLARE @source INT = 1; UPDATE dbo.Items SET @target = @source;");
        assertThat(updateIssues).hasSize(1);
        assertThat(updateIssues.get(0)).contains("@target");
    }

    // TRACEABILITY_MAPPING: REQ-025
    @Test
    void testHandlesNullRootGracefully() {
        CheckTestUtils.CollectingContext context = new CheckTestUtils.CollectingContext("DECLARE @x INT;");
        check.scan(null, context);
        assertThat(context.issues).isEmpty();
    }
}
