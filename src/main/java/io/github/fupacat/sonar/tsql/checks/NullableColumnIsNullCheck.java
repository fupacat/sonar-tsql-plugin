package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.ColumnReferenceExpression;
import io.github.fupacat.ast.FunctionCall;
import io.github.fupacat.ast.Literal;
import io.github.fupacat.sonar.tsql.catalog.ProjectSchemaCatalog;
import java.util.Locale;
import java.util.Set;

public class NullableColumnIsNullCheck extends TSqlCheck {
    public static final String RULE_KEY = "SR0007";
    private static final Set<String> CHARACTER_TYPES = Set.of("char", "varchar", "nchar", "nvarchar");

    // TRACEABILITY_MAPPING: REQ-017
    @Override
    public String getRuleKey() { return RULE_KEY; }

    // TRACEABILITY_MAPPING: REQ-017
    @Override
    public void visit(FunctionCall node) {
        checkReplacement(node);
        super.visit(node);
    }

    private void checkReplacement(FunctionCall node) {
        if (node.FunctionName == null || !"ISNULL".equalsIgnoreCase(node.FunctionName.Value)
                || node.Parameters == null || node.Parameters.size() != 2
                || !(node.Parameters.get(0) instanceof ColumnReferenceExpression column)
                || !(node.Parameters.get(1) instanceof Literal literal)) {
            return;
        }
        ProjectSchemaCatalog.ColumnInfo metadata = getProjectSchema().resolveColumn(column).orElse(null);
        if (metadata == null || !metadata.nullabilityKnown() || !metadata.nullable() || metadata.length() == null || metadata.length() <= 0
                || !CHARACTER_TYPES.contains(metadata.sqlType().toLowerCase(Locale.ROOT))) {
            return;
        }
        String replacement = stringValue(literal.Value);
        // Non-ASCII VARCHAR byte lengths depend on collation; keep the proof conservative.
        if (replacement != null && replacement.chars().allMatch(value -> value <= 127)
                && replacement.length() > metadata.length()) {
            reportIssue(node, "This ISNULL replacement is longer than the column's declared length and can be truncated when the column is NULL.");
        }
    }

    private static String stringValue(String value) {
        if (value == null) { return null; }
        String text = value;
        if (text.startsWith("N'") || text.startsWith("n'")) { text = text.substring(1); }
        if (text.length() < 2 || !text.startsWith("'") || !text.endsWith("'")) { return null; }
        return text.substring(1, text.length() - 1).replace("''", "'");
    }
}
