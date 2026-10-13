package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.TSqlScript;

import java.util.List;

public class S125Check extends TSqlCheck {

    public static final String RULE_KEY = "S125";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(TSqlScript node) {
        List<String> lines = TextRuleSupport.lines(TextRuleSupport.sourceOf(node));
        for (int i = 0; i < lines.size(); i++) {
            if (TextRuleSupport.looksLikeCommentedCode(lines.get(i))) {
                reportIssueAtLine(i + 1, "Sections of T-SQL code should not be commented out.");
            }
        }
        super.visit(node);
    }
}
