package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.SqlDataTypeOption;
import io.github.fupacat.ast.SqlDataTypeReference;

/**
 * Rule: Explicit length should be specified for VARCHAR, NVARCHAR, and VARBINARY.
 */
public class ExplicitLengthForVariableTypesCheck extends TSqlCheck {

    public static final String RULE_KEY = "S4078";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(SqlDataTypeReference node) {
        if (requiresExplicitLength(node.SqlDataTypeOption) && (node.Parameters == null || node.Parameters.isEmpty())) {
            reportIssue(node, "Specify an explicit length for VARCHAR, NVARCHAR, and VARBINARY data types.");
        }
        super.visit(node);
    }


    private boolean requiresExplicitLength(SqlDataTypeOption option) {
        return option == SqlDataTypeOption.VarChar
                || option == SqlDataTypeOption.NVarChar
                || option == SqlDataTypeOption.VarBinary;
    }
}
