package io.github.fupacat.sonar.tsql.rules;

import io.github.fupacat.sonar.tsql.TSqlLanguage;
<<<<<<< HEAD
import io.github.fupacat.sonar.tsql.checks.AvoidDeprecatedTypesCheck;
import io.github.fupacat.sonar.tsql.checks.AvoidLeadingWildcardInLikeCheck;
import io.github.fupacat.sonar.tsql.checks.AvoidNoLockHintCheck;
import io.github.fupacat.sonar.tsql.checks.AvoidSelectAllCheck;
import io.github.fupacat.sonar.tsql.checks.CaseInputInvariantCheck;
import io.github.fupacat.sonar.tsql.checks.CheckList;
import io.github.fupacat.sonar.tsql.checks.DeleteAndUpdateWithoutWhereCheck;
import io.github.fupacat.sonar.tsql.checks.NotInSubqueryCheck;
import io.github.fupacat.sonar.tsql.checks.NullComparisonCheck;
import io.github.fupacat.sonar.tsql.checks.TopWithoutOrderByCheck;
import io.github.fupacat.sonar.tsql.checks.AvoidDeprecatedSystemTablesCheck;
import io.github.fupacat.sonar.tsql.checks.AvoidLikeWithoutWildcardCheck;
import io.github.fupacat.sonar.tsql.checks.AvoidNonStandardComparisonOperatorsCheck;
import io.github.fupacat.sonar.tsql.checks.CaseSubqueryCheck;
import io.github.fupacat.sonar.tsql.checks.FetchIntoCursorCountCheck;
import io.github.fupacat.sonar.tsql.checks.SetAnsiNullsCheck;
import io.github.fupacat.sonar.tsql.checks.SetAnsiWarningsAndArithabortCheck;
import io.github.fupacat.sonar.tsql.checks.SetNoCountCheck;
import io.github.fupacat.sonar.tsql.checks.TriggerResultSetCheck;
=======
import io.github.fupacat.sonar.tsql.checks.*;
>>>>>>> master
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
<<<<<<< HEAD
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, SetNoCountCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, SetAnsiWarningsAndArithabortCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, SetAnsiNullsCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, AvoidDeprecatedSystemTablesCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, FetchIntoCursorCountCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, CaseSubqueryCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, TriggerResultSetCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, AvoidLikeWithoutWildcardCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, AvoidNonStandardComparisonOperatorsCheck.RULE_KEY);
=======
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
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, SetNoCountCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, SetAnsiWarningsAndArithabortCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, SetAnsiNullsCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, AvoidDeprecatedSystemTablesCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, FetchIntoCursorCountCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, CaseSubqueryCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, TriggerResultSetCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, AvoidLikeWithoutWildcardCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, AvoidNonStandardComparisonOperatorsCheck.RULE_KEY);
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
>>>>>>> master
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, CheckList.SYNTAX_ERROR_RULE_KEY);

        profile.done();
    }
}
