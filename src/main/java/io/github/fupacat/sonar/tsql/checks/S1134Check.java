package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.TSqlScript;

public class S1134Check extends TSqlCheck {

    public static final String RULE_KEY = "S1134";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(TSqlScript node) {
        TextRuleSupport.reportKeywordIssues(this, TextRuleSupport.sourceOf(node), "fixme",
                "FIXME tags should be handled.");
        super.visit(node);
    }
}
