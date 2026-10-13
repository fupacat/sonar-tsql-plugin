package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.TSqlFragment;

import java.util.regex.Pattern;

/**
 * Rule: ANSI_NULLS, ANSI_PADDING, and CONCAT_NULL_YIELDS_NULL should not be set OFF.
 */
public class SetAnsiNullsCheck extends TSqlCheck {

    public static final String RULE_KEY = "S4154";
    private static final Pattern OFF_PATTERN = Pattern.compile("(?is)\\bset\\s+(?:ansi_nulls|ansi_padding|concat_null_yields_null)\\s+off\\b");

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void scan(TSqlFragment root, TSqlCheckContext context) {
        super.scan(root, context);
        String sql = getSourceText();
        if (sql != null && OFF_PATTERN.matcher(sql).find()) {
            reportIssue(root, "ANSI_NULLS, ANSI_PADDING, and CONCAT_NULL_YIELDS_NULL should not be toggled off.");
        }
    }
}
