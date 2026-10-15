package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.CoalesceExpression;
import io.github.fupacat.ast.ColumnReferenceExpression;
import io.github.fupacat.sonar.tsql.catalog.ProjectSchemaCatalog;

public class NullableColumnIsNullCheck extends TSqlCheck {

    public static final String RULE_KEY = "SR0007";

    // TRACEABILITY_MAPPING: REQ-017
    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    // TRACEABILITY_MAPPING: REQ-017
    @Override
    public void visit(CoalesceExpression node) {
        if (node == null || node.Expressions == null || node.Expressions.size() <= 2) {
            super.visit(node);
            return;
        }

        ProjectSchemaCatalog catalog = getProjectSchema();
        if (!(node.Expressions.get(0) instanceof ColumnReferenceExpression firstColumnReferenceExpression)
                || !catalog.isNullable(firstColumnReferenceExpression)) {
            super.visit(node);
            return;
        }

        reportIssue(node,
                "COALESCE with more than two arguments cannot be rewritten to ISNULL without changing semantics; ISNULL is binary and COALESCE evaluates each argument in order, including type and length precedence.");
        super.visit(node);
    }
}
