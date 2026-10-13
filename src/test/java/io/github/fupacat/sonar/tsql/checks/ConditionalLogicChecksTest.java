package io.github.fupacat.sonar.tsql.checks;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConditionalLogicChecksTest {

    private final DuplicateBinaryOperandCheck duplicateBinaryOperandCheck = new DuplicateBinaryOperandCheck();
    private final DuplicateIfConditionCheck duplicateIfConditionCheck = new DuplicateIfConditionCheck();
    private final IdenticalBranchCheck identicalBranchCheck = new IdenticalBranchCheck();
    private final RepeatedUnaryOperatorCheck repeatedUnaryOperatorCheck = new RepeatedUnaryOperatorCheck();
    private final WhereClauseRedundancyCheck whereClauseRedundancyCheck = new WhereClauseRedundancyCheck();
    private final ControlStructureBlockCheck controlStructureBlockCheck = new ControlStructureBlockCheck();
    private final CaseElseMissingCheck caseElseMissingCheck = new CaseElseMissingCheck();
    private final GotoStatementCheck gotoStatementCheck = new GotoStatementCheck();
    private final NestedIfMergeCheck nestedIfMergeCheck = new NestedIfMergeCheck();

    // TRACEABILITY_MAPPING: REQ-016
    @Test
    void testDuplicateBinaryOperands() {
        assertThat(duplicateBinaryOperandCheck.getRuleKey()).isEqualTo("S1764");
        List<String> issues = CheckTestUtils.scan(duplicateBinaryOperandCheck,
                "SELECT * FROM Orders WHERE a = a OR x = x;");
        assertThat(issues).hasSize(2);
    }

    // TRACEABILITY_MAPPING: REQ-016
    @Test
    void testDistinctBinaryOperandsAreCompliant() {
        List<String> issues = CheckTestUtils.scan(duplicateBinaryOperandCheck,
                "SELECT * FROM Orders WHERE a = b OR x = y AND (z + 1) > (z + 2);");
        assertThat(issues).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-017
    @Test
    void testDuplicateIfConditionsAndCaseConditions() {
        List<String> issues = CheckTestUtils.scan(duplicateIfConditionCheck,
                "IF a = 1 BEGIN SELECT 1; END ELSE IF a = 1 BEGIN SELECT 2; END; SELECT CASE WHEN a = 1 THEN 1 WHEN a = 1 THEN 2 ELSE 0 END;");
        assertThat(issues).hasSize(2);
    }

    // TRACEABILITY_MAPPING: REQ-017
    @Test
    void testDistinctIfAndCaseConditionsAreCompliant() {
        List<String> issues = CheckTestUtils.scan(duplicateIfConditionCheck,
                "IF a = 1 BEGIN SELECT 1; END ELSE IF a = 2 BEGIN SELECT 2; END; SELECT CASE WHEN a = 1 THEN 1 WHEN a = 2 THEN 2 ELSE 0 END;");
        assertThat(issues).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-018
    @Test
    void testIdenticalBranches() {
        List<String> issues = CheckTestUtils.scan(identicalBranchCheck,
                "IF a = 1 BEGIN SELECT 1; END ELSE BEGIN SELECT 1; END; SELECT CASE WHEN a = 1 THEN 1 WHEN b = 2 THEN 1 ELSE 0 END;");
        assertThat(issues).hasSize(2);
    }

    // TRACEABILITY_MAPPING: REQ-018
    @Test
    void testDistinctBranchesAreCompliant() {
        List<String> issues = CheckTestUtils.scan(identicalBranchCheck,
                "IF a = 1 BEGIN SELECT 1; END ELSE BEGIN SELECT 2; END; "
                        + "SELECT CASE Score WHEN 1 THEN 10 WHEN 2 THEN 20 ELSE 0 END; "
                        + "SELECT CASE WHEN a = 1 THEN 10 WHEN b = 2 THEN 20 ELSE 0 END;");
        assertThat(issues).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-019
    @Test
    void testRepeatedUnaryOperators() {
        assertThat(repeatedUnaryOperatorCheck.getRuleKey()).isEqualTo("S2761");
        List<String> issues = CheckTestUtils.scan(repeatedUnaryOperatorCheck,
                "SELECT ~~~x, - -x FROM T WHERE NOT (NOT (a = 1));");
        assertThat(issues).hasSize(4);
    }

    // TRACEABILITY_MAPPING: REQ-019
    @Test
    void testSingleUnaryOperatorsAreCompliant() {
        List<String> issues = CheckTestUtils.scan(repeatedUnaryOperatorCheck,
                "SELECT ~x, -x, +x FROM T WHERE NOT (a = 1);");
        assertThat(issues).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-020
    @Test
    void testWhereClauseRedundancy() {
        List<String> issues = CheckTestUtils.scan(whereClauseRedundancyCheck,
                "SELECT * FROM Orders WHERE a = a AND a = a;");
        assertThat(issues).hasSize(1);
    }

    // TRACEABILITY_MAPPING: REQ-020
    @Test
    void testNonRedundantWhereClauseIsCompliant() {
        List<String> issues = CheckTestUtils.scan(whereClauseRedundancyCheck,
                "SELECT * FROM Orders WHERE a = 1 OR b = 2;");
        assertThat(issues).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-021
    @Test
    void testControlStructuresMustUseBeginEnd() {
        List<String> issues = CheckTestUtils.scan(controlStructureBlockCheck,
                "IF a = 1 SELECT 1; WHILE a = 1 SELECT 1;");
        assertThat(issues).hasSize(2);
    }

    // TRACEABILITY_MAPPING: REQ-021
    @Test
    void testControlStructuresWithBlocksAreCompliant() {
        List<String> issues = CheckTestUtils.scan(controlStructureBlockCheck,
                "IF a = 1 BEGIN SELECT 1; END ELSE IF a = 2 BEGIN SELECT 2; END; "
                        + "WHILE a = 1 BEGIN SELECT 1; END;");
        assertThat(issues).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-022
    @Test
    void testCaseAndIfElseChainsShouldEndWithElse() {
        List<String> issues = CheckTestUtils.scan(caseElseMissingCheck,
                "SELECT CASE WHEN a = 1 THEN 'x' WHEN a = 2 THEN 'y' END; IF a = 1 BEGIN SELECT 1; END ELSE IF a = 2 BEGIN SELECT 2; END;");
        assertThat(issues).hasSize(2);
    }

    // TRACEABILITY_MAPPING: REQ-022
    @Test
    void testCaseAndIfElseChainsWithElseAreCompliant() {
        List<String> issues = CheckTestUtils.scan(caseElseMissingCheck,
                "SELECT CASE WHEN a = 1 THEN 'x' ELSE 'y' END; "
                        + "SELECT CASE a WHEN 1 THEN 'x' ELSE 'y' END; "
                        + "IF a = 1 BEGIN SELECT 1; END ELSE IF a = 2 BEGIN SELECT 2; END ELSE BEGIN SELECT 3; END;");
        assertThat(issues).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-023
    @Test
    void testGotoIsDisallowed() {
        assertThat(gotoStatementCheck.getRuleKey()).isEqualTo("S907");
        List<String> issues = CheckTestUtils.scan(gotoStatementCheck, "BEGIN GOTO label; END;");
        assertThat(issues).hasSize(1);
    }

    // TRACEABILITY_MAPPING: REQ-023
    @Test
    void testWithoutGotoIsCompliant() {
        List<String> issues = CheckTestUtils.scan(gotoStatementCheck, "BEGIN SELECT 1; END;");
        assertThat(issues).isEmpty();
    }

    // TRACEABILITY_MAPPING: REQ-024
    @Test
    void testNestedIfMerge() {
        List<String> issues = CheckTestUtils.scan(nestedIfMergeCheck,
                "IF a = 1 BEGIN IF b = 2 BEGIN SELECT 1; END; END;");
        assertThat(issues).hasSize(1);
    }

    // TRACEABILITY_MAPPING: REQ-024
    @Test
    void testNestedIfAlreadySeparatedIsCompliant() {
        List<String> issues = CheckTestUtils.scan(nestedIfMergeCheck,
                "IF a = 1 BEGIN SELECT 1; IF b = 2 BEGIN SELECT 2; END; END;");
        assertThat(issues).isEmpty();
    }
}
