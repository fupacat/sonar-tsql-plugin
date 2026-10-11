package io.github.fupacat.sonar.tsql.checks;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AvoidDeprecatedTypesCheckTest {

    private final AvoidDeprecatedTypesCheck check = new AvoidDeprecatedTypesCheck();

    // TRACEABILITY_MAPPING: REQ-010
    @Test
    void testFlagsTextType() {
        String sql = "CREATE TABLE Articles (Id INT, Content TEXT);";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0)).contains("TEXT").contains("VARCHAR(MAX)");
    }

    // TRACEABILITY_MAPPING: REQ-010
    @Test
    void testFlagsNTextType() {
        String sql = "CREATE TABLE Articles (Id INT, Content NTEXT);";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0)).contains("NTEXT").contains("NVARCHAR(MAX)");
    }

    // TRACEABILITY_MAPPING: REQ-010
    @Test
    void testFlagsImageType() {
        String sql = "CREATE TABLE Documents (Id INT, Scan IMAGE);";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0)).contains("IMAGE").contains("VARBINARY(MAX)");
    }

    // TRACEABILITY_MAPPING: REQ-010
    @Test
    void testCompliantModernTypes() {
        String sql = "CREATE TABLE Articles (Id INT, Content NVARCHAR(MAX), Photo VARBINARY(MAX));";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).isEmpty();
    }
}
