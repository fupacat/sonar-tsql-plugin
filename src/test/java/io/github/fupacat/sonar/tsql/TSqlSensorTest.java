package io.github.fupacat.sonar.tsql;

import io.github.fupacat.TSqlParserToken;
import io.github.fupacat.TSqlTokenType;
import io.github.fupacat.ast.TSqlScript;
import io.github.fupacat.sonar.tsql.catalog.ProjectSchemaCatalog;
import io.github.fupacat.sonar.tsql.checks.AvoidSelectAllCheck;
import io.github.fupacat.sonar.tsql.checks.CheckList;
import io.github.fupacat.sonar.tsql.rules.TSqlRulesDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.batch.sensor.measure.NewMeasure;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.rule.RuleKey;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TSqlSensorTest {

    private TSqlSensor sensor;

    // TRACEABILITY_MAPPING: REQ-005
    @BeforeEach
    void setUp() {
        sensor = new TSqlSensor();
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void testDescriptor() {
        SensorDescriptor descriptor = mock(SensorDescriptor.class, org.mockito.Mockito.RETURNS_SELF);
        sensor.describe(descriptor);

        verify(descriptor).name("T-SQL ScriptDOM Sensor");
        verify(descriptor).onlyOnLanguage(TSqlLanguage.KEY);
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void testExecutesActiveRuleOnValidSql() throws IOException {
        RuleKey ruleKey = RuleKey.of(TSqlRulesDefinition.REPOSITORY_KEY, AvoidSelectAllCheck.RULE_KEY);

        SensorContext context = mock(SensorContext.class);
        FileSystem fs = mock(FileSystem.class);
        FilePredicates predicates = mock(FilePredicates.class);
        FilePredicate predicate = mock(FilePredicate.class);
        InputFile inputFile = mock(InputFile.class);
        ActiveRules activeRules = mock(ActiveRules.class);
        ActiveRule activeRule = mock(ActiveRule.class);

        when(context.fileSystem()).thenReturn(fs);
        when(fs.predicates()).thenReturn(predicates);
        when(predicates.hasLanguage(TSqlLanguage.KEY)).thenReturn(predicate);
        when(fs.inputFiles(predicate)).thenReturn(List.of(inputFile));

        when(inputFile.uri()).thenReturn(URI.create("file:///sample.sql"));
        when(inputFile.contents()).thenReturn("SELECT * FROM Customers;\n");
        when(inputFile.lines()).thenReturn(1);

        when(context.activeRules()).thenReturn(activeRules);
        when(activeRules.find(ruleKey)).thenReturn(activeRule);

        NewIssue issue = mock(NewIssue.class, org.mockito.Mockito.RETURNS_SELF);
        NewIssueLocation location = mock(NewIssueLocation.class, org.mockito.Mockito.RETURNS_SELF);
        when(context.newIssue()).thenReturn(issue);
        when(issue.newLocation()).thenReturn(location);

        sensor.execute(context);

        verify(issue).forRule(ruleKey);
        verify(issue).at(location);
        verify(issue).save();
        verify(location).on(inputFile);
        verify(location).message(argThat(msg -> msg != null && msg.contains("SELECT *")));
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void testReportsSyntaxError() throws IOException {
        RuleKey syntaxRuleKey = RuleKey.of(TSqlRulesDefinition.REPOSITORY_KEY, CheckList.SYNTAX_ERROR_RULE_KEY);

        SensorContext context = mock(SensorContext.class);
        FileSystem fs = mock(FileSystem.class);
        FilePredicates predicates = mock(FilePredicates.class);
        FilePredicate predicate = mock(FilePredicate.class);
        InputFile inputFile = mock(InputFile.class);
        ActiveRules activeRules = mock(ActiveRules.class);
        ActiveRule syntaxRule = mock(ActiveRule.class);

        when(context.fileSystem()).thenReturn(fs);
        when(fs.predicates()).thenReturn(predicates);
        when(predicates.hasLanguage(TSqlLanguage.KEY)).thenReturn(predicate);
        when(fs.inputFiles(predicate)).thenReturn(List.of(inputFile));

        when(inputFile.uri()).thenReturn(URI.create("file:///broken.sql"));
        when(inputFile.contents()).thenReturn("SELECT FROM WHERE;\n");
        when(inputFile.lines()).thenReturn(1);

        when(context.activeRules()).thenReturn(activeRules);
        when(activeRules.find(syntaxRuleKey)).thenReturn(syntaxRule);

        NewIssue issue = mock(NewIssue.class, org.mockito.Mockito.RETURNS_SELF);
        NewIssueLocation location = mock(NewIssueLocation.class, org.mockito.Mockito.RETURNS_SELF);
        when(context.newIssue()).thenReturn(issue);
        when(issue.newLocation()).thenReturn(location);

        sensor.execute(context);

        verify(issue, atLeastOnce()).forRule(syntaxRuleKey);
        verify(issue, atLeastOnce()).at(location);
        verify(issue, atLeastOnce()).save();
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void testPublishesSourceMetrics() throws IOException {
        SensorContext context = mock(SensorContext.class);
        FileSystem fs = mock(FileSystem.class);
        FilePredicates predicates = mock(FilePredicates.class);
        FilePredicate predicate = mock(FilePredicate.class);
        InputFile inputFile = mock(InputFile.class);
        ActiveRules activeRules = mock(ActiveRules.class);
        Map<Metric<?>, Integer> measureValues = new LinkedHashMap<>();

        when(context.fileSystem()).thenReturn(fs);
        when(fs.predicates()).thenReturn(predicates);
        when(predicates.hasLanguage(TSqlLanguage.KEY)).thenReturn(predicate);
        when(fs.inputFiles(predicate)).thenReturn(List.of(inputFile));
        when(context.activeRules()).thenReturn(activeRules);
        when(context.newMeasure()).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            NewMeasure<Integer> measure = mock(NewMeasure.class, org.mockito.Mockito.RETURNS_SELF);
            @SuppressWarnings("unchecked")
            Metric<Integer>[] metricHolder = new Metric[1];
            int[] valueHolder = new int[1];

            doAnswer(metricInvocation -> {
                metricHolder[0] = metricInvocation.getArgument(0);
                return measure;
            }).when(measure).forMetric(org.mockito.ArgumentMatchers.<Metric<Integer>>any());
            doAnswer(valueInvocation -> {
                valueHolder[0] = valueInvocation.getArgument(0);
                return measure;
            }).when(measure).withValue(org.mockito.ArgumentMatchers.anyInt());
            doAnswer(saveInvocation -> {
                measureValues.put(metricHolder[0], valueHolder[0]);
                return null;
            }).when(measure).save();

            return measure;
        });
        when(inputFile.uri()).thenReturn(URI.create("file:///metrics.sql"));
        when(inputFile.contents()).thenReturn("-- comment\nBEGIN\n  IF 1 = 1\n    SELECT 1;\n  END;\n\nBEGIN TRY\n  SELECT 1;\nEND TRY\nBEGIN CATCH\n  SELECT 2;\nEND CATCH;\n");
        when(inputFile.lines()).thenReturn(12);

        sensor.execute(context);

        assertThat(measureValues).containsEntry(CoreMetrics.NCLOC, 10);
        assertThat(measureValues).containsEntry(CoreMetrics.COMMENT_LINES, 1);
        assertThat(measureValues).containsEntry(CoreMetrics.STATEMENTS, 5);
        assertThat(measureValues).containsEntry(CoreMetrics.COMPLEXITY, 3);
        assertThat(measureValues).hasSize(4);
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void testHandlesIOExceptionGracefully() throws IOException {
        SensorContext context = mock(SensorContext.class);
        FileSystem fs = mock(FileSystem.class);
        FilePredicates predicates = mock(FilePredicates.class);
        FilePredicate predicate = mock(FilePredicate.class);
        InputFile inputFile = mock(InputFile.class);
        ActiveRules activeRules = mock(ActiveRules.class);

        when(context.fileSystem()).thenReturn(fs);
        when(fs.predicates()).thenReturn(predicates);
        when(predicates.hasLanguage(TSqlLanguage.KEY)).thenReturn(predicate);
        when(fs.inputFiles(predicate)).thenReturn(List.of(inputFile));
        when(context.activeRules()).thenReturn(activeRules);

        when(inputFile.uri()).thenReturn(URI.create("file:///unreadable.sql"));
        when(inputFile.contents()).thenThrow(new IOException("Disk error"));

        sensor.execute(context);

        verify(context, never()).newIssue();
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void testSensorCheckContextTokenRangeAndFallback() {
        InputFile inputFile = mock(InputFile.class);
        SensorContext sensorContext = mock(SensorContext.class);
        NewIssue issue = mock(NewIssue.class, org.mockito.Mockito.RETURNS_SELF);
        NewIssueLocation location = mock(NewIssueLocation.class, org.mockito.Mockito.RETURNS_SELF);
        TextRange expectedRange = mock(TextRange.class);

        when(sensorContext.newIssue()).thenReturn(issue);
        when(issue.newLocation()).thenReturn(location);
        when(inputFile.lines()).thenReturn(5);
        when(inputFile.newRange(1, 0, 1, 8)).thenReturn(expectedRange);

        TSqlScript root = new TSqlScript();
        TSqlParserToken tok0 = new TSqlParserToken(TSqlTokenType.Select, 0, "SELECT", 1, 1);
        TSqlParserToken tok1 = new TSqlParserToken(TSqlTokenType.Select, 7, "*", 1, 8);
        root.ScriptTokenStream = List.of(tok0, tok1);

        TSqlScript child = new TSqlScript();
        child.firstTokenIndex = 0;
        child.lastTokenIndex = 1;

        TSqlSensor.SensorCheckContext checkContext = new TSqlSensor.SensorCheckContext(inputFile, sensorContext, root, ProjectSchemaCatalog.empty());
        assertThat(checkContext.getFile()).isSameAs(inputFile);

        AvoidSelectAllCheck check = new AvoidSelectAllCheck();
        checkContext.reportIssue(check, child, "Avoid SELECT *");
        verify(location).at(expectedRange);

        // Out-of-bounds token line falls back to selectLine
        TSqlParserToken outOfBoundsTok = new TSqlParserToken(TSqlTokenType.Select, 0, "SELECT", 99, 1);
        TSqlScript fragmentWithOwnStream = new TSqlScript();
        fragmentWithOwnStream.ScriptTokenStream = List.of(outOfBoundsTok);
        fragmentWithOwnStream.firstTokenIndex = 0;
        fragmentWithOwnStream.lastTokenIndex = -1;
        checkContext.reportIssue(check, fragmentWithOwnStream, "Fallback line");
        verify(inputFile).selectLine(5);

        // Explicit reportIssueAtLine
        checkContext.reportIssueAtLine(check, 2, "Line 2 issue");
        verify(inputFile).selectLine(2);
    }
}
