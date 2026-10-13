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

    @Test
    void testDuplicateBinaryOperands() {
        assertThat(duplicateBinaryOperandCheck.getRuleKey()).isEqualTo("S1764");
        List<String> issues = CheckTestUtils.scan(duplicateBinaryOperandCheck,
                "SELECT * FROM Orders WHERE a = a OR x = x;");
        assertThat(issues).hasSize(2);
    }

    @Test
    void testDuplicateIfConditionsAndCaseConditions() {
        List<String> issues = CheckTestUtils.scan(duplicateIfConditionCheck,
                "IF a = 1 BEGIN SELECT 1; END ELSE IF a = 1 BEGIN SELECT 2; END; SELECT CASE WHEN a = 1 THEN 1 WHEN a = 1 THEN 2 ELSE 0 END;");
        assertThat(issues).hasSize(2);
    }

    @Test
    void testIdenticalBranches() {
        List<String> issues = CheckTestUtils.scan(identicalBranchCheck,
                "IF a = 1 BEGIN SELECT 1; END ELSE BEGIN SELECT 1; END; SELECT CASE WHEN a = 1 THEN 1 WHEN b = 2 THEN 1 ELSE 0 END;");
        assertThat(issues).hasSize(2);
    }

    @Test
    void testRepeatedUnaryOperators() {
        assertThat(repeatedUnaryOperatorCheck.getRuleKey()).isEqualTo("S2761");
        List<String> issues = CheckTestUtils.scan(repeatedUnaryOperatorCheck, "SELECT ~~~x, NOT NOT flag FROM T;");
        assertThat(issues).hasSize(2);
    }

    @Test
    void testWhereClauseRedundancy() {
        List<String> issues = CheckTestUtils.scan(whereClauseRedundancyCheck,
                "SELECT * FROM Orders WHERE a = a AND a = a;");
        assertThat(issues).hasSize(1);
    }

    @Test
    void testControlStructuresMustUseBeginEnd() {
        List<String> issues = CheckTestUtils.scan(controlStructureBlockCheck,
                "IF a = 1 SELECT 1; WHILE a = 1 SELECT 1;");
        assertThat(issues).hasSize(2);
    }

    @Test
    void testCaseAndIfElseChainsShouldEndWithElse() {
        List<String> issues = CheckTestUtils.scan(caseElseMissingCheck,
                "SELECT CASE WHEN a = 1 THEN 'x' WHEN a = 2 THEN 'y' END; IF a = 1 BEGIN SELECT 1; END ELSE IF a = 2 BEGIN SELECT 2; END;");
        assertThat(issues).hasSize(2);
    }

    @Test
    void testGotoIsDisallowed() {
        assertThat(gotoStatementCheck.getRuleKey()).isEqualTo("S907");
        List<String> issues = CheckTestUtils.scan(gotoStatementCheck, "BEGIN GOTO label; END;");
        assertThat(issues).hasSize(1);
    }

    @Test
    void testNestedIfMerge() {
        List<String> issues = CheckTestUtils.scan(nestedIfMergeCheck,
                "IF a = 1 BEGIN IF b = 2 BEGIN SELECT 1; END; END;");
        assertThat(issues).hasSize(1);
    }
}
