package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.TSqlFragment;

import java.util.regex.Pattern;

/**
 * Rule: COALESCE, IIF, and CASE input expressions should not contain subqueries.
 */
public class CaseSubqueryCheck extends TSqlCheck {

    public static final String RULE_KEY = "S4104";
    private static final Pattern SUBQUERY_INPUT_PATTERN = Pattern.compile(
            "(?is)(?:\\b(?:coalesce|iif)\\s*\\([^;)]*?\\(\\s*select\\b|\\bcase\\s+(?:when\\s+)?\\(\\s*select\\b)"
    );

    // TRACEABILITY_MAPPING: REQ-051
    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    // TRACEABILITY_MAPPING: REQ-051
    @Override
    public void scan(TSqlFragment root, TSqlCheckContext context) {
        super.scan(root, context);
        String sql = getSourceText();
        if (sql != null && SUBQUERY_INPUT_PATTERN.matcher(sql).find()) {
            reportIssue(root, "COALESCE, IIF, and CASE input expressions should not contain subqueries.");
        }
    }
}
