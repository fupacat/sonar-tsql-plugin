package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.TSqlScript;

import java.util.List;

public class S103Check extends TSqlCheck {

    public static final String RULE_KEY = "S103";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(TSqlScript node) {
        String source = TextRuleSupport.sourceOf(node);
        List<String> lines = TextRuleSupport.lines(source);
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).length() > 120) {
                reportIssueAtLine(i + 1, "Lines should not be too long.");
            }
        }
        super.visit(node);
    }
}
