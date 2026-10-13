package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.BooleanExpression;
import io.github.fupacat.ast.BooleanNotExpression;
import io.github.fupacat.ast.BooleanParenthesisExpression;
import io.github.fupacat.ast.ParenthesisExpression;
import io.github.fupacat.ast.ScalarExpression;
import io.github.fupacat.ast.UnaryExpression;

public class RepeatedUnaryOperatorCheck extends TSqlCheck {
    public static final String RULE_KEY = "S2761";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(UnaryExpression node) {
        ScalarExpression inner = unwrapParenthesis(node.Expression);
        if (inner instanceof UnaryExpression nested && node.UnaryExpressionType == nested.UnaryExpressionType) {
            reportIssue(node, "Repeated unary operators should not be used.");
        }
        super.visit(node);
    }

    @Override
    public void visit(BooleanNotExpression node) {
        BooleanExpression inner = unwrapParenthesis(node.Expression);
        if (inner instanceof BooleanNotExpression nested) {
            reportIssue(node, "Repeated unary operators should not be used.");
        }
        super.visit(node);
    }

    private static ScalarExpression unwrapParenthesis(ScalarExpression expression) {
        while (expression instanceof ParenthesisExpression parenthesis) {
            expression = parenthesis.Expression;
        }
        return expression;
    }

    private static BooleanExpression unwrapParenthesis(BooleanExpression expression) {
        while (expression instanceof BooleanParenthesisExpression parenthesis) {
            expression = parenthesis.Expression;
        }
        return expression;
    }
}
