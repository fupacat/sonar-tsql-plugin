package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.BeginEndBlockStatement;
import io.github.fupacat.ast.IfStatement;
import io.github.fupacat.ast.WhileStatement;

public class ControlStructureBlockCheck extends TSqlCheck {
    public static final String RULE_KEY = "S121";
    private static final String MESSAGE = "Control structures should use BEGIN...END blocks for their bodies.";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(IfStatement node) {
        if (node.ThenStatement != null && !(node.ThenStatement instanceof BeginEndBlockStatement)) {
            reportIssue(node, MESSAGE);
        }
        if (node.ElseStatement != null && !(node.ElseStatement instanceof BeginEndBlockStatement)
                && !(node.ElseStatement instanceof IfStatement)) {
            reportIssue(node, MESSAGE);
        }
        super.visit(node);
    }

    @Override
    public void visit(WhileStatement node) {
        if (node.Statement != null && !(node.Statement instanceof BeginEndBlockStatement)) {
            reportIssue(node, MESSAGE);
        }
        super.visit(node);
    }
}
