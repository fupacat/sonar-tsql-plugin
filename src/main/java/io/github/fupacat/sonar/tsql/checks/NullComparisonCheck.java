package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.BooleanComparisonExpression;
import io.github.fupacat.ast.BooleanComparisonType;
import io.github.fupacat.ast.NullLiteral;

/**
 * Rule: NULL should not be compared directly with = or <>.
 */
public class NullComparisonCheck extends TSqlCheck {

    public static final String RULE_KEY = "S2527";

    // TRACEABILITY_MAPPING: REQ-013
    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    // TRACEABILITY_MAPPING: REQ-013
    @Override
    public void visit(BooleanComparisonExpression node) {
        boolean nullComparison = (node.FirstExpression instanceof NullLiteral || node.SecondExpression instanceof NullLiteral)
                && (node.ComparisonType == BooleanComparisonType.Equals
                || node.ComparisonType == BooleanComparisonType.NotEqualToBrackets
                || node.ComparisonType == BooleanComparisonType.NotEqualToExclamation);

        if (nullComparison) {
            reportIssue(node, "NULL should not be compared directly with '=' or '<>'; use IS NULL or IS NOT NULL instead.");
        }
        super.visit(node);
    }
}
