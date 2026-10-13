package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.ExpressionWithSortOrder;
import io.github.fupacat.ast.IntegerLiteral;
import io.github.fupacat.ast.OrderByClause;

/**
 * Rule: ORDER BY clauses should use column names instead of positional ordinals.
 */
public class OrderByColumnNamesCheck extends TSqlCheck {

    public static final String RULE_KEY = "S1735";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(OrderByClause node) {
        if (node.OrderByElements != null) {
            for (ExpressionWithSortOrder element : node.OrderByElements) {
                if (element != null && element.Expression instanceof IntegerLiteral) {
                    reportIssue(element, "ORDER BY clauses should use column names instead of positional ordinals.");
                }
            }
        }
        super.visit(node);
    }
}
