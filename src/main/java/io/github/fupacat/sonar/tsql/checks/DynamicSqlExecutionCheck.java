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
        if (expressions == null) {
            return false;
        }
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
        if (expression instanceof BinaryExpression binaryExpression
                && binaryExpression.BinaryExpressionType == BinaryExpressionType.Concat) {
            return containsDangerousScalarExpression(binaryExpression.FirstExpression)
                    || containsDangerousScalarExpression(binaryExpression.SecondExpression);
        }
        return false;
    }

    private boolean containsDangerousProcedureParameter(List<ExecuteParameter> parameters) {
        if (parameters == null) {
            return false;
        }
        for (ExecuteParameter parameter : parameters) {
            if (parameter != null && containsDangerousScalarExpression(parameter.ParameterValue)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSpExecuteSql(ExecutableProcedureReference procedureReference) {
        if (procedureReference == null || procedureReference.ProcedureReference == null
                || procedureReference.ProcedureReference.ProcedureReference == null) {
            return false;
        }
        ProcedureReference procedure = procedureReference.ProcedureReference.ProcedureReference;
        if (procedure.Name == null) {
            return false;
        }
        SchemaObjectName name = procedure.Name;
        String baseIdentifier = name.BaseIdentifier != null ? name.BaseIdentifier.Value : null;
        String schemaIdentifier = name.SchemaIdentifier != null ? name.SchemaIdentifier.Value : null;
        String normalizedName = (schemaIdentifier == null ? "" : schemaIdentifier + ".") + (baseIdentifier == null ? "" : baseIdentifier);
        return "sp_executesql".equalsIgnoreCase(normalizedName)
                || "sp_executesql".equalsIgnoreCase(baseIdentifier);
    }
}
