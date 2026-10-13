package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.CastCall;
import io.github.fupacat.ast.ColumnReferenceExpression;
import io.github.fupacat.ast.ConvertCall;
import io.github.fupacat.ast.DataTypeReference;
import io.github.fupacat.ast.ParameterizedDataTypeReference;
import io.github.fupacat.ast.SqlDataTypeReference;
import io.github.fupacat.ast.TryCastCall;
import io.github.fupacat.ast.TryConvertCall;
import io.github.fupacat.sonar.tsql.catalog.ProjectSchemaCatalog;

public class NarrowingCastCheck extends TSqlCheck {

    public static final String RULE_KEY = "SR0014";
    private static final String UNKNOWN_TYPE = "unknown";

    // TRACEABILITY_MAPPING: REQ-018
    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    // TRACEABILITY_MAPPING: REQ-018
    @Override
    public void visit(CastCall node) {
        checkCast(node.DataType, node.Parameter, node);
        super.visit(node);
    }

    // TRACEABILITY_MAPPING: REQ-018
    @Override
    public void visit(TryCastCall node) {
        checkCast(node.DataType, node.Parameter, node);
        super.visit(node);
    }

    // TRACEABILITY_MAPPING: REQ-018
    @Override
    public void visit(ConvertCall node) {
        checkCast(node.DataType, node.Parameter, node);
        super.visit(node);
    }

    // TRACEABILITY_MAPPING: REQ-018
    @Override
    public void visit(TryConvertCall node) {
        checkCast(node.DataType, node.Parameter, node);
        super.visit(node);
    }

    private void checkCast(DataTypeReference targetType, io.github.fupacat.ast.ScalarExpression sourceExpression, io.github.fupacat.ast.TSqlFragment node) {
        if (targetType == null || sourceExpression == null) {
            return;
        }
        ProjectSchemaCatalog catalog = getProjectSchema();
        if (!(sourceExpression instanceof ColumnReferenceExpression columnReferenceExpression)) {
            return;
        }
        ProjectSchemaCatalog.TypeInfo sourceType = catalog.typeOf(columnReferenceExpression).orElse(null);
        ProjectSchemaCatalog.TypeInfo targetTypeInfo = toTypeInfo(targetType);
        if (sourceType == null || !targetTypeInfo.isNarrowerThan(sourceType)) {
            return;
        }
        reportIssue(node, "Data loss might occur when casting or assigning between columns or variables of narrower data types or lengths.");
    }

    private ProjectSchemaCatalog.TypeInfo toTypeInfo(DataTypeReference dataTypeReference) {
        if (dataTypeReference == null) {
            return null;
        }
        String typeName = UNKNOWN_TYPE;
        if (dataTypeReference instanceof SqlDataTypeReference sqlDataTypeReference) {
            if (sqlDataTypeReference.SqlDataTypeOption != null) {
                typeName = sqlDataTypeReference.SqlDataTypeOption.name();
            }
        } else if (dataTypeReference.Name != null && dataTypeReference.Name.BaseIdentifier != null) {
            typeName = dataTypeReference.Name.BaseIdentifier.Value;
        }
        Integer length = dataTypeReference instanceof ParameterizedDataTypeReference parameterized ? readLength(parameterized) : null;
        return new ProjectSchemaCatalog.TypeInfo(typeName, length);
    }

    private Integer readLength(ParameterizedDataTypeReference typeReference) {
        if (typeReference.Parameters == null) {
            return null;
        }
        for (var literal : typeReference.Parameters) {
            Integer parsed = parseLengthLiteral(literal == null ? null : literal.Value);
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    private Integer parseLengthLiteral(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            // ignore metadata that is not a scalar length value
            return null;
        }
    }
}
