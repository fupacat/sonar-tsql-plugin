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
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, SetNoCountCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, SetAnsiWarningsAndArithabortCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, SetAnsiNullsCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, AvoidDeprecatedSystemTablesCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, FetchIntoCursorCountCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, CaseSubqueryCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, TriggerResultSetCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, AvoidLikeWithoutWildcardCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, AvoidNonStandardComparisonOperatorsCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, UnusedLocalVariableCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, UnusedParameterCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, UnusedLabelCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, UnreachableCodeCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, SingleIterationLoopCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, SelfAssignmentCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, CatchClauseShouldDoMoreThanRethrowCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, RedundantJumpCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, DuplicateBinaryOperandCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, DuplicateIfConditionCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, IdenticalBranchCheck.RULE_KEY_S1871);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, RepeatedUnaryOperatorCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, WhereClauseRedundancyCheck.RULE_KEY_S3633);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, ControlStructureBlockCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, CaseElseMissingCheck.RULE_KEY_S131);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, GotoStatementCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, NestedIfMergeCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, InPredicateIndexedColumnCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, NullableColumnIsNullCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, NarrowingCastCheck.RULE_KEY);
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
