package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.BooleanComparisonExpression;
import io.github.fupacat.ast.BooleanComparisonType;
import io.github.fupacat.ast.BooleanExpression;
import io.github.fupacat.ast.IntegerLiteral;
import io.github.fupacat.ast.NumericLiteral;
import io.github.fupacat.ast.WhileStatement;

/**
 * Rule: Loops with at most one iteration should be refactored.
 */
public class SingleIterationLoopCheck extends TSqlCheck {

    public static final String RULE_KEY = "S1751";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(WhileStatement node) {
        if (isConstantBoolean(node.Predicate)) {
            reportIssue(node, "Refactor this loop because it has at most one iteration.");
        }
        super.visit(node);
    }

    private static boolean isConstantBoolean(BooleanExpression expression) {
        if (!(expression instanceof BooleanComparisonExpression comparison)) {
            return false;
        }

        Integer left = parseInteger(comparison.FirstExpression);
        Integer right = parseInteger(comparison.SecondExpression);
        if (left == null || right == null) {
            return false;
        }

        int comparisonResult = Integer.compare(left, right);
        if (comparison.ComparisonType == BooleanComparisonType.Equals) {
            return true;
        }
        if (comparison.ComparisonType == BooleanComparisonType.NotEqualToBrackets
                || comparison.ComparisonType == BooleanComparisonType.NotEqualToExclamation) {
            return comparisonResult != 0;
        }
        if (comparison.ComparisonType == BooleanComparisonType.GreaterThanOrEqualTo) {
            return comparisonResult >= 0;
        }
        if (comparison.ComparisonType == BooleanComparisonType.LessThanOrEqualTo) {
            return comparisonResult <= 0;
        }
        if (comparison.ComparisonType == BooleanComparisonType.NotGreaterThan) {
            return comparisonResult <= 0;
        }
        if (comparison.ComparisonType == BooleanComparisonType.NotLessThan) {
            return comparisonResult >= 0;
        }
        return false;
    }

    private static Integer parseInteger(io.github.fupacat.ast.ScalarExpression expression) {
        if (expression instanceof IntegerLiteral literal && literal.Value != null) {
            try {
                return Integer.parseInt(literal.Value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (expression instanceof NumericLiteral literal && literal.Value != null) {
            try {
                return Integer.parseInt(literal.Value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
