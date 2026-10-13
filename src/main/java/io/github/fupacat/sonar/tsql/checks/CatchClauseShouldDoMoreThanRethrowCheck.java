package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.StatementList;
import io.github.fupacat.ast.ThrowStatement;
import io.github.fupacat.ast.TryCatchStatement;

/**
 * Rule: CATCH clauses should do more than simply rethrow.
 */
public class CatchClauseShouldDoMoreThanRethrowCheck extends TSqlCheck {

    public static final String RULE_KEY = "S2737";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(TryCatchStatement node) {
        if (node.CatchStatements != null && node.CatchStatements.Statements != null) {
            boolean onlyRethrow = node.CatchStatements.Statements.size() == 1
                    && node.CatchStatements.Statements.get(0) instanceof ThrowStatement;
            if (onlyRethrow) {
                reportIssue(node, "CATCH blocks should do more than simply rethrow the exception.");
            }
        }
        super.visit(node);
    }
}
