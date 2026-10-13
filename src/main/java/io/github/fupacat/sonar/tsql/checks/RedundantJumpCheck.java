package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.CreateProcedureStatement;
import io.github.fupacat.ast.CreateOrAlterProcedureStatement;
import io.github.fupacat.ast.ProcedureStatementBodyBase;
import io.github.fupacat.ast.ReturnStatement;
import io.github.fupacat.ast.StatementList;
import io.github.fupacat.ast.TSqlStatement;

import java.util.List;

/**
 * Rule: Redundant RETURN statements at the end of procedures should be removed.
 */
public class RedundantJumpCheck extends TSqlCheck {

    public static final String RULE_KEY = "S3626";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(io.github.fupacat.ast.CreateProcedureStatement node) {
        checkProcedureBody(node);
        super.visit(node);
    }

    @Override
    public void visit(io.github.fupacat.ast.CreateOrAlterProcedureStatement node) {
        checkProcedureBody(node);
        super.visit(node);
    }

    @Override
    public void visit(io.github.fupacat.ast.AlterProcedureStatement node) {
        checkProcedureBody(node);
        super.visit(node);
    }

    private void checkProcedureBody(ProcedureStatementBodyBase node) {
        StatementList statementList = node.StatementList;
        if (statementList != null && statementList.Statements != null) {
            List<TSqlStatement> statements = statementList.Statements;
            if (!statements.isEmpty() && statements.get(statements.size() - 1) instanceof ReturnStatement) {
                reportIssue(statements.get(statements.size() - 1), "Remove this redundant RETURN at the end of the procedure.");
            }
        }
    }
}
