package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.ColumnReferenceExpression;
import io.github.fupacat.ast.MultiPartIdentifier;

/**
 * Rule: Column references should not use more than two parts.
 */
public class ColumnReferenceDepthCheck extends TSqlCheck {

    public static final String RULE_KEY = "S4149";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(ColumnReferenceExpression node) {
        if (hasTooManyIdentifierParts(node.MultiPartIdentifier)) {
            reportIssue(node, "Column references should not use more than two parts (table.column).");
        }
        super.visit(node);
    }

    private boolean hasTooManyIdentifierParts(MultiPartIdentifier identifier) {
        return identifier != null && identifier.Identifiers != null && identifier.Identifiers.size() > 2;
    }
}
