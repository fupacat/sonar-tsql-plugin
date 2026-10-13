package io.github.fupacat.sonar.tsql.checks;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UnreachableCodeCheckTest {

    private final UnreachableCodeCheck check = new UnreachableCodeCheck();

    @Test
    void testFlagsStatementsAfterReturn() {
        String sql = "BEGIN IF 1 = 1 RETURN; SELECT 1; END;";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0)).contains("unreachable");
    }

    @Test
    void testAllowsReachableCode() {
        String sql = "BEGIN IF 1 = 1 SELECT 1; ELSE SELECT 2; END;";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).isEmpty();
    }

    @Test
    void testFlagsStatementsAfterAlwaysTrueTerminatingIfBlock() {
        String sql = "BEGIN IF 2 > 1 BEGIN THROW 50000, 'x', 1; END; SELECT 1; SELECT 2; END;";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).hasSize(2);
    }

    @Test
    void testAllowsCodeAfterNonTerminatingIfBlock() {
        String sql = "BEGIN IF 1 = 1 BEGIN SELECT 1; END; SELECT 2; END;";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).isEmpty();
    }

    @Test
    void testFlagsOtherAlwaysTrueComparisonKinds() {
        String sql = "BEGIN IF 1 != 2 RETURN; SELECT 1; END; "
                + "BEGIN IF 2 >= 1 RETURN; SELECT 2; END; "
                + "BEGIN IF 1 <= 2 RETURN; SELECT 3; END; "
                + "BEGIN IF 2 !< 1 RETURN; SELECT 4; END; "
                + "BEGIN IF 1 !> 2 RETURN; SELECT 5; END;";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).hasSize(5);
    }

    @Test
    void testIgnoresNonConstantAndNonIntegerComparisons() {
        String sql = "BEGIN IF @flag RETURN; SELECT 1; END; "
                + "BEGIN IF 1.5 = 1.5 RETURN; SELECT 2; END;";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).isEmpty();
    }
}
