package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.BinaryExpression;
import io.github.fupacat.ast.BooleanBinaryExpression;
import io.github.fupacat.ast.BooleanComparisonExpression;
import io.github.fupacat.ast.BooleanExpression;
import io.github.fupacat.ast.ColumnReferenceExpression;
import io.github.fupacat.ast.FunctionCall;
import io.github.fupacat.ast.ParenthesisExpression;
import io.github.fupacat.ast.ScalarExpression;
import io.github.fupacat.ast.WhereClause;

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
    public void visit(WhereClause node) {
        if (node != null && node.SearchCondition != null && isNonSargable(node.SearchCondition)) {
            reportIssue(node, "Move the column reference to one side of the comparison in WHERE clauses and avoid wrapping it in functions or arithmetic for better index SARGability.");
        }
        super.visit(node);
    }

    private boolean isNonSargable(BooleanExpression expression) {
        if (expression instanceof BooleanBinaryExpression booleanBinaryExpression) {
            return isNonSargable(booleanBinaryExpression.FirstExpression)
                    || isNonSargable(booleanBinaryExpression.SecondExpression);
        }
        if (expression instanceof BooleanComparisonExpression comparisonExpression) {
            return containsEmbeddedColumn(comparisonExpression.FirstExpression)
                    || containsEmbeddedColumn(comparisonExpression.SecondExpression);
        }
        return false;
    }

    private boolean containsEmbeddedColumn(ScalarExpression expression) {
        if (expression instanceof ColumnReferenceExpression) {
            return false;
        }
        return containsColumnReference(expression);
    }

    private boolean containsColumnReference(ScalarExpression expression) {
        if (expression instanceof ColumnReferenceExpression) {
            return true;
        }
        if (expression instanceof BinaryExpression binaryExpression) {
            return containsColumnReference(binaryExpression.FirstExpression)
                    || containsColumnReference(binaryExpression.SecondExpression);
        }
        if (expression instanceof FunctionCall functionCall && functionCall.Parameters != null) {
            return functionCall.Parameters.stream().anyMatch(this::containsColumnReference);
        }
        if (expression instanceof ParenthesisExpression parenthesisExpression) {
            return containsColumnReference(parenthesisExpression.Expression);
        }
        return false;
    }
}
