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
import org.sonar.api.batch.sensor.cpd.NewCpdTokens;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
        publishCpdTokens(inputFile, context, content);
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
    private void publishCpdTokens(InputFile inputFile, SensorContext context, String sourceText) {
        if (sourceText == null || sourceText.isBlank()) {
            return;
        }
        NewCpdTokens cpdTokens = context.newCpdTokens();
        if (cpdTokens == null) {
            return;
        }
        cpdTokens.onFile(inputFile);
        for (CpdToken token : generateCpdTokens(sourceText)) {
            cpdTokens.addToken(token.startLine, token.startColumn, token.endLine, token.endColumn, token.normalized);
        }
        cpdTokens.save();
    }

    private static final Set<String> TSQL_KEYWORDS = Set.of(
            "SELECT", "FROM", "WHERE", "INSERT", "UPDATE", "DELETE",
            "CREATE", "ALTER", "DROP", "TABLE", "VIEW", "INDEX", "PROCEDURE", "FUNCTION",
            "TRIGGER", "DATABASE", "SCHEMA", "AS", "ON", "IN", "INTO", "VALUES", "SET",
            "BY", "GROUP", "ORDER", "HAVING", "JOIN", "INNER", "LEFT", "RIGHT", "FULL",
            "OUTER", "CROSS", "UNION", "ALL", "DISTINCT", "TOP", "WITH", "EXEC", "EXECUTE",
            "BEGIN", "END", "IF", "ELSE", "WHILE", "RETURN", "DECLARE", "CASE", "WHEN",
            "THEN", "AND", "OR", "NOT", "IS", "NULL", "LIKE", "BETWEEN", "EXISTS", "GO"
    );

    private static final class CpdToken {
        private final int startLine;
        private final int startColumn;
        private final int endLine;
        private final int endColumn;
        private final String normalized;

        private CpdToken(int startLine, int startColumn, int endLine, int endColumn, String normalized) {
            this.startLine = startLine;
            this.startColumn = startColumn;
            this.endLine = endLine;
            this.endColumn = endColumn;
            this.normalized = normalized;
        }
    }

    // TRACEABILITY_MAPPING: REQ-005
    private List<CpdToken> generateCpdTokens(String sourceText) {
        List<CpdToken> tokens = new ArrayList<>();
        List<Integer> lineStarts = computeLineStarts(sourceText);
        int index = 0;
        while (index < sourceText.length()) {
            index = skipIgnoredContent(sourceText, index);
            if (index >= sourceText.length()) {
                break;
            }
            TokenMatch match = readNextToken(sourceText, lineStarts, index);
            if (match == null) {
                index++;
                continue;
            }
            tokens.add(match.token);
            index = match.nextIndex;
        }
        return tokens;
    }

    // TRACEABILITY_MAPPING: REQ-005
    private int skipIgnoredContent(String sourceText, int index) {
        while (index < sourceText.length()) {
            char ch = sourceText.charAt(index);
            if (Character.isWhitespace(ch)) {
                index++;
                continue;
            }
            if (ch == '-' && index + 1 < sourceText.length() && sourceText.charAt(index + 1) == '-') {
                index = skipSingleLineComment(sourceText, index);
                continue;
            }
            if (ch == '/' && index + 1 < sourceText.length() && sourceText.charAt(index + 1) == '*') {
                index = skipMultiLineComment(sourceText, index);
                continue;
            }
            break;
        }
        return index;
    }

    // TRACEABILITY_MAPPING: REQ-005
    private TokenMatch readNextToken(String sourceText, List<Integer> lineStarts, int index) {
        char ch = sourceText.charAt(index);
        if (isStringTokenStart(sourceText, index)) {
            return consumeStringToken(sourceText, lineStarts, index);
        }
        if (ch == '[') {
            return consumeDelimitedIdentifierToken(sourceText, lineStarts, index);
        }
        if (isIdentifierStart(ch)) {
            return consumeIdentifierToken(sourceText, lineStarts, index);
        }
        if (isNumberStart(sourceText, index)) {
            return consumeNumberToken(sourceText, lineStarts, index);
        }
        String operator = readOperator(sourceText, index);
        if (operator != null) {
            int end = index + operator.length();
            return new TokenMatch(toToken(lineStarts, index, end, operator), end);
        }
        return null;
    }

    // TRACEABILITY_MAPPING: REQ-005
    private boolean isStringTokenStart(String sourceText, int index) {
        char ch = sourceText.charAt(index);
        if (ch == '\'') {
            return true;
        }
        return (ch == 'N' || ch == 'n') && index + 1 < sourceText.length() && sourceText.charAt(index + 1) == '\'';
    }

    // TRACEABILITY_MAPPING: REQ-005
    private TokenMatch consumeStringToken(String sourceText, List<Integer> lineStarts, int index) {
        int start = index;
        int end = skipStringLiteral(sourceText, index + (sourceText.charAt(index) == '\'' ? 0 : 1));
        return new TokenMatch(toToken(lineStarts, start, end, "<string>"), end);
    }

    // TRACEABILITY_MAPPING: REQ-005
    private TokenMatch consumeDelimitedIdentifierToken(String sourceText, List<Integer> lineStarts, int index) {
        int start = index;
        int end = skipDelimitedIdentifier(sourceText, index);
        String normalized = normalizeIdentifier(sourceText.substring(start, end));
        if (normalized == null) {
            return null;
        }
        return new TokenMatch(toToken(lineStarts, start, end, normalized), end);
    }

    // TRACEABILITY_MAPPING: REQ-005
    private TokenMatch consumeIdentifierToken(String sourceText, List<Integer> lineStarts, int index) {
        int start = index;
        int end = index + 1;
        while (end < sourceText.length() && isIdentifierPart(sourceText.charAt(end))) {
            end++;
        }
        String normalized = normalizeIdentifier(sourceText.substring(start, end));
        if (normalized == null) {
            return null;
        }
        return new TokenMatch(toToken(lineStarts, start, end, normalized), end);
    }

    // TRACEABILITY_MAPPING: REQ-005
    private boolean isNumberStart(String sourceText, int index) {
        char ch = sourceText.charAt(index);
        return Character.isDigit(ch) || (ch == '.' && index + 1 < sourceText.length() && Character.isDigit(sourceText.charAt(index + 1)));
    }

    // TRACEABILITY_MAPPING: REQ-005
    private TokenMatch consumeNumberToken(String sourceText, List<Integer> lineStarts, int index) {
        int start = index;
        int end = skipNumberLiteral(sourceText, index);
        return new TokenMatch(toToken(lineStarts, start, end, "<number>"), end);
    }

    // TRACEABILITY_MAPPING: REQ-005
    private static final class TokenMatch {
        private final CpdToken token;
        private final int nextIndex;

        private TokenMatch(CpdToken token, int nextIndex) {
            this.token = token;
            this.nextIndex = nextIndex;
        }
    }

    // TRACEABILITY_MAPPING: REQ-005
    private int skipSingleLineComment(String sourceText, int index) {
        while (index < sourceText.length() && sourceText.charAt(index) != '\n') {
            index++;
        }
        return index;
    }

    // TRACEABILITY_MAPPING: REQ-005
    private int skipMultiLineComment(String sourceText, int index) {
        int depth = 1;
        index += 2;
        while (index + 1 < sourceText.length()) {
            if (sourceText.charAt(index) == '/' && sourceText.charAt(index + 1) == '*') {
                depth++;
                index += 2;
                continue;
            }
            if (sourceText.charAt(index) == '*' && sourceText.charAt(index + 1) == '/') {
                depth--;
                index += 2;
                if (depth == 0) {
                    return index;
                }
                continue;
            }
            index++;
        }
        return sourceText.length();
    }

    // TRACEABILITY_MAPPING: REQ-005
    private int skipStringLiteral(String sourceText, int index) {
        index++;
        while (index < sourceText.length()) {
            char ch = sourceText.charAt(index);
            if (ch == '\'') {
                if (index + 1 < sourceText.length() && sourceText.charAt(index + 1) == '\'') {
                    index += 2;
                    continue;
                }
                return index + 1;
            }
            index++;
        }
        return sourceText.length();
    }

    // TRACEABILITY_MAPPING: REQ-005
    private int skipDelimitedIdentifier(String sourceText, int index) {
        index++;
        while (index < sourceText.length()) {
            char ch = sourceText.charAt(index);
            if (ch == ']') {
                if (index + 1 < sourceText.length() && sourceText.charAt(index + 1) == ']') {
                    index += 2;
                    continue;
                }
                return index + 1;
            }
            index++;
        }
        return sourceText.length();
    }

    // TRACEABILITY_MAPPING: REQ-005
    private int skipNumberLiteral(String sourceText, int index) {
        int i = index;
        if (sourceText.charAt(i) == '.') {
            i++;
        }
        while (i < sourceText.length() && Character.isDigit(sourceText.charAt(i))) {
            i++;
        }
        if (i < sourceText.length() && sourceText.charAt(i) == '.'
                && i + 1 < sourceText.length() && Character.isDigit(sourceText.charAt(i + 1))) {
            i++;
            while (i < sourceText.length() && Character.isDigit(sourceText.charAt(i))) {
                i++;
            }
        }
        if (i < sourceText.length() && (sourceText.charAt(i) == 'e' || sourceText.charAt(i) == 'E')) {
            int expIndex = i + 1;
            if (expIndex < sourceText.length() && (sourceText.charAt(expIndex) == '+' || sourceText.charAt(expIndex) == '-')) {
                expIndex++;
            }
            if (expIndex < sourceText.length() && Character.isDigit(sourceText.charAt(expIndex))) {
                i = expIndex;
                while (i < sourceText.length() && Character.isDigit(sourceText.charAt(i))) {
                    i++;
                }
            }
        }
        return i;
    }

    // TRACEABILITY_MAPPING: REQ-005
    private String normalizeIdentifier(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).replace("]]", "]");
        }
        String upper = trimmed.toUpperCase(Locale.ROOT);
        if (TSQL_KEYWORDS.contains(upper)) {
            return upper;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    // TRACEABILITY_MAPPING: REQ-005
    private boolean isIdentifierStart(char ch) {
        return Character.isLetter(ch) || ch == '_' || ch == '@' || ch == '#';
    }

    // TRACEABILITY_MAPPING: REQ-005
    private boolean isIdentifierPart(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '_' || ch == '$';
    }

    // TRACEABILITY_MAPPING: REQ-005
    private String readOperator(String sourceText, int index) {
        if (index >= sourceText.length()) {
            return null;
        }
        String[] operators = {
                "<=", ">=", "<>", "!=", ":=", "||", "&&",
                "+", "-", "*", "/", "%", "=", "<", ">", "|", "&", "^", ",", ".", "(", ")", ";", "[", "]", "{", "}", ":"
        };
        for (String operator : operators) {
            if (sourceText.startsWith(operator, index)) {
                return operator;
            }
        }
        return null;
    }

    // TRACEABILITY_MAPPING: REQ-005
    private CpdToken toToken(List<Integer> lineStarts, int start, int end, String normalized) {
        int startLine = lineNumberForOffset(lineStarts, start);
        int endLine = lineNumberForOffset(lineStarts, end);
        int startColumn = start - lineStarts.get(startLine - 1);
        int endColumn = end - lineStarts.get(endLine - 1);
        return new CpdToken(startLine, startColumn, endLine, endColumn, normalized);
    }

    // TRACEABILITY_MAPPING: REQ-005
    private List<Integer> computeLineStarts(String text) {
        List<Integer> lineStarts = new ArrayList<>();
        lineStarts.add(0);
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lineStarts.add(i + 1);
            }
        }
        return lineStarts;
    }

    // TRACEABILITY_MAPPING: REQ-005
    private int lineNumberForOffset(List<Integer> lineStarts, int offset) {
        int low = 0;
        int high = lineStarts.size() - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (lineStarts.get(mid) <= offset) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return Math.max(1, low);
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
