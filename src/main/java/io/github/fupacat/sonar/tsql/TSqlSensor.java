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
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
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
        ProjectSchemaCatalog projectSchema = needsSchema
                ? ProjectSchemaCatalog.fromParsedResults(parsedFiles.values().stream()
                .map(ParsedFile::result)
                .filter(Objects::nonNull)
                .toList())
                : ProjectSchemaCatalog.empty();
        LOG.info("Analyzing T-SQL files with {} active rules{}", activeChecks.size(), needsSchema ? " and schema indexing" : "");

        for (InputFile inputFile : inputFiles) {
            ParsedFile parsedFile = parsedFiles.get(inputFile);
            if (parsedFile != null) {
                analyzeFile(inputFile, context, activeChecks, parsedFile, projectSchema);
            }
        }
    }

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

    private Map<InputFile, ParsedFile> parseInputFiles(List<InputFile> inputFiles) {
        Map<InputFile, ParsedFile> parsedFiles = new LinkedHashMap<>();
        for (InputFile inputFile : inputFiles) {
            if (inputFile == null) {
                continue;
            }
            try {
                String content = inputFile.contents();
                ParseResult<TSqlScript> result = TSqlParser.parse(content);
                parsedFiles.put(inputFile, new ParsedFile(content, result, null));
            } catch (IOException e) {
                LOG.error("Failed to read contents of file: " + inputFile.uri(), e);
                parsedFiles.put(inputFile, new ParsedFile(null, null, e));
            } catch (Exception e) {
                LOG.warn("Unexpected parser exception on file: " + inputFile.uri(), e);
                parsedFiles.put(inputFile, new ParsedFile(null, null, e));
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
        if (parsedFile.parseException() != null) {
            reportSyntaxError(inputFile, context, 1, "Failed to parse T-SQL script: " + parsedFile.parseException().getMessage());
            return;
        }
        if (parsedFile.result() == null) {
            return;
        }
        reportParseErrors(inputFile, context, parsedFile.result());
        if (parsedFile.result().fragment() == null) {
            return;
        }
        executeChecks(inputFile, context, parsedFile.result().fragment(), parsedFile.content(), activeChecks, projectSchema);
    }

    private record ParsedFile(String content, ParseResult<TSqlScript> result, Exception parseException) {
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
                return null;
            }
            TSqlParserToken firstToken = tokenStream.get(firstIdx) instanceof TSqlParserToken token ? token : null;
            TSqlParserToken lastToken = tokenStream.get(lastIdx) instanceof TSqlParserToken token ? token : null;
            if (firstToken == null || lastToken == null) {
                return null;
            }
            int startLine = Math.max(1, firstToken.getLine());
            int startColumn = Math.max(0, firstToken.getColumn() - 1);
            int endLine = Math.max(startLine, lastToken.getLine());
            int endColumn = Math.max(startColumn, lastToken.getColumn() - 1 + Math.max(1, lastToken.getText() == null ? 0 : lastToken.getText().length()));
            return file.newRange(startLine, startColumn, endLine, endColumn);
        }
    }
}
