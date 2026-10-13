package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.IfStatement;

public class NestedIfMergeCheck extends TSqlCheck {
    public static final String RULE_KEY = "S1066";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(IfStatement node) {
        if (node.ThenStatement instanceof io.github.fupacat.ast.BeginEndBlockStatement block
                && block.StatementList != null && block.StatementList.Statements != null
                && block.StatementList.Statements.size() == 1
                && block.StatementList.Statements.get(0) instanceof IfStatement innerIf) {
            reportIssue(node, "Mergeable nested IF statements should be combined.");
        }
        super.visit(node);
    }
}
