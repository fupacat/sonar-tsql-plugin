package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.TSqlScript;

import java.util.List;

public class S1151Check extends TSqlCheck {

    public static final String RULE_KEY = "S1151";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(TSqlScript node) {
        List<String> lines = TextRuleSupport.lines(TextRuleSupport.sourceOf(node));
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).toLowerCase().contains("when ")) {
                int blockLength = 0;
                for (int j = i; j < lines.size() && j < i + 20; j++) {
                    if (lines.get(j).toLowerCase().contains("then") || lines.get(j).toLowerCase().contains("else")) {
                        break;
                    }
                    blockLength++;
                }
                if (blockLength > 10) {
                    reportIssueAtLine(i + 1, "WHEN blocks should not be too long.");
                }
            }
        }
        super.visit(node);
    }
}
