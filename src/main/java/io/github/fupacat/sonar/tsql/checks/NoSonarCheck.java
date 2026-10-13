package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.TSqlScript;

import java.util.List;

public class NoSonarCheck extends TSqlCheck {

    public static final String RULE_KEY = "NoSonar";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(TSqlScript node) {
        List<String> lines = TextRuleSupport.lines(TextRuleSupport.sourceOf(node));
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).toLowerCase().contains("nosonar")) {
                reportIssueAtLine(i + 1, "NOSONAR comments should be reviewed and removed when not needed.");
            }
        }
        super.visit(node);
    }
}
