package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.AlterFunctionStatement;
import io.github.fupacat.ast.AlterProcedureStatement;
import io.github.fupacat.ast.AssignmentSetClause;
import io.github.fupacat.ast.CreateFunctionStatement;
import io.github.fupacat.ast.CreateOrAlterFunctionStatement;
import io.github.fupacat.ast.CreateOrAlterProcedureStatement;
import io.github.fupacat.ast.CreateProcedureStatement;
import io.github.fupacat.ast.DeclareVariableElement;
import io.github.fupacat.ast.DeclareVariableStatement;
import io.github.fupacat.ast.ProcedureStatementBodyBase;
import io.github.fupacat.ast.SelectSetVariable;
import io.github.fupacat.ast.SetVariableStatement;
import io.github.fupacat.ast.TSqlBatch;
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
 * Rule: Local variables that are declared and never used should be removed.
 */
public class UnusedLocalVariableCheck extends TSqlCheck {

    public static final String RULE_KEY = "S1481";

    private final Map<Object, Map<String, DeclareVariableElement>> declarationsByScope = new HashMap<>();
    private final Map<Object, Set<String>> usedVariablesByScope = new HashMap<>();
    private final Deque<Object> activeScopeStack = new ArrayDeque<>();
    private TSqlFragment rootScope;
    private TSqlCheckContext analysisContext;

    // TRACEABILITY_MAPPING: REQ-018
    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    // TRACEABILITY_MAPPING: REQ-018
    @Override
    public void scan(TSqlFragment root, TSqlCheckContext context) {
        declarationsByScope.clear();
        usedVariablesByScope.clear();
        activeScopeStack.clear();
        rootScope = root;
        analysisContext = context;
        try {
            if (root != null) {
                root.accept(this);
            }
            for (Map.Entry<Object, Map<String, DeclareVariableElement>> entry : declarationsByScope.entrySet()) {
                Object scope = entry.getKey();
                Set<String> used = usedVariablesByScope.getOrDefault(scope, Set.of());
                for (Map.Entry<String, DeclareVariableElement> declarationEntry : entry.getValue().entrySet()) {
                    if (!used.contains(declarationEntry.getKey())) {
                        reportIssue(declarationEntry.getValue(), "Remove this unused local variable '" + declarationEntry.getKey() + "'.");
                    }
                }
            }
        } finally {
            analysisContext = null;
            rootScope = null;
        }
    }

    @Override
    protected void reportIssue(TSqlFragment fragment, String message) {
        if (analysisContext != null) {
            analysisContext.reportIssue(this, fragment, message);
        }
    }

    @Override
    public void visit(TSqlBatch node) {
        enterScope(node);
        super.visit(node);
        leaveScope();
    }

    @Override
    public void visit(CreateProcedureStatement node) {
        enterScope(node);
        super.visit(node);
        leaveScope();
    }

    @Override
    public void visit(CreateOrAlterProcedureStatement node) {
        enterScope(node);
        super.visit(node);
        leaveScope();
    }

    @Override
    public void visit(AlterProcedureStatement node) {
        enterScope(node);
        super.visit(node);
        leaveScope();
    }

    @Override
    public void visit(CreateFunctionStatement node) {
        enterScope(node);
        super.visit(node);
        leaveScope();
    }

    @Override
    public void visit(CreateOrAlterFunctionStatement node) {
        enterScope(node);
        super.visit(node);
        leaveScope();
    }

    @Override
    public void visit(AlterFunctionStatement node) {
        enterScope(node);
        super.visit(node);
        leaveScope();
    }

    @Override
    public void visit(DeclareVariableStatement node) {
        Object scope = currentScope();
        if (node.Declarations != null) {
            for (DeclareVariableElement declaration : node.Declarations) {
                if (declaration.VariableName != null && declaration.VariableName.Value != null) {
                    String name = normalizeName(declaration.VariableName.Value);
                    declarationsByScope.computeIfAbsent(scope, ignored -> new HashMap<>()).putIfAbsent(name, declaration);
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
    public void visit(io.github.fupacat.ast.AssignmentSetClause node) {
        if (node.NewValue != null) {
            node.NewValue.accept(this);
        }
    }

    @Override
    public void visit(io.github.fupacat.ast.SelectSetVariable node) {
        if (node.Expression != null) {
            node.Expression.accept(this);
        }
    }

    @Override
    public void visit(VariableReference node) {
        if (node.Name != null) {
            Object scope = currentScope();
            if (scope != null) {
                usedVariablesByScope.computeIfAbsent(scope, ignored -> new HashSet<>()).add(normalizeName(node.Name));
            }
        }
        super.visit(node);
    }

    private void enterScope(Object scope) {
        ensureScope(scope);
        activeScopeStack.push(scope);
    }

    private void leaveScope() {
        if (!activeScopeStack.isEmpty()) {
            activeScopeStack.pop();
        }
    }

    private Object currentScope() {
        if (!activeScopeStack.isEmpty()) {
            return activeScopeStack.peek();
        }
        return rootScope;
    }

    private void ensureScope(Object scope) {
        declarationsByScope.computeIfAbsent(scope, ignored -> new HashMap<>());
        usedVariablesByScope.computeIfAbsent(scope, ignored -> new HashSet<>());
    }

    private static String normalizeName(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
