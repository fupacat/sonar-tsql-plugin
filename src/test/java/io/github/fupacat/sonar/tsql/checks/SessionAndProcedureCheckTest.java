package io.github.fupacat.sonar.tsql.checks;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SessionAndProcedureCheckTest {

    private final SetNoCountCheck setNoCountCheck = new SetNoCountCheck();
    private final SetAnsiWarningsAndArithabortCheck warningsCheck = new SetAnsiWarningsAndArithabortCheck();
    private final SetAnsiNullsCheck ansiNullsCheck = new SetAnsiNullsCheck();
    private final AvoidDeprecatedSystemTablesCheck deprecatedTablesCheck = new AvoidDeprecatedSystemTablesCheck();
    private final FetchIntoCursorCountCheck fetchIntoCursorCountCheck = new FetchIntoCursorCountCheck();
    private final CaseSubqueryCheck caseSubqueryCheck = new CaseSubqueryCheck();
    private final TriggerResultSetCheck triggerResultSetCheck = new TriggerResultSetCheck();
    private final AvoidLikeWithoutWildcardCheck likeWithoutWildcardCheck = new AvoidLikeWithoutWildcardCheck();
    private final AvoidNonStandardComparisonOperatorsCheck nonStandardComparisonCheck = new AvoidNonStandardComparisonOperatorsCheck();

    // TRACEABILITY_MAPPING: REQ-046
    @Test
    void testSetNoCountRule() {
        List<String> issues = CheckTestUtils.scan(setNoCountCheck,
                "CREATE PROCEDURE p AS BEGIN SET NOCOUNT OFF; SELECT 1; END");
        assertThat(issues).hasSize(1);
    }

    // TRACEABILITY_MAPPING: REQ-046
    @Test
    void testSetNoCountIsCompliantWhenEnabled() {
        List<String> issues = CheckTestUtils.scan(setNoCountCheck,
                "CREATE PROCEDURE p AS BEGIN SET NOCOUNT ON; SELECT 1; END");
        assertThat(issues).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-047
    @Test
    void testSetWarningsRule() {
        List<String> issues = CheckTestUtils.scan(warningsCheck,
                "SET ANSI_WARNINGS OFF; SET ARITHABORT OFF;");
        assertThat(issues).hasSize(1);
    }

    // TRACEABILITY_MAPPING: REQ-047
    @Test
    void testSetWarningsAreCompliantWhenEnabled() {
        List<String> issues = CheckTestUtils.scan(warningsCheck,
                "SET ANSI_WARNINGS ON; SET ARITHABORT ON;");
        assertThat(issues).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-048
    @Test
    void testSetAnsiNullsRule() {
        List<String> issues = CheckTestUtils.scan(ansiNullsCheck,
                "SET ANSI_NULLS OFF; SET ANSI_PADDING OFF; SET CONCAT_NULL_YIELDS_NULL OFF;");
        assertThat(issues).hasSize(1);
    }

    // TRACEABILITY_MAPPING: REQ-048
    @Test
    void testSetAnsiNullsAreCompliantWhenEnabled() {
        List<String> issues = CheckTestUtils.scan(ansiNullsCheck,
                "SET ANSI_NULLS ON; SET ANSI_PADDING ON; SET CONCAT_NULL_YIELDS_NULL ON;");
        assertThat(issues).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-049
    @Test
    void testDeprecatedSystemTablesRule() {
        List<String> issues = CheckTestUtils.scan(deprecatedTablesCheck,
                "SELECT * FROM sysobjects;");
        assertThat(issues).hasSize(1);
    }

    // TRACEABILITY_MAPPING: REQ-049
    @Test
    void testDeprecatedSystemTablesAreCompliantWhenUsingSysCatalogViews() {
        List<String> issues = CheckTestUtils.scan(deprecatedTablesCheck,
                "SELECT * FROM sys.objects;");
        assertThat(issues).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-050
    @Test
    void testFetchIntoCursorCountRule() {
        List<String> issues = CheckTestUtils.scan(fetchIntoCursorCountCheck,
                "DECLARE c CURSOR FOR SELECT a, b FROM dbo.T; FETCH NEXT FROM c INTO @a;");
        assertThat(issues).hasSize(1);
    }

    // TRACEABILITY_MAPPING: REQ-050
    @Test
    void testFetchIntoCursorCountIsCompliantWhenCountsMatch() {
        List<String> issues = CheckTestUtils.scan(fetchIntoCursorCountCheck,
                "DECLARE c CURSOR FOR SELECT a, b FROM dbo.T; FETCH NEXT FROM c INTO @a, @b;");
        assertThat(issues).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-051
    @Test
    void testCaseSubqueryRule() {
        List<String> issues = CheckTestUtils.scan(caseSubqueryCheck,
                "SELECT COALESCE((SELECT MAX(Id) FROM Orders), 0);");
        assertThat(issues).hasSize(1);
    }

    // TRACEABILITY_MAPPING: REQ-051
    @Test
    void testCaseSubqueryInWhenBranch() {
        List<String> issues = CheckTestUtils.scan(caseSubqueryCheck,
                "SELECT CASE WHEN (SELECT COUNT(*) FROM Orders) > 0 THEN 1 ELSE 0 END;");
        assertThat(issues).hasSize(1);
    }

    // TRACEABILITY_MAPPING: REQ-051
    @Test
    void testCaseSubqueryIsCompliantWhenUsingSimpleValues() {
        List<String> issues = CheckTestUtils.scan(caseSubqueryCheck,
                "SELECT COALESCE(LastOrderId, 0) AS LastOrderId FROM dbo.Customers;");
        assertThat(issues).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-052
    @Test
    void testTriggerResultSetRule() {
        List<String> issues = CheckTestUtils.scan(triggerResultSetCheck,
                "CREATE TRIGGER t ON dbo.Test AFTER UPDATE AS SELECT 1;");
        assertThat(issues).hasSize(1);
    }

    // TRACEABILITY_MAPPING: REQ-052
    @Test
    void testTriggerResultSetIsCompliantWithoutResultEmission() {
        List<String> issues = CheckTestUtils.scan(triggerResultSetCheck,
                "CREATE TRIGGER t ON dbo.Test AFTER UPDATE AS SET NOCOUNT ON; UPDATE dbo.Test SET Value = Value + 1;");
        assertThat(issues).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-053
    @Test
    void testLikeWithoutWildcardRule() {
        List<String> issues = CheckTestUtils.scan(likeWithoutWildcardCheck,
                "SELECT Id FROM Customers WHERE CompanyName LIKE 'Acme';");
        assertThat(issues).hasSize(1);
    }

    // TRACEABILITY_MAPPING: REQ-053
    @Test
    void testLikeWithoutWildcardIsCompliantWhenUsingWildcards() {
        List<String> issues = CheckTestUtils.scan(likeWithoutWildcardCheck,
                "SELECT Id FROM Customers WHERE CompanyName LIKE '%Acme%';");
        assertThat(issues).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-053
    @Test
    void testLikeWithBracketWildcardIsCompliant() {
        List<String> issues = CheckTestUtils.scan(likeWithoutWildcardCheck,
                "SELECT Id FROM Customers WHERE CompanyName LIKE '[A-C]%';");
        assertThat(issues).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-054
    @Test
    void testNonStandardComparisonRule() {
        List<String> issues = CheckTestUtils.scan(nonStandardComparisonCheck,
                "SELECT Id FROM Customers WHERE Name != 'Acme' OR Amount !< 10 OR Score !> 9;");
        assertThat(issues).hasSize(3);
    }

    // TRACEABILITY_MAPPING: REQ-054
    @Test
    void testNonStandardComparisonIsCompliantWhenUsingIsoOperators() {
        List<String> issues = CheckTestUtils.scan(nonStandardComparisonCheck,
                "SELECT Id FROM Customers WHERE Name <> 'Acme' OR Amount <= 10 OR Score >= 9;");
        assertThat(issues).isEmpty();
    }
}
