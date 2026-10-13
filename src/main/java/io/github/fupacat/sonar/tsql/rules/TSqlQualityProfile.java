package io.github.fupacat.sonar.tsql.rules;

import io.github.fupacat.sonar.tsql.TSqlLanguage;
import io.github.fupacat.sonar.tsql.checks.*;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;

/**
 * Built-in Quality Profile for T-SQL ("Sonar way").
 */
public class TSqlQualityProfile implements BuiltInQualityProfilesDefinition {

    public static final String PROFILE_NAME = "Sonar way";

    // TRACEABILITY_MAPPING: REQ-004
    @Override
    public void define(Context context) {
        NewBuiltInQualityProfile profile = context.createBuiltInQualityProfile(PROFILE_NAME, TSqlLanguage.KEY);
        profile.setDefault(true);

        // Activate all built-in rules
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, AvoidSelectAllCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, AvoidLeadingWildcardInLikeCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, AvoidNoLockHintCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, AvoidDeprecatedTypesCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, DeleteAndUpdateWithoutWhereCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, NotInSubqueryCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, NullComparisonCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, TopWithoutOrderByCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, CaseInputInvariantCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, DynamicSqlExecutionCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, WeakHashCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, HardCodedCredentialsCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, PrimaryKeyRequiredCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, InsertColumnsExplicitCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, ExplicitLengthForVariableTypesCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, OrderByColumnNamesCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, ExistsSelectOneCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, AlterTableCheckEnforcementCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, ColumnReferenceDepthCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, SargableWhereCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, UseScopeIdentityCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, AvoidVariableLengthDataTypeCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, AvoidDeprecatedJoinSyntaxCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, AvoidSpecialCharactersInObjectNamesCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, AvoidReservedWordsInObjectNamesCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, StoredProcedureOutputParameterCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, DeterministicFunctionInWhereCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, AvoidSpPrefixProcedureNameCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, S103Check.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, S104Check.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, S105Check.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, S122Check.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, S125Check.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, S1134Check.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, S1135Check.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, NoSonarCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, S1451Check.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, S3972Check.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, S3973Check.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, S138Check.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, S1151Check.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, CheckList.SYNTAX_ERROR_RULE_KEY);

        profile.done();
    }
}
