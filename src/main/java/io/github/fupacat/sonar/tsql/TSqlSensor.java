package io.github.fupacat.sonar.tsql;

import io.github.fupacat.ParseError;
import io.github.fupacat.ParseResult;
import io.github.fupacat.TSqlParser;
import io.github.fupacat.TSqlParserToken;
import io.github.fupacat.ast.TSqlFragment;
import io.github.fupacat.ast.TSqlScript;
import io.github.fupacat.sonar.tsql.catalog.ProjectSchemaCatalog;
import io.github.fupacat.sonar.tsql.checks.CheckList;
import io.github.fupacat.sonar.tsql.checks.InPredicateIndexedColumnCheck;
import io.github.fupacat.sonar.tsql.checks.NarrowingCastCheck;
import io.github.fupacat.sonar.tsql.checks.NullableColumnIsNullCheck;
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
import org.sonar.api.batch.sensor.measure.NewMeasure;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.rule.RuleKey;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * SonarQube sensor that parses Transact-SQL source files with Microsoft T-SQL ScriptDOM
 * and executes AST-based static analysis checks.
 */
public class TSqlSensor implements Sensor {

    private static final Logger LOG = LoggerFactory.getLogger(TSqlSensor.class);
    private static final Set<String> SCHEMA_AWARE_RULE_KEYS = Set.of(
            InPredicateIndexedColumnCheck.RULE_KEY,
            NullableColumnIsNullCheck.RULE_KEY,
            NarrowingCastCheck.RULE_KEY
    );

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
        List<InputFile> inputFiles = new ArrayList<>();
        for (InputFile inputFile : fs.inputFiles(predicate)) {
            inputFiles.add(inputFile);
        }

        List<TSqlCheck> activeChecks = getActiveChecks(context);
        boolean needsSchema = requiresProjectSchema(activeChecks);
        Map<InputFile, ParsedFile> parsedFiles = parseInputFiles(inputFiles);
        ProjectSchemaCatalog projectSchema = ProjectSchemaCatalog.empty();
        if (needsSchema) {
            try {
                projectSchema = buildProjectSchema(parsedFiles.values().stream()
                        .map(ParsedFile::result)
                        .filter(Objects::nonNull)
                        .toList());
            } catch (Exception e) {
                LOG.warn("Failed to build project schema catalog; continuing analysis without schema metadata", e);
            }
        }
        LOG.info("Analyzing T-SQL files with {} active rules{}", activeChecks.size(), needsSchema ? " and schema indexing" : "");

