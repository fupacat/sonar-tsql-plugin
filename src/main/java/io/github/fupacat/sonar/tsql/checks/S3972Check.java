package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.TSqlScript;

import java.util.Locale;

public class S3972Check extends TSqlCheck {

    public static final String RULE_KEY = "S3972";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(TSqlScript node) {
        TextRuleSupport.reportIssues(this, TextRuleSupport.sourceOf(node),
                line -> line.toLowerCase(Locale.ROOT).contains("if ") && line.toLowerCase(Locale.ROOT).contains("begin"),
                "Conditional statements should be formatted on separate lines.");
        super.visit(node);
    }
}
