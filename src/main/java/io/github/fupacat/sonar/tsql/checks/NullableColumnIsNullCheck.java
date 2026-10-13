package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.CoalesceExpression;
import io.github.fupacat.ast.ColumnReferenceExpression;
import io.github.fupacat.sonar.tsql.catalog.ProjectSchemaCatalog;

public class NullableColumnIsNullCheck extends TSqlCheck {

    public static final String RULE_KEY = "SR0007";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(CoalesceExpression node) {
        ProjectSchemaCatalog catalog = getProjectSchema();
        if (node != null && node.Expressions != null && !node.Expressions.isEmpty()
                && node.Expressions.get(0) instanceof ColumnReferenceExpression columnReferenceExpression
                && catalog.isNullable(columnReferenceExpression)) {
            reportIssue(node, "Use ISNULL(column, default_value) on nullable columns in expressions to avoid NULL propagation.");
        }
        super.visit(node);
    }
}
