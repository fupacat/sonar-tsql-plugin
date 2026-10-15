package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.BinaryExpression;
import io.github.fupacat.ast.BinaryExpressionType;
import io.github.fupacat.ast.ExecutableEntity;
import io.github.fupacat.ast.ExecutableProcedureReference;
import io.github.fupacat.ast.ExecutableStringList;
import io.github.fupacat.ast.ExecuteParameter;
import io.github.fupacat.ast.ExecuteStatement;
import io.github.fupacat.ast.ProcedureReference;
import io.github.fupacat.ast.ScalarExpression;
import io.github.fupacat.ast.SchemaObjectName;
import io.github.fupacat.ast.ValueExpression;
import io.github.fupacat.ast.VariableReference;

import java.util.List;

/**
 * Rule: Dynamic SQL should not concatenate unvalidated variables or parameters.
 */
public class DynamicSqlExecutionCheck extends TSqlCheck {

    public static final String RULE_KEY = "S1523";

    // TRACEABILITY_MAPPING: REQ-016
    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    // TRACEABILITY_MAPPING: REQ-016
    @Override
    public void visit(ExecuteStatement node) {
        if (node.ExecuteSpecification != null) {
            ExecutableEntity executableEntity = node.ExecuteSpecification.ExecutableEntity;
            if (executableEntity instanceof ExecutableStringList stringList && containsDangerousExpression(stringList.Strings)) {
                reportIssue(node, "Make sure that dynamic SQL is not built from untrusted input.");
            }
            if (executableEntity instanceof ExecutableProcedureReference procedureReference
                    && isSpExecuteSql(procedureReference)
                    && containsDangerousProcedureParameter(procedureReference.Parameters)) {
                reportIssue(node, "Make sure that dynamic SQL is not built from untrusted input.");
            }
        }
        super.visit(node);
    }

    private boolean containsDangerousExpression(List<? extends ValueExpression> expressions) {
        for (ValueExpression expression : expressions) {
            if (containsDangerousScalarExpression(expression)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsDangerousScalarExpression(ScalarExpression expression) {
        if (expression instanceof VariableReference) {
            return true;
        }
        if (expression instanceof BinaryExpression binaryExpression) {
            return containsDangerousScalarExpression(binaryExpression.FirstExpression)
                    || containsDangerousScalarExpression(binaryExpression.SecondExpression);
        }
        return false;
    }

    private boolean containsDangerousProcedureParameter(List<ExecuteParameter> parameters) {
        if (parameters == null) {
            return false;
        }
        for (int index = 0; index < parameters.size(); index++) {
            ExecuteParameter parameter = parameters.get(index);
            if (parameter != null && isStatementParameter(index, parameter)
                    && containsDangerousScalarExpression(parameter.ParameterValue)) {
                return true;
            }
        }
        return false;
    }

    private boolean isStatementParameter(int index, ExecuteParameter parameter) {
        if (parameter.Variable != null) {
            return matchesNamedParameter(parameter.Variable.Name, "@stmt");
        }
        return index == 0;
    }

    private boolean matchesNamedParameter(String candidate, String expected) {
        return candidate != null && expected.equalsIgnoreCase(candidate);
    }

    private boolean isSpExecuteSql(ExecutableProcedureReference procedureReference) {
        try {
            SchemaObjectName name = procedureReference.ProcedureReference.ProcedureReference.Name;
            return "sp_executesql".equalsIgnoreCase(name.BaseIdentifier.Value);
        } catch (NullPointerException e) {
            return false;
        }
    }
}
