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
        String source = TextRuleSupport.sourceOf(node);
        if (!TextRuleSupport.hasHeader(source)) {
            reportIssueAtLine(1, "Track file copyright and license headers.");
        }
        super.visit(node);
    }
}
