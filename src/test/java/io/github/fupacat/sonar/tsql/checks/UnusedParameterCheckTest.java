package io.github.fupacat.sonar.tsql.checks;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UnusedParameterCheckTest {

    private final UnusedParameterCheck check = new UnusedParameterCheck();

    @Test
    void testFlagsUnusedParameter() {
        String sql = "CREATE PROCEDURE dbo.GetOrders (@customerId INT, @unused INT) AS BEGIN SELECT @customerId; END;";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0)).contains("unused");
    }

    @Test
    void testAllowsUsedParameter() {
        String sql = "CREATE PROCEDURE dbo.GetOrders (@customerId INT) AS BEGIN SELECT @customerId; END;";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).isEmpty();
    }

    @Test
    void testFlagsUnusedParameterInCreateOrAlterProcedure() {
        String sql = "CREATE OR ALTER PROCEDURE dbo.p1 (@a INT) AS BEGIN SELECT 1; END;";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testFlagsUnusedParameterInAlterProcedure() {
        String sql = "ALTER PROCEDURE dbo.p2 (@b INT) AS BEGIN SELECT 1; END;";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testFlagsUnusedParameterInFunctions() {
        List<String> createIssues = CheckTestUtils.scan(check,
                "CREATE FUNCTION dbo.f1 (@c INT) RETURNS INT AS BEGIN RETURN 1; END;");
        List<String> createOrAlterIssues = CheckTestUtils.scan(check,
                "CREATE OR ALTER FUNCTION dbo.f2 (@d INT) RETURNS INT AS BEGIN RETURN 1; END;");
        List<String> alterIssues = CheckTestUtils.scan(check,
                "ALTER FUNCTION dbo.f3 (@e INT) RETURNS INT AS BEGIN RETURN 1; END;");
        assertThat(createIssues).hasSize(1);
        assertThat(createOrAlterIssues).hasSize(1);
        assertThat(alterIssues).hasSize(1);
    }

    @Test
    void testTreatsParameterUsageAsCaseInsensitive() {
        String sql = "CREATE PROCEDURE dbo.GetOrders (@CustomerId INT) AS BEGIN SELECT @customerid; END;";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).isEmpty();
    }
}
