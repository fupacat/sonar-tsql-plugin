package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.TSqlScript;

import java.util.List;

public class S122Check extends TSqlCheck {

    public static final String RULE_KEY = "S122";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(TSqlScript node) {
        List<String> lines = TextRuleSupport.lines(TextRuleSupport.sourceOf(node));
        for (int i = 0; i < lines.size(); i++) {
            if (TextRuleSupport.isMultistatementLine(lines.get(i))) {
                reportIssueAtLine(i + 1, "Multiple statements should not appear on a single line.");
            }
        }
        super.visit(node);
    }
}
