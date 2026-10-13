package io.github.fupacat.sonar.tsql.rules;

import io.github.fupacat.sonar.tsql.TSqlLanguage;
import io.github.fupacat.sonar.tsql.checks.*;
import org.junit.jupiter.api.Test;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;

import static org.assertj.core.api.Assertions.assertThat;

class TSqlQualityProfileTest {

    // TRACEABILITY_MAPPING: REQ-004
    @Test
    void testQualityProfileDefinition() {
        TSqlQualityProfile qualityProfile = new TSqlQualityProfile();
        BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
        qualityProfile.define(context);

        BuiltInQualityProfilesDefinition.BuiltInQualityProfile profile =
                context.profile(TSqlLanguage.KEY, TSqlQualityProfile.PROFILE_NAME);

        assertThat(profile).isNotNull();
        assertThat(profile.language()).isEqualTo(TSqlLanguage.KEY);
        assertThat(profile.isDefault()).isTrue();

        assertThat(profile.rules()).extracting(BuiltInQualityProfilesDefinition.BuiltInActiveRule::ruleKey)
                .containsExactlyInAnyOrder(
                        AvoidSelectAllCheck.RULE_KEY,
                        AvoidLeadingWildcardInLikeCheck.RULE_KEY,
                        AvoidNoLockHintCheck.RULE_KEY,
                        AvoidDeprecatedTypesCheck.RULE_KEY,
                        DeleteAndUpdateWithoutWhereCheck.RULE_KEY,
                        NotInSubqueryCheck.RULE_KEY,
                        NullComparisonCheck.RULE_KEY,
                        TopWithoutOrderByCheck.RULE_KEY,
                        CaseInputInvariantCheck.RULE_KEY,
                        DynamicSqlExecutionCheck.RULE_KEY,
                        WeakHashCheck.RULE_KEY,
                        HardCodedCredentialsCheck.RULE_KEY,
                        PrimaryKeyRequiredCheck.RULE_KEY,
                        InsertColumnsExplicitCheck.RULE_KEY,
                        ExplicitLengthForVariableTypesCheck.RULE_KEY,
                        OrderByColumnNamesCheck.RULE_KEY,
                        ExistsSelectOneCheck.RULE_KEY,
                        AlterTableCheckEnforcementCheck.RULE_KEY,
                        ColumnReferenceDepthCheck.RULE_KEY,
                        SargableWhereCheck.RULE_KEY,
                        UseScopeIdentityCheck.RULE_KEY,
                        AvoidVariableLengthDataTypeCheck.RULE_KEY,
                        AvoidDeprecatedJoinSyntaxCheck.RULE_KEY,
                        AvoidSpecialCharactersInObjectNamesCheck.RULE_KEY,
                        AvoidReservedWordsInObjectNamesCheck.RULE_KEY,
                        StoredProcedureOutputParameterCheck.RULE_KEY,
                        DeterministicFunctionInWhereCheck.RULE_KEY,
                        AvoidSpPrefixProcedureNameCheck.RULE_KEY,
                        SetNoCountCheck.RULE_KEY,
                        SetAnsiWarningsAndArithabortCheck.RULE_KEY,
                        SetAnsiNullsCheck.RULE_KEY,
                        AvoidDeprecatedSystemTablesCheck.RULE_KEY,
                        FetchIntoCursorCountCheck.RULE_KEY,
                        CaseSubqueryCheck.RULE_KEY,
                        TriggerResultSetCheck.RULE_KEY,
                        AvoidLikeWithoutWildcardCheck.RULE_KEY,
                        AvoidNonStandardComparisonOperatorsCheck.RULE_KEY,
                        UnusedLocalVariableCheck.RULE_KEY,
                        UnusedParameterCheck.RULE_KEY,
                        UnusedLabelCheck.RULE_KEY,
                        UnreachableCodeCheck.RULE_KEY,
                        SingleIterationLoopCheck.RULE_KEY,
                        SelfAssignmentCheck.RULE_KEY,
                        CatchClauseShouldDoMoreThanRethrowCheck.RULE_KEY,
                        RedundantJumpCheck.RULE_KEY,
                        S103Check.RULE_KEY,
                        S104Check.RULE_KEY,
                        S105Check.RULE_KEY,
                        S122Check.RULE_KEY,
                        S125Check.RULE_KEY,
                        S1134Check.RULE_KEY,
                        S1135Check.RULE_KEY,
                        NoSonarCheck.RULE_KEY,
                        S1451Check.RULE_KEY,
                        S3972Check.RULE_KEY,
                        S3973Check.RULE_KEY,
                        S138Check.RULE_KEY,
                        S1151Check.RULE_KEY,
                        CheckList.SYNTAX_ERROR_RULE_KEY
                );
    }
}
