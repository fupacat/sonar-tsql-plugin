package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.BeginEndBlockStatement;
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
    public void visit(CreateProcedureStatement node) {
        checkProcedureBody(node);
        super.visit(node);
    }

    @Override
    public void visit(CreateOrAlterProcedureStatement node) {
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
            TSqlStatement lastStatement = statements.isEmpty() ? null : statements.get(statements.size() - 1);
            if (lastStatement instanceof BeginEndBlockStatement beginEndBlock) {
                lastStatement = getLastConcreteStatement(beginEndBlock.StatementList);
            }
            if (lastStatement instanceof ReturnStatement returnStatement) {
                reportIssue(returnStatement, "Remove this redundant RETURN at the end of the procedure.");
            }
        }
    }

    private static TSqlStatement getLastConcreteStatement(StatementList statementList) {
        if (statementList == null || statementList.Statements == null || statementList.Statements.isEmpty()) {
            return null;
        }
        TSqlStatement last = statementList.Statements.get(statementList.Statements.size() - 1);
        if (last instanceof BeginEndBlockStatement beginEndBlock) {
            return getLastConcreteStatement(beginEndBlock.StatementList);
        }
        return last;
    }
}
