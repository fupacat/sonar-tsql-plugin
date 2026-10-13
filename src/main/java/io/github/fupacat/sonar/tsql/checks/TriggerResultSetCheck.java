package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.TSqlFragment;

import java.util.regex.Pattern;

/**
 * Rule: triggers should not return result sets via SELECT, PRINT, or FETCH.
 */
public class TriggerResultSetCheck extends TSqlCheck {

    public static final String RULE_KEY = "S4109";
    private static final Pattern TRIGGER_PATTERN = Pattern.compile("(?is)create\\s+trigger\\b");
    private static final Pattern RESULT_SET_PATTERN = Pattern.compile("(?is)\\b(?:select|print|fetch)\\b");

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void scan(TSqlFragment root, TSqlCheckContext context) {
        super.scan(root, context);
        String sql = getSourceText();
        if (sql != null && TRIGGER_PATTERN.matcher(sql).find() && RESULT_SET_PATTERN.matcher(sql).find()) {
            reportIssue(root, "Triggers should not return result sets via SELECT, PRINT, or FETCH.");
        }
    }
}
