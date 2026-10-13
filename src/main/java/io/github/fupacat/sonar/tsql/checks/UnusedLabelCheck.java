package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.GoToStatement;
import io.github.fupacat.ast.LabelStatement;
import io.github.fupacat.ast.TSqlFragment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Rule: Unused labels should be removed.
 */
public class UnusedLabelCheck extends TSqlCheck {

    public static final String RULE_KEY = "S1065";

    private final Map<String, LabelStatement> labels = new HashMap<>();
    private final Set<String> referencedLabels = new HashSet<>();

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(LabelStatement node) {
        if (node.Value != null) {
            String labelName = normalizeLabel(node.Value);
            if (labelName != null) {
                labels.putIfAbsent(labelName, node);
            }
        }
        super.visit(node);
    }

    @Override
    public void visit(GoToStatement node) {
        if (node.LabelName != null && node.LabelName.Value != null) {
            String labelName = normalizeLabel(node.LabelName.Value);
            if (labelName != null) {
                referencedLabels.add(labelName);
            }
        }
        super.visit(node);
    }

    @Override
    public void scan(TSqlFragment root, TSqlCheckContext context) {
        labels.clear();
        referencedLabels.clear();
        setField("context", context);
        setField("root", root);
        try {
            if (root != null) {
                root.accept(this);
            }
            for (Map.Entry<String, LabelStatement> entry : labels.entrySet()) {
                if (!referencedLabels.contains(entry.getKey())) {
                    reportIssue(entry.getValue(), "Remove this unused label '" + entry.getKey() + "'.");
                }
            }
        } finally {
            setField("context", null);
            setField("root", null);
        }
    }

    private static String normalizeLabel(String value) {
        String candidate = value.trim();
        if (candidate.endsWith(":")) {
            candidate = candidate.substring(0, candidate.length() - 1);
        }
        if (candidate.isEmpty()) {
            return null;
        }
        return candidate.toLowerCase(Locale.ROOT);
    }

    private void setField(String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = TSqlCheck.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(this, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to set field " + fieldName, e);
        }
    }
}
