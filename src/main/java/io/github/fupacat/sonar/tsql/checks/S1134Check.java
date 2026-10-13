package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.TSqlScript;

import java.util.List;

public class S1134Check extends TSqlCheck {

    public static final String RULE_KEY = "S1134";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(TSqlScript node) {
        TextRuleSupport.reportIssues(this, TextRuleSupport.sourceOf(node),
                line -> TextRuleSupport.containsAnyIgnoreCase(line, "fixme"),
                "FIXME tags should be handled.");
        super.visit(node);
    }
}
