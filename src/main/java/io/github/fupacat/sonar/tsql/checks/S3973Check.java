package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.TSqlScript;

import java.util.List;

public class S3973Check extends TSqlCheck {

    public static final String RULE_KEY = "S3973";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(TSqlScript node) {
        List<String> lines = TextRuleSupport.lines(TextRuleSupport.sourceOf(node));
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String trimmed = line.trim();
            if ("BEGIN".equalsIgnoreCase(trimmed) || "END".equalsIgnoreCase(trimmed)) {
                int indent = TextRuleSupport.leadingSpaces(line);
                if (indent % 2 != 0) {
                    reportIssueAtLine(i + 1, "Blocks should be indented consistently.");
                }
            }
        }
        super.visit(node);
    }
}
