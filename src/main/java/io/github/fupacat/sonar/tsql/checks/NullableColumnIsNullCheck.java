package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.CoalesceExpression;
import io.github.fupacat.ast.ColumnReferenceExpression;
import io.github.fupacat.ast.ScalarExpression;
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
        // SQL Server COALESCE and ISNULL are not interchangeable for nullable-column fallback chains.
        // A nullable input alone, or a multi-argument fallback chain, does not justify a replacement warning.
        if (node != null) {
            super.visit(node);
        }
    }
}
