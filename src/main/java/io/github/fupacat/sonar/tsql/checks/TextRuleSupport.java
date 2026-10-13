package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.TSqlParserToken;
import io.github.fupacat.ast.TSqlFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

final class TextRuleSupport {

    private static final String[] COMMENT_KEYWORDS = {"select", "insert", "update", "delete", "create", "alter", "drop", "exec",
            "if", "else", "begin", "end", "declare", "with", "case", "when", "then"};

    private TextRuleSupport() {
    }

    static String sourceOf(TSqlFragment fragment) {
        if (fragment == null) {
            return "";
        }
        if (fragment.Value != null && !fragment.Value.isEmpty()) {
            return fragment.Value;
        }
        if (fragment.ScriptTokenStream != null && !fragment.ScriptTokenStream.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Object token : fragment.ScriptTokenStream) {
                if (token instanceof TSqlParserToken parserToken && parserToken.getText() != null) {
                    sb.append(parserToken.getText());
                }
            }
            return sb.toString();
        }
        return "";
    }

    static List<String> lines(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(Arrays.asList(text.split("\\R", -1)));
    }

    static List<Integer> matchingLines(String text, java.util.function.Predicate<String> predicate) {
        List<Integer> lineNumbers = new ArrayList<>();
        List<String> lines = lines(text);
        for (int i = 0; i < lines.size(); i++) {
            if (predicate.test(lines.get(i))) {
                lineNumbers.add(i);
            }
        }
        return lineNumbers;
    }

    static boolean containsAnyIgnoreCase(String text, String... values) {
        String lower = text.toLowerCase(Locale.ROOT);
        for (String value : values) {
            if (lower.contains(value.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    static boolean isCommentLine(String line) {
        String trimmed = line.trim();
        return trimmed.startsWith("--") || trimmed.startsWith("/*") || trimmed.startsWith("*") || trimmed.startsWith("*/");
    }

    static boolean containsCommentFlag(String line) {
        String normalized = line.toLowerCase(Locale.ROOT);
        return normalized.contains("todo") || normalized.contains("fixme") || normalized.contains("nosonar");
    }

    static boolean hasKeyword(String candidate, String... keywords) {
        String lower = candidate.toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (lower.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    static boolean isMultistatementLine(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        int semicolons = 0;
        for (int i = 0; i < trimmed.length(); i++) {
            if (trimmed.charAt(i) == ';') {
                semicolons++;
            }
        }
        if (semicolons > 1) {
            return true;
        }
        return Pattern.compile("(?is);\\s*(?:SELECT|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP|EXEC|BEGIN|MERGE|IF|WHILE|DECLARE)")
                .matcher(trimmed)
                .find();
    }

    static boolean looksLikeCommentedCode(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || !trimmed.startsWith("--") && !trimmed.startsWith("/*") && !trimmed.startsWith("*")) {
            return false;
        }
        String withoutCommentPrefix = trimmed.replaceFirst("^--", "")
                .replaceFirst("^/\\*+", "")
                .replaceFirst("^\\*+", "")
                .replaceFirst("\\*/$", "")
                .trim();
        return hasKeyword(withoutCommentPrefix, COMMENT_KEYWORDS);
    }

    static int leadingSpaces(String line) {
        int count = 0;
        for (int i = 0; i < line.length() && line.charAt(i) == ' '; i++) {
            count++;
        }
        return count;
    }
}
