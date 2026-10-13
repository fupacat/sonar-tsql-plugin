package io.github.fupacat.sonar.tsql.rules;

import io.github.fupacat.sonar.tsql.TSqlLanguage;
import io.github.fupacat.sonar.tsql.checks.AvoidDeprecatedTypesCheck;
import io.github.fupacat.sonar.tsql.checks.AvoidLeadingWildcardInLikeCheck;
import io.github.fupacat.sonar.tsql.checks.AvoidNoLockHintCheck;
import io.github.fupacat.sonar.tsql.checks.AvoidSelectAllCheck;
import io.github.fupacat.sonar.tsql.checks.CaseElseMissingCheck;
import io.github.fupacat.sonar.tsql.checks.CaseInputInvariantCheck;
import io.github.fupacat.sonar.tsql.checks.CheckList;
import io.github.fupacat.sonar.tsql.checks.ControlStructureBlockCheck;
import io.github.fupacat.sonar.tsql.checks.DeleteAndUpdateWithoutWhereCheck;
import io.github.fupacat.sonar.tsql.checks.DuplicateBinaryOperandCheck;
import io.github.fupacat.sonar.tsql.checks.DuplicateIfConditionCheck;
import io.github.fupacat.sonar.tsql.checks.GotoStatementCheck;
import io.github.fupacat.sonar.tsql.checks.IdenticalBranchCheck;
import io.github.fupacat.sonar.tsql.checks.NestedIfMergeCheck;
import io.github.fupacat.sonar.tsql.checks.NotInSubqueryCheck;
import io.github.fupacat.sonar.tsql.checks.NullComparisonCheck;
import io.github.fupacat.sonar.tsql.checks.RepeatedUnaryOperatorCheck;
import io.github.fupacat.sonar.tsql.checks.TopWithoutOrderByCheck;
import io.github.fupacat.sonar.tsql.checks.WhereClauseRedundancyCheck;
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

        // Verify all 19 rules are registered
        assertThat(repository.rules()).hasSize(19);

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

        RulesDefinition.Rule duplicateBinaryRule = repository.rule(DuplicateBinaryOperandCheck.RULE_KEY);
        assertThat(duplicateBinaryRule).isNotNull();
        assertThat(duplicateBinaryRule.severity()).isEqualTo(Severity.MAJOR);
        assertThat(duplicateBinaryRule.type()).isEqualTo(RuleType.BUG);

        RulesDefinition.Rule duplicateIfRule = repository.rule(DuplicateIfConditionCheck.RULE_KEY);
        assertThat(duplicateIfRule).isNotNull();
        assertThat(duplicateIfRule.severity()).isEqualTo(Severity.MAJOR);
        assertThat(duplicateIfRule.type()).isEqualTo(RuleType.BUG);

        RulesDefinition.Rule identicalBranchRule = repository.rule(IdenticalBranchCheck.RULE_KEY_S1871);
        assertThat(identicalBranchRule).isNotNull();
        assertThat(identicalBranchRule.severity()).isEqualTo(Severity.MAJOR);
        assertThat(identicalBranchRule.type()).isEqualTo(RuleType.CODE_SMELL);

        RulesDefinition.Rule repeatedUnaryRule = repository.rule(RepeatedUnaryOperatorCheck.RULE_KEY);
        assertThat(repeatedUnaryRule).isNotNull();
        assertThat(repeatedUnaryRule.severity()).isEqualTo(Severity.MAJOR);
        assertThat(repeatedUnaryRule.type()).isEqualTo(RuleType.BUG);

        RulesDefinition.Rule redundancyRule = repository.rule(WhereClauseRedundancyCheck.RULE_KEY_S3633);
        assertThat(redundancyRule).isNotNull();
        assertThat(redundancyRule.severity()).isEqualTo(Severity.MAJOR);
        assertThat(redundancyRule.type()).isEqualTo(RuleType.BUG);

        RulesDefinition.Rule blockRule = repository.rule(ControlStructureBlockCheck.RULE_KEY);
        assertThat(blockRule).isNotNull();
        assertThat(blockRule.severity()).isEqualTo(Severity.CRITICAL);
        assertThat(blockRule.type()).isEqualTo(RuleType.CODE_SMELL);

        RulesDefinition.Rule elseRule = repository.rule(CaseElseMissingCheck.RULE_KEY_S131);
        assertThat(elseRule).isNotNull();
        assertThat(elseRule.severity()).isEqualTo(Severity.CRITICAL);
        assertThat(elseRule.type()).isEqualTo(RuleType.CODE_SMELL);

        RulesDefinition.Rule gotoRule = repository.rule(GotoStatementCheck.RULE_KEY);
        assertThat(gotoRule).isNotNull();
        assertThat(gotoRule.severity()).isEqualTo(Severity.MAJOR);
        assertThat(gotoRule.type()).isEqualTo(RuleType.CODE_SMELL);

        RulesDefinition.Rule nestedIfRule = repository.rule(NestedIfMergeCheck.RULE_KEY);
        assertThat(nestedIfRule).isNotNull();
        assertThat(nestedIfRule.severity()).isEqualTo(Severity.MAJOR);
        assertThat(nestedIfRule.type()).isEqualTo(RuleType.CODE_SMELL);

        RulesDefinition.Rule syntaxErrorRule = repository.rule(CheckList.SYNTAX_ERROR_RULE_KEY);
        assertThat(syntaxErrorRule).isNotNull();
        assertThat(syntaxErrorRule.severity()).isEqualTo(Severity.BLOCKER);
        assertThat(syntaxErrorRule.type()).isEqualTo(RuleType.BUG);
    }
}
