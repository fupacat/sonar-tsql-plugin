package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.TSqlScript;

public class S125Check extends TSqlCheck {

    public static final String RULE_KEY = "S125";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(TSqlScript node) {
        TextRuleSupport.reportIssues(this, TextRuleSupport.sourceOf(node),
                TextRuleSupport::looksLikeCommentedCode,
                "Sections of T-SQL code should not be commented out.");
        super.visit(node);
    }
}
