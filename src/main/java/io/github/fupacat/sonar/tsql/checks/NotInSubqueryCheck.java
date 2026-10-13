package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.InPredicate;

/**
 * Rule: Subqueries should not be used in NOT IN conditions.
 * NULL values in subqueries make the result three-valued logic and can produce surprising behavior.
 */
public class NotInSubqueryCheck extends TSqlCheck {

    public static final String RULE_KEY = "S3641";

    // TRACEABILITY_MAPPING: REQ-012
    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    // TRACEABILITY_MAPPING: REQ-012
    @Override
    public void visit(InPredicate node) {
        if (node.NotDefined && node.Subquery != null) {
            reportIssue(node, "Avoid NOT IN with subqueries because NULL values make the predicate three-valued.");
        }
        super.visit(node);
    }
}
