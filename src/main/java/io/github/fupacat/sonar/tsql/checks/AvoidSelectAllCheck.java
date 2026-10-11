package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.SelectStarExpression;

/**
 * Rule: Avoid using SELECT * in queries.
 * Explicitly naming columns prevents unnecessary data transfer, improves index coverage,
 * and insulates application code from schema changes.
 */
public class AvoidSelectAllCheck extends TSqlCheck {

    public static final String RULE_KEY = "AvoidSelectAll";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(SelectStarExpression node) {
        reportIssue(node, "Avoid using 'SELECT *'. Specify explicit column names instead.");
    }
}
