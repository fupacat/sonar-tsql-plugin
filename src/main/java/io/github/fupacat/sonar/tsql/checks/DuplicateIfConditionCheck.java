package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.IfStatement;
import io.github.fupacat.ast.SearchedCaseExpression;
import io.github.fupacat.ast.SearchedWhenClause;

public class DuplicateIfConditionCheck extends TSqlCheck {
    public static final String RULE_KEY = "S1862";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(IfStatement node) {
        if (node.ElseStatement instanceof IfStatement elseIf && node.Predicate != null && elseIf.Predicate != null
                && ConditionalLogicUtils.structurallySame(node.Predicate, elseIf.Predicate)) {
            reportIssue(node, "Related IF/ELSE IF statements should not use duplicate conditions.");
        }
        super.visit(node);
    }

    @Override
    public void visit(SearchedCaseExpression node) {
        if (node.WhenClauses != null) {
            for (int i = 0; i < node.WhenClauses.size(); i++) {
                for (int j = i + 1; j < node.WhenClauses.size(); j++) {
                    if (node.WhenClauses.get(i).WhenExpression != null && node.WhenClauses.get(j).WhenExpression != null
                            && ConditionalLogicUtils.structurallySame(node.WhenClauses.get(i).WhenExpression,
                                    node.WhenClauses.get(j).WhenExpression)) {
                        reportIssue(node, "CASE WHEN conditions should not be duplicated.");
                        break;
                    }
                }
            }
        }
        super.visit(node);
    }
}
