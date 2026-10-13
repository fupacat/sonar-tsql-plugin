package io.github.fupacat.sonar.tsql.rules;

import io.github.fupacat.sonar.tsql.TSqlLanguage;
import io.github.fupacat.sonar.tsql.checks.AvoidDeprecatedTypesCheck;
import io.github.fupacat.sonar.tsql.checks.AvoidLeadingWildcardInLikeCheck;
import io.github.fupacat.sonar.tsql.checks.AvoidNoLockHintCheck;
import io.github.fupacat.sonar.tsql.checks.AvoidSelectAllCheck;
import io.github.fupacat.sonar.tsql.checks.CaseElseMissingCheck;
import io.github.fupacat.sonar.tsql.checks.CaseInputInvariantCheck;
import io.github.fupacat.sonar.tsql.checks.CheckList;
import io.github.fupacat.sonar.tsql.checks.ControlStructureBlockCheck;
import io.github.fupacat.sonar.tsql.checks.DeleteAndUpdateWithoutWhereCheck;
import io.github.fupacat.sonar.tsql.checks.DuplicateBinaryOperandCheck;
import io.github.fupacat.sonar.tsql.checks.DuplicateIfConditionCheck;
import io.github.fupacat.sonar.tsql.checks.GotoStatementCheck;
import io.github.fupacat.sonar.tsql.checks.IdenticalBranchCheck;
import io.github.fupacat.sonar.tsql.checks.NestedIfMergeCheck;
import io.github.fupacat.sonar.tsql.checks.NotInSubqueryCheck;
import io.github.fupacat.sonar.tsql.checks.NullComparisonCheck;
import io.github.fupacat.sonar.tsql.checks.RepeatedUnaryOperatorCheck;
import io.github.fupacat.sonar.tsql.checks.TopWithoutOrderByCheck;
import io.github.fupacat.sonar.tsql.checks.WhereClauseRedundancyCheck;
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
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, DuplicateBinaryOperandCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, DuplicateIfConditionCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, IdenticalBranchCheck.RULE_KEY_S1871);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, RepeatedUnaryOperatorCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, WhereClauseRedundancyCheck.RULE_KEY_S3633);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, ControlStructureBlockCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, CaseElseMissingCheck.RULE_KEY_S131);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, GotoStatementCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, NestedIfMergeCheck.RULE_KEY);
        profile.activateRule(TSqlRulesDefinition.REPOSITORY_KEY, CheckList.SYNTAX_ERROR_RULE_KEY);

        profile.done();
    }
}
