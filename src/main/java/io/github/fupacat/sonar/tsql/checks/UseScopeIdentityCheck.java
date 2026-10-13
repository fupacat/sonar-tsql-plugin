package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.GlobalVariableExpression;

/**
 * Rule: Prefer SCOPE_IDENTITY() over @@IDENTITY.
 */
public class UseScopeIdentityCheck extends TSqlCheck {

    public static final String RULE_KEY = "S4108";

    // TRACEABILITY_MAPPING: REQ-017
    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    // TRACEABILITY_MAPPING: REQ-017
    @Override
    public void visit(GlobalVariableExpression node) {
        if (node != null && "@@IDENTITY".equalsIgnoreCase(node.Name)) {
            reportIssue(node, "Use SCOPE_IDENTITY() instead of @@IDENTITY to avoid returning the last identity value from a different scope.");
        }
        super.visit(node);
    }
}
