package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.TSqlScript;

import java.util.List;

public class NoSonarCheck extends TSqlCheck {

    public static final String RULE_KEY = "NoSonar";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(TSqlScript node) {
        TextRuleSupport.reportIssues(this, TextRuleSupport.sourceOf(node),
                line -> TextRuleSupport.containsAnyIgnoreCase(line, "nosonar"),
                "NOSONAR comments should be reviewed and removed when not needed.");
        super.visit(node);
    }
}
