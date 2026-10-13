package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.IfStatement;
import io.github.fupacat.ast.SearchedCaseExpression;
import io.github.fupacat.ast.SimpleCaseExpression;
import io.github.fupacat.ast.SearchedWhenClause;
import io.github.fupacat.ast.SimpleWhenClause;

public class IdenticalBranchCheck extends TSqlCheck {
    public static final String RULE_KEY_S1871 = "S1871";
    public static final String RULE_KEY_S3923 = "S3923";

    @Override
    public String getRuleKey() {
        return RULE_KEY_S1871;
    }

    @Override
    public void visit(IfStatement node) {
        if (node.ThenStatement != null && node.ElseStatement != null
                && ConditionalLogicUtils.structurallySame(node.ThenStatement, node.ElseStatement)) {
            reportIssue(node, "Conditional branches should not have identical implementations.");
        }
        super.visit(node);
    }

    @Override
    public void visit(SimpleCaseExpression node) {
        if (node.WhenClauses != null) {
            for (int i = 0; i < node.WhenClauses.size(); i++) {
                for (int j = i + 1; j < node.WhenClauses.size(); j++) {
                    if (node.WhenClauses.get(i).ThenExpression != null && node.WhenClauses.get(j).ThenExpression != null
                            && ConditionalLogicUtils.structurallySame(node.WhenClauses.get(i).ThenExpression,
                                    node.WhenClauses.get(j).ThenExpression)) {
                        reportIssue(node, "CASE branches should not have identical results.");
                        return;
                    }
                }
            }
        }
        super.visit(node);
    }

    @Override
    public void visit(SearchedCaseExpression node) {
        if (node.WhenClauses != null) {
            for (int i = 0; i < node.WhenClauses.size(); i++) {
                for (int j = i + 1; j < node.WhenClauses.size(); j++) {
                    if (node.WhenClauses.get(i).ThenExpression != null && node.WhenClauses.get(j).ThenExpression != null
                            && ConditionalLogicUtils.structurallySame(node.WhenClauses.get(i).ThenExpression,
                                    node.WhenClauses.get(j).ThenExpression)) {
                        reportIssue(node, "CASE branches should not have identical results.");
                        return;
                    }
                }
            }
        }
        super.visit(node);
    }
}
