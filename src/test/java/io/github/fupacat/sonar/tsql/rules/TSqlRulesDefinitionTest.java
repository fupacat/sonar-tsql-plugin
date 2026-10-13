package io.github.fupacat.sonar.tsql.rules;

import io.github.fupacat.sonar.tsql.TSqlLanguage;
import io.github.fupacat.sonar.tsql.checks.AvoidDeprecatedTypesCheck;
import io.github.fupacat.sonar.tsql.checks.AvoidLeadingWildcardInLikeCheck;
import io.github.fupacat.sonar.tsql.checks.AvoidNoLockHintCheck;
import io.github.fupacat.sonar.tsql.checks.AvoidSelectAllCheck;
import io.github.fupacat.sonar.tsql.checks.CaseInputInvariantCheck;
import io.github.fupacat.sonar.tsql.checks.CheckList;
import io.github.fupacat.sonar.tsql.checks.DeleteAndUpdateWithoutWhereCheck;
import io.github.fupacat.sonar.tsql.checks.DynamicSqlExecutionCheck;
import io.github.fupacat.sonar.tsql.checks.HardCodedCredentialsCheck;
import io.github.fupacat.sonar.tsql.checks.NotInSubqueryCheck;
import io.github.fupacat.sonar.tsql.checks.NullComparisonCheck;
import io.github.fupacat.sonar.tsql.checks.TopWithoutOrderByCheck;
import io.github.fupacat.sonar.tsql.checks.WeakHashCheck;
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

        // Verify all built-in rules are registered
        assertThat(repository.rules()).hasSize(13);

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

        RulesDefinition.Rule dynamicSqlRule = repository.rule(DynamicSqlExecutionCheck.RULE_KEY);
        assertThat(dynamicSqlRule).isNotNull();
        assertThat(dynamicSqlRule.severity()).isEqualTo(Severity.CRITICAL);
        assertThat(dynamicSqlRule.type()).isEqualTo(RuleType.VULNERABILITY);

        RulesDefinition.Rule weakHashRule = repository.rule(WeakHashCheck.RULE_KEY);
        assertThat(weakHashRule).isNotNull();
        assertThat(weakHashRule.severity()).isEqualTo(Severity.CRITICAL);
        assertThat(weakHashRule.type()).isEqualTo(RuleType.VULNERABILITY);

        RulesDefinition.Rule hardCodedCredentialsRule = repository.rule(HardCodedCredentialsCheck.RULE_KEY);
        assertThat(hardCodedCredentialsRule).isNotNull();
        assertThat(hardCodedCredentialsRule.severity()).isEqualTo(Severity.MAJOR);
        assertThat(hardCodedCredentialsRule.type()).isEqualTo(RuleType.VULNERABILITY);

        RulesDefinition.Rule syntaxErrorRule = repository.rule(CheckList.SYNTAX_ERROR_RULE_KEY);
        assertThat(syntaxErrorRule).isNotNull();
        assertThat(syntaxErrorRule.severity()).isEqualTo(Severity.BLOCKER);
        assertThat(syntaxErrorRule.type()).isEqualTo(RuleType.BUG);
    }
}
