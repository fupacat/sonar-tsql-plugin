package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.TSqlScript;

import java.util.List;

public class S1451Check extends TSqlCheck {

    public static final String RULE_KEY = "S1451";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(TSqlScript node) {
        List<String> lines = TextRuleSupport.lines(TextRuleSupport.sourceOf(node));
        int checkWindow = Math.min(lines.size(), 20);
        boolean hasHeader = false;
        for (int i = 0; i < checkWindow; i++) {
            String line = lines.get(i).toLowerCase();
            if (line.contains("copyright") || line.contains("license")) {
                hasHeader = true;
                break;
            }
        }
        if (!hasHeader) {
            reportIssueAtLine(1, "Track file copyright and license headers.");
        }
        super.visit(node);
    }
}
