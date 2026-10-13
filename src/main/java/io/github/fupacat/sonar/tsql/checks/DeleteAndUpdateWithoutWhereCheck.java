package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.DeleteSpecification;
import io.github.fupacat.ast.TSqlConcreteFragmentVisitor;
import io.github.fupacat.ast.TSqlScript;
import io.github.fupacat.ast.UpdateSpecification;

/**
 * Rule: DELETE and UPDATE statements should include WHERE clauses.
 * Missing WHERE clauses can affect every row in the target table.
 */
public class DeleteAndUpdateWithoutWhereCheck extends TSqlCheck {

    public static final String RULE_KEY = "S1590";

    // TRACEABILITY_MAPPING: REQ-011
    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    // TRACEABILITY_MAPPING: REQ-011
    @Override
    public void visit(TSqlScript node) {
        node.acceptChildren(new TSqlConcreteFragmentVisitor() {
            @Override
            public void visit(DeleteSpecification deleteNode) {
                if (deleteNode.WhereClause == null) {
                    reportIssue(deleteNode, "DELETE statements should contain a WHERE clause to avoid deleting all rows.");
                }
                super.visit(deleteNode);
            }

            @Override
            public void visit(UpdateSpecification updateNode) {
                if (updateNode.WhereClause == null) {
                    reportIssue(updateNode, "UPDATE statements should contain a WHERE clause to avoid updating all rows.");
                }
                super.visit(updateNode);
            }
        });
    }
}

