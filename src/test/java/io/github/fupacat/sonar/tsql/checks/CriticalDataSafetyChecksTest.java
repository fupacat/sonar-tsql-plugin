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
        List<String> issues = CheckTestUtils.scan(deleteAndUpdateCheck, "DELETE FROM Employees;");
        assertThat(issues).hasSize(1);
    }

    // TRACEABILITY_MAPPING: REQ-011
    @Test
    void testUpdateWithoutWhere() {
        List<String> issues = CheckTestUtils.scan(deleteAndUpdateCheck, "UPDATE Employees SET Status = 'Active';");
        assertThat(issues).hasSize(1);
    }

    // TRACEABILITY_MAPPING: REQ-012
    @Test
    void testNotInSubquery() {
        List<String> issues = CheckTestUtils.scan(notInSubqueryCheck,
                "SELECT * FROM Customers WHERE CustomerId NOT IN (SELECT CustomerId FROM Orders);");
        assertThat(issues).hasSize(1);
    }

    // TRACEABILITY_MAPPING: REQ-013
    @Test
    void testNullComparison() {
        List<String> issues = CheckTestUtils.scan(nullComparisonCheck, "SELECT * FROM Employees WHERE Salary = NULL;");
        assertThat(issues).hasSize(1);
    }

    // TRACEABILITY_MAPPING: REQ-014
    @Test
    void testTopWithoutOrderBy() {
        List<String> issues = CheckTestUtils.scan(topWithoutOrderByCheck, "SELECT TOP 10 * FROM Employees;");
        assertThat(issues).hasSize(1);
    }

    // TRACEABILITY_MAPPING: REQ-015
    @Test
    void testCaseInputInvariant() {
        List<String> issues = CheckTestUtils.scan(caseInputInvariantCheck,
                "SELECT CASE RAND() WHEN 1 THEN 'A' ELSE 'B' END;");
        assertThat(issues).hasSize(1);
    }
}
