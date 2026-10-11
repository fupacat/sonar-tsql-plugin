package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.LikePredicate;
import io.github.fupacat.ast.StringLiteral;

/**
 * Rule: Avoid leading wildcards in LIKE predicates.
 * Patterns starting with '%' or '_' prevent the query optimizer from using index seeks,
 * resulting in full index or table scans.
 */
public class AvoidLeadingWildcardInLikeCheck extends TSqlCheck {

    public static final String RULE_KEY = "AvoidLeadingWildcardInLike";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(LikePredicate node) {
        if (node.SecondExpression instanceof StringLiteral stringLiteral) {
            String raw = stringLiteral.Value;
            if (raw != null) {
                String pattern = raw;
                if (pattern.startsWith("N'") || pattern.startsWith("n'")) {
                    pattern = pattern.substring(2);
                } else if (pattern.startsWith("'")) {
                    pattern = pattern.substring(1);
                }
                if (pattern.startsWith("%") || pattern.startsWith("_")) {
                    reportIssue(node.SecondExpression, 
                            "Avoid leading wildcards ('%' or '_') in LIKE conditions; they preclude index seeks.");
                }
            }
        }
        super.visit(node);
    }
}
