package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.UnaryExpression;

public class RepeatedUnaryOperatorCheck extends TSqlCheck {
    public static final String RULE_KEY = "S2761";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(UnaryExpression node) {
        if (node.Expression instanceof UnaryExpression inner) {
            if (node.UnaryExpressionType == inner.UnaryExpressionType) {
                reportIssue(node, "Repeated unary operators should not be used.");
            }
        }
        super.visit(node);
    }
}
