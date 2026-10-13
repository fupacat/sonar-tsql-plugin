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
