package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.AlterTableAddTableElementStatement;
import io.github.fupacat.ast.AlterTableConstraintModificationStatement;
import io.github.fupacat.ast.ConstraintEnforcement;

/**
 * Rule: CHECK or NOCHECK should be specified explicitly on ALTER TABLE constraint changes.
 */
public class AlterTableCheckEnforcementCheck extends TSqlCheck {

    public static final String RULE_KEY = "S4102";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(AlterTableAddTableElementStatement node) {
        if (isMissingEnforcement(node.ExistingRowsCheckEnforcement)) {
            reportIssue(node, "Specify WITH CHECK or WITH NOCHECK explicitly when altering table constraints.");
        }
        super.visit(node);
    }

    @Override
    public void visit(AlterTableConstraintModificationStatement node) {
        if (isMissingEnforcement(node.ConstraintEnforcement) || isMissingEnforcement(node.ExistingRowsCheckEnforcement)) {
            reportIssue(node, "Specify WITH CHECK or WITH NOCHECK explicitly when altering table constraints.");
        }
        super.visit(node);
    }

    private boolean isMissingEnforcement(ConstraintEnforcement enforcement) {
        return enforcement == null
                || enforcement == ConstraintEnforcement.DEFAULT
                || enforcement == ConstraintEnforcement.NotSpecified;
    }
}
