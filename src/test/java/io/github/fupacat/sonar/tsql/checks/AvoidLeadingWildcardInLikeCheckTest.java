package io.github.fupacat.sonar.tsql.checks;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AvoidLeadingWildcardInLikeCheckTest {

    private final AvoidLeadingWildcardInLikeCheck check = new AvoidLeadingWildcardInLikeCheck();

    // TRACEABILITY_MAPPING: REQ-008
    @Test
    void testFlagsLeadingPercentWildcard() {
        String sql = "SELECT Id FROM Customers WHERE CompanyName LIKE '%Tech';";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0)).contains("Avoid leading wildcards");
    }

    // TRACEABILITY_MAPPING: REQ-008
    @Test
    void testFlagsLeadingUnderscoreWildcard() {
        String sql = "SELECT Id FROM Customers WHERE Code LIKE '_XYZ';";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0)).contains("Avoid leading wildcards");
    }

    // TRACEABILITY_MAPPING: REQ-008
    @Test
    void testCompliantTrailingWildcard() {
        String sql = "SELECT Id FROM Customers WHERE CompanyName LIKE 'Tech%';";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-008
    @Test
    void testCompliantExactMatch() {
        String sql = "SELECT Id FROM Customers WHERE CompanyName LIKE 'Acme Corp';";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).isEmpty();
    }
}
