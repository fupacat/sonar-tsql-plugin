package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.FunctionCall;
import io.github.fupacat.ast.Literal;
import io.github.fupacat.ast.ScalarExpression;

import java.util.Locale;
import java.util.Set;

/**
 * Rule: Weak hash algorithms should not be used for cryptographic hashing.
 */
public class WeakHashCheck extends TSqlCheck {

    public static final String RULE_KEY = "S2070";
    private static final Set<String> WEAK_HASH_ALGORITHMS = Set.of("MD2", "MD4", "MD5", "SHA", "SHA1");

    // TRACEABILITY_MAPPING: REQ-017
    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    // TRACEABILITY_MAPPING: REQ-017
    @Override
    public void visit(FunctionCall node) {
        if (node.FunctionName != null && "HASHBYTES".equalsIgnoreCase(node.FunctionName.Value)
                && node.Parameters != null && node.Parameters.size() >= 2) {
            ScalarExpression algorithmExpression = node.Parameters.get(0);
            String algorithm = extractAlgorithm(algorithmExpression);
            if (algorithm != null && isWeakHashAlgorithm(algorithm)) {
                reportIssue(node,
                        "Use SHA2_256 or SHA2_512 instead of weak hash algorithms like " + algorithm + ".");
            }
        }
        super.visit(node);
    }

    private String extractAlgorithm(ScalarExpression expression) {
        if (expression == null) {
            return null;
        }
        if (expression instanceof Literal literal && literal.Value != null) {
            String value = literal.Value.trim();
            if (value.length() >= 2 && value.startsWith("'") && value.endsWith("'")) {
                value = value.substring(1, value.length() - 1);
            }
            if (value.length() >= 2 && value.startsWith("N'") && value.endsWith("'")) {
                value = value.substring(2, value.length() - 1);
            }
            return value;
        }
        return null;
    }

    private boolean isWeakHashAlgorithm(String algorithm) {
        String normalized = algorithm.replace("-", "").replace("_", "").toUpperCase(Locale.ROOT);
        return WEAK_HASH_ALGORITHMS.contains(normalized);
    }
}
