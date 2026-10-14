package io.github.fupacat.sonar.tsql;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TSqlSourceMetrics {
    private static final Pattern KEYWORD_PATTERN = Pattern.compile("(?i)\\b(IF|WHILE|CASE|TRY|CATCH)\\b");
    private static final Pattern STATEMENT_SEPARATOR_PATTERN = Pattern.compile("(?i)(?:;|\\bGO\\b)");

    private final int lines;
    private final int commentLines;
    private final int statements;
    private final int complexity;

    private TSqlSourceMetrics(int lines, int commentLines, int statements, int complexity) {
        this.lines = lines;
        this.commentLines = commentLines;
        this.statements = statements;
        this.complexity = complexity;
    }

    static TSqlSourceMetrics fromText(String sourceText) {
        if (sourceText == null || sourceText.isEmpty()) {
            return new TSqlSourceMetrics(0, 0, 0, 0);
        }

        List<String> lines = sourceText.lines().toList();
        int lineCount = lines.size();
        int commentLineCount = 0;
        boolean inBlockComment = false;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (inBlockComment) {
                commentLineCount++;
                if (trimmed.contains("*/")) {
                    inBlockComment = false;
                }
                continue;
            }
            if (trimmed.startsWith("--") || trimmed.startsWith("/*") || trimmed.startsWith("*") || trimmed.startsWith("*/")) {
                commentLineCount++;
                if (trimmed.contains("/*") && !trimmed.contains("*/")) {
                    inBlockComment = true;
                }
            }
        }

        String sanitized = stripCommentsAndStringLiterals(sourceText);
        int statementCount = countMatches(sanitized, STATEMENT_SEPARATOR_PATTERN);
        int complexityCount = countMatches(sanitized, KEYWORD_PATTERN);

        return new TSqlSourceMetrics(lineCount, commentLineCount, statementCount, complexityCount);
    }

    int lines() {
        return lines;
    }

    int commentLines() {
        return commentLines;
    }

    int nonCommentLines() {
        return Math.max(0, lines - commentLines);
    }

    int statements() {
        return statements;
    }

    int complexity() {
        return complexity;
    }

    private static String stripCommentsAndStringLiterals(String sourceText) {
        StringBuilder result = new StringBuilder(sourceText.length());
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;

        for (int i = 0; i < sourceText.length(); i++) {
            char current = sourceText.charAt(i);
            char next = i + 1 < sourceText.length() ? sourceText.charAt(i + 1) : '\0';

            if (inLineComment) {
                if (current == '\n') {
                    inLineComment = false;
                    result.append(current);
                }
                continue;
            }
            if (inBlockComment) {
                if (current == '*' && next == '/') {
                    inBlockComment = false;
                    result.append(' ');
                    i++;
                } else if (current == '\n') {
                    result.append(current);
                }
                continue;
            }
            if (inSingleQuote) {
                if (current == '\'' && next == '\'') {
                    result.append("''");
                    i++;
                } else {
                    result.append(current);
                    if (current == '\'') {
                        inSingleQuote = false;
                    }
                }
                continue;
            }
            if (inDoubleQuote) {
                if (current == '"' && next == '"') {
                    result.append("\"\"");
                    i++;
                } else {
                    result.append(current);
                    if (current == '"') {
                        inDoubleQuote = false;
                    }
                }
                continue;
            }
            if (current == '-' && next == '-') {
                inLineComment = true;
                i++;
                continue;
            }
            if (current == '/' && next == '*') {
                inBlockComment = true;
                i++;
                continue;
            }
            if (current == '\'') {
                inSingleQuote = true;
                result.append(current);
                continue;
            }
            if (current == '"') {
                inDoubleQuote = true;
                result.append(current);
                continue;
            }
            result.append(current);
        }

        return result.toString();
    }

    private static int countMatches(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
}
