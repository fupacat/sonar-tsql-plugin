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
                        UnusedLocalVariableCheck.RULE_KEY,
                        UnusedParameterCheck.RULE_KEY,
                        UnusedLabelCheck.RULE_KEY,
                        UnreachableCodeCheck.RULE_KEY,
                        SingleIterationLoopCheck.RULE_KEY,
                        SelfAssignmentCheck.RULE_KEY,
                        CatchClauseShouldDoMoreThanRethrowCheck.RULE_KEY,
                        RedundantJumpCheck.RULE_KEY,
                        CheckList.SYNTAX_ERROR_RULE_KEY
                );
    }
}
