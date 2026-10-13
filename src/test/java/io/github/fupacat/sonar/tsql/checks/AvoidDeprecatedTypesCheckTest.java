package io.github.fupacat.sonar.tsql.checks;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AvoidDeprecatedTypesCheckTest {

    private final AvoidDeprecatedTypesCheck check = new AvoidDeprecatedTypesCheck();

    // TRACEABILITY_MAPPING: REQ-010
    @ParameterizedTest
    @CsvSource({
            "'CREATE TABLE Articles (Id INT, Content TEXT);', TEXT, VARCHAR(MAX)",
            "'CREATE TABLE Articles (Id INT, Content NTEXT);', NTEXT, NVARCHAR(MAX)",
            "'CREATE TABLE Documents (Id INT, Scan IMAGE);', IMAGE, VARBINARY(MAX)"
    })
    void testFlagsDeprecatedTypes(String sql, String deprecatedType, String replacementType) {
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0)).contains(deprecatedType).contains(replacementType);
    }

    // TRACEABILITY_MAPPING: REQ-010
    @Test
    void testCompliantModernTypes() {
        String sql = "CREATE TABLE Articles (Id INT, Content NVARCHAR(MAX), Photo VARBINARY(MAX));";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).isEmpty();
    }
}
