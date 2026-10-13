package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.BinaryExpression;
import io.github.fupacat.ast.BooleanBinaryExpression;
import io.github.fupacat.ast.BooleanComparisonExpression;

public class DuplicateBinaryOperandCheck extends TSqlCheck {
    public static final String RULE_KEY = "S1764";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(BooleanComparisonExpression node) {
        if (node.FirstExpression != null && node.SecondExpression != null
                && ConditionalLogicUtils.structurallySame(node.FirstExpression, node.SecondExpression)) {
            reportIssue(node, "Identical expressions should not be used on both sides of a comparison.");
        }
        super.visit(node);
    }

    @Override
    public void visit(BooleanBinaryExpression node) {
        if (node.FirstExpression != null && node.SecondExpression != null
                && ConditionalLogicUtils.structurallySame(node.FirstExpression, node.SecondExpression)) {
            reportIssue(node, "Identical expressions should not be used on both sides of a boolean operator.");
        }
        super.visit(node);
    }

    @Override
    public void visit(BinaryExpression node) {
        if (node.FirstExpression != null && node.SecondExpression != null
                && ConditionalLogicUtils.structurallySame(node.FirstExpression, node.SecondExpression)) {
            reportIssue(node, "Identical expressions should not be used on both sides of a binary operator.");
        }
        super.visit(node);
    }
}
