package io.github.fupacat.sonar.tsql;

import io.github.fupacat.ParseError;
import io.github.fupacat.ParseResult;
import io.github.fupacat.TSqlParser;
import io.github.fupacat.TSqlParserToken;
import io.github.fupacat.ast.TSqlFragment;
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

    @Override
    public void describe(SensorDescriptor descriptor) {
        descriptor.name("T-SQL ScriptDOM Sensor")
                .onlyOnLanguage(TSqlLanguage.KEY);
    }

    @Override
    public void execute(SensorContext context) {
        FileSystem fs = context.fileSystem();
        FilePredicate predicate = fs.predicates().hasLanguage(TSqlLanguage.KEY);
        Iterable<InputFile> inputFiles = fs.inputFiles(predicate);

        List<TSqlCheck> activeChecks = getActiveChecks(context);
        LOG.info("Analyzing T-SQL files with {} active rules", activeChecks.size());

        TSqlParser parser = new TSqlParser();

        for (InputFile inputFile : inputFiles) {
            analyzeFile(inputFile, context, parser, activeChecks);
        }
    }

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

    private void analyzeFile(InputFile inputFile, SensorContext context, TSqlParser parser, List<TSqlCheck> activeChecks) {
        LOG.debug("Analyzing T-SQL file: {}", inputFile.uri());
        String content;
        try {
            content = inputFile.contents();
        } catch (IOException e) {
            LOG.error("Failed to read contents of file: " + inputFile.uri(), e);
            return;
        }

        ParseResult result;
        try {
            result = parser.parse(content);
        } catch (Exception e) {
            LOG.warn("Unexpected parser exception on file: " + inputFile.uri(), e);
            reportSyntaxError(inputFile, context, 1, "Failed to parse T-SQL script: " + e.getMessage());
            return;
        }

        // Report syntax errors if any occurred
        if (result.hasErrors() && result.errors() != null) {
            for (ParseError error : result.errors()) {
                int line = Math.max(1, Math.min(error.line, inputFile.lines()));
                String msg = (error.message != null && !error.message.isBlank())
                        ? error.message
                        : "T-SQL syntax error";
                reportSyntaxError(inputFile, context, line, msg);
            }
        }

        // Run AST visitor checks
        TSqlFragment rootFragment = result.fragment();
        if (rootFragment != null && !activeChecks.isEmpty()) {
            TSqlCheckContext checkContext = new SensorCheckContext(inputFile, context);
            for (TSqlCheck check : activeChecks) {
                try {
                    check.scan(rootFragment, checkContext);
                } catch (Exception e) {
                    LOG.error("Error running check " + check.getRuleKey() + " on file " + inputFile.uri(), e);
                }
            }
        }
    }

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

    private static class SensorCheckContext implements TSqlCheckContext {
        private final InputFile inputFile;
        private final SensorContext sensorContext;

        public SensorCheckContext(InputFile inputFile, SensorContext sensorContext) {
            this.inputFile = inputFile;
            this.sensorContext = sensorContext;
        }

        @Override
        public InputFile getFile() {
            return inputFile;
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
        public void reportIssue(TSqlCheck check, int line, String message) {
            RuleKey ruleKey = RuleKey.of(TSqlRulesDefinition.REPOSITORY_KEY, check.getRuleKey());
            int validLine = Math.max(1, Math.min(line, inputFile.lines()));
            NewIssue issue = sensorContext.newIssue().forRule(ruleKey);
            NewIssueLocation location = issue.newLocation()
                    .on(inputFile)
                    .at(inputFile.selectLine(validLine))
                    .message(message);
            issue.at(location).save();
        }

        private int getStartLine(TSqlFragment fragment) {
            if (fragment != null && fragment.ScriptTokenStream != null
                    && fragment.firstTokenIndex >= 0 && fragment.firstTokenIndex < fragment.ScriptTokenStream.size()) {
                Object tokenObj = fragment.ScriptTokenStream.get(fragment.firstTokenIndex);
                if (tokenObj instanceof TSqlParserToken token) {
                    return Math.max(1, token.getLine());
                }
            }
            return 1;
        }

        private TextRange computeRange(TSqlFragment fragment, InputFile file) {
            if (fragment == null || fragment.ScriptTokenStream == null) {
                return null;
            }
            int firstIdx = fragment.firstTokenIndex;
            int lastIdx = fragment.lastTokenIndex;
            if (firstIdx < 0 || firstIdx >= fragment.ScriptTokenStream.size()) {
                return null;
            }
            if (lastIdx < firstIdx || lastIdx >= fragment.ScriptTokenStream.size()) {
                lastIdx = firstIdx;
            }

            Object startObj = fragment.ScriptTokenStream.get(firstIdx);
            Object endObj = fragment.ScriptTokenStream.get(lastIdx);

            if (startObj instanceof TSqlParserToken startToken && endObj instanceof TSqlParserToken endToken) {
                int startLine = startToken.getLine();
                int startCol = Math.max(0, startToken.getColumn() - 1);
                int endLine = endToken.getLine();
                int endCol = Math.max(startCol, endToken.getColumn() - 1 + (endToken.getText() != null ? endToken.getText().length() : 0));

                if (startLine >= 1 && endLine >= startLine && startLine <= file.lines() && endLine <= file.lines()) {
                    try {
                        return file.newRange(startLine, startCol, endLine, endCol);
                    } catch (Exception ignored) {
                        // If exact column bounds are out of range for whatever reason, fall back
                    }
                }
            }
            return null;
        }
    }
}
