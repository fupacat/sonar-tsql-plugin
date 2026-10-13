package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.BeginEndBlockStatement;
import io.github.fupacat.ast.BooleanComparisonExpression;
import io.github.fupacat.ast.BooleanExpression;
import io.github.fupacat.ast.BreakStatement;
import io.github.fupacat.ast.ContinueStatement;
import io.github.fupacat.ast.IfStatement;
import io.github.fupacat.ast.IntegerLiteral;
import io.github.fupacat.ast.NumericLiteral;
import io.github.fupacat.ast.ReturnStatement;
import io.github.fupacat.ast.StatementList;
import io.github.fupacat.ast.ThrowStatement;
import io.github.fupacat.ast.TSqlStatement;

import java.util.List;

/**
 * Rule: Code should not be reachable after unconditional terminators.
 */
public class UnreachableCodeCheck extends TSqlCheck {

    public static final String RULE_KEY = "S1763";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(StatementList node) {
        List<TSqlStatement> statements = node.Statements;
        if (statements != null) {
            for (int i = 0; i < statements.size() - 1; i++) {
                TSqlStatement current = statements.get(i);
                if (isUnconditionalTerminator(current) || hasAlwaysTrueTerminatingThenBranch(current)) {
                    for (int j = i + 1; j < statements.size(); j++) {
                        reportIssue(statements.get(j), "Remove this unreachable statement.");
                    }
                    break;
                }
            }
        }
        super.visit(node);
    }

    private static boolean hasAlwaysTrueTerminatingThenBranch(TSqlStatement statement) {
        if (!(statement instanceof IfStatement ifStatement)) {
            return false;
        }
        if (!isConstantTrue(ifStatement.Predicate)) {
            return false;
        }
        return isUnconditionalTerminator(ifStatement.ThenStatement)
                || (ifStatement.ThenStatement instanceof BeginEndBlockStatement beginEnd
                        && isStatementListTerminating(beginEnd.StatementList));
    }

    private static boolean isStatementListTerminating(StatementList statementList) {
        if (statementList == null || statementList.Statements == null || statementList.Statements.isEmpty()) {
            return false;
        }
        TSqlStatement last = statementList.Statements.get(statementList.Statements.size() - 1);
        return isUnconditionalTerminator(last) || (last instanceof BeginEndBlockStatement beginEnd
                && isStatementListTerminating(beginEnd.StatementList));
    }

    private static boolean isUnconditionalTerminator(TSqlStatement statement) {
        if (statement == null) {
            return false;
        }
        if (statement instanceof ReturnStatement
                || statement instanceof BreakStatement
                || statement instanceof ContinueStatement
                || statement instanceof ThrowStatement) {
            return true;
        }
        if (statement instanceof BeginEndBlockStatement beginEnd) {
            return isStatementListTerminating(beginEnd.StatementList);
        }
        return false;
    }

    private static boolean isConstantTrue(BooleanExpression expression) {
        if (!(expression instanceof BooleanComparisonExpression comparison)) {
            return false;
        }

        Integer left = parseInteger(comparison.FirstExpression);
        Integer right = parseInteger(comparison.SecondExpression);
        if (left == null || right == null) {
            return false;
        }

        int comparisonResult = Integer.compare(left, right);
        switch (comparison.ComparisonType) {
            case Equals -> { return comparisonResult == 0; }
            case NotEqualToBrackets, NotEqualToExclamation -> { return comparisonResult != 0; }
            case GreaterThan -> { return comparisonResult > 0; }
            case GreaterThanOrEqualTo -> { return comparisonResult >= 0; }
            case LessThan -> { return comparisonResult < 0; }
            case LessThanOrEqualTo -> { return comparisonResult <= 0; }
            case NotGreaterThan -> { return comparisonResult <= 0; }
            case NotLessThan -> { return comparisonResult >= 0; }
            default -> { return false; }
        }
    }

    private static Integer parseInteger(io.github.fupacat.ast.ScalarExpression expression) {
        if (expression instanceof IntegerLiteral literal && literal.Value != null) {
            try {
                return Integer.parseInt(literal.Value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (expression instanceof NumericLiteral literal && literal.Value != null) {
            try {
                return Integer.parseInt(literal.Value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
