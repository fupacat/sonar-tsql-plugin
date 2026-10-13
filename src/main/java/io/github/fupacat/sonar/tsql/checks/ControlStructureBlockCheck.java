package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.IfStatement;
import io.github.fupacat.ast.WhileStatement;

public class ControlStructureBlockCheck extends TSqlCheck {
    public static final String RULE_KEY = "S121";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(IfStatement node) {
        if (node.ThenStatement != null && !(node.ThenStatement instanceof io.github.fupacat.ast.BeginEndBlockStatement)) {
            reportIssue(node, "Control structures should use BEGIN...END blocks for their bodies.");
        }
        if (node.ElseStatement != null && !(node.ElseStatement instanceof io.github.fupacat.ast.BeginEndBlockStatement)
                && !(node.ElseStatement instanceof IfStatement)) {
            reportIssue(node, "Control structures should use BEGIN...END blocks for their bodies.");
        }
        super.visit(node);
    }

    @Override
    public void visit(WhileStatement node) {
        if (node.Statement != null && !(node.Statement instanceof io.github.fupacat.ast.BeginEndBlockStatement)) {
            reportIssue(node, "Control structures should use BEGIN...END blocks for their bodies.");
        }
        super.visit(node);
    }
}
