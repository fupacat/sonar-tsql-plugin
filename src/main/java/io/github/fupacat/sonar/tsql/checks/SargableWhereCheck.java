package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.BooleanComparisonExpression;
import io.github.fupacat.ast.BinaryExpression;
import io.github.fupacat.ast.ColumnReferenceExpression;
import io.github.fupacat.ast.FunctionCall;
import io.github.fupacat.ast.ParenthesisExpression;
import io.github.fupacat.ast.ScalarExpression;

/**
 * Rule: Move column references away from functions and arithmetic in WHERE predicates.
 * Column expressions embedded in function calls or arithmetic prevent index seeks.
 */
public class SargableWhereCheck extends TSqlCheck {

    public static final String RULE_KEY = "SR0006";

    // TRACEABILITY_MAPPING: REQ-016
    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    // TRACEABILITY_MAPPING: REQ-016
    @Override
    public void visit(BooleanComparisonExpression node) {
        if (node != null && isNonSargable(node)) {
            reportIssue(node, "Move the column reference to one side of the comparison in WHERE clauses and avoid wrapping it in functions or arithmetic for better index SARGability.");
        }
        super.visit(node);
    }

    private boolean isNonSargable(BooleanComparisonExpression node) {
        return hasEmbeddedColumn(node.FirstExpression) || hasEmbeddedColumn(node.SecondExpression);
    }

    private boolean hasEmbeddedColumn(ScalarExpression expression) {
        if (expression == null) {
            return false;
        }
        if (expression instanceof ColumnReferenceExpression) {
            return false;
        }
        if (expression instanceof BinaryExpression binaryExpression) {
            return hasEmbeddedColumn(binaryExpression.FirstExpression)
                    || hasEmbeddedColumn(binaryExpression.SecondExpression);
        }
        if (expression instanceof FunctionCall functionCall) {
            if (functionCall.Parameters == null) {
                return false;
            }
            return functionCall.Parameters.stream().anyMatch(this::hasEmbeddedColumn);
        }
        if (expression instanceof ParenthesisExpression parenthesisExpression) {
            return hasEmbeddedColumn(parenthesisExpression.Expression);
        }
        return false;
    }
}
