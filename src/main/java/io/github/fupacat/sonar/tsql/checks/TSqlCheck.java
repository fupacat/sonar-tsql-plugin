package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.TSqlConcreteFragmentVisitor;
import io.github.fupacat.ast.TSqlFragment;

/**
 * Base class for all T-SQL static analysis rules implemented as AST visitors.
 * Extends {@link TSqlConcreteFragmentVisitor} to ensure full traversal through all AST nodes.
 */
public abstract class TSqlCheck extends TSqlConcreteFragmentVisitor {

    private TSqlCheckContext context;
    private TSqlFragment root;

    /**
     * @return unique rule key registered in SonarQube rules repository
     */
    // TRACEABILITY_MAPPING: REQ-007, REQ-008, REQ-009, REQ-010
    public abstract String getRuleKey();

    /**
     * Executes the check against the given parsed AST fragment.
     *
     * @param root root AST node
     * @param context analysis context
     */
    // TRACEABILITY_MAPPING: REQ-007, REQ-008, REQ-009, REQ-010
    public void scan(TSqlFragment root, TSqlCheckContext context) {
        this.context = context;
        this.root = root;
        try {
            if (root != null) {
                root.accept(this);
            }
        } finally {
            this.context = null;
            this.root = null;
        }
    }

    /**
     * @return the root AST fragment of the current scan, if available
     */
    // TRACEABILITY_MAPPING: REQ-007, REQ-008, REQ-009, REQ-010
    public TSqlFragment getRoot() {
        return root;
    }

    /**
     * @return the raw source text associated with the current root fragment, if any
     */
    protected String getSourceText() {
        if (root != null && root.Value != null) {
            return root.Value;
        }
        return "";
    }

    /**
     * Reports an issue on a specific AST fragment.
     *
     * @param fragment node causing the violation
     * @param message descriptive issue message
     */
    // TRACEABILITY_MAPPING: REQ-007, REQ-008, REQ-009, REQ-010
    protected void reportIssue(TSqlFragment fragment, String message) {
        if (context != null) {
            context.reportIssue(this, fragment, message);
        }
    }

    /**
     * Reports an issue at a given line number.
     *
     * @param line 1-indexed line number
     * @param message descriptive issue message
     */
    // TRACEABILITY_MAPPING: REQ-007, REQ-008, REQ-009, REQ-010
    protected void reportIssueAtLine(int line, String message) {
        if (context != null) {
            context.reportIssueAtLine(this, line, message);
        }
    }
}
