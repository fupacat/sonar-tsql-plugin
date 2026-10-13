package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.TSqlScript;

public class S122Check extends TSqlCheck {

    public static final String RULE_KEY = "S122";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(TSqlScript node) {
        TextRuleSupport.reportIssues(this, TextRuleSupport.sourceOf(node),
                TextRuleSupport::isMultistatementLine,
                "Multiple statements should not appear on a single line.");
        super.visit(node);
    }
}
