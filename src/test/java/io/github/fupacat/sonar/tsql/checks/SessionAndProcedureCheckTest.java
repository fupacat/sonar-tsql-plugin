package io.github.fupacat.sonar.tsql.checks;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SessionAndProcedureCheckTest {

    private final SetNoCountCheck setNoCountCheck = new SetNoCountCheck();
    private final SetAnsiWarningsAndArithabortCheck warningsCheck = new SetAnsiWarningsAndArithabortCheck();
    private final SetAnsiNullsCheck ansiNullsCheck = new SetAnsiNullsCheck();
    private final AvoidDeprecatedSystemTablesCheck deprecatedTablesCheck = new AvoidDeprecatedSystemTablesCheck();
    private final AvoidLikeWithoutWildcardCheck likeWithoutWildcardCheck = new AvoidLikeWithoutWildcardCheck();
    private final AvoidNonStandardComparisonOperatorsCheck nonStandardComparisonCheck = new AvoidNonStandardComparisonOperatorsCheck();

    @Test
    void testSetNoCountRule() {
        List<String> issues = CheckTestUtils.scan(setNoCountCheck,
                "CREATE PROCEDURE p AS BEGIN SET NOCOUNT OFF; SELECT 1; END");
        assertThat(issues).hasSize(1);
    }

    @Test
    void testSetWarningsRule() {
        List<String> issues = CheckTestUtils.scan(warningsCheck,
                "SET ANSI_WARNINGS OFF; SET ARITHABORT OFF;");
        assertThat(issues).hasSize(1);
    }

    @Test
    void testSetAnsiNullsRule() {
        List<String> issues = CheckTestUtils.scan(ansiNullsCheck,
                "SET ANSI_NULLS OFF; SET ANSI_PADDING OFF; SET CONCAT_NULL_YIELDS_NULL OFF;");
        assertThat(issues).hasSize(1);
    }

    @Test
    void testDeprecatedSystemTablesRule() {
        List<String> issues = CheckTestUtils.scan(deprecatedTablesCheck,
                "SELECT * FROM sysobjects;");
        assertThat(issues).hasSize(1);
    }

    @Test
    void testLikeWithoutWildcardRule() {
        List<String> issues = CheckTestUtils.scan(likeWithoutWildcardCheck,
                "SELECT Id FROM Customers WHERE CompanyName LIKE 'Acme';");
        assertThat(issues).hasSize(1);
    }

    @Test
    void testNonStandardComparisonRule() {
        List<String> issues = CheckTestUtils.scan(nonStandardComparisonCheck,
                "SELECT Id FROM Customers WHERE Name != 'Acme' OR Amount !< 10 OR Score !> 9;");
        assertThat(issues).hasSize(3);
    }
}
