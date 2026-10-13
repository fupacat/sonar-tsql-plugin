package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.BeginEndBlockStatement;
import io.github.fupacat.ast.BinaryExpression;
import io.github.fupacat.ast.BinaryExpressionType;
import io.github.fupacat.ast.BooleanBinaryExpression;
import io.github.fupacat.ast.BooleanBinaryExpressionType;
import io.github.fupacat.ast.BooleanComparisonExpression;
import io.github.fupacat.ast.BooleanComparisonType;
import io.github.fupacat.ast.BooleanNotExpression;
import io.github.fupacat.ast.ColumnReferenceExpression;
import io.github.fupacat.ast.FunctionCall;
import io.github.fupacat.ast.Identifier;
import io.github.fupacat.ast.IfStatement;
import io.github.fupacat.ast.IntegerLiteral;
import io.github.fupacat.ast.MultiPartIdentifier;
import io.github.fupacat.ast.NullLiteral;
import io.github.fupacat.ast.ParenthesisExpression;
import io.github.fupacat.ast.StatementList;
import io.github.fupacat.ast.StringLiteral;
import io.github.fupacat.ast.UnaryExpression;
import io.github.fupacat.ast.UnaryExpressionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConditionalLogicUtilsTest {

    // TRACEABILITY_MAPPING: REQ-020
    @Test
    void testExpressionSignaturesAcrossNodeTypes() {
        MultiPartIdentifier multiPart = new MultiPartIdentifier();
        multiPart.Identifiers = List.of(identifier("dbo"), identifier("Orders"), identifier("Id"));

        ColumnReferenceExpression column = new ColumnReferenceExpression();
        column.MultiPartIdentifier = multiPart;

        IntegerLiteral one = intLiteral("1");
        StringLiteral value = new StringLiteral();
        value.Value = "value";

        BooleanComparisonExpression comparison = new BooleanComparisonExpression();
        comparison.ComparisonType = BooleanComparisonType.Equals;
        comparison.FirstExpression = column;
        comparison.SecondExpression = one;

        BooleanBinaryExpression andExpression = new BooleanBinaryExpression();
        andExpression.BinaryExpressionType = BooleanBinaryExpressionType.And;
        andExpression.FirstExpression = comparison;
        andExpression.SecondExpression = comparison;

        BooleanNotExpression notExpression = new BooleanNotExpression();
        notExpression.Expression = andExpression;

        UnaryExpression unary = new UnaryExpression();
        unary.UnaryExpressionType = UnaryExpressionType.Negative;
        unary.Expression = one;

        BinaryExpression math = new BinaryExpression();
        math.BinaryExpressionType = BinaryExpressionType.Add;
        math.FirstExpression = one;
        math.SecondExpression = one;

        ParenthesisExpression parenthesis = new ParenthesisExpression();
        parenthesis.Expression = math;

        FunctionCall call = new FunctionCall();
        call.FunctionName = identifier("ABS");
        call.Parameters = List.of(one);

        assertThat(ConditionalLogicUtils.expressionSignature(identifier("x"))).isEqualTo("id:x");
        assertThat(ConditionalLogicUtils.expressionSignature(column)).isEqualTo("col:id:dbo.id:Orders.id:Id");
        assertThat(ConditionalLogicUtils.expressionSignature(one)).isEqualTo("int:1");
        assertThat(ConditionalLogicUtils.expressionSignature(value)).isEqualTo("str:value");
        assertThat(ConditionalLogicUtils.expressionSignature(new NullLiteral())).isEqualTo("null");
        assertThat(ConditionalLogicUtils.expressionSignature(comparison)).contains("cmp:Equals:col:");
        assertThat(ConditionalLogicUtils.expressionSignature(andExpression)).contains("boolBin:And:");
        assertThat(ConditionalLogicUtils.expressionSignature(notExpression)).contains("not:boolBin:And:");
        assertThat(ConditionalLogicUtils.expressionSignature(unary)).contains("unary:Negative:int:1");
        assertThat(ConditionalLogicUtils.expressionSignature(parenthesis)).contains("paren:bin:Add:");
        assertThat(ConditionalLogicUtils.expressionSignature(call)).isEqualTo("call:ABS(int:1)");
    }

    // TRACEABILITY_MAPPING: REQ-020
    @Test
    void testStructuralComparisonForFragmentsAndFallbackObjects() {
        IfStatement left = ifEqualsOne("a");
        IfStatement same = ifEqualsOne("a");
        IfStatement different = ifEqualsOne("b");

        assertThat(ConditionalLogicUtils.structurallySame(left, same)).isTrue();
        assertThat(ConditionalLogicUtils.structurallySame(left, different)).isFalse();
        assertThat(ConditionalLogicUtils.expressionSignature(new Object())).startsWith("Object:");
    }

    private static IfStatement ifEqualsOne(String variableName) {
        BooleanComparisonExpression predicate = new BooleanComparisonExpression();
        predicate.ComparisonType = BooleanComparisonType.Equals;
        predicate.FirstExpression = column(variableName);
        predicate.SecondExpression = intLiteral("1");

        StatementList statements = new StatementList();
        statements.Statements = List.of();

        BeginEndBlockStatement block = new BeginEndBlockStatement();
        block.StatementList = statements;

        IfStatement statement = new IfStatement();
        statement.Predicate = predicate;
        statement.ThenStatement = block;
        return statement;
    }

    private static Identifier identifier(String value) {
        Identifier identifier = new Identifier();
        identifier.Value = value;
        return identifier;
    }

    private static ColumnReferenceExpression column(String value) {
        MultiPartIdentifier multiPart = new MultiPartIdentifier();
        multiPart.Identifiers = List.of(identifier(value));

        ColumnReferenceExpression column = new ColumnReferenceExpression();
        column.MultiPartIdentifier = multiPart;
        return column;
    }

    private static IntegerLiteral intLiteral(String value) {
        IntegerLiteral literal = new IntegerLiteral();
        literal.Value = value;
        return literal;
    }
}
