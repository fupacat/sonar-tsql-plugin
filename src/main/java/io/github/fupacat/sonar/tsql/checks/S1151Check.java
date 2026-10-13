package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.TSqlScript;

import java.util.List;
import java.util.Locale;

public class S1151Check extends TSqlCheck {

    public static final String RULE_KEY = "S1151";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(TSqlScript node) {
        List<String> lines = TextRuleSupport.lines(TextRuleSupport.sourceOf(node));
        boolean inCase = false;
        int caseStart = -1;
        int whenCount = 0;
        for (int i = 0; i < lines.size(); i++) {
            String upper = lines.get(i).toUpperCase(Locale.ROOT);
            if (!inCase && upper.contains("CASE")) {
                inCase = true;
                caseStart = i;
                whenCount = 0;
                continue;
            }
            if (inCase) {
                if (upper.contains("WHEN")) {
                    whenCount++;
                }
                if (upper.contains("END") || upper.contains("ELSE")) {
                    if (whenCount > 10) {
                        reportIssueAtLine(caseStart + 1, "WHEN blocks should not be too long.");
                    }
                    inCase = false;
                    caseStart = -1;
                    whenCount = 0;
                }
            }
        }
        super.visit(node);
    }
}
