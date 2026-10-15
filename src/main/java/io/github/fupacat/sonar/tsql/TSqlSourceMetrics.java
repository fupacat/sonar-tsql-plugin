package io.github.fupacat.sonar.tsql;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TSqlSourceMetrics {
    private static final Pattern KEYWORD_PATTERN = Pattern.compile("(?i)(?:\\b(IF|WHILE|CASE)\\b|\\bBEGIN\\s+TRY\\b|\\bBEGIN\\s+CATCH\\b)");
    private static final Pattern STATEMENT_SEPARATOR_PATTERN = Pattern.compile("(?i)(?:;|\\bGO\\b)");

    private final int lines;
    private final int commentLines;
    private final int nonCommentLines;
    private final int statements;
    private final int complexity;

    private TSqlSourceMetrics(int lines, int commentLines, int nonCommentLines, int statements, int complexity) {
        this.lines = lines;
        this.commentLines = commentLines;
        this.nonCommentLines = nonCommentLines;
        this.statements = statements;
        this.complexity = complexity;
    }

    static TSqlSourceMetrics fromText(String sourceText) {
        if (sourceText == null || sourceText.isEmpty()) {
            return new TSqlSourceMetrics(0, 0, 0, 0, 0);
        }

        int lineCount = sourceText.lines().toList().size();
        ScanResult scanResult = scanSourceText(sourceText);
        int statementCount = countMatches(scanResult.sanitizedText(), STATEMENT_SEPARATOR_PATTERN);
        int complexityCount = countMatches(scanResult.sanitizedText(), KEYWORD_PATTERN);

        return new TSqlSourceMetrics(lineCount, scanResult.commentLines(), scanResult.ncloc(), statementCount, complexityCount);
    }

    private static ScanResult scanSourceText(String sourceText) {
        StringBuilder sanitized = new StringBuilder(sourceText.length());
        ParseState state = ParseState.CODE;
        int commentLines = 0;
        int ncloc = 0;
        boolean lineHasCode = false;
        boolean lineHasComment = false;
        int blockCommentDepth = 0;

        for (int i = 0; i < sourceText.length(); i++) {
            char current = sourceText.charAt(i);
            char next = i + 1 < sourceText.length() ? sourceText.charAt(i + 1) : '\0';

            if (state == ParseState.LINE_COMMENT) {
                if (current == '\n') {
                    LineTotals totals = closeLine(lineHasCode, lineHasComment);
                    commentLines += totals.commentLines();
                    ncloc += totals.ncloc();
                    lineHasCode = false;
                    lineHasComment = false;
                    state = ParseState.CODE;
                    sanitized.append('\n');
                } else {
                    lineHasComment = true;
                    sanitized.append(' ');
                }
                continue;
            }

            if (state == ParseState.BLOCK_COMMENT) {
                if (current == '/' && next == '*') {
                    blockCommentDepth++;
                    lineHasComment = true;
                    sanitized.append(' ');
                    i++;
                } else if (current == '*' && next == '/') {
                    lineHasComment = true;
                    blockCommentDepth--;
                    sanitized.append(' ');
                    i++;
                    if (blockCommentDepth <= 0) {
                        blockCommentDepth = 0;
                        state = ParseState.CODE;
                    }
                } else if (current == '\n') {
                    LineTotals totals = closeLine(lineHasCode, lineHasComment);
                    commentLines += totals.commentLines();
                    ncloc += totals.ncloc();
                    lineHasCode = false;
                    lineHasComment = false;
                    sanitized.append('\n');
                } else {
                    lineHasComment = true;
                    sanitized.append(' ');
                }
                continue;
            }

            if (state == ParseState.SINGLE_QUOTE) {
                if (current == '\n') {
                    LineTotals totals = closeLine(lineHasCode, lineHasComment);
                    commentLines += totals.commentLines();
                    ncloc += totals.ncloc();
                    lineHasCode = false;
                    lineHasComment = false;
                    sanitized.append('\n');
                } else {
                    lineHasCode = true;
                    if (current == '\'' && next == '\'') {
                        sanitized.append(' ');
                        i++;
                    } else if (current == '\'') {
                        sanitized.append(' ');
                        state = ParseState.CODE;
                    } else {
                        sanitized.append(' ');
                    }
                }
                continue;
            }

            if (state == ParseState.DOUBLE_QUOTE) {
                if (current == '\n') {
                    LineTotals totals = closeLine(lineHasCode, lineHasComment);
                    commentLines += totals.commentLines();
                    ncloc += totals.ncloc();
                    lineHasCode = false;
                    lineHasComment = false;
                    sanitized.append('\n');
                } else {
                    lineHasCode = true;
                    if (current == '"' && next == '"') {
                        sanitized.append(' ');
                        i++;
                    } else if (current == '"') {
                        sanitized.append(' ');
                        state = ParseState.CODE;
                    } else {
                        sanitized.append(' ');
                    }
                }
                continue;
            }

            if (state == ParseState.BRACKET_IDENTIFIER) {
                if (current == '\n') {
                    LineTotals totals = closeLine(lineHasCode, lineHasComment);
                    commentLines += totals.commentLines();
                    ncloc += totals.ncloc();
                    lineHasCode = false;
                    lineHasComment = false;
                    sanitized.append('\n');
                } else {
                    lineHasCode = true;
                    if (current == ']' && next == ']') {
                        sanitized.append(' ');
                        i++;
                    } else if (current == ']') {
                        sanitized.append(' ');
                        state = ParseState.CODE;
                    } else {
                        sanitized.append(' ');
                    }
                }
                continue;
            }

            if (current == '\n') {
                LineTotals totals = closeLine(lineHasCode, lineHasComment);
                commentLines += totals.commentLines();
                ncloc += totals.ncloc();
                lineHasCode = false;
                lineHasComment = false;
                sanitized.append('\n');
                continue;
            }

            if (current == '-' && next == '-') {
                lineHasComment = true;
                state = ParseState.LINE_COMMENT;
                sanitized.append(' ');
                i++;
                continue;
            }

            if (current == '/' && next == '*') {
                lineHasComment = true;
                blockCommentDepth = 1;
                state = ParseState.BLOCK_COMMENT;
                sanitized.append(' ');
                i++;
                continue;
            }

            if (current == '\'') {
                lineHasCode = true;
                state = ParseState.SINGLE_QUOTE;
                sanitized.append(' ');
                continue;
            }

            if (current == '"') {
                lineHasCode = true;
                state = ParseState.DOUBLE_QUOTE;
                sanitized.append(' ');
                continue;
            }

            if (current == '[') {
                lineHasCode = true;
                state = ParseState.BRACKET_IDENTIFIER;
                sanitized.append(' ');
                continue;
            }

            if (!Character.isWhitespace(current)) {
                lineHasCode = true;
            }
            sanitized.append(current);
        }

        LineTotals totals = closeLine(lineHasCode, lineHasComment);
        commentLines += totals.commentLines();
        ncloc += totals.ncloc();
        return new ScanResult(sanitized.toString(), commentLines, ncloc);
    }

    private static LineTotals closeLine(boolean lineHasCode, boolean lineHasComment) {
        if (lineHasComment && !lineHasCode) {
            return new LineTotals(1, 0);
        }
        return new LineTotals(0, lineHasCode ? 1 : 0);
    }

    int lines() {
        return lines;
    }

    int commentLines() {
        return commentLines;
    }

    int nonCommentLines() {
        return nonCommentLines;
    }

    int statements() {
        return statements;
    }

    int complexity() {
        return complexity;
    }

    private static int countMatches(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private enum ParseState {
        CODE,
        LINE_COMMENT,
        BLOCK_COMMENT,
        SINGLE_QUOTE,
        DOUBLE_QUOTE,
        BRACKET_IDENTIFIER
    }

    private record LineTotals(int commentLines, int ncloc) {
    }

    private record ScanResult(String sanitizedText, int commentLines, int ncloc) {
    }
}
