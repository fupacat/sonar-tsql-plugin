package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.CreateProcedureStatement;
import io.github.fupacat.ast.ParameterModifier;
import io.github.fupacat.ast.ProcedureParameter;
import io.github.fupacat.ast.SetVariableStatement;
import io.github.fupacat.ast.TSqlConcreteFragmentVisitor;

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

        List<String> outputParameters = extractOutputParameterNames(node.Parameters);
        if (outputParameters.isEmpty()) {
            super.visit(node);
            return;
        }

        Set<String> assignedParameters = collectAssignedParameters(node.StatementList);
        for (String parameterName : outputParameters) {
            if (!assignedParameters.contains(parameterName)) {
                reportIssue(node, "Ensure OUTPUT parameter '@" + parameterName + "' is assigned in all code paths before the stored procedure returns.");
                break;
            }
        }
        super.visit(node);
    }

    private List<String> extractOutputParameterNames(List<ProcedureParameter> parameters) {
        List<String> outputParameters = new ArrayList<>();
        for (ProcedureParameter parameter : parameters) {
            if (parameter != null && parameter.VariableName != null
                    && parameter.VariableName.Value != null
                    && parameter.Modifier == ParameterModifier.Output) {
                String name = parameter.VariableName.Value.trim().replaceFirst("^@", "");
                outputParameters.add(name.toLowerCase(Locale.ROOT));
            }
        }
        return outputParameters;
    }

    private Set<String> collectAssignedParameters(io.github.fupacat.ast.StatementList statementList) {
        Set<String> assignedParameters = new HashSet<>();
        if (statementList == null) {
            return assignedParameters;
        }
        TSqlConcreteFragmentVisitor collector = new TSqlConcreteFragmentVisitor() {
            @Override
            public void visit(SetVariableStatement setVariableStatement) {
                if (setVariableStatement != null && setVariableStatement.Variable != null
                        && setVariableStatement.Variable.Name != null) {
                    String name = setVariableStatement.Variable.Name.trim();
                    if (name.startsWith("@")) {
                        name = name.substring(1);
                    }
                    assignedParameters.add(name.toLowerCase(Locale.ROOT));
                }
                super.visit(setVariableStatement);
            }
        };
        statementList.accept(collector);
        return assignedParameters;
    }
}
