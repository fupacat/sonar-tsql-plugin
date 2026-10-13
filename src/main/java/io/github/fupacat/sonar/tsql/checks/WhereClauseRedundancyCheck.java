package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.BooleanBinaryExpression;
import io.github.fupacat.ast.BooleanBinaryExpressionType;
import io.github.fupacat.ast.WhereClause;

public class WhereClauseRedundancyCheck extends TSqlCheck {
    public static final String RULE_KEY_S3633 = "S3633";
    public static final String RULE_KEY_S3650 = "S3650";

    @Override
    public String getRuleKey() {
        return RULE_KEY_S3633;
    }

    @Override
    public void visit(WhereClause node) {
        if (node.SearchCondition instanceof BooleanBinaryExpression binary && binary.BinaryExpressionType == BooleanBinaryExpressionType.And) {
            if (ConditionalLogicUtils.structurallySame(binary.FirstExpression, binary.SecondExpression)) {
                reportIssue(node, "WHERE clauses should not contain redundant or contradictory conditions.");
            }
        }
        super.visit(node);
    }
}
