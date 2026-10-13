package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.TSqlFragment;

import java.util.regex.Pattern;

/**
 * Rule: ANSI_WARNINGS and ARITHABORT should not be set OFF.
 */
public class SetAnsiWarningsAndArithabortCheck extends TSqlCheck {

    public static final String RULE_KEY = "S6498";
    private static final Pattern OFF_PATTERN = Pattern.compile("(?is)\\bset\\s+(?:ansi_warnings|arithabort)\\s+off\\b");

    // TRACEABILITY_MAPPING: REQ-047
    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    // TRACEABILITY_MAPPING: REQ-047
    @Override
    public void scan(TSqlFragment root, TSqlCheckContext context) {
        super.scan(root, context);
        String sql = getSourceText();
        if (sql != null && OFF_PATTERN.matcher(sql).find()) {
            reportIssue(root, "ANSI_WARNINGS and ARITHABORT should not be set to OFF.");
        }
    }
}
