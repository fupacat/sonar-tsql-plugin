package io.github.fupacat.sonar.tsql;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TSqlSourceMetrics {
    private static final Pattern KEYWORD_PATTERN = Pattern.compile("(?i)(\\b(IF|WHILE|CASE)\\b|\\bBEGIN\\s+(TRY|CATCH)\\b)");
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

        List<String> lines = sourceText.lines().toList();
        int lineCount = lines.size();
        int commentLineCount = countCommentLines(sourceText);
        int ncloc = countNonCommentCodeLines(sourceText);
        String sanitized = stripCommentsAndStringLiterals(sourceText);
        int statementCount = countMatches(sanitized, STATEMENT_SEPARATOR_PATTERN);
        int complexityCount = countMatches(sanitized, KEYWORD_PATTERN);

        return new TSqlSourceMetrics(lineCount, commentLineCount, ncloc, statementCount, complexityCount);
    }

    private static int countCommentLines(String sourceText) {
        int commentLineCount = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        boolean lineHasCode = false;
        boolean lineHasComment = false;

        for (int i = 0; i < sourceText.length(); i++) {
            char current = sourceText.charAt(i);
            char next = i + 1 < sourceText.length() ? sourceText.charAt(i + 1) : '\0';

            if (inLineComment) {
                if (current == '\n') {
                    if (lineHasComment && !lineHasCode) {
                        commentLineCount++;
                    }
                    inLineComment = false;
                    lineHasCode = false;
                    lineHasComment = false;
                } else {
                    lineHasComment = true;
                }
                continue;
            }

            if (inBlockComment) {
                if (current == '*' && next == '/') {
                    lineHasComment = true;
                    inBlockComment = false;
                    i++;
                } else if (current == '\n') {
                    if (lineHasComment && !lineHasCode) {
                        commentLineCount++;
                    }
                    lineHasCode = false;
                    lineHasComment = false;
                } else {
                    lineHasComment = true;
                }
                continue;
            }

            if (inSingleQuote) {
                if (current == '\'' && next == '\'') {
                    i++;
                } else if (current == '\'') {
                    inSingleQuote = false;
                }
                continue;
            }

            if (inDoubleQuote) {
                if (current == '"' && next == '"') {
                    i++;
                } else if (current == '"') {
                    inDoubleQuote = false;
                }
                continue;
            }

            if (current == '\n') {
                if (lineHasComment && !lineHasCode) {
                    commentLineCount++;
                }
                lineHasCode = false;
                lineHasComment = false;
                continue;
            }

            if (current == '-' && next == '-') {
                lineHasComment = true;
                inLineComment = true;
                i++;
                continue;
            }

            if (current == '/' && next == '*') {
                lineHasComment = true;
                inBlockComment = true;
                i++;
                continue;
            }

            if (current == '\'') {
                inSingleQuote = true;
                continue;
            }

            if (current == '"') {
                inDoubleQuote = true;
                continue;
            }

            if (!Character.isWhitespace(current)) {
                lineHasCode = true;
            }
        }

        if (lineHasComment && !lineHasCode && !inSingleQuote && !inDoubleQuote) {
            commentLineCount++;
        }

        return commentLineCount;
    }

    private static int countNonCommentCodeLines(String sourceText) {
        int ncloc = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        boolean lineHasCode = false;

        for (int i = 0; i < sourceText.length(); i++) {
            char current = sourceText.charAt(i);
            char next = i + 1 < sourceText.length() ? sourceText.charAt(i + 1) : '\0';

            if (inLineComment) {
                if (current == '\n') {
                    if (lineHasCode) {
                        ncloc++;
                    }
                    inLineComment = false;
                    lineHasCode = false;
                }
                continue;
            }

            if (inBlockComment) {
                if (current == '*' && next == '/') {
                    inBlockComment = false;
                    i++;
                } else if (current == '\n') {
                    if (lineHasCode) {
                        ncloc++;
                    }
                    lineHasCode = false;
                }
                continue;
            }

            if (inSingleQuote) {
                if (current == '\n') {
                    if (lineHasCode) {
                        ncloc++;
                    }
                    lineHasCode = false;
                } else if (current == '\'' && next == '\'') {
                    i++;
                } else if (current == '\'') {
                    inSingleQuote = false;
                }
                continue;
            }

            if (inDoubleQuote) {
                if (current == '\n') {
                    if (lineHasCode) {
                        ncloc++;
                    }
                    lineHasCode = false;
                } else if (current == '"' && next == '"') {
                    i++;
                } else if (current == '"') {
                    inDoubleQuote = false;
                }
                continue;
            }

            if (current == '\n') {
                if (lineHasCode) {
                    ncloc++;
                }
                lineHasCode = false;
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
                lineHasCode = true;
                continue;
            }

            if (current == '"') {
                inDoubleQuote = true;
                lineHasCode = true;
                continue;
            }

            if (!Character.isWhitespace(current)) {
                lineHasCode = true;
            }
        }

        if (lineHasCode) {
            ncloc++;
        }

        return ncloc;
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
                if (current == '\n') {
                    result.append(current);
                }
                if (current == '\'' && next == '\'') {
                    i++;
                } else {
                    if (current == '\'') {
                        inSingleQuote = false;
                    }
                }
                continue;
            }
            if (inDoubleQuote) {
                if (current == '\n') {
                    result.append(current);
                }
                if (current == '"' && next == '"') {
                    i++;
                } else {
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
                result.append(' ');
                continue;
            }
            if (current == '"') {
                inDoubleQuote = true;
                result.append(' ');
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
