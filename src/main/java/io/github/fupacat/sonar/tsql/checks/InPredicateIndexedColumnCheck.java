package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.ColumnReferenceExpression;
import io.github.fupacat.ast.InPredicate;
import io.github.fupacat.sonar.tsql.catalog.ProjectSchemaCatalog;

public class InPredicateIndexedColumnCheck extends TSqlCheck {

    public static final String RULE_KEY = "SR0004";

    // TRACEABILITY_MAPPING: REQ-016
    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    // TRACEABILITY_MAPPING: REQ-016
    @Override
    public void visit(InPredicate node) {
        ProjectSchemaCatalog catalog = getProjectSchema();
        if (node != null && node.Expression instanceof ColumnReferenceExpression expression && !catalog.isIndexed(expression)) {
            reportIssue(node.Expression, "Avoid using columns that are not indexed as test expressions in IN predicates.");
        }
        super.visit(node);
    }
}
