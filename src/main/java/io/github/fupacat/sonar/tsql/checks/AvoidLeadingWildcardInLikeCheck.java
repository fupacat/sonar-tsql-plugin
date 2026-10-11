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
            String val = stringLiteral.Value;
            if (val != null && (val.startsWith("%") || val.startsWith("_"))) {
                reportIssue(node.SecondExpression, 
                        "Avoid leading wildcards ('%' or '_') in LIKE conditions; they preclude index seeks.");
            }
        }
        super.visit(node);
    }
}
