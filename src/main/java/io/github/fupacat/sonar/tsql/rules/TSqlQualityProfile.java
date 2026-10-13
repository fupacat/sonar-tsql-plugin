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
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, CheckList.SYNTAX_ERROR_RULE_KEY);

        profile.done();
    }
}
