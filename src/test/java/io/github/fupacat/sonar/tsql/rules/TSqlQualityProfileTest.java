package io.github.fupacat.sonar.tsql.rules;

import io.github.fupacat.sonar.tsql.TSqlLanguage;
import io.github.fupacat.sonar.tsql.checks.AlterTableCheckEnforcementCheck;
import io.github.fupacat.sonar.tsql.checks.AvoidDeprecatedTypesCheck;
import io.github.fupacat.sonar.tsql.checks.AvoidLeadingWildcardInLikeCheck;
import io.github.fupacat.sonar.tsql.checks.AvoidNoLockHintCheck;
import io.github.fupacat.sonar.tsql.checks.AvoidSelectAllCheck;
import io.github.fupacat.sonar.tsql.checks.CaseInputInvariantCheck;
import io.github.fupacat.sonar.tsql.checks.CheckList;
import io.github.fupacat.sonar.tsql.checks.ColumnReferenceDepthCheck;
import io.github.fupacat.sonar.tsql.checks.DeleteAndUpdateWithoutWhereCheck;
import io.github.fupacat.sonar.tsql.checks.ExistsSelectOneCheck;
import io.github.fupacat.sonar.tsql.checks.ExplicitLengthForVariableTypesCheck;
import io.github.fupacat.sonar.tsql.checks.InsertColumnsExplicitCheck;
import io.github.fupacat.sonar.tsql.checks.NotInSubqueryCheck;
import io.github.fupacat.sonar.tsql.checks.NullComparisonCheck;
import io.github.fupacat.sonar.tsql.checks.OrderByColumnNamesCheck;
import io.github.fupacat.sonar.tsql.checks.PrimaryKeyRequiredCheck;
import io.github.fupacat.sonar.tsql.checks.TopWithoutOrderByCheck;
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
                        PrimaryKeyRequiredCheck.RULE_KEY,
                        InsertColumnsExplicitCheck.RULE_KEY,
                        ExplicitLengthForVariableTypesCheck.RULE_KEY,
                        OrderByColumnNamesCheck.RULE_KEY,
                        ExistsSelectOneCheck.RULE_KEY,
                        AlterTableCheckEnforcementCheck.RULE_KEY,
                        ColumnReferenceDepthCheck.RULE_KEY,
                        CheckList.SYNTAX_ERROR_RULE_KEY
                );
    }
}
