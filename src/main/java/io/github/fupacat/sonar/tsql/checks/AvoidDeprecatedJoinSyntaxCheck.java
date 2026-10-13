package io.github.fupacat.sonar.tsql.checks;

import org.sonar.api.batch.fs.InputFile;

/**
 * Rule: Avoid legacy *= and =* join syntax.
 */
public class AvoidDeprecatedJoinSyntaxCheck extends TSqlCheck {

    public static final String RULE_KEY = "SR0010";

    // TRACEABILITY_MAPPING: REQ-019
    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    // TRACEABILITY_MAPPING: REQ-019
    @Override
    public void scan(io.github.fupacat.ast.TSqlFragment root, TSqlCheckContext context) {
        if (context == null || context.getFile() == null) {
            return;
        }

        try {
            String content = context.getFile().contents();
            if (content != null && (content.contains("*=") || content.contains("=*"))) {
                reportIssueAtLine(1, "Avoid using deprecated join syntax ('*=' or '=*'); use explicit JOIN conditions instead.");
            }
        } catch (Exception ignored) {
            // The check is intentionally conservative when file contents are unavailable.
        }
    }
}
