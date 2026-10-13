package io.github.fupacat.sonar.tsql.rules;

import io.github.fupacat.sonar.tsql.TSqlLanguage;
import io.github.fupacat.sonar.tsql.checks.AvoidDeprecatedTypesCheck;
import io.github.fupacat.sonar.tsql.checks.AvoidLeadingWildcardInLikeCheck;
import io.github.fupacat.sonar.tsql.checks.AvoidNoLockHintCheck;
import io.github.fupacat.sonar.tsql.checks.AvoidSelectAllCheck;
import io.github.fupacat.sonar.tsql.checks.CaseInputInvariantCheck;
import io.github.fupacat.sonar.tsql.checks.CatchClauseShouldDoMoreThanRethrowCheck;
import io.github.fupacat.sonar.tsql.checks.CheckList;
import io.github.fupacat.sonar.tsql.checks.DeleteAndUpdateWithoutWhereCheck;
import io.github.fupacat.sonar.tsql.checks.NotInSubqueryCheck;
import io.github.fupacat.sonar.tsql.checks.NullComparisonCheck;
import io.github.fupacat.sonar.tsql.checks.RedundantJumpCheck;
import io.github.fupacat.sonar.tsql.checks.SelfAssignmentCheck;
import io.github.fupacat.sonar.tsql.checks.SingleIterationLoopCheck;
import io.github.fupacat.sonar.tsql.checks.TopWithoutOrderByCheck;
import io.github.fupacat.sonar.tsql.checks.UnreachableCodeCheck;
import io.github.fupacat.sonar.tsql.checks.UnusedLabelCheck;
import io.github.fupacat.sonar.tsql.checks.UnusedLocalVariableCheck;
import io.github.fupacat.sonar.tsql.checks.UnusedParameterCheck;
import org.junit.jupiter.api.Test;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;

import static org.assertj.core.api.Assertions.assertThat;

class TSqlRulesDefinitionTest {

