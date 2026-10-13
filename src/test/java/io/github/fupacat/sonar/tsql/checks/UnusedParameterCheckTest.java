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
}
