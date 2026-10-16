package io.github.fupacat.sonar.tsql.catalog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Recovers explicit nullability only from bounded CREATE TABLE column declarations. */
final class SourceNullability {
    private record Token(String text, boolean quoted) {
        boolean keyword(String word) { return !quoted && text.equalsIgnoreCase(word); }
    }
    private final Map<String, String> declarations = new HashMap<>();

    SourceNullability(String source) {
        List<Token> tokens = tokenize(source == null ? "" : source);
        for (int i = 0; i + 3 < tokens.size(); i++) {
            if (!tokens.get(i).keyword("CREATE") || !tokens.get(i + 1).keyword("TABLE")) { continue; }
            StringBuilder table = new StringBuilder();
            int cursor = i + 2;
            while (cursor < tokens.size() && !tokens.get(cursor).keyword("(")) {
                table.append(tokens.get(cursor++).text);
            }
            if (cursor == tokens.size()) { continue; }
            int start = ++cursor;
            int depth = 0;
            for (; cursor < tokens.size(); cursor++) {
                Token token = tokens.get(cursor);
                if (token.keyword(")") && depth == 0) {
                    addColumn(table.toString(), tokens.subList(start, cursor));
                    break;
                }
                if (token.keyword(",") && depth == 0) {
                    addColumn(table.toString(), tokens.subList(start, cursor));
                    start = cursor + 1;
                } else if (token.keyword("(")) { depth++; }
                else if (token.keyword(")")) { depth--; }
            }
            i = cursor;
        }
    }

    Optional<Boolean> nullable(String table, String column) {
        String value = declarations.get(key(table, column));
        return value == null || value.equals("unknown") ? Optional.empty() : Optional.of(value.equals("null"));
    }

    private void addColumn(String table, List<Token> tokens) {
        if (tokens.size() < 2) { return; }
        Token name = tokens.get(0);
        if (name.keyword("CONSTRAINT") || name.keyword("PRIMARY") || name.keyword("UNIQUE")
                || name.keyword("CHECK") || name.keyword("FOREIGN")) { return; }
        String value = "unknown";
        int depth = 0;
        for (int i = 1; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.keyword("(")) { depth++; }
            else if (token.keyword(")")) { depth--; }
            else if (depth == 0) {
                // Expressions and table references after these clauses cannot prove column nullability.
                if (token.keyword("DEFAULT") || token.keyword("CHECK") || token.keyword("REFERENCES")
                        || token.keyword("AS")) { break; }
                if (token.keyword("NULL")) {
                    value = i > 0 && tokens.get(i - 1).keyword("NOT") ? "not-null" : "null";
                    break;
                }
            }
        }
        declarations.merge(key(table, name.text), value, (oldValue, newValue) ->
                oldValue.equals(newValue) ? oldValue : "unknown");
    }

    private static String key(String table, String column) {
        return table.toLowerCase(Locale.ROOT) + "\u0000" + column.toLowerCase(Locale.ROOT);
    }

    private static List<Token> tokenize(String text) {
        List<Token> result = new ArrayList<>();
        int i = 0;
        while (i < text.length()) {
            char ch = text.charAt(i);
            if (Character.isWhitespace(ch)) { i++; continue; }
            if (text.startsWith("--", i)) {
                while (i < text.length() && text.charAt(i) != '\n' && text.charAt(i) != '\r') { i++; }
                continue;
            }
            if (text.startsWith("/*", i)) {
                int depth = 1;
                i += 2;
                while (i < text.length() && depth > 0) {
                    if (text.startsWith("/*", i)) { depth++; i += 2; }
                    else if (text.startsWith("*/", i)) { depth--; i += 2; }
                    else { i++; }
                }
                continue;
            }
            if (ch == '\'' || ch == '[' || ch == '"') {
                char close = ch == '[' ? ']' : ch;
                StringBuilder value = new StringBuilder();
                i++;
                boolean closed = false;
                while (i < text.length()) {
                    char next = text.charAt(i++);
                    if (next == close) {
                        if (i < text.length() && text.charAt(i) == close) { value.append(close); i++; }
                        else { closed = true; break; }
                    } else { value.append(next); }
                }
                if (!closed) { break; }
                result.add(new Token(ch == '\'' ? "<literal>" : value.toString(), true));
                continue;
            }
            int start = i++;
            if (Character.isLetterOrDigit(ch) || ch == '_' || ch == '#' || ch == '@') {
                while (i < text.length() && (Character.isLetterOrDigit(text.charAt(i))
                        || "_#$@".indexOf(text.charAt(i)) >= 0)) { i++; }
            }
            result.add(new Token(text.substring(start, i), false));
        }
        return result;
    }
}
