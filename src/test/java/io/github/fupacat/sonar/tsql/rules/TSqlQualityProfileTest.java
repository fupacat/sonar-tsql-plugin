package io.github.fupacat.sonar.tsql.rules;

import io.github.fupacat.sonar.tsql.TSqlLanguage;
<<<<<<< HEAD
import io.github.fupacat.sonar.tsql.checks.AvoidDeprecatedSystemTablesCheck;
import io.github.fupacat.sonar.tsql.checks.AvoidDeprecatedTypesCheck;
import io.github.fupacat.sonar.tsql.checks.AvoidLeadingWildcardInLikeCheck;
import io.github.fupacat.sonar.tsql.checks.AvoidLikeWithoutWildcardCheck;
import io.github.fupacat.sonar.tsql.checks.AvoidNoLockHintCheck;
import io.github.fupacat.sonar.tsql.checks.AvoidNonStandardComparisonOperatorsCheck;
import io.github.fupacat.sonar.tsql.checks.AvoidSelectAllCheck;
import io.github.fupacat.sonar.tsql.checks.CaseInputInvariantCheck;
import io.github.fupacat.sonar.tsql.checks.CaseSubqueryCheck;
import io.github.fupacat.sonar.tsql.checks.CheckList;
import io.github.fupacat.sonar.tsql.checks.DeleteAndUpdateWithoutWhereCheck;
import io.github.fupacat.sonar.tsql.checks.FetchIntoCursorCountCheck;
import io.github.fupacat.sonar.tsql.checks.NotInSubqueryCheck;
import io.github.fupacat.sonar.tsql.checks.NullComparisonCheck;
import io.github.fupacat.sonar.tsql.checks.SetAnsiNullsCheck;
import io.github.fupacat.sonar.tsql.checks.SetAnsiWarningsAndArithabortCheck;
import io.github.fupacat.sonar.tsql.checks.SetNoCountCheck;
import io.github.fupacat.sonar.tsql.checks.TopWithoutOrderByCheck;
import io.github.fupacat.sonar.tsql.checks.TriggerResultSetCheck;
=======
import io.github.fupacat.sonar.tsql.checks.*;
>>>>>>> master
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
<<<<<<< HEAD
                        SetNoCountCheck.RULE_KEY,
                        SetAnsiWarningsAndArithabortCheck.RULE_KEY,
                        SetAnsiNullsCheck.RULE_KEY,
                        AvoidDeprecatedSystemTablesCheck.RULE_KEY,
                        FetchIntoCursorCountCheck.RULE_KEY,
                        CaseSubqueryCheck.RULE_KEY,
                        TriggerResultSetCheck.RULE_KEY,
                        AvoidLikeWithoutWildcardCheck.RULE_KEY,
                        AvoidNonStandardComparisonOperatorsCheck.RULE_KEY,
=======
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
>>>>>>> master
                        CheckList.SYNTAX_ERROR_RULE_KEY
                );
    }
}
