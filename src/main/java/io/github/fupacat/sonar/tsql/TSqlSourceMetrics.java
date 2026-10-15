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
        ScanContext context = new ScanContext(sourceText.length());

        for (int i = 0; i < sourceText.length(); i++) {
            char current = sourceText.charAt(i);
            char next = i + 1 < sourceText.length() ? sourceText.charAt(i + 1) : '\0';
            i += handleCurrentCharacter(context, current, next);
        }

        return context.finish();
    }

    private static int handleCurrentCharacter(ScanContext context, char current, char next) {
        return switch (context.state) {
            case LINE_COMMENT -> handleLineComment(context, current);
            case BLOCK_COMMENT -> handleBlockComment(context, current, next);
            case SINGLE_QUOTE -> handleQuoteContent(context, current, next, '\'');
            case DOUBLE_QUOTE -> handleQuoteContent(context, current, next, '"');
            case BRACKET_IDENTIFIER -> handleBracketIdentifier(context, current, next);
            case CODE -> handleCode(context, current, next);
        };
    }

    private static int handleLineComment(ScanContext context, char current) {
        if (current == '\n') {
            context.closeLine();
            context.state = ParseState.CODE;
            context.sanitized.append('\n');
            return 0;
        }
        context.lineHasComment = true;
        context.sanitized.append(' ');
        return 0;
    }

    private static int handleBlockComment(ScanContext context, char current, char next) {
        if (current == '/' && next == '*') {
            context.blockCommentDepth++;
            context.lineHasComment = true;
            context.sanitized.append(' ');
            return 1;
        }
        if (current == '*' && next == '/') {
            context.lineHasComment = true;
            context.blockCommentDepth--;
            context.sanitized.append(' ');
            if (context.blockCommentDepth <= 0) {
                context.blockCommentDepth = 0;
                context.state = ParseState.CODE;
            }
            return 1;
        }
        if (current == '\n') {
            context.closeLine();
            context.sanitized.append('\n');
            return 0;
        }
        context.lineHasComment = true;
        context.sanitized.append(' ');
        return 0;
    }

    private static int handleQuoteContent(
            ScanContext context,
            char current,
            char next,
            char quoteCharacter) {
        if (current == '\n') {
            context.closeLine();
            context.sanitized.append('\n');
            return 0;
        }
        context.lineHasCode = true;
        if (current == quoteCharacter && next == quoteCharacter) {
            context.sanitized.append(' ');
            return 1;
        }
        if (current == quoteCharacter) {
            context.sanitized.append(' ');
            context.state = ParseState.CODE;
            return 0;
        }
        context.sanitized.append(' ');
        return 0;
    }

    private static int handleBracketIdentifier(ScanContext context, char current, char next) {
        if (current == '\n') {
            context.closeLine();
            context.sanitized.append('\n');
            return 0;
        }
        context.lineHasCode = true;
        if (current == ']' && next == ']') {
            context.sanitized.append(' ');
            return 1;
        }
        if (current == ']') {
            context.sanitized.append(' ');
            context.state = ParseState.CODE;
            return 0;
        }
        context.sanitized.append(' ');
        return 0;
    }

    private static int handleCode(ScanContext context, char current, char next) {
        if (current == '\n') {
            context.closeLine();
            context.sanitized.append('\n');
            return 0;
        }
        if (current == '-' && next == '-') {
            context.lineHasComment = true;
            context.state = ParseState.LINE_COMMENT;
            context.sanitized.append(' ');
            return 1;
        }
        if (current == '/' && next == '*') {
            context.lineHasComment = true;
            context.blockCommentDepth = 1;
            context.state = ParseState.BLOCK_COMMENT;
            context.sanitized.append(' ');
            return 1;
        }
        if (current == '\'') {
            context.lineHasCode = true;
            context.state = ParseState.SINGLE_QUOTE;
            context.sanitized.append(' ');
            return 0;
        }
        if (current == '"') {
            context.lineHasCode = true;
            context.state = ParseState.DOUBLE_QUOTE;
            context.sanitized.append(' ');
            return 0;
        }
        if (current == '[') {
            context.lineHasCode = true;
            context.state = ParseState.BRACKET_IDENTIFIER;
            context.sanitized.append(' ');
            return 0;
        }
        if (!Character.isWhitespace(current)) {
            context.lineHasCode = true;
        }
        context.sanitized.append(current);
        return 0;
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

    private static final class ScanContext {
        private final StringBuilder sanitized;
        private ParseState state = ParseState.CODE;
        private int commentLines;
        private int ncloc;
        private int blockCommentDepth;
        private boolean lineHasCode;
        private boolean lineHasComment;

        private ScanContext(int textLength) {
            this.sanitized = new StringBuilder(textLength);
        }

        private void closeLine() {
            LineTotals totals = TSqlSourceMetrics.closeLine(lineHasCode, lineHasComment);
            commentLines += totals.commentLines();
            ncloc += totals.ncloc();
            lineHasCode = false;
            lineHasComment = false;
        }

        private ScanResult finish() {
            closeLine();
            return new ScanResult(sanitized.toString(), commentLines, ncloc);
        }
    }
}
