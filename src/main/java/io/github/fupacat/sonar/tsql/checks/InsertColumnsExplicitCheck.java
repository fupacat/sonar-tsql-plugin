package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.InsertSpecification;

/**
 * Rule: INSERT statements should explicitly list the target columns.
 */
public class InsertColumnsExplicitCheck extends TSqlCheck {

    public static final String RULE_KEY = "S1745";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(InsertSpecification node) {
        if (node.Columns == null || node.Columns.isEmpty()) {
            reportIssue(node, "INSERT statements should explicitly list the target columns.");
        }
        super.visit(node);
    }
}
