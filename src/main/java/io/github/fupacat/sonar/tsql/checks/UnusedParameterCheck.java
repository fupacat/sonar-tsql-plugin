package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.ProcedureParameter;
import io.github.fupacat.ast.ProcedureStatementBodyBase;
import io.github.fupacat.ast.TSqlFragment;
import io.github.fupacat.ast.VariableReference;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Rule: Unused parameters should be removed from SQL procedures and functions.
 */
public class UnusedParameterCheck extends TSqlCheck {

    public static final String RULE_KEY = "S1172";

    private final Map<ProcedureStatementBodyBase, Map<String, ProcedureParameter>> parametersByProcedure = new HashMap<>();
    private final Map<ProcedureStatementBodyBase, Set<String>> usedParametersByProcedure = new HashMap<>();
    private final Deque<ProcedureStatementBodyBase> activeProcedureStack = new ArrayDeque<>();
    private TSqlCheckContext analysisContext;

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(io.github.fupacat.ast.CreateProcedureStatement node) {
        registerProcedureParameters(node);
        super.visit(node);
        popCurrentProcedure(node);
    }

    @Override
    public void visit(io.github.fupacat.ast.CreateOrAlterProcedureStatement node) {
        registerProcedureParameters(node);
        super.visit(node);
        popCurrentProcedure(node);
    }

    @Override
    public void visit(io.github.fupacat.ast.AlterProcedureStatement node) {
        registerProcedureParameters(node);
        super.visit(node);
        popCurrentProcedure(node);
    }

    @Override
    public void visit(io.github.fupacat.ast.CreateFunctionStatement node) {
        registerProcedureParameters(node);
        super.visit(node);
        popCurrentProcedure(node);
    }

    @Override
    public void visit(io.github.fupacat.ast.CreateOrAlterFunctionStatement node) {
        registerProcedureParameters(node);
        super.visit(node);
        popCurrentProcedure(node);
    }

    @Override
    public void visit(io.github.fupacat.ast.AlterFunctionStatement node) {
        registerProcedureParameters(node);
        super.visit(node);
        popCurrentProcedure(node);
    }

    private void registerProcedureParameters(ProcedureStatementBodyBase node) {
        Map<String, ProcedureParameter> parameters = new HashMap<>();
        if (node.Parameters != null) {
            for (ProcedureParameter parameter : node.Parameters) {
                if (parameter != null && parameter.VariableName != null && parameter.VariableName.Value != null) {
                    String name = normalizeName(parameter.VariableName.Value);
                    parameters.put(name, parameter);
                }
            }
        }

        if (!parameters.isEmpty()) {
            parametersByProcedure.put(node, parameters);
            usedParametersByProcedure.put(node, new HashSet<>());
            activeProcedureStack.push(node);
        }
    }

    private void popCurrentProcedure(ProcedureStatementBodyBase node) {
        if (!activeProcedureStack.isEmpty() && activeProcedureStack.peek() == node) {
            activeProcedureStack.pop();
        }
    }

    @Override
    public void visit(VariableReference node) {
        if (node.Name != null && !activeProcedureStack.isEmpty()) {
            String name = normalizeName(node.Name);
            ProcedureStatementBodyBase currentProcedure = activeProcedureStack.peek();
            Set<String> usedParameters = usedParametersByProcedure.get(currentProcedure);
            Map<String, ProcedureParameter> parameters = parametersByProcedure.get(currentProcedure);
            if (usedParameters != null && parameters != null && parameters.containsKey(name)) {
                usedParameters.add(name);
            }
        }
        super.visit(node);
    }

    @Override
    public void scan(TSqlFragment root, TSqlCheckContext context) {
        parametersByProcedure.clear();
        usedParametersByProcedure.clear();
        activeProcedureStack.clear();
        analysisContext = context;
        try {
            if (root != null) {
                root.accept(this);
            }
            for (Map.Entry<ProcedureStatementBodyBase, Map<String, ProcedureParameter>> entry : parametersByProcedure.entrySet()) {
                ProcedureStatementBodyBase procedure = entry.getKey();
                Set<String> used = usedParametersByProcedure.getOrDefault(procedure, Set.of());
                for (Map.Entry<String, ProcedureParameter> parameterEntry : entry.getValue().entrySet()) {
                    if (!used.contains(parameterEntry.getKey())) {
                        reportIssue(parameterEntry.getValue(), "Remove this unused parameter '" + parameterEntry.getKey() + "'.");
                    }
                }
            }
        } finally {
            analysisContext = null;
        }
    }

    @Override
    protected void reportIssue(TSqlFragment fragment, String message) {
        if (analysisContext != null) {
            analysisContext.reportIssue(this, fragment, message);
        }
    }

    private static String normalizeName(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
