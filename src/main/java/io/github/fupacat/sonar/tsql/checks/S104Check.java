package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.TSqlScript;

public class S104Check extends TSqlCheck {

    public static final String RULE_KEY = "S104";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(TSqlScript node) {
        String source = TextRuleSupport.sourceOf(node);
        int lineCount = TextRuleSupport.lines(source).size();
        if (lineCount > 1000) {
            reportIssueAtLine(1, "Files should not have too many lines.");
        }
        super.visit(node);
    }
}
