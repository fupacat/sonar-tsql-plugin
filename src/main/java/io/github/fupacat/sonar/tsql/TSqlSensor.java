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
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.batch.sensor.measure.NewMeasure;
import org.sonar.api.measures.CoreMetrics;
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
        publishSourceMetrics(inputFile, context, content);
        executeChecks(inputFile, context, result.fragment(), content, activeChecks, projectSchema);
    }

    // TRACEABILITY_MAPPING: REQ-005
    private void publishSourceMetrics(InputFile inputFile, SensorContext context, String content) {
        TSqlSourceMetrics metrics = TSqlSourceMetrics.fromText(content);
        saveMeasure(context, inputFile, CoreMetrics.NCLOC, metrics.nonCommentLines());
        saveMeasure(context, inputFile, CoreMetrics.COMMENT_LINES, metrics.commentLines());
        saveMeasure(context, inputFile, CoreMetrics.STATEMENTS, metrics.statements());
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
