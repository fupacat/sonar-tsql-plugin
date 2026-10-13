package io.github.fupacat.sonar.tsql.checks;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CriticalDataSafetyChecksTest {

    private final DeleteAndUpdateWithoutWhereCheck deleteAndUpdateCheck = new DeleteAndUpdateWithoutWhereCheck();
    private final NotInSubqueryCheck notInSubqueryCheck = new NotInSubqueryCheck();
    private final NullComparisonCheck nullComparisonCheck = new NullComparisonCheck();
    private final TopWithoutOrderByCheck topWithoutOrderByCheck = new TopWithoutOrderByCheck();
    private final CaseInputInvariantCheck caseInputInvariantCheck = new CaseInputInvariantCheck();

    // TRACEABILITY_MAPPING: REQ-011
    @Test
    void testDeleteWithoutWhere() {
        assertThat(deleteAndUpdateCheck.getRuleKey()).isEqualTo("S1590");
        List<String> issues = CheckTestUtils.scan(deleteAndUpdateCheck, "DELETE FROM Employees;");
        assertThat(issues).hasSize(1);
    }

    // TRACEABILITY_MAPPING: REQ-011
    @Test
    void testUpdateWithoutWhere() {
        List<String> issues = CheckTestUtils.scan(deleteAndUpdateCheck, "UPDATE Employees SET Status = 'Active';");
        assertThat(issues).hasSize(1);
    }

    // TRACEABILITY_MAPPING: REQ-011
    @Test
    void testDeleteAndUpdateWithWhereAreCompliant() {
        List<String> issues = CheckTestUtils.scan(deleteAndUpdateCheck,
                "DELETE FROM Employees WHERE Id = 1; UPDATE Employees SET Status = 'Active' WHERE Id = 1;");
        assertThat(issues).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-012
    @Test
    void testNotInSubquery() {
        assertThat(notInSubqueryCheck.getRuleKey()).isEqualTo("S3641");
        List<String> issues = CheckTestUtils.scan(notInSubqueryCheck,
                "SELECT Id FROM Customers WHERE CustomerId NOT IN (SELECT CustomerId FROM Orders);");
        assertThat(issues).hasSize(1);
    }

    // TRACEABILITY_MAPPING: REQ-012
    @Test
    void testInSubqueryAndNotInValueListAreCompliant() {
        List<String> issues = CheckTestUtils.scan(notInSubqueryCheck,
                "SELECT Id FROM Customers WHERE CustomerId IN (SELECT CustomerId FROM Orders) AND Status NOT IN (1, 2);");
        assertThat(issues).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-013
    @Test
    void testNullComparison() {
        assertThat(nullComparisonCheck.getRuleKey()).isEqualTo("S2527");
        List<String> issues = CheckTestUtils.scan(nullComparisonCheck,
                "SELECT Id FROM Employees WHERE Salary = NULL OR NULL = Bonus OR Salary <> NULL OR Salary != NULL;");
        assertThat(issues).hasSize(4);
    }

    // TRACEABILITY_MAPPING: REQ-013
    @Test
    void testIsNullAndNonNullComparisonsAreCompliant() {
        List<String> issues = CheckTestUtils.scan(nullComparisonCheck,
                "SELECT Id FROM Employees WHERE Salary IS NULL OR Salary = 100 OR Salary > NULL;");
        assertThat(issues).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-014
    @Test
    void testTopWithoutOrderBy() {
        assertThat(topWithoutOrderByCheck.getRuleKey()).isEqualTo("S4054");
        List<String> issues = CheckTestUtils.scan(topWithoutOrderByCheck, "SELECT TOP 10 Id FROM Employees;");
        assertThat(issues).hasSize(1);
    }

    // TRACEABILITY_MAPPING: REQ-014
    @Test
    void testTopWithOrderByAndSelectWithoutTopAreCompliant() {
        List<String> issues = CheckTestUtils.scan(topWithoutOrderByCheck,
                "SELECT TOP 10 Id FROM Employees ORDER BY Id; SELECT Id FROM Employees;");
        assertThat(issues).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-015
    @Test
    void testCaseInputInvariant() {
        assertThat(caseInputInvariantCheck.getRuleKey()).isEqualTo("S4094");
        List<String> issues = CheckTestUtils.scan(caseInputInvariantCheck,
                "SELECT CASE RAND() WHEN 1 THEN 'A' ELSE 'B' END, CASE NEWID() WHEN 'X' THEN 'A' ELSE 'B' END;");
        assertThat(issues).hasSize(2);
    }

    // TRACEABILITY_MAPPING: REQ-015
    @Test
    void testCaseWithInvariantExpressionsAreCompliant() {
        List<String> issues = CheckTestUtils.scan(caseInputInvariantCheck,
                "SELECT CASE Status WHEN 'A' THEN 1 ELSE 0 END, CASE ABS(Score) WHEN 1 THEN 'A' ELSE 'B' END;");
        assertThat(issues).isEmpty();
    }
}
