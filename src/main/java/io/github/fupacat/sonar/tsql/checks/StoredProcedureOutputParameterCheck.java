package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.AssignmentSetClause;
import io.github.fupacat.ast.CreateProcedureStatement;
import io.github.fupacat.ast.ParameterModifier;
import io.github.fupacat.ast.ProcedureParameter;
import io.github.fupacat.ast.TSqlConcreteFragmentVisitor;
import io.github.fupacat.ast.TSqlFragment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Rule: Ensure stored procedure OUTPUT parameters are assigned before returning.
 */
public class StoredProcedureOutputParameterCheck extends TSqlCheck {

    public static final String RULE_KEY = "S4196";

    // TRACEABILITY_MAPPING: REQ-022
    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    // TRACEABILITY_MAPPING: REQ-022
    @Override
    public void visit(CreateProcedureStatement node) {
        if (node == null || node.Parameters == null || node.Parameters.isEmpty()) {
            super.visit(node);
            return;
        }

        List<String> outputParameters = new ArrayList<>();
        for (ProcedureParameter parameter : node.Parameters) {
            if (parameter != null && parameter.VariableName != null
                    && parameter.VariableName.Value != null
                    && parameter.Modifier == ParameterModifier.Output) {
                outputParameters.add(parameter.VariableName.Value.trim().toLowerCase(Locale.ROOT));
            }
        }
        if (!outputParameters.isEmpty()) {
            Set<String> assignedParameters = new HashSet<>();
            TSqlConcreteFragmentVisitor collector = new TSqlConcreteFragmentVisitor() {
                @Override
                public void visit(AssignmentSetClause assignmentSetClause) {
                    if (assignmentSetClause != null && assignmentSetClause.Variable != null
                            && assignmentSetClause.Variable.Name != null) {
                        assignedParameters.add(assignmentSetClause.Variable.Name.trim().toLowerCase(Locale.ROOT));
                    }
                    super.visit(assignmentSetClause);
                }
            };
            if (node.StatementList != null) {
                node.StatementList.accept(collector);
            }

            for (String parameterName : outputParameters) {
                if (!assignedParameters.contains(parameterName)) {
                    reportIssue(node, "Ensure OUTPUT parameter '@" + parameterName + "' is assigned in all code paths before the stored procedure returns.");
                    break;
                }
            }
        }
        super.visit(node);
    }
}
