package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.TSqlFragment;

import java.util.regex.Pattern;

/**
 * Rule: ANSI_NULLS, ANSI_PADDING, and CONCAT_NULL_YIELDS_NULL should not be set OFF.
 */
public class SetAnsiNullsCheck extends TSqlCheck {

    public static final String RULE_KEY = "S4154";
    private static final Pattern SET_ANSI_NULLS_OFF = Pattern.compile("(?is)\\bset\\s+(?:ansi_nulls|ansi_padding|concat_null_yields_null)\\s+off\\b");

    // TRACEABILITY_MAPPING: REQ-048
    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    // TRACEABILITY_MAPPING: REQ-048
    @Override
    public void scan(TSqlFragment root, TSqlCheckContext context) {
        super.scan(root, context);
        String sql = getSourceText();
        boolean hasDisabledOption = sql != null && SET_ANSI_NULLS_OFF.matcher(sql).find();
        if (hasDisabledOption) {
            reportIssue(root, "ANSI_NULLS, ANSI_PADDING, and CONCAT_NULL_YIELDS_NULL should not be toggled off.");
        }
    }
}
