package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.ExistsPredicate;
import io.github.fupacat.ast.IntegerLiteral;
import io.github.fupacat.ast.QuerySpecification;
import io.github.fupacat.ast.SelectElement;
import io.github.fupacat.ast.SelectScalarExpression;
import io.github.fupacat.ast.SelectStarExpression;

/**
 * Rule: EXISTS subqueries should use SELECT 1 or SELECT *.
 */
public class ExistsSelectOneCheck extends TSqlCheck {

    public static final String RULE_KEY = "S4801";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(ExistsPredicate node) {
        if (node.Subquery != null && node.Subquery.QueryExpression instanceof QuerySpecification specification) {
            if (hasColumnList(specification)) {
                reportIssue(node, "EXISTS subqueries should use SELECT 1 or SELECT * instead of selecting columns.");
            }
        }
        super.visit(node);
    }

    private boolean hasColumnList(QuerySpecification specification) {
        if (specification.SelectElements == null || specification.SelectElements.isEmpty()) {
            return false;
        }
        if (specification.SelectElements.size() != 1) {
            return true;
        }

        SelectElement element = specification.SelectElements.get(0);
        if (element instanceof SelectStarExpression) {
            return false;
        }
        if (element instanceof SelectScalarExpression scalarExpression) {
            return !(scalarExpression.Expression instanceof IntegerLiteral);
        }
        return true;
    }
}
