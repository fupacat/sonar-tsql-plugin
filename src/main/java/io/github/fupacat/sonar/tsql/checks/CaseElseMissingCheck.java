package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.IfStatement;
import io.github.fupacat.ast.SearchedCaseExpression;
import io.github.fupacat.ast.SimpleCaseExpression;

public class CaseElseMissingCheck extends TSqlCheck {
    public static final String RULE_KEY_S131 = "S131";
    public static final String RULE_KEY_S126 = "S126";

    @Override
    public String getRuleKey() {
        return RULE_KEY_S131;
    }

    @Override
    public void visit(SimpleCaseExpression node) {
        if (node.ElseExpression == null) {
            reportIssue(node, "CASE expressions should end with an ELSE clause.");
        }
        super.visit(node);
    }

    @Override
    public void visit(SearchedCaseExpression node) {
        if (node.ElseExpression == null) {
            reportIssue(node, "CASE expressions should end with an ELSE clause.");
        }
        super.visit(node);
    }

    @Override
    public void visit(IfStatement node) {
        IfStatement current = node;
        while (current != null && current.ElseStatement != null) {
            if (current.ElseStatement instanceof IfStatement nestedIf) {
                current = nestedIf;
                continue;
            }
            if (current.ElseStatement != null) {
                return;
            }
            break;
        }
        if (node.ElseStatement == null) {
            reportIssue(node, "IF/ELSE IF chains should end with ELSE.");
        }
        super.visit(node);
    }
}
