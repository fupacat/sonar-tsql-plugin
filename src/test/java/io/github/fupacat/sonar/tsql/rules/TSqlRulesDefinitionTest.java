package io.github.fupacat.sonar.tsql.rules;

import io.github.fupacat.sonar.tsql.TSqlLanguage;
import io.github.fupacat.sonar.tsql.checks.*;
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

        // Verify all 42 rules are registered
        assertThat(repository.rules()).hasSize(51);

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

        RulesDefinition.Rule sargableRule = repository.rule(SargableWhereCheck.RULE_KEY);
        assertThat(sargableRule).isNotNull();
        assertThat(sargableRule.severity()).isEqualTo(Severity.MAJOR);
        assertThat(sargableRule.type()).isEqualTo(RuleType.CODE_SMELL);

        RulesDefinition.Rule scopeIdentityRule = repository.rule(UseScopeIdentityCheck.RULE_KEY);
        assertThat(scopeIdentityRule).isNotNull();
        assertThat(scopeIdentityRule.severity()).isEqualTo(Severity.MAJOR);
        assertThat(scopeIdentityRule.type()).isEqualTo(RuleType.BUG);

        RulesDefinition.Rule variableLengthTypeRule = repository.rule(AvoidVariableLengthDataTypeCheck.RULE_KEY);
        assertThat(variableLengthTypeRule).isNotNull();
        assertThat(variableLengthTypeRule.severity()).isEqualTo(Severity.MAJOR);
        assertThat(variableLengthTypeRule.type()).isEqualTo(RuleType.CODE_SMELL);

        RulesDefinition.Rule deprecatedJoinRule = repository.rule(AvoidDeprecatedJoinSyntaxCheck.RULE_KEY);
        assertThat(deprecatedJoinRule).isNotNull();
        assertThat(deprecatedJoinRule.severity()).isEqualTo(Severity.MAJOR);
        assertThat(deprecatedJoinRule.type()).isEqualTo(RuleType.CODE_SMELL);

        RulesDefinition.Rule specialCharactersRule = repository.rule(AvoidSpecialCharactersInObjectNamesCheck.RULE_KEY);
        assertThat(specialCharactersRule).isNotNull();
        assertThat(specialCharactersRule.severity()).isEqualTo(Severity.MAJOR);
        assertThat(specialCharactersRule.type()).isEqualTo(RuleType.CODE_SMELL);

        RulesDefinition.Rule reservedWordsRule = repository.rule(AvoidReservedWordsInObjectNamesCheck.RULE_KEY);
        assertThat(reservedWordsRule).isNotNull();
        assertThat(reservedWordsRule.severity()).isEqualTo(Severity.MAJOR);
        assertThat(reservedWordsRule.type()).isEqualTo(RuleType.CODE_SMELL);

        RulesDefinition.Rule outputParamRule = repository.rule(StoredProcedureOutputParameterCheck.RULE_KEY);
        assertThat(outputParamRule).isNotNull();
        assertThat(outputParamRule.severity()).isEqualTo(Severity.CRITICAL);
        assertThat(outputParamRule.type()).isEqualTo(RuleType.BUG);

        RulesDefinition.Rule deterministicFunctionRule = repository.rule(DeterministicFunctionInWhereCheck.RULE_KEY);
        assertThat(deterministicFunctionRule).isNotNull();
        assertThat(deterministicFunctionRule.severity()).isEqualTo(Severity.MAJOR);
        assertThat(deterministicFunctionRule.type()).isEqualTo(RuleType.CODE_SMELL);

        RulesDefinition.Rule spPrefixRule = repository.rule(AvoidSpPrefixProcedureNameCheck.RULE_KEY);
        assertThat(spPrefixRule).isNotNull();
        assertThat(spPrefixRule.severity()).isEqualTo(Severity.MAJOR);
        assertThat(spPrefixRule.type()).isEqualTo(RuleType.CODE_SMELL);

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

        RulesDefinition.Rule primaryKeyRule = repository.rule(PrimaryKeyRequiredCheck.RULE_KEY);
        assertThat(primaryKeyRule).isNotNull();
        assertThat(primaryKeyRule.severity()).isEqualTo(Severity.BLOCKER);
        assertThat(primaryKeyRule.type()).isEqualTo(RuleType.CODE_SMELL);

        RulesDefinition.Rule insertColumnsRule = repository.rule(InsertColumnsExplicitCheck.RULE_KEY);
        assertThat(insertColumnsRule).isNotNull();
        assertThat(insertColumnsRule.severity()).isEqualTo(Severity.CRITICAL);
        assertThat(insertColumnsRule.type()).isEqualTo(RuleType.CODE_SMELL);

        RulesDefinition.Rule lengthRule = repository.rule(ExplicitLengthForVariableTypesCheck.RULE_KEY);
        assertThat(lengthRule).isNotNull();
        assertThat(lengthRule.severity()).isEqualTo(Severity.CRITICAL);
        assertThat(lengthRule.type()).isEqualTo(RuleType.CODE_SMELL);

        RulesDefinition.Rule orderByRule = repository.rule(OrderByColumnNamesCheck.RULE_KEY);
        assertThat(orderByRule).isNotNull();
        assertThat(orderByRule.severity()).isEqualTo(Severity.MAJOR);
        assertThat(orderByRule.type()).isEqualTo(RuleType.CODE_SMELL);

        RulesDefinition.Rule existsRule = repository.rule(ExistsSelectOneCheck.RULE_KEY);
        assertThat(existsRule).isNotNull();
        assertThat(existsRule.severity()).isEqualTo(Severity.CRITICAL);
        assertThat(existsRule.type()).isEqualTo(RuleType.CODE_SMELL);

        RulesDefinition.Rule alterTableRule = repository.rule(AlterTableCheckEnforcementCheck.RULE_KEY);
        assertThat(alterTableRule).isNotNull();
        assertThat(alterTableRule.severity()).isEqualTo(Severity.MAJOR);
        assertThat(alterTableRule.type()).isEqualTo(RuleType.CODE_SMELL);

        RulesDefinition.Rule columnDepthRule = repository.rule(ColumnReferenceDepthCheck.RULE_KEY);
        assertThat(columnDepthRule).isNotNull();
        assertThat(columnDepthRule.severity()).isEqualTo(Severity.MINOR);
        assertThat(columnDepthRule.type()).isEqualTo(RuleType.CODE_SMELL);

        RulesDefinition.Rule s103Rule = repository.rule(S103Check.RULE_KEY);
        assertThat(s103Rule).isNotNull();
        assertThat(s103Rule.severity()).isEqualTo(Severity.MINOR);

        RulesDefinition.Rule syntaxErrorRule = repository.rule(CheckList.SYNTAX_ERROR_RULE_KEY);
        assertThat(syntaxErrorRule).isNotNull();
        assertThat(syntaxErrorRule.severity()).isEqualTo(Severity.BLOCKER);
        assertThat(syntaxErrorRule.type()).isEqualTo(RuleType.BUG);
    }
}
