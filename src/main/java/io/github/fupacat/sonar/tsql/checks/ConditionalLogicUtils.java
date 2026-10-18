package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class ConditionalLogicUtils {
    private static final Set<String> NON_SEMANTIC_FIELDS = Set.of(
            "ScriptTokenStream",
            "Value",
            "firstTokenIndex",
            "lastTokenIndex",
            "offset",
            "tokenType",
            "convertStringToIdentifier");

    private ConditionalLogicUtils() {
    }

    static String expressionSignature(Object node) {
        if (node == null) {
            return "null";
        }
        String basicSignature = basicSignature(node);
        if (basicSignature != null) {
            return basicSignature;
        }
        String expressionSignature = expressionNodeSignature(node);
        if (expressionSignature != null) {
            return expressionSignature;
        }
        if (node instanceof FunctionCall functionCall) {
            return functionSignature(functionCall);
        }
        if (node instanceof TSqlFragment fragment) {
            return reflectFragmentSignature(fragment);
        }
        return node.getClass().getSimpleName() + ':' + String.valueOf(node);
    }

    private static String basicSignature(Object node) {
        if (node instanceof Identifier identifier && identifier.Value != null) {
            return "id:" + identifier.Value;
        }
        if (node instanceof ColumnReferenceExpression column) {
            return "col:" + expressionSignature(column.MultiPartIdentifier);
        }
        if (node instanceof MultiPartIdentifier multiPart) {
            return joinIdentifierParts(multiPart);
        }
        if (node instanceof IntegerLiteral literal && literal.Value != null) {
            return "int:" + literal.Value;
        }
        if (node instanceof StringLiteral literal && literal.Value != null) {
            return "str:" + literal.Value;
        }
        if (node instanceof NullLiteral) {
            return "null";
        }
        return null;
    }

    private static String expressionNodeSignature(Object node) {
        if (node instanceof BooleanComparisonExpression comparison) {
            return "cmp:" + comparison.ComparisonType + ":" + expressionSignature(comparison.FirstExpression)
                    + ":" + expressionSignature(comparison.SecondExpression);
        }
        if (node instanceof BooleanBinaryExpression binary) {
            return "boolBin:" + binary.BinaryExpressionType + ":" + expressionSignature(binary.FirstExpression)
                    + ":" + expressionSignature(binary.SecondExpression);
        }
        if (node instanceof BooleanNotExpression not) {
            return "not:" + expressionSignature(not.Expression);
        }
        if (node instanceof UnaryExpression unary) {
            return "unary:" + unary.UnaryExpressionType + ":" + expressionSignature(unary.Expression);
        }
        if (node instanceof BinaryExpression binary) {
            return "bin:" + binary.BinaryExpressionType + ":" + expressionSignature(binary.FirstExpression)
                    + ":" + expressionSignature(binary.SecondExpression);
        }
        if (node instanceof ParenthesisExpression parenthesis) {
            return "paren:" + expressionSignature(parenthesis.Expression);
        }
        return null;
    }

    private static String joinIdentifierParts(MultiPartIdentifier multiPart) {
        List<String> parts = new ArrayList<>();
        if (multiPart.Identifiers != null) {
            for (Object id : multiPart.Identifiers) {
                parts.add(expressionSignature(id));
            }
        }
        return String.join(".", parts);
    }

    private static String functionSignature(FunctionCall functionCall) {
        StringBuilder builder = new StringBuilder();
        builder.append("call:");
        if (functionCall.FunctionName != null) {
            builder.append(functionCall.FunctionName.Value);
        }
        if (functionCall.Parameters != null) {
            builder.append("(");
            for (int i = 0; i < functionCall.Parameters.size(); i++) {
                if (i > 0) {
                    builder.append(",");
                }
                builder.append(expressionSignature(functionCall.Parameters.get(i)));
            }
            builder.append(")");
        }
        return builder.toString();
    }

    private static String reflectFragmentSignature(TSqlFragment fragment) {
        StringBuilder builder = new StringBuilder();
        builder.append(fragment.getClass().getSimpleName());
        for (Field field : fragment.getClass().getFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            Object value = getFieldValue(fragment, field);
            if (value == null) {
                continue;
            }
            appendFieldSignature(builder, field.getName(), value);
        }
        return builder.toString();
    }

    private static void appendFieldSignature(StringBuilder builder, String fieldName, Object value) {
        if (NON_SEMANTIC_FIELDS.contains(fieldName)) {
            return;
        }
        if (value instanceof Iterable<?> iterable) {
            List<String> items = new ArrayList<>();
            for (Object item : iterable) {
                if (item != null) {
                    items.add(expressionSignature(item));
                }
            }
            if (!items.isEmpty()) {
                builder.append(':').append(fieldName).append('[').append(String.join(",", items)).append(']');
            }
            return;
        }
        if (value instanceof Number || value instanceof Boolean || value instanceof String || value instanceof Enum<?>) {
            builder.append(':').append(fieldName).append('=').append(value);
            return;
        }
        builder.append(':').append(fieldName).append('=').append(expressionSignature(value));
    }

    private static Object getFieldValue(TSqlFragment fragment, Field field) {
        try {
            return field.get(fragment);
        } catch (IllegalAccessException ignored) {
            return null;
        }
    }

    static boolean structurallySame(Object left, Object right) {
        return expressionSignature(left).equals(expressionSignature(right));
    }
}
