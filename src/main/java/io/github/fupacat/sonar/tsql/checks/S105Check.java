package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.TSqlScript;

public class S105Check extends TSqlCheck {

    public static final String RULE_KEY = "S105";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(TSqlScript node) {
        String source = TextRuleSupport.sourceOf(node);
        for (int i = 0; i < source.length(); i++) {
            if (source.charAt(i) == '\t') {
                reportIssueAtLine(Math.max(1, source.substring(0, i).split("\\R", -1).length),
                        "Tabulation characters should not be used.");
                break;
            }
        }
        super.visit(node);
    }
}
