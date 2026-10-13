package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.TSqlScript;

import java.util.List;

public class S3972Check extends TSqlCheck {

    public static final String RULE_KEY = "S3972";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(TSqlScript node) {
        List<String> lines = TextRuleSupport.lines(TextRuleSupport.sourceOf(node));
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.toLowerCase().contains("if ") && line.toLowerCase().contains("begin")) {
                reportIssueAtLine(i + 1, "Conditional statements should be formatted on separate lines.");
            }
        }
        super.visit(node);
    }
}
