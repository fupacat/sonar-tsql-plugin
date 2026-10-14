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
            if (parameter == null) {
                continue;
            }

            if (isStatementParameter(index, parameter)) {
                if (containsDangerousScalarExpression(parameter.ParameterValue)) {
                    return true;
                }
                continue;
            }

            if (isParameterDeclarationParameter(index, parameter)) {
                continue;
            }

            if (isBoundDataParameter(index, parameter)) {
                continue;
            }

            if (index == 0 && containsDangerousScalarExpression(parameter.ParameterValue)) {
                return true;
            }
        }
        return false;
    }

    private boolean isStatementParameter(int index, ExecuteParameter parameter) {
        if (parameter.Variable != null) {
            String variableName = parameter.Variable.Name;
            if (variableName != null && ("@stmt".equalsIgnoreCase(variableName) || "@statement".equalsIgnoreCase(variableName))) {
                return true;
            }
        }
        return index == 0 && parameter.Variable == null;
    }

    private boolean isParameterDeclarationParameter(int index, ExecuteParameter parameter) {
        if (parameter.Variable != null) {
            String variableName = parameter.Variable.Name;
            if (variableName != null && ("@params".equalsIgnoreCase(variableName) || "@parameterlist".equalsIgnoreCase(variableName))) {
                return true;
            }
        }
        return index == 1 && parameter.Variable == null;
    }

    private boolean isBoundDataParameter(int index, ExecuteParameter parameter) {
        if (parameter.Variable != null) {
            String variableName = parameter.Variable.Name;
            return variableName != null
                    && !"@stmt".equalsIgnoreCase(variableName)
                    && !"@statement".equalsIgnoreCase(variableName)
                    && !"@params".equalsIgnoreCase(variableName)
                    && !"@parameterlist".equalsIgnoreCase(variableName);
        }
        return index >= 2;
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
