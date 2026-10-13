package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.GoToStatement;

public class GotoStatementCheck extends TSqlCheck {
    public static final String RULE_KEY = "S907";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(GoToStatement node) {
        reportIssue(node, "GOTO statements should not be used.");
        super.visit(node);
    }
}
