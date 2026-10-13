package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.BooleanComparisonExpression;
import io.github.fupacat.ast.BooleanExpression;
import io.github.fupacat.ast.BooleanBinaryExpression;
import io.github.fupacat.ast.ColumnReferenceExpression;
import io.github.fupacat.ast.FunctionCall;
import io.github.fupacat.ast.ScalarExpression;
import io.github.fupacat.ast.WhereClause;

/**
 * Rule: Extract deterministic function calls from WHERE predicates when they wrap column references.
 */
public class DeterministicFunctionInWhereCheck extends TSqlCheck {

    public static final String RULE_KEY = "SR0015";

    // TRACEABILITY_MAPPING: REQ-023
    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    // TRACEABILITY_MAPPING: REQ-023
    @Override
    public void visit(WhereClause node) {
        if (node != null && node.SearchCondition != null && containsFunctionCallOnColumn(node.SearchCondition)) {
            reportIssue(node, "Extract deterministic function calls from WHERE predicates to avoid preventing index seeks on filtered columns.");
        }
        super.visit(node);
    }

    private boolean containsFunctionCallOnColumn(BooleanExpression expression) {
        if (expression instanceof BooleanBinaryExpression booleanBinaryExpression) {
            return containsFunctionCallOnColumn(booleanBinaryExpression.FirstExpression)
                    || containsFunctionCallOnColumn(booleanBinaryExpression.SecondExpression);
        }
        if (expression instanceof BooleanComparisonExpression comparisonExpression) {
            return containsFunctionCallOnScalar(comparisonExpression.FirstExpression)
                    || containsFunctionCallOnScalar(comparisonExpression.SecondExpression);
        }
        return false;
    }

    private boolean containsFunctionCallOnScalar(ScalarExpression expression) {
        if (expression instanceof FunctionCall functionCall && functionCall.Parameters != null) {
            return functionCall.Parameters.stream().anyMatch(parameter ->
                    parameter instanceof ColumnReferenceExpression || containsFunctionCallOnScalar(parameter));
        }
        return false;
    }
}
