package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.TSqlFragment;

import java.util.regex.Pattern;

/**
 * Rule: non-standard comparison operators should be replaced by ISO equivalents.
 */
public class AvoidNonStandardComparisonOperatorsCheck extends TSqlCheck {

    public static final String RULE_KEY = "S1742";
    private static final Pattern NON_STANDARD_COMPARISON = Pattern.compile("(?i)(?:!=|!<|!>)");

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void scan(TSqlFragment root, TSqlCheckContext context) {
        super.scan(root, context);
        String sql = getSourceText();
        if (sql != null && NON_STANDARD_COMPARISON.matcher(sql).find()) {
            reportIssue(root, "Replace non-standard comparison operators with ISO standard <> , >=, and <=.");
        }
    }
}
