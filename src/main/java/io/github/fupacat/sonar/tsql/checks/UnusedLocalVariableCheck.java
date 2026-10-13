package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.DeclareVariableElement;
import io.github.fupacat.ast.DeclareVariableStatement;
import io.github.fupacat.ast.SetVariableStatement;
import io.github.fupacat.ast.TSqlFragment;
import io.github.fupacat.ast.VariableReference;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Rule: Local variables that are declared and never used should be removed.
 */
public class UnusedLocalVariableCheck extends TSqlCheck {

    public static final String RULE_KEY = "S1481";

    private final Map<String, DeclareVariableElement> declaredVariables = new HashMap<>();
    private final Set<String> usedVariables = new HashSet<>();
    private TSqlCheckContext analysisContext;

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void scan(TSqlFragment root, TSqlCheckContext context) {
        declaredVariables.clear();
        usedVariables.clear();
        analysisContext = context;
        try {
            if (root != null) {
                root.accept(this);
            }
            for (Map.Entry<String, DeclareVariableElement> entry : declaredVariables.entrySet()) {
                if (!usedVariables.contains(entry.getKey())) {
                    reportIssue(entry.getValue(), "Remove this unused local variable '" + entry.getKey() + "'.");
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

    @Override
    public void visit(DeclareVariableStatement node) {
        if (node.Declarations != null) {
            for (DeclareVariableElement declaration : node.Declarations) {
                if (declaration.VariableName != null && declaration.VariableName.Value != null) {
                    String name = normalizeName(declaration.VariableName.Value);
                    declaredVariables.putIfAbsent(name, declaration);
                }
            }
        }
        super.visit(node);
    }

    @Override
    public void visit(SetVariableStatement node) {
        if (node.Expression != null) {
            node.Expression.accept(this);
        }
        if (node.Parameters != null) {
            for (var parameter : node.Parameters) {
                if (parameter != null) {
                    parameter.accept(this);
                }
            }
        }
    }

    @Override
    public void visit(VariableReference node) {
        if (node.Name != null) {
            usedVariables.add(normalizeName(node.Name));
        }
        super.visit(node);
    }

    private static String normalizeName(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
