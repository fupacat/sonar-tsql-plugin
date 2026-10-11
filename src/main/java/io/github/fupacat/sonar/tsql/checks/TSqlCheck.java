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
    public abstract String getRuleKey();

    /**
     * Executes the check against the given parsed AST fragment.
     *
     * @param root root AST node
     * @param context analysis context
     */
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
    public TSqlFragment getRoot() {
        return root;
    }

    /**
     * Reports an issue on a specific AST fragment.
     *
     * @param fragment node causing the violation
     * @param message descriptive issue message
     */
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
    protected void reportIssue(int line, String message) {
        if (context != null) {
            context.reportIssue(this, line, message);
        }
    }
}
