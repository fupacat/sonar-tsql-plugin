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
        List<String> lines = TextRuleSupport.lines(TextRuleSupport.sourceOf(node));
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).toLowerCase().contains("fixme")) {
                reportIssueAtLine(i + 1, "FIXME tags should be handled.");
            }
        }
        super.visit(node);
    }
}
