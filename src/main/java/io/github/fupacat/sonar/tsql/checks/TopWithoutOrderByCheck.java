package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.QuerySpecification;

/**
 * Rule: TOP without ORDER BY returns non-deterministic rows.
 */
public class TopWithoutOrderByCheck extends TSqlCheck {

    public static final String RULE_KEY = "S4054";

    // TRACEABILITY_MAPPING: REQ-014
    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    // TRACEABILITY_MAPPING: REQ-014
    @Override
    public void visit(QuerySpecification node) {
        if (node.TopRowFilter != null && node.OrderByClause == null) {
            reportIssue(node, "Queries using TOP without ORDER BY return non-deterministic rows.");
        }
        super.visit(node);
    }
}