    // TRACEABILITY_MAPPING: REQ-003, REQ-006
    @Test
    void testRulesDefinition() {
        TSqlRulesDefinition definition = new TSqlRulesDefinition();
        RulesDefinition.Context context = new RulesDefinition.Context();
        definition.define(context);

        RulesDefinition.Repository repository = context.repository(TSqlRulesDefinition.REPOSITORY_KEY);
        assertThat(repository).isNotNull();
        assertThat(repository.name()).isEqualTo(TSqlRulesDefinition.REPOSITORY_NAME);
        assertThat(repository.language()).isEqualTo(TSqlLanguage.KEY);

        // Verify all 18 rules are registered
        assertThat(repository.rules()).hasSize(18);

        RulesDefinition.Rule selectAllRule = repository.rule(AvoidSelectAllCheck.RULE_KEY);
        assertThat(selectAllRule).isNotNull();
        assertThat(selectAllRule.name()).contains("SELECT *");
        assertThat(selectAllRule.severity()).isEqualTo(Severity.MAJOR);
        assertThat(selectAllRule.type()).isEqualTo(RuleType.CODE_SMELL);

        RulesDefinition.Rule likeRule = repository.rule(AvoidLeadingWildcardInLikeCheck.RULE_KEY);
        assertThat(likeRule).isNotNull();
        assertThat(likeRule.severity()).isEqualTo(Severity.CRITICAL);
        assertThat(likeRule.type()).isEqualTo(RuleType.BUG);

        RulesDefinition.Rule noLockRule = repository.rule(AvoidNoLockHintCheck.RULE_KEY);
        assertThat(noLockRule).isNotNull();
        assertThat(noLockRule.severity()).isEqualTo(Severity.MAJOR);
        assertThat(noLockRule.type()).isEqualTo(RuleType.BUG);

        RulesDefinition.Rule deprecatedTypesRule = repository.rule(AvoidDeprecatedTypesCheck.RULE_KEY);
        assertThat(deprecatedTypesRule).isNotNull();
        assertThat(deprecatedTypesRule.severity()).isEqualTo(Severity.MAJOR);
        assertThat(deprecatedTypesRule.type()).isEqualTo(RuleType.CODE_SMELL);

        RulesDefinition.Rule deleteUpdateRule = repository.rule(DeleteAndUpdateWithoutWhereCheck.RULE_KEY);
        assertThat(deleteUpdateRule).isNotNull();
        assertThat(deleteUpdateRule.severity()).isEqualTo(Severity.BLOCKER);
        assertThat(deleteUpdateRule.type()).isEqualTo(RuleType.BUG);

        RulesDefinition.Rule notInRule = repository.rule(NotInSubqueryCheck.RULE_KEY);
        assertThat(notInRule).isNotNull();
        assertThat(notInRule.severity()).isEqualTo(Severity.CRITICAL);
        assertThat(notInRule.type()).isEqualTo(RuleType.BUG);

        RulesDefinition.Rule nullComparisonRule = repository.rule(NullComparisonCheck.RULE_KEY);
        assertThat(nullComparisonRule).isNotNull();
        assertThat(nullComparisonRule.severity()).isEqualTo(Severity.MAJOR);
        assertThat(nullComparisonRule.type()).isEqualTo(RuleType.BUG);

        RulesDefinition.Rule topWithoutOrderByRule = repository.rule(TopWithoutOrderByCheck.RULE_KEY);
        assertThat(topWithoutOrderByRule).isNotNull();
        assertThat(topWithoutOrderByRule.severity()).isEqualTo(Severity.MAJOR);
        assertThat(topWithoutOrderByRule.type()).isEqualTo(RuleType.BUG);

        RulesDefinition.Rule caseInvariantRule = repository.rule(CaseInputInvariantCheck.RULE_KEY);
        assertThat(caseInvariantRule).isNotNull();
        assertThat(caseInvariantRule.severity()).isEqualTo(Severity.CRITICAL);
        assertThat(caseInvariantRule.type()).isEqualTo(RuleType.BUG);

        RulesDefinition.Rule unusedLocalVariableRule = repository.rule(UnusedLocalVariableCheck.RULE_KEY);
        assertThat(unusedLocalVariableRule).isNotNull();
        assertThat(unusedLocalVariableRule.severity()).isEqualTo(Severity.MINOR);
        assertThat(unusedLocalVariableRule.type()).isEqualTo(RuleType.CODE_SMELL);

        RulesDefinition.Rule unusedParameterRule = repository.rule(UnusedParameterCheck.RULE_KEY);
        assertThat(unusedParameterRule).isNotNull();
        assertThat(unusedParameterRule.severity()).isEqualTo(Severity.MAJOR);
        assertThat(unusedParameterRule.type()).isEqualTo(RuleType.CODE_SMELL);

        RulesDefinition.Rule unusedLabelRule = repository.rule(UnusedLabelCheck.RULE_KEY);
        assertThat(unusedLabelRule).isNotNull();
        assertThat(unusedLabelRule.severity()).isEqualTo(Severity.MAJOR);
        assertThat(unusedLabelRule.type()).isEqualTo(RuleType.CODE_SMELL);

        RulesDefinition.Rule unreachableCodeRule = repository.rule(UnreachableCodeCheck.RULE_KEY);
        assertThat(unreachableCodeRule).isNotNull();
        assertThat(unreachableCodeRule.severity()).isEqualTo(Severity.MAJOR);
        assertThat(unreachableCodeRule.type()).isEqualTo(RuleType.BUG);

        RulesDefinition.Rule singleIterationLoopRule = repository.rule(SingleIterationLoopCheck.RULE_KEY);
        assertThat(singleIterationLoopRule).isNotNull();
        assertThat(singleIterationLoopRule.severity()).isEqualTo(Severity.MAJOR);
        assertThat(singleIterationLoopRule.type()).isEqualTo(RuleType.BUG);

        RulesDefinition.Rule selfAssignmentRule = repository.rule(SelfAssignmentCheck.RULE_KEY);
        assertThat(selfAssignmentRule).isNotNull();
        assertThat(selfAssignmentRule.severity()).isEqualTo(Severity.MAJOR);
        assertThat(selfAssignmentRule.type()).isEqualTo(RuleType.BUG);

        RulesDefinition.Rule catchRethrowRule = repository.rule(CatchClauseShouldDoMoreThanRethrowCheck.RULE_KEY);
        assertThat(catchRethrowRule).isNotNull();
        assertThat(catchRethrowRule.severity()).isEqualTo(Severity.MINOR);
        assertThat(catchRethrowRule.type()).isEqualTo(RuleType.CODE_SMELL);

        RulesDefinition.Rule redundantJumpRule = repository.rule(RedundantJumpCheck.RULE_KEY);
        assertThat(redundantJumpRule).isNotNull();
        assertThat(redundantJumpRule.severity()).isEqualTo(Severity.MINOR);
        assertThat(redundantJumpRule.type()).isEqualTo(RuleType.CODE_SMELL);

        RulesDefinition.Rule syntaxErrorRule = repository.rule(CheckList.SYNTAX_ERROR_RULE_KEY);
        assertThat(syntaxErrorRule).isNotNull();
        assertThat(syntaxErrorRule.severity()).isEqualTo(Severity.BLOCKER);
        assertThat(syntaxErrorRule.type()).isEqualTo(RuleType.BUG);
    }
}
