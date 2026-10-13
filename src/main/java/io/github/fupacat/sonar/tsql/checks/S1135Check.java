package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.TSqlScript;

import java.util.List;

public class S1135Check extends TSqlCheck {

    public static final String RULE_KEY = "S1135";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(TSqlScript node) {
        TextRuleSupport.reportIssues(this, TextRuleSupport.sourceOf(node),
                line -> TextRuleSupport.containsAnyIgnoreCase(line, "todo"),
                "TODO tags should be handled.");
        super.visit(node);
    }
}