        for (InputFile inputFile : inputFiles) {
            ParsedFile parsedFile = parsedFiles.remove(inputFile);
            if (parsedFile != null) {
                analyzeFile(inputFile, context, activeChecks, parsedFile, projectSchema);
            }
        }
    }

    // TRACEABILITY_MAPPING: REQ-005
    private boolean requiresProjectSchema(List<TSqlCheck> activeChecks) {
        if (activeChecks == null || activeChecks.isEmpty()) {
            return false;
        }
        for (TSqlCheck check : activeChecks) {
            if (SCHEMA_AWARE_RULE_KEYS.contains(check.getRuleKey())) {
                return true;
            }
        }
        return false;
    }

    // TRACEABILITY_MAPPING: REQ-005
    ProjectSchemaCatalog buildProjectSchema(Iterable<ParseResult<TSqlScript>> parsedResults) {
        return ProjectSchemaCatalog.fromParsedResults(parsedResults);
    }

    // TRACEABILITY_MAPPING: REQ-005
    private Map<InputFile, ParsedFile> parseInputFiles(List<InputFile> inputFiles) {
        Map<InputFile, ParsedFile> parsedFiles = new LinkedHashMap<>();
        for (InputFile inputFile : inputFiles) {
            if (inputFile == null) {
                continue;
            }
            try {
                String content = inputFile.contents();
                ParseResult<TSqlScript> result;
                try {
                    result = TSqlParser.parse(content);
                } catch (Exception e) {
                    LOG.warn("Unexpected parser exception on file: " + inputFile.uri(), e);
                    parsedFiles.put(inputFile, new ParsedFile(content, null, null, e));
                    continue;
                }
                parsedFiles.put(inputFile, new ParsedFile(content, result, null, null));
            } catch (IOException e) {
                LOG.error("Failed to read contents of file: " + inputFile.uri(), e);
                parsedFiles.put(inputFile, new ParsedFile(null, null, e, null));
            }
        }
        return parsedFiles;
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
    private void analyzeFile(InputFile inputFile, SensorContext context, List<TSqlCheck> activeChecks, ParsedFile parsedFile, ProjectSchemaCatalog projectSchema) {
        LOG.debug("Analyzing T-SQL file: {}", inputFile.uri());
        if (parsedFile == null) {
            return;
        }
        if (parsedFile.readException() != null) {
            return;
        }
        if (parsedFile.parseException() != null) {
            reportSyntaxError(inputFile, context, 1, "Failed to parse T-SQL script: " + parsedFile.parseException().getMessage());
            return;
        }
        if (parsedFile.result() == null) {
            return;
        }
        reportParseErrors(inputFile, context, parsedFile.result());
        publishHighlighting(inputFile, context, parsedFile.result(), parsedFile.content());
        publishSourceMetrics(inputFile, context, parsedFile.content());
        if (parsedFile.result().fragment() == null) {
            return;
        }
        executeChecks(inputFile, context, parsedFile.result().fragment(), parsedFile.content(), activeChecks, projectSchema);
    }

    private record ParsedFile(String content, ParseResult<TSqlScript> result, IOException readException, Exception parseException) {
    }

    // TRACEABILITY_MAPPING: REQ-005
    private void publishSourceMetrics(InputFile inputFile, SensorContext context, String content) {
        TSqlSourceMetrics metrics = TSqlSourceMetrics.fromText(content);
        saveMeasure(context, inputFile, CoreMetrics.NCLOC, metrics.nonCommentLines());
        saveMeasure(context, inputFile, CoreMetrics.COMMENT_LINES, metrics.commentLines());
        saveMeasure(context, inputFile, CoreMetrics.COMPLEXITY, metrics.complexity());
    }

    // TRACEABILITY_MAPPING: REQ-005
    private void saveMeasure(SensorContext context, InputFile inputFile,
            org.sonar.api.measures.Metric<Integer> metric, int value) {
        if (metric == null || context == null) {
            return;
        }
        NewMeasure<Integer> measure = context.newMeasure();
        if (measure == null) {
            return;
        }
        measure.on(inputFile)
                .forMetric(metric)
                .withValue(value)
                .save();
    }

    // TRACEABILITY_MAPPING: REQ-005
    private void reportParseErrors(InputFile inputFile, SensorContext context, ParseResult<TSqlScript> result) {
        if (result == null || !result.hasErrors() || result.errors() == null) {
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

        int[] lineStarts = buildLineStarts(content);
        int index = 0;
        while (index < content.length()) {
            char ch = content.charAt(index);
            if (Character.isWhitespace(ch)) {
                index++;
                continue;
            }
            int end = scanComment(content, index);
            if (end > index) {
                highlightRange(inputFile, highlighting, content, index, end, TypeOfText.COMMENT, lineStarts);
                index = end;
                continue;
            }
            end = scanStringToken(content, index);
            if (end > index) {
                highlightRange(inputFile, highlighting, content, index, end, TypeOfText.STRING, lineStarts);
                index = end;
                continue;
            }
            if (ch == '[') {
                int bracketEnd = index + 1;
                while (bracketEnd < content.length()) {
                    if (content.charAt(bracketEnd) == ']') {
                        if (bracketEnd + 1 < content.length() && content.charAt(bracketEnd + 1) == ']') {
                            bracketEnd += 2;
                            continue;
                        }
                        bracketEnd++;
                        break;
                    }
                    bracketEnd++;
                }
                highlightRange(inputFile, highlighting, content, index, bracketEnd, TypeOfText.KEYWORD_LIGHT, lineStarts);
                index = bracketEnd;
                continue;
            }
            if (isIdentifierStart(ch)) {
                int wordEnd = index + 1;
                while (wordEnd < content.length() && isIdentifierPart(content.charAt(wordEnd))) {
                    wordEnd++;
                }
                String word = content.substring(index, wordEnd);
                TypeOfText type = isKeyword(word) ? TypeOfText.KEYWORD : TypeOfText.KEYWORD_LIGHT;
                highlightRange(inputFile, highlighting, content, index, wordEnd, type, lineStarts);
                index = wordEnd;
                continue;
            }
            index++;
        }
    }

    // TRACEABILITY_MAPPING: REQ-005
    private static int scanComment(String content, int start) {
        if (content == null || start < 0 || start + 1 >= content.length()) {
            return start;
        }
        if (content.charAt(start) == '-' && content.charAt(start + 1) == '-') {
            int end = start + 2;
            while (end < content.length() && content.charAt(end) != '\n' && content.charAt(end) != '\r') {
                end++;
            }
            return end;
        }
        if (content.charAt(start) == '/' && content.charAt(start + 1) == '*') {
            int end = start + 2;
            while (end + 1 < content.length() && !(content.charAt(end) == '*' && content.charAt(end + 1) == '/')) {
                end++;
            }
            return (end + 1 < content.length()) ? end + 2 : content.length();
        }
        return start;
    }

    // TRACEABILITY_MAPPING: REQ-005
    private static int scanStringToken(String content, int start) {
        if (content == null || start < 0 || start >= content.length()) {
            return start;
        }
        char ch = content.charAt(start);
        if (ch == '\'') {
            return scanStringLiteral(content, start);
        }
        if ((ch == 'N' || ch == 'n') && start + 1 < content.length() && content.charAt(start + 1) == '\'') {
            return scanStringLiteral(content, start + 1);
        }
        return start;
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
    private static void highlightRange(InputFile inputFile, NewHighlighting highlighting, String content, int start, int end,
            TypeOfText type, int[] lineStarts) {
        if (inputFile == null || highlighting == null || content == null || start < 0 || end < start || end > content.length()) {
            return;
        }
        TextRange range = rangeForOffsets(start, end, inputFile, lineStarts);
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
        return safeNewRange(inputFile, startLine, startColumn, endLine, endColumn);
    }

    // TRACEABILITY_MAPPING: REQ-005
    private static TextRange rangeForOffsets(int start, int end, InputFile file, int[] lineStarts) {
        if (file == null || lineStarts == null || lineStarts.length == 0 || start < 0 || end < start) {
            return null;
        }
        int lineCount = lineStarts.length;

        int startLine = findLineForOffset(lineStarts, lineCount, start);
        int startColumn = start - lineStarts[startLine - 1];
        int endLine = findLineForOffset(lineStarts, lineCount, end);
        int endColumn = end - lineStarts[endLine - 1];

        return safeNewRange(file, startLine, startColumn, endLine, endColumn);
    }

    // TRACEABILITY_MAPPING: REQ-005
    private static int scanStringLiteral(String content, int quoteIndex) {
        if (content == null || quoteIndex < 0 || quoteIndex >= content.length() || content.charAt(quoteIndex) != '\'') {
            return quoteIndex;
        }
        int end = quoteIndex + 1;
        while (end < content.length()) {
            if (content.charAt(end) == '\'') {
                if (end + 1 < content.length() && content.charAt(end + 1) == '\'') {
                    end += 2;
                    continue;
                }
                return end + 1;
            }
            end++;
        }
        return content.length();
    }

    // TRACEABILITY_MAPPING: REQ-005
    private static int[] buildLineStarts(String content) {
        if (content == null || content.isEmpty()) {
            return new int[] {0};
        }
        List<Integer> starts = new ArrayList<>();
        starts.add(0);
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (ch == '\r') {
                if (i + 1 < content.length() && content.charAt(i + 1) == '\n') {
                    i++;
                }
                starts.add(i + 1);
            } else if (ch == '\n') {
                starts.add(i + 1);
            }
        }
        int[] lineStarts = new int[starts.size()];
        for (int i = 0; i < starts.size(); i++) {
            lineStarts[i] = starts.get(i);
        }
        return lineStarts;
    }

    // TRACEABILITY_MAPPING: REQ-005
    private static int findLineForOffset(int[] lineStarts, int lineCount, int offset) {
        int low = 0;
        int high = lineCount - 1;
        while (low <= high) {
            int mid = low + ((high - low) / 2);
            if (lineStarts[mid] <= offset) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return Math.max(1, low);
    }

    // TRACEABILITY_MAPPING: REQ-005
    private static TextRange safeNewRange(InputFile file, int startLine, int startColumn, int endLine, int endColumn) {
        try {
            return file.newRange(startLine, startColumn, endLine, endColumn);
        } catch (Exception ignored) {
            return null;
        }
    }

    // TRACEABILITY_MAPPING: REQ-005
    private void executeChecks(InputFile inputFile, SensorContext context, TSqlFragment rootFragment, String sourceText, List<TSqlCheck> activeChecks, ProjectSchemaCatalog projectSchema) {
        if (rootFragment == null || activeChecks.isEmpty()) {
            return;
        }
        ProjectSchemaCatalog schemaForChecks = projectSchema;
        try {
            projectSchema.indexScript(rootFragment);
        } catch (Exception e) {
            LOG.warn("Failed to index per-file schema scope for {}; continuing without schema metadata for this file",
                    inputFile != null ? inputFile.uri() : "<unknown>", e);
            schemaForChecks = ProjectSchemaCatalog.empty();
        }
        TSqlCheckContext checkContext = new SensorCheckContext(inputFile, context, rootFragment, sourceText, schemaForChecks);
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
