package io.github.fupacat.sonar.tsql;

import io.github.fupacat.ParseError;
import io.github.fupacat.ParseResult;
import io.github.fupacat.TSqlParser;
import io.github.fupacat.TSqlParserToken;
import io.github.fupacat.ast.TSqlFragment;
import io.github.fupacat.ast.TSqlScript;
import io.github.fupacat.sonar.tsql.catalog.ProjectSchemaCatalog;
import io.github.fupacat.sonar.tsql.checks.CheckList;
import io.github.fupacat.sonar.tsql.checks.TSqlCheck;
import io.github.fupacat.sonar.tsql.checks.TSqlCheckContext;
import io.github.fupacat.sonar.tsql.rules.TSqlRulesDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * SonarQube sensor that parses Transact-SQL source files with Microsoft T-SQL ScriptDOM
 * and executes AST-based static analysis checks.
 */
public class TSqlSensor implements Sensor {

    private static final Logger LOG = LoggerFactory.getLogger(TSqlSensor.class);

    // TRACEABILITY_MAPPING: REQ-005
    @Override
    public void describe(SensorDescriptor descriptor) {
        descriptor.name("T-SQL ScriptDOM Sensor")
                .onlyOnLanguage(TSqlLanguage.KEY);
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Override
    public void execute(SensorContext context) {
        FileSystem fs = context.fileSystem();
        FilePredicate predicate = fs.predicates().hasLanguage(TSqlLanguage.KEY);
        Iterable<InputFile> inputFiles = fs.inputFiles(predicate);

        ProjectSchemaCatalog projectSchema = ProjectSchemaCatalog.fromInputFiles(inputFiles);
        List<TSqlCheck> activeChecks = getActiveChecks(context);
        LOG.info("Analyzing T-SQL files with {} active rules", activeChecks.size());

        for (InputFile inputFile : inputFiles) {
            analyzeFile(inputFile, context, activeChecks, projectSchema);
        }
    }

    // TRACEABILITY_MAPPING: REQ-005
    private List<TSqlCheck> getActiveChecks(SensorContext context) {
        List<TSqlCheck> activeChecks = new ArrayList<>();
        for (TSqlCheck check : CheckList.instantiateChecks()) {
            RuleKey ruleKey = RuleKey.of(TSqlRulesDefinition.REPOSITORY_KEY, check.getRuleKey());
            if (context.activeRules().find(ruleKey) != null) {
                activeChecks.add(check);
            }
        }
        return activeChecks;
    }

    // TRACEABILITY_MAPPING: REQ-005
    private void analyzeFile(InputFile inputFile, SensorContext context, List<TSqlCheck> activeChecks, ProjectSchemaCatalog projectSchema) {
        LOG.debug("Analyzing T-SQL file: {}", inputFile.uri());
        String content;
        try {
            content = inputFile.contents();
        } catch (IOException e) {
            LOG.error("Failed to read contents of file: " + inputFile.uri(), e);
            return;
        }

        ParseResult<TSqlScript> result;
        try {
            result = TSqlParser.parse(content);
        } catch (Exception e) {
            LOG.warn("Unexpected parser exception on file: " + inputFile.uri(), e);
            reportSyntaxError(inputFile, context, 1, "Failed to parse T-SQL script: " + e.getMessage());
            return;
        }

        reportParseErrors(inputFile, context, result);
        publishHighlighting(inputFile, context, result, content);
        executeChecks(inputFile, context, result.fragment(), content, activeChecks, projectSchema);
    }

    // TRACEABILITY_MAPPING: REQ-005
    private void reportParseErrors(InputFile inputFile, SensorContext context, ParseResult<TSqlScript> result) {
        if (!result.hasErrors() || result.errors() == null) {
            return;
        }
        for (ParseError error : result.errors()) {
            int line = Math.max(1, Math.min(error.line, inputFile.lines()));
            String msg = (error.message != null && !error.message.isBlank())
                    ? error.message
                    : "T-SQL syntax error";
            reportSyntaxError(inputFile, context, line, msg);
        }
    }

    // TRACEABILITY_MAPPING: REQ-005
    private void publishHighlighting(InputFile inputFile, SensorContext context, ParseResult<TSqlScript> result, String content) {
        if (context == null || inputFile == null || content == null) {
            return;
        }

        NewHighlighting highlighting = context.newHighlighting();
        if (highlighting == null) {
            return;
        }
        highlighting.onFile(inputFile);

        TSqlScript script = result != null ? result.fragment() : null;
        if (script != null && script.ScriptTokenStream != null && !script.ScriptTokenStream.isEmpty()) {
            publishHighlightingFromTokens(inputFile, highlighting, script.ScriptTokenStream);
        } else {
            publishHighlightingFromSourceText(inputFile, highlighting, content);
        }
        highlighting.save();
    }

    // TRACEABILITY_MAPPING: REQ-005
    private void publishHighlightingFromTokens(InputFile inputFile, NewHighlighting highlighting, List<?> tokenStream) {
        if (inputFile == null || highlighting == null || tokenStream == null || tokenStream.isEmpty()) {
            return;
        }
        for (Object tokenObject : tokenStream) {
            if (!(tokenObject instanceof TSqlParserToken token)) {
                continue;
            }
            TypeOfText type = mapTokenToType(token);
            if (type == null) {
                continue;
            }
            TextRange range = rangeForToken(inputFile, token);
            if (range != null) {
                highlighting.highlight(range, type);
            }
        }
    }

    // TRACEABILITY_MAPPING: REQ-005
    private void publishHighlightingFromSourceText(InputFile inputFile, NewHighlighting highlighting, String content) {
        if (inputFile == null || highlighting == null || content == null || content.isEmpty()) {
            return;
        }

        int index = 0;
        while (index < content.length()) {
            char ch = content.charAt(index);
            if (Character.isWhitespace(ch)) {
                index++;
                continue;
            }
            if (ch == '-' && index + 1 < content.length() && content.charAt(index + 1) == '-') {
                int end = index + 2;
                while (end < content.length() && content.charAt(end) != '\n' && content.charAt(end) != '\r') {
                    end++;
                }
                highlightRange(inputFile, highlighting, content, index, end, TypeOfText.COMMENT);
                index = end;
                continue;
            }
            if (ch == '/' && index + 1 < content.length() && content.charAt(index + 1) == '*') {
                int end = index + 2;
                while (end + 1 < content.length() && !(content.charAt(end) == '*' && content.charAt(end + 1) == '/')) {
                    end++;
                }
                if (end + 1 < content.length()) {
                    end += 2;
                } else {
                    end = content.length();
                }
                highlightRange(inputFile, highlighting, content, index, end, TypeOfText.COMMENT);
                index = end;
                continue;
            }
            if (ch == 'N' || ch == 'n' || ch == '\'') {
                if ((ch == 'N' || ch == 'n') && index + 1 < content.length() && content.charAt(index + 1) == '\'') {
                    int end = index + 2;
                    while (end < content.length()) {
                        if (content.charAt(end) == '\'') {
                            if (end + 1 < content.length() && content.charAt(end + 1) == '\'') {
                                end += 2;
                                continue;
                            }
                            end++;
                            break;
                        }
                        end++;
                    }
                    highlightRange(inputFile, highlighting, content, index, end, TypeOfText.STRING);
                    index = end;
                    continue;
                }
                if (ch == '\'') {
                    int end = index + 1;
                    while (end < content.length()) {
                        if (content.charAt(end) == '\'') {
                            if (end + 1 < content.length() && content.charAt(end + 1) == '\'') {
                                end += 2;
                                continue;
                            }
                            end++;
                            break;
                        }
                        end++;
                    }
                    highlightRange(inputFile, highlighting, content, index, end, TypeOfText.STRING);
                    index = end;
                    continue;
                }
            }
            if (ch == '[') {
                int end = index + 1;
                while (end < content.length() && content.charAt(end) != ']') {
                    end++;
                }
                if (end < content.length()) {
                    end++;
                } else {
                    end = content.length();
                }
                highlightRange(inputFile, highlighting, content, index, end, TypeOfText.KEYWORD_LIGHT);
                index = end;
                continue;
            }
            if (isIdentifierStart(ch)) {
                int end = index + 1;
                while (end < content.length() && isIdentifierPart(content.charAt(end))) {
                    end++;
                }
                String word = content.substring(index, end);
                TypeOfText type = isKeyword(word) ? TypeOfText.KEYWORD : TypeOfText.KEYWORD_LIGHT;
                highlightRange(inputFile, highlighting, content, index, end, type);
                index = end;
                continue;
            }
            index++;
        }
    }

    // TRACEABILITY_MAPPING: REQ-005
    private static TypeOfText mapTokenToType(TSqlParserToken token) {
        if (token == null || token.tokenType == null) {
            return null;
        }
        String tokenText = token.getText();
        return switch (token.tokenType) {
            case SingleLineComment, MultilineComment -> TypeOfText.COMMENT;
            case AsciiStringLiteral, UnicodeStringLiteral -> TypeOfText.STRING;
            case Identifier, QuotedIdentifier, Variable, AsciiStringOrQuotedIdentifier -> TypeOfText.KEYWORD_LIGHT;
            default -> tokenText != null && isKeyword(tokenText) ? TypeOfText.KEYWORD : null;
        };
    }

    // TRACEABILITY_MAPPING: REQ-005
    private static boolean isIdentifierStart(char ch) {
        return Character.isLetter(ch) || ch == '_' || ch == '@';
    }

    // TRACEABILITY_MAPPING: REQ-005
    private static boolean isIdentifierPart(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '_' || ch == '@' || ch == '$';
    }

    // TRACEABILITY_MAPPING: REQ-005
    private static boolean isKeyword(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return switch (value.toUpperCase(java.util.Locale.ROOT)) {
            case "SELECT", "FROM", "WHERE", "INSERT", "UPDATE", "DELETE", "CREATE", "ALTER", "DROP",
                "TABLE", "VIEW", "INDEX", "PROCEDURE", "FUNCTION", "TRIGGER", "DATABASE", "SCHEMA",
                "AS", "ON", "IN", "INTO", "VALUES", "SET", "BY", "GROUP", "ORDER", "HAVING",
                "JOIN", "INNER", "LEFT", "RIGHT", "FULL", "OUTER", "CROSS", "UNION", "ALL",
                "DISTINCT", "TOP", "WITH", "EXEC", "EXECUTE", "BEGIN", "END", "IF", "ELSE",
                "WHILE", "RETURN", "DECLARE", "CASE", "WHEN", "THEN", "AND", "OR", "NOT", "IS",
                "NULL", "LIKE", "BETWEEN", "EXISTS" -> true;
            default -> false;
        };
    }

    // TRACEABILITY_MAPPING: REQ-005
    private static void highlightRange(InputFile inputFile, NewHighlighting highlighting, String content, int start, int end, TypeOfText type) {
        if (inputFile == null || highlighting == null || content == null || start < 0 || end < start || end > content.length()) {
            return;
        }
        TextRange range = rangeForOffsets(content, start, end, inputFile);
        if (range != null) {
            highlighting.highlight(range, type);
        }
    }

    // TRACEABILITY_MAPPING: REQ-005
    private static TextRange rangeForToken(InputFile inputFile, TSqlParserToken token) {
        if (inputFile == null || token == null || token.getText() == null || token.getText().isEmpty()) {
            return null;
        }
        int startLine = token.getLine();
        int startColumn = token.getColumn() - 1;
        if (startLine < 1 || startColumn < 0 || startLine > inputFile.lines()) {
            return null;
        }
        int endLine = startLine;
        int endColumn = startColumn;
        for (int i = 0; i < token.getText().length(); i++) {
            char ch = token.getText().charAt(i);
            if (ch == '\r') {
                if (i + 1 < token.getText().length() && token.getText().charAt(i + 1) == '\n') {
                    i++;
                }
                endLine++;
                endColumn = 0;
            } else if (ch == '\n') {
                endLine++;
                endColumn = 0;
            } else {
                endColumn++;
            }
        }
        try {
            return inputFile.newRange(startLine, startColumn, endLine, endColumn);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static TextRange rangeForOffsets(String content, int start, int end, InputFile file) {
        if (content == null || file == null || start < 0 || end < start || end > content.length()) {
            return null;
        }
        int line = 1;
        int column = 0;
        int rangeStartLine = 1;
        int rangeStartColumn = 0;
        int rangeEndLine = 1;
        int rangeEndColumn = 0;
        boolean started = false;
        for (int i = 0; i <= content.length(); i++) {
            if (i == start) {
                rangeStartLine = line;
                rangeStartColumn = column;
                started = true;
            }
            if (i == end) {
                rangeEndLine = line;
                rangeEndColumn = column;
                break;
            }
            if (i >= content.length()) {
                break;
            }
            char ch = content.charAt(i);
            if (ch == '\r') {
                if (i + 1 < content.length() && content.charAt(i + 1) == '\n') {
                    i++;
                }
                line++;
                column = 0;
            } else if (ch == '\n') {
                line++;
                column = 0;
            } else {
                column++;
            }
        }
        if (!started) {
            return null;
        }
        try {
            return file.newRange(rangeStartLine, rangeStartColumn, rangeEndLine, rangeEndColumn);
        } catch (Exception ignored) {
            return null;
        }
    }

    // TRACEABILITY_MAPPING: REQ-005
    private void executeChecks(InputFile inputFile, SensorContext context, TSqlFragment rootFragment, String sourceText, List<TSqlCheck> activeChecks, ProjectSchemaCatalog projectSchema) {
        if (rootFragment == null || activeChecks.isEmpty()) {
            return;
        }
        TSqlCheckContext checkContext = new SensorCheckContext(inputFile, context, rootFragment, sourceText, projectSchema);
        for (TSqlCheck check : activeChecks) {
            try {
                check.scan(rootFragment, checkContext);
            } catch (Exception e) {
                LOG.error("Error running check " + check.getRuleKey() + " on file " + inputFile.uri(), e);
            }
        }
    }

    // TRACEABILITY_MAPPING: REQ-005
    private void reportSyntaxError(InputFile inputFile, SensorContext context, int line, String message) {
        RuleKey syntaxRuleKey = RuleKey.of(TSqlRulesDefinition.REPOSITORY_KEY, CheckList.SYNTAX_ERROR_RULE_KEY);
        if (context.activeRules().find(syntaxRuleKey) != null) {
            NewIssue issue = context.newIssue().forRule(syntaxRuleKey);
            NewIssueLocation location = issue.newLocation()
                    .on(inputFile)
                    .at(inputFile.selectLine(line))
                    .message(message);
            issue.at(location).save();
        }
    }

    static class SensorCheckContext implements TSqlCheckContext {
        private final InputFile inputFile;
        private final SensorContext sensorContext;
        private final TSqlFragment rootFragment;
        private final String sourceText;
        private final ProjectSchemaCatalog projectSchema;

        public SensorCheckContext(InputFile inputFile, SensorContext sensorContext, TSqlFragment rootFragment) {
            this(inputFile, sensorContext, rootFragment, null, null);
        }

        public SensorCheckContext(InputFile inputFile, SensorContext sensorContext, TSqlFragment rootFragment, ProjectSchemaCatalog projectSchema) {
            this(inputFile, sensorContext, rootFragment, null, projectSchema);
        }

        public SensorCheckContext(InputFile inputFile, SensorContext sensorContext, TSqlFragment rootFragment, String sourceText, ProjectSchemaCatalog projectSchema) {
            this.inputFile = inputFile;
            this.sensorContext = sensorContext;
            this.rootFragment = rootFragment;
            this.sourceText = sourceText;
            this.projectSchema = projectSchema == null ? ProjectSchemaCatalog.empty() : projectSchema;
        }

        @Override
        public InputFile getFile() {
            return inputFile;
        }

        @Override
        public String getSourceText() {
            return sourceText;
        }

        @Override
        public ProjectSchemaCatalog getProjectSchema() {
            return projectSchema;
        }

        @Override
        public void reportIssue(TSqlCheck check, TSqlFragment fragment, String message) {
            RuleKey ruleKey = RuleKey.of(TSqlRulesDefinition.REPOSITORY_KEY, check.getRuleKey());
            NewIssue issue = sensorContext.newIssue().forRule(ruleKey);
            NewIssueLocation location = issue.newLocation().on(inputFile).message(message);

            TextRange textRange = computeRange(fragment, inputFile);
            if (textRange != null) {
                location.at(textRange);
            } else {
                int line = getStartLine(fragment);
                line = Math.max(1, Math.min(line, inputFile.lines()));
                location.at(inputFile.selectLine(line));
            }

            issue.at(location).save();
        }

        @Override
        public void reportIssueAtLine(TSqlCheck check, int line, String message) {
            RuleKey ruleKey = RuleKey.of(TSqlRulesDefinition.REPOSITORY_KEY, check.getRuleKey());
            int validLine = Math.max(1, Math.min(line, inputFile.lines()));
            NewIssue issue = sensorContext.newIssue().forRule(ruleKey);
            NewIssueLocation location = issue.newLocation()
                    .on(inputFile)
                    .at(inputFile.selectLine(validLine))
                    .message(message);
            issue.at(location).save();
        }

        private List<?> getTokenStream(TSqlFragment fragment) {
            if (fragment != null && fragment.ScriptTokenStream != null && !fragment.ScriptTokenStream.isEmpty()) {
                return fragment.ScriptTokenStream;
            }
            if (rootFragment != null && rootFragment.ScriptTokenStream != null && !rootFragment.ScriptTokenStream.isEmpty()) {
                return rootFragment.ScriptTokenStream;
            }
            return List.of();
        }

        private int getStartLine(TSqlFragment fragment) {
            List<?> tokenStream = getTokenStream(fragment);
            if (fragment != null && !tokenStream.isEmpty()
                    && fragment.firstTokenIndex >= 0 && fragment.firstTokenIndex < tokenStream.size()) {
                Object tokenObj = tokenStream.get(fragment.firstTokenIndex);
                if (tokenObj instanceof TSqlParserToken token) {
                    return Math.max(1, token.getLine());
                }
            }
            return 1;
        }

        private TextRange computeRange(TSqlFragment fragment, InputFile file) {
            List<?> tokenStream = getTokenStream(fragment);
            if (fragment == null || tokenStream.isEmpty()) {
                return null;
            }
            int firstIdx = fragment.firstTokenIndex;
            if (firstIdx < 0 || firstIdx >= tokenStream.size()) {
                return null;
            }
            int lastIdx = fragment.lastTokenIndex;
            if (lastIdx < firstIdx || lastIdx >= tokenStream.size()) {
                lastIdx = firstIdx;
            }

            Object startObj = tokenStream.get(firstIdx);
            Object endObj = tokenStream.get(lastIdx);

            if (startObj instanceof TSqlParserToken startToken && endObj instanceof TSqlParserToken endToken) {
                return createRangeFromTokens(startToken, endToken, file);
            }
            return null;
        }

        private TextRange createRangeFromTokens(TSqlParserToken startToken, TSqlParserToken endToken, InputFile file) {
            int startLine = startToken.getLine();
            int endLine = endToken.getLine();
            if (startLine < 1 || endLine < startLine || startLine > file.lines() || endLine > file.lines()) {
                return null;
            }
            int startCol = Math.max(0, startToken.getColumn() - 1);
            int endTextLen = endToken.getText() != null ? endToken.getText().length() : 0;
            int endCol = Math.max(startCol, endToken.getColumn() - 1 + endTextLen);
            try {
                return file.newRange(startLine, startCol, endLine, endCol);
            } catch (Exception ignored) {
                return null;
            }
        }
    }
}
