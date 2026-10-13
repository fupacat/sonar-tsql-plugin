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
        List<String> lines = TextRuleSupport.lines(TextRuleSupport.sourceOf(node));
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).toLowerCase().contains("todo")) {
                reportIssueAtLine(i + 1, "TODO tags should be handled.");
            }
        }
        super.visit(node);
    }
}
