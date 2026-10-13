package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.TSqlFragment;

import java.util.regex.Pattern;

/**
 * Rule: SET NOCOUNT ON should be enabled in procedures and triggers.
 */
public class SetNoCountCheck extends TSqlCheck {

    public static final String RULE_KEY = "S4819";
    private static final Pattern PROCEDURE_TRIGGER_PATTERN = Pattern.compile("(?is)create\\s+(?:or\\s+alter\\s+)?(?:procedure|trigger)\\b");
    private static final Pattern NOCOUNT_ON_PATTERN = Pattern.compile("(?is)set\\s+nocount\\s+on\\b");
    private static final Pattern NOCOUNT_OFF_PATTERN = Pattern.compile("(?is)set\\s+nocount\\s+off\\b");

    // TRACEABILITY_MAPPING: REQ-046
    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    // TRACEABILITY_MAPPING: REQ-046
    @Override
    public void scan(TSqlFragment root, TSqlCheckContext context) {
        super.scan(root, context);
        String sql = getSourceText();
        if (sql == null || sql.isBlank() || !PROCEDURE_TRIGGER_PATTERN.matcher(sql).find()) {
            return;
        }
        if (!NOCOUNT_ON_PATTERN.matcher(sql).find() || NOCOUNT_OFF_PATTERN.matcher(sql).find()) {
            reportIssue(root, "SET NOCOUNT ON should be enabled in procedures and triggers.");
        }
    }
}
