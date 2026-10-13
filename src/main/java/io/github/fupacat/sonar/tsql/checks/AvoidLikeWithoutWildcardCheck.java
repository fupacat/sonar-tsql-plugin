package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.TSqlFragment;

import java.util.regex.Pattern;

/**
 * Rule: LIKE predicates without wildcards should use =.
 */
public class AvoidLikeWithoutWildcardCheck extends TSqlCheck {

    public static final String RULE_KEY = "S3643";
    private static final Pattern LIKE_WITHOUT_WILDCARD = Pattern.compile("(?is)\\b\w+\\s+like\\s+'[^'%_]*'");

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void scan(TSqlFragment root, TSqlCheckContext context) {
        super.scan(root, context);
        String sql = getSourceText();
        if (sql != null && LIKE_WITHOUT_WILDCARD.matcher(sql).find()) {
            reportIssue(root, "LIKE predicates without wildcards should use '=' instead.");
        }
    }
}
