package io.github.fupacat.sonar.tsql.checks;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AvoidSelectAllCheckTest {

    private final AvoidSelectAllCheck check = new AvoidSelectAllCheck();

    @Test
    void testFlagsSelectStar() {
        String sql = "SELECT * FROM Employees;";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0)).contains("SELECT *");
    }

    @Test
    void testFlagsQualifiedSelectStar() {
        String sql = "SELECT e.* FROM Employees e;";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0)).contains("SELECT *");
    }

    @Test
    void testCompliantExplicitColumns() {
        String sql = "SELECT EmployeeId, FirstName, LastName FROM Employees;";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).isEmpty();
    }
}
