package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.SqlDataTypeOption;
import io.github.fupacat.ast.SqlDataTypeReference;

/**
 * Rule: Avoid deprecated data types (TEXT, NTEXT, IMAGE).
 * These types are deprecated in Microsoft SQL Server and will be removed in future releases.
 * Use VARCHAR(MAX), NVARCHAR(MAX), or VARBINARY(MAX) instead.
 */
public class AvoidDeprecatedTypesCheck extends TSqlCheck {

    public static final String RULE_KEY = "AvoidDeprecatedTypes";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(SqlDataTypeReference node) {
        if (node.SqlDataTypeOption != null) {
            SqlDataTypeOption option = node.SqlDataTypeOption;
            if (option == SqlDataTypeOption.Text || option == SqlDataTypeOption.NText || option == SqlDataTypeOption.Image) {
                reportIssue(node, 
                        String.format("Avoid using deprecated data type '%s'. Use %s instead.",
                                option.name().toUpperCase(),
                                getRecommendedReplacement(option)));
            }
        }
    }

    private String getRecommendedReplacement(SqlDataTypeOption option) {
        return switch (option) {
            case Text -> "VARCHAR(MAX)";
            case NText -> "NVARCHAR(MAX)";
            case Image -> "VARBINARY(MAX)";
            default -> "a modern large-value data type";
        };
    }
}
