package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.BreakStatement;
import io.github.fupacat.ast.ContinueStatement;
import io.github.fupacat.ast.ReturnStatement;
import io.github.fupacat.ast.StatementList;
import io.github.fupacat.ast.ThrowStatement;
import io.github.fupacat.ast.TSqlStatement;

import java.util.List;

/**
 * Rule: Code should not be reachable after unconditional terminators.
 */
public class UnreachableCodeCheck extends TSqlCheck {

    public static final String RULE_KEY = "S1763";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(StatementList node) {
        List<TSqlStatement> statements = node.Statements;
        if (statements != null) {
            for (int i = 0; i < statements.size() - 1; i++) {
                if (isUnconditionalTerminator(statements.get(i))) {
                    for (int j = i + 1; j < statements.size(); j++) {
                        reportIssue(statements.get(j), "Remove this unreachable statement.");
                    }
                    break;
                }
            }
        }
        super.visit(node);
    }

    private static boolean isUnconditionalTerminator(TSqlStatement statement) {
        return statement instanceof ReturnStatement
                || statement instanceof BreakStatement
                || statement instanceof ContinueStatement
                || statement instanceof ThrowStatement;
    }
}
