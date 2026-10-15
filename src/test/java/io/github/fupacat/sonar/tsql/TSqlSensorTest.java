package io.github.fupacat.sonar.tsql;

import org.sonar.api.batch.sensor.measure.NewMeasure;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.mockito.Mockito.doAnswer;
import io.github.fupacat.ParseResult;
import io.github.fupacat.TSqlParserToken;
import io.github.fupacat.TSqlTokenType;
import io.github.fupacat.ast.TSqlScript;
import io.github.fupacat.sonar.tsql.catalog.ProjectSchemaCatalog;
import io.github.fupacat.sonar.tsql.checks.AvoidSelectAllCheck;
import io.github.fupacat.sonar.tsql.checks.CheckList;
import io.github.fupacat.sonar.tsql.checks.InPredicateIndexedColumnCheck;
import io.github.fupacat.sonar.tsql.checks.TSqlCheck;
import io.github.fupacat.sonar.tsql.checks.TSqlCheckContext;
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
import org.sonar.api.batch.sensor.cpd.NewCpdTokens;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
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
    void testSkipsSchemaCatalogWhenOnlyNonSchemaRulesAreActive() throws IOException {
        RuleKey ruleKey = RuleKey.of(TSqlRulesDefinition.REPOSITORY_KEY, AvoidSelectAllCheck.RULE_KEY);
        TSqlSensor spySensor = spy(new TSqlSensor());

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

        spySensor.execute(context);

        verify(spySensor, never()).buildProjectSchema(any());
        verify(inputFile, times(1)).contents();
        verify(issue).forRule(ruleKey);
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void testParsesEachFileOnceWhenSchemaChecksAreActive() throws IOException {
        RuleKey schemaRuleKey = RuleKey.of(TSqlRulesDefinition.REPOSITORY_KEY, InPredicateIndexedColumnCheck.RULE_KEY);

        SensorContext context = mock(SensorContext.class);
        FileSystem fs = mock(FileSystem.class);
        FilePredicates predicates = mock(FilePredicates.class);
        FilePredicate predicate = mock(FilePredicate.class);
        InputFile ddlFile = mock(InputFile.class);
        InputFile queryFile = mock(InputFile.class);
        ActiveRules activeRules = mock(ActiveRules.class);
        ActiveRule schemaRule = mock(ActiveRule.class);
        TextRange ddlLine = mock(TextRange.class);
        TextRange queryLine = mock(TextRange.class);

        when(context.fileSystem()).thenReturn(fs);
        when(fs.predicates()).thenReturn(predicates);
        when(predicates.hasLanguage(TSqlLanguage.KEY)).thenReturn(predicate);
        when(fs.inputFiles(predicate)).thenReturn(List.of(ddlFile, queryFile));
        when(context.activeRules()).thenReturn(activeRules);
        when(activeRules.find(schemaRuleKey)).thenReturn(schemaRule);

        when(ddlFile.uri()).thenReturn(URI.create("file:///ddl.sql"));
        when(ddlFile.lines()).thenReturn(1);
        when(ddlFile.selectLine(1)).thenReturn(ddlLine);
        when(ddlFile.contents()).thenReturn("CREATE TABLE dbo.Orders (OrderId INT NOT NULL, CustomerId INT NOT NULL);");

        when(queryFile.uri()).thenReturn(URI.create("file:///query.sql"));
        when(queryFile.lines()).thenReturn(1);
        when(queryFile.selectLine(1)).thenReturn(queryLine);
        when(queryFile.contents()).thenReturn("SELECT OrderId FROM dbo.Orders WHERE CustomerId IN (1, 2);");

        NewIssue issue = mock(NewIssue.class, org.mockito.Mockito.RETURNS_SELF);
        NewIssueLocation location = mock(NewIssueLocation.class, org.mockito.Mockito.RETURNS_SELF);
        when(context.newIssue()).thenReturn(issue);
        when(issue.newLocation()).thenReturn(location);

        sensor.execute(context);

        verify(ddlFile, times(1)).contents();
        verify(queryFile, times(1)).contents();
        verify(issue, atLeastOnce()).forRule(schemaRuleKey);
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void testSchemaAwareCheckUsesCrossFileDdlMetadata() throws IOException {
        RuleKey schemaRuleKey = RuleKey.of(TSqlRulesDefinition.REPOSITORY_KEY, InPredicateIndexedColumnCheck.RULE_KEY);

        SensorContext context = mock(SensorContext.class);
        FileSystem fs = mock(FileSystem.class);
        FilePredicates predicates = mock(FilePredicates.class);
        FilePredicate predicate = mock(FilePredicate.class);
        InputFile ddlFile = mock(InputFile.class);
        InputFile queryFile = mock(InputFile.class);
        ActiveRules activeRules = mock(ActiveRules.class);
        ActiveRule schemaRule = mock(ActiveRule.class);
        TextRange ddlLine = mock(TextRange.class);
        TextRange queryLine = mock(TextRange.class);

        when(context.fileSystem()).thenReturn(fs);
        when(fs.predicates()).thenReturn(predicates);
        when(predicates.hasLanguage(TSqlLanguage.KEY)).thenReturn(predicate);
        when(fs.inputFiles(predicate)).thenReturn(List.of(ddlFile, queryFile));
        when(context.activeRules()).thenReturn(activeRules);
        when(activeRules.find(schemaRuleKey)).thenReturn(schemaRule);

        when(ddlFile.uri()).thenReturn(URI.create("file:///ddl.sql"));
        when(ddlFile.lines()).thenReturn(1);
        when(ddlFile.selectLine(1)).thenReturn(ddlLine);
        when(ddlFile.contents()).thenReturn("CREATE TABLE dbo.Orders (OrderId INT NOT NULL, CustomerId INT NOT NULL);");

        when(queryFile.uri()).thenReturn(URI.create("file:///query.sql"));
        when(queryFile.lines()).thenReturn(1);
        when(queryFile.selectLine(1)).thenReturn(queryLine);
        when(queryFile.contents()).thenReturn("SELECT OrderId FROM dbo.Orders WHERE CustomerId IN (1, 2);");

        NewIssue issue = mock(NewIssue.class, org.mockito.Mockito.RETURNS_SELF);
        NewIssueLocation location = mock(NewIssueLocation.class, org.mockito.Mockito.RETURNS_SELF);
        when(context.newIssue()).thenReturn(issue);
        when(issue.newLocation()).thenReturn(location);

        sensor.execute(context);

        verify(issue, atLeastOnce()).forRule(schemaRuleKey);
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void testContinuesAnalysisWhenCatalogBuildFails() throws IOException {
        RuleKey schemaRuleKey = RuleKey.of(TSqlRulesDefinition.REPOSITORY_KEY, InPredicateIndexedColumnCheck.RULE_KEY);
        RuleKey selectAllRuleKey = RuleKey.of(TSqlRulesDefinition.REPOSITORY_KEY, AvoidSelectAllCheck.RULE_KEY);
        TSqlSensor spySensor = spy(new TSqlSensor());

        SensorContext context = mock(SensorContext.class);
        FileSystem fs = mock(FileSystem.class);
        FilePredicates predicates = mock(FilePredicates.class);
        FilePredicate predicate = mock(FilePredicate.class);
        InputFile inputFile = mock(InputFile.class);
        ActiveRules activeRules = mock(ActiveRules.class);
        ActiveRule schemaRule = mock(ActiveRule.class);
        ActiveRule selectAllRule = mock(ActiveRule.class);
        TextRange lineRange = mock(TextRange.class);

        when(context.fileSystem()).thenReturn(fs);
        when(fs.predicates()).thenReturn(predicates);
        when(predicates.hasLanguage(TSqlLanguage.KEY)).thenReturn(predicate);
        when(fs.inputFiles(predicate)).thenReturn(List.of(inputFile));
        when(context.activeRules()).thenReturn(activeRules);
        when(activeRules.find(schemaRuleKey)).thenReturn(schemaRule);
        when(activeRules.find(selectAllRuleKey)).thenReturn(selectAllRule);

        when(inputFile.uri()).thenReturn(URI.create("file:///sample.sql"));
        when(inputFile.lines()).thenReturn(1);
        when(inputFile.selectLine(1)).thenReturn(lineRange);
        when(inputFile.contents()).thenReturn("SELECT * FROM dbo.Users;");

        doThrow(new RuntimeException("catalog failure")).when(spySensor).buildProjectSchema(any());

        NewIssue issue = mock(NewIssue.class, org.mockito.Mockito.RETURNS_SELF);
        NewIssueLocation location = mock(NewIssueLocation.class, org.mockito.Mockito.RETURNS_SELF);
        when(context.newIssue()).thenReturn(issue);
        when(issue.newLocation()).thenReturn(location);

        spySensor.execute(context);

        verify(issue, atLeastOnce()).forRule(selectAllRuleKey);
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void testHandlesIOExceptionGracefullyEvenWithSyntaxRuleActive() throws IOException {
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
        when(context.activeRules()).thenReturn(activeRules);
        when(activeRules.find(syntaxRuleKey)).thenReturn(syntaxRule);

        when(inputFile.uri()).thenReturn(URI.create("file:///unreadable.sql"));
        when(inputFile.contents()).thenThrow(new IOException("Disk error"));

        sensor.execute(context);

        verify(context, never()).newIssue();
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void testPublishesNormalizedCpdTokens() throws IOException {
        SensorContext context = mock(SensorContext.class);
        FileSystem fs = mock(FileSystem.class);
        FilePredicates predicates = mock(FilePredicates.class);
        FilePredicate predicate = mock(FilePredicate.class);
        InputFile inputFile = mock(InputFile.class);
        ActiveRules activeRules = mock(ActiveRules.class);
        NewCpdTokens cpdTokens = mock(NewCpdTokens.class, org.mockito.Mockito.RETURNS_SELF);

        when(context.fileSystem()).thenReturn(fs);
        when(fs.predicates()).thenReturn(predicates);
        when(predicates.hasLanguage(TSqlLanguage.KEY)).thenReturn(predicate);
        when(fs.inputFiles(predicate)).thenReturn(List.of(inputFile));
        when(context.activeRules()).thenReturn(activeRules);
        when(context.newCpdTokens()).thenReturn(cpdTokens);

        when(inputFile.uri()).thenReturn(URI.create("file:///cpd.sql"));
        when(inputFile.contents()).thenReturn("SELECT firstName, N'Bob', 42 FROM dbo.Users;\n");
        when(inputFile.lines()).thenReturn(1);

        sensor.execute(context);

        verify(cpdTokens).onFile(inputFile);
        verify(cpdTokens).addToken(1, 0, 1, 6, "SELECT");
        verify(cpdTokens).addToken(1, 7, 1, 16, "firstname");
        verify(cpdTokens).addToken(1, 16, 1, 17, ",");
        verify(cpdTokens).addToken(1, 18, 1, 24, "<string>");
        verify(cpdTokens).addToken(1, 24, 1, 25, ",");
        verify(cpdTokens).addToken(1, 26, 1, 28, "<number>");
        verify(cpdTokens).addToken(1, 29, 1, 33, "FROM");
        verify(cpdTokens).addToken(1, 34, 1, 37, "dbo");
        verify(cpdTokens).addToken(1, 37, 1, 38, ".");
        verify(cpdTokens).addToken(1, 38, 1, 43, "users");
        verify(cpdTokens).addToken(1, 43, 1, 44, ";");
        verify(cpdTokens).save();
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void testPublishesNormalizedCpdTokensWithArithmeticAndQuotedIdentifiers() throws IOException {
        SensorContext context = mock(SensorContext.class);
        FileSystem fs = mock(FileSystem.class);
        FilePredicates predicates = mock(FilePredicates.class);
        FilePredicate predicate = mock(FilePredicate.class);
        InputFile inputFile = mock(InputFile.class);
        ActiveRules activeRules = mock(ActiveRules.class);
        NewCpdTokens cpdTokens = mock(NewCpdTokens.class, org.mockito.Mockito.RETURNS_SELF);

        when(context.fileSystem()).thenReturn(fs);
        when(fs.predicates()).thenReturn(predicates);
        when(predicates.hasLanguage(TSqlLanguage.KEY)).thenReturn(predicate);
        when(fs.inputFiles(predicate)).thenReturn(List.of(inputFile));
        when(context.activeRules()).thenReturn(activeRules);
        when(context.newCpdTokens()).thenReturn(cpdTokens);
        when(inputFile.uri()).thenReturn(URI.create("file:///cpd-arith.sql"));
        when(inputFile.contents()).thenReturn("SELECT a+b, 1e2, [First Name], /*c*/ a-b FROM t;\n");
        when(inputFile.lines()).thenReturn(1);

        sensor.execute(context);

        verify(cpdTokens).addToken(1, 0, 1, 6, "SELECT");
        verify(cpdTokens).addToken(1, 7, 1, 8, "a");
        verify(cpdTokens).addToken(1, 8, 1, 9, "+");
        verify(cpdTokens).addToken(1, 9, 1, 10, "b");
        verify(cpdTokens).addToken(1, 10, 1, 11, ",");
        verify(cpdTokens).addToken(1, 12, 1, 15, "<number>");
        verify(cpdTokens).addToken(1, 17, 1, 29, "first name");
        verify(cpdTokens).addToken(1, 29, 1, 30, ",");
        verify(cpdTokens).addToken(1, 37, 1, 38, "a");
        verify(cpdTokens).addToken(1, 38, 1, 39, "-");
        verify(cpdTokens).addToken(1, 39, 1, 40, "b");
        verify(cpdTokens).addToken(1, 41, 1, 45, "FROM");
        verify(cpdTokens).addToken(1, 46, 1, 47, "t");
        verify(cpdTokens).addToken(1, 47, 1, 48, ";");
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void testPublishesMultilineCpdTokensWithStableRanges() throws IOException {
        SensorContext context = mock(SensorContext.class);
        FileSystem fs = mock(FileSystem.class);
        FilePredicates predicates = mock(FilePredicates.class);
        FilePredicate predicate = mock(FilePredicate.class);
        InputFile inputFile = mock(InputFile.class);
        ActiveRules activeRules = mock(ActiveRules.class);
        NewCpdTokens cpdTokens = mock(NewCpdTokens.class, org.mockito.Mockito.RETURNS_SELF);

        when(context.fileSystem()).thenReturn(fs);
        when(fs.predicates()).thenReturn(predicates);
        when(predicates.hasLanguage(TSqlLanguage.KEY)).thenReturn(predicate);
        when(fs.inputFiles(predicate)).thenReturn(List.of(inputFile));
        when(context.activeRules()).thenReturn(activeRules);
        when(context.newCpdTokens()).thenReturn(cpdTokens);
        when(inputFile.uri()).thenReturn(URI.create("file:///cpd-multiline.sql"));
        when(inputFile.contents()).thenReturn("SELECT 1 +\n  2 FROM t;\n");
        when(inputFile.lines()).thenReturn(2);

        sensor.execute(context);

        verify(cpdTokens).addToken(1, 0, 1, 6, "SELECT");
        verify(cpdTokens).addToken(1, 7, 1, 8, "<number>");
        verify(cpdTokens).addToken(1, 9, 1, 10, "+");
        verify(cpdTokens).addToken(2, 2, 2, 3, "<number>");
        verify(cpdTokens).addToken(2, 4, 2, 8, "FROM");
        verify(cpdTokens).addToken(2, 9, 2, 10, "t");
        verify(cpdTokens).addToken(2, 10, 2, 11, ";");
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void testPublishesCpdTokensWithAtSignAndHashIdentifiersAndCrLfRanges() throws IOException {
        SensorContext context = mock(SensorContext.class);
        FileSystem fs = mock(FileSystem.class);
        FilePredicates predicates = mock(FilePredicates.class);
        FilePredicate predicate = mock(FilePredicate.class);
        InputFile inputFile = mock(InputFile.class);
        ActiveRules activeRules = mock(ActiveRules.class);
        NewCpdTokens cpdTokens = mock(NewCpdTokens.class, org.mockito.Mockito.RETURNS_SELF);

        when(context.fileSystem()).thenReturn(fs);
        when(fs.predicates()).thenReturn(predicates);
        when(predicates.hasLanguage(TSqlLanguage.KEY)).thenReturn(predicate);
        when(fs.inputFiles(predicate)).thenReturn(List.of(inputFile));
        when(context.activeRules()).thenReturn(activeRules);
        when(context.newCpdTokens()).thenReturn(cpdTokens);
        when(inputFile.uri()).thenReturn(URI.create("file:///cpd-identifier.sql"));
        when(inputFile.contents()).thenReturn("SELECT @@ROWCOUNT +\r\n  #Temp FROM t;\n");
        when(inputFile.lines()).thenReturn(2);

        sensor.execute(context);

        verify(cpdTokens).addToken(1, 0, 1, 6, "SELECT");
        verify(cpdTokens).addToken(1, 7, 1, 17, "@@rowcount");
        verify(cpdTokens).addToken(1, 18, 1, 19, "+");
        verify(cpdTokens).addToken(2, 2, 2, 7, "#temp");
        verify(cpdTokens).addToken(2, 8, 2, 12, "FROM");
        verify(cpdTokens).addToken(2, 13, 2, 14, "t");
        verify(cpdTokens).addToken(2, 14, 2, 15, ";");
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

        // newRange failure falls back to selectLine
        TSqlParserToken badStart = new TSqlParserToken(TSqlTokenType.Select, 0, "SELECT", 2, 1);
        TSqlParserToken badEnd = new TSqlParserToken(TSqlTokenType.Select, 7, "*", 2, 8);
        TSqlScript fragmentWithThrowingRange = new TSqlScript();
        fragmentWithThrowingRange.ScriptTokenStream = List.of(badStart, badEnd);
        fragmentWithThrowingRange.firstTokenIndex = 0;
        fragmentWithThrowingRange.lastTokenIndex = 1;
        when(inputFile.newRange(2, 0, 2, 8)).thenThrow(new IllegalArgumentException("invalid range"));
        checkContext.reportIssue(check, fragmentWithThrowingRange, "Fallback when newRange throws");
        verify(inputFile).selectLine(2);

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
        verify(inputFile, atLeastOnce()).selectLine(2);
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void testPublishesHighlightingForKeywordsStringsAndComments() throws IOException {
        SensorContext context = mock(SensorContext.class);
        FileSystem fs = mock(FileSystem.class);
        FilePredicates predicates = mock(FilePredicates.class);
        FilePredicate predicate = mock(FilePredicate.class);
        InputFile inputFile = mock(InputFile.class);
        NewHighlighting highlighting = mock(NewHighlighting.class, org.mockito.Mockito.RETURNS_SELF);
        TextRange keywordRange = mock(TextRange.class);
        TextRange stringRange = mock(TextRange.class);
        TextRange commentRange = mock(TextRange.class);

        when(context.fileSystem()).thenReturn(fs);
        when(fs.predicates()).thenReturn(predicates);
        when(predicates.hasLanguage(TSqlLanguage.KEY)).thenReturn(predicate);
        when(fs.inputFiles(predicate)).thenReturn(List.of(inputFile));
        when(context.activeRules()).thenReturn(mock(ActiveRules.class));
        when(context.newHighlighting()).thenReturn(highlighting);
        when(inputFile.uri()).thenReturn(URI.create("file:///highlight.sql"));
        when(inputFile.contents()).thenReturn("SELECT 'x';\n-- comment\n");
        when(inputFile.lines()).thenReturn(3);
        when(inputFile.newRange(1, 0, 1, 6)).thenReturn(keywordRange);
        when(inputFile.newRange(1, 7, 1, 10)).thenReturn(stringRange);
        when(inputFile.newRange(2, 0, 2, 10)).thenReturn(commentRange);

        sensor.execute(context);

        verify(highlighting).onFile(inputFile);
        verify(highlighting, atLeastOnce()).highlight(any(TextRange.class), eq(TypeOfText.KEYWORD));
        verify(highlighting, atLeastOnce()).highlight(any(TextRange.class), eq(TypeOfText.STRING));
        verify(highlighting, atLeastOnce()).highlight(any(TextRange.class), eq(TypeOfText.COMMENT));
        verify(highlighting, atLeastOnce()).save();
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void testTokenHighlightingSupportsMultilineRanges() throws Exception {
        InputFile inputFile = mock(InputFile.class);
        NewHighlighting highlighting = mock(NewHighlighting.class, org.mockito.Mockito.RETURNS_SELF);
        TextRange keywordRange = mock(TextRange.class);
        TextRange stringRange = mock(TextRange.class);
        TextRange commentRange = mock(TextRange.class);

        when(inputFile.lines()).thenReturn(6);
        when(inputFile.newRange(1, 0, 1, 6)).thenReturn(keywordRange);
        when(inputFile.newRange(2, 0, 3, 2)).thenReturn(stringRange);
        when(inputFile.newRange(4, 0, 4, 10)).thenReturn(commentRange);

        TSqlParserToken keywordToken = new TSqlParserToken(TSqlTokenType.Select, 0, "SELECT", 1, 1);
        TSqlParserToken multilineStringToken = new TSqlParserToken(TSqlTokenType.UnicodeStringLiteral, 0, "'a\nb'", 2, 1);
        TSqlParserToken commentToken = new TSqlParserToken(TSqlTokenType.SingleLineComment, 0, "-- comment", 4, 1);

        invokePrivate("publishHighlightingFromTokens", new Class<?>[] {InputFile.class, NewHighlighting.class, List.class},
                inputFile, highlighting, List.of(keywordToken, multilineStringToken, commentToken));

        verify(highlighting).highlight(keywordRange, TypeOfText.KEYWORD);
        verify(highlighting).highlight(stringRange, TypeOfText.STRING);
        verify(highlighting).highlight(commentRange, TypeOfText.COMMENT);
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void testTokenHighlightingSkipsInvalidTokensAndRanges() throws Exception {
        InputFile inputFile = mock(InputFile.class);
        NewHighlighting highlighting = mock(NewHighlighting.class, org.mockito.Mockito.RETURNS_SELF);
        TextRange keywordRange = mock(TextRange.class);

        when(inputFile.lines()).thenReturn(5);
        when(inputFile.newRange(1, 0, 1, 6)).thenReturn(keywordRange);

        TSqlParserToken validKeyword = new TSqlParserToken(TSqlTokenType.Select, 0, "SELECT", 1, 1);
        TSqlParserToken outOfBoundsComment = new TSqlParserToken(TSqlTokenType.SingleLineComment, 0, "-- x", 99, 1);
        TSqlParserToken nullType = new TSqlParserToken(null, 0, "foo", 1, 1);

        invokePrivate("publishHighlightingFromTokens", new Class<?>[] {InputFile.class, NewHighlighting.class, List.class},
                inputFile, highlighting, List.of("not-a-token", validKeyword, outOfBoundsComment, nullType));

        verify(highlighting).highlight(keywordRange, TypeOfText.KEYWORD);
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void testRangeForTokenAndScannersHandleMultilineAndPartialInputs() throws Exception {
        InputFile inputFile = mock(InputFile.class);
        TextRange tokenRange = mock(TextRange.class);

        when(inputFile.lines()).thenReturn(10);
        when(inputFile.newRange(2, 3, 3, 1)).thenReturn(tokenRange);

        TSqlParserToken multilineToken = new TSqlParserToken(TSqlTokenType.UnicodeStringLiteral, 0, "ab\r\nc", 2, 4);
        Object resolvedRange = invokePrivate("rangeForToken", new Class<?>[] {InputFile.class, TSqlParserToken.class}, inputFile, multilineToken);
        assertThat(resolvedRange).isSameAs(tokenRange);

        TSqlParserToken invalidToken = new TSqlParserToken(TSqlTokenType.UnicodeStringLiteral, 0, "x", 99, 1);
        Object invalidRange = invokePrivate("rangeForToken", new Class<?>[] {InputFile.class, TSqlParserToken.class}, inputFile, invalidToken);
        assertThat(invalidRange).isNull();

        assertThat(invokePrivate("scanComment", new Class<?>[] {String.class, int.class}, "-- hi\r\n", 0)).isEqualTo(5);
        assertThat(invokePrivate("scanComment", new Class<?>[] {String.class, int.class}, "/*open", 0)).isEqualTo(6);
        assertThat(invokePrivate("scanStringToken", new Class<?>[] {String.class, int.class}, "N'ab'", 0)).isEqualTo(5);
        assertThat(invokePrivate("scanStringToken", new Class<?>[] {String.class, int.class}, "'a''b'", 0)).isEqualTo(6);
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void testFallbackHighlightingHandlesMultilineStringEscapedIdentifierAndPartialComment() throws IOException {
        SensorContext context = mock(SensorContext.class);
        FileSystem fs = mock(FileSystem.class);
        FilePredicates predicates = mock(FilePredicates.class);
        FilePredicate predicate = mock(FilePredicate.class);
        InputFile inputFile = mock(InputFile.class);
        NewHighlighting highlighting = mock(NewHighlighting.class, org.mockito.Mockito.RETURNS_SELF);
        TextRange keywordRange = mock(TextRange.class);
        TextRange stringRange = mock(TextRange.class);
        TextRange escapedIdentifierRange = mock(TextRange.class);
        TextRange commentRange = mock(TextRange.class);

        when(context.fileSystem()).thenReturn(fs);
        when(fs.predicates()).thenReturn(predicates);
        when(predicates.hasLanguage(TSqlLanguage.KEY)).thenReturn(predicate);
        when(fs.inputFiles(predicate)).thenReturn(List.of(inputFile));
        when(context.activeRules()).thenReturn(mock(ActiveRules.class));
        when(context.newHighlighting()).thenReturn(highlighting);
        when(inputFile.uri()).thenReturn(URI.create("file:///highlight-fallback.sql"));
        when(inputFile.contents()).thenReturn("SELECT N'a\nb', [a]]b]\n/*open comment");
        when(inputFile.lines()).thenReturn(3);
        when(inputFile.newRange(1, 0, 1, 6)).thenReturn(keywordRange);
        when(inputFile.newRange(1, 7, 2, 2)).thenReturn(stringRange);
        when(inputFile.newRange(2, 4, 2, 10)).thenReturn(escapedIdentifierRange);
        when(inputFile.newRange(3, 0, 3, 14)).thenReturn(commentRange);

        sensor.execute(context);

        verify(highlighting, atLeastOnce()).highlight(keywordRange, TypeOfText.KEYWORD);
        verify(highlighting, atLeastOnce()).highlight(stringRange, TypeOfText.STRING);
        verify(highlighting, atLeastOnce()).highlight(escapedIdentifierRange, TypeOfText.KEYWORD_LIGHT);
        verify(highlighting, atLeastOnce()).highlight(commentRange, TypeOfText.COMMENT);
        verify(highlighting, atLeastOnce()).save();
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void testHighlightingHelperGuardsAndErrorPaths() throws Exception {
        InputFile inputFile = mock(InputFile.class);
        NewHighlighting highlighting = mock(NewHighlighting.class, org.mockito.Mockito.RETURNS_SELF);
        SensorContext sensorContext = mock(SensorContext.class);
        TextRange validRange = mock(TextRange.class);

        when(sensorContext.newHighlighting()).thenReturn(highlighting);
        when(inputFile.lines()).thenReturn(4);
        when(inputFile.newRange(1, 0, 1, 6)).thenReturn(validRange);

        // publishHighlighting should use token stream when available.
        ParseResult<TSqlScript> tokenResult = mock(ParseResult.class);
        TSqlScript script = new TSqlScript();
        script.ScriptTokenStream = List.of(new TSqlParserToken(TSqlTokenType.Select, 0, "SELECT", 1, 1));
        when(tokenResult.fragment()).thenReturn(script);

        invokePrivate("publishHighlighting", new Class<?>[] {InputFile.class, SensorContext.class, ParseResult.class, String.class},
                inputFile, sensorContext, tokenResult, "ignored");
        verify(highlighting).highlight(validRange, TypeOfText.KEYWORD);

        // mapTokenToType null guards.
        assertThat(invokePrivate("mapTokenToType", new Class<?>[] {TSqlParserToken.class}, new Object[] {null})).isNull();
        assertThat(invokePrivate("mapTokenToType", new Class<?>[] {TSqlParserToken.class},
                new TSqlParserToken(null, 0, "x", 1, 1))).isNull();

        // rangeForToken invalid/exception path.
        assertThat(invokePrivate("rangeForToken", new Class<?>[] {InputFile.class, TSqlParserToken.class}, inputFile, null)).isNull();
        InputFile throwingFile = mock(InputFile.class);
        when(throwingFile.lines()).thenReturn(4);
        when(throwingFile.newRange(any(Integer.class), any(Integer.class), any(Integer.class), any(Integer.class)))
                .thenThrow(new IllegalArgumentException("bad range"));
        assertThat(invokePrivate("rangeForToken", new Class<?>[] {InputFile.class, TSqlParserToken.class},
                throwingFile, new TSqlParserToken(TSqlTokenType.Select, 0, "SELECT", 1, 1))).isNull();

        // rangeForOffsets invalid/exception path.
        assertThat(invokePrivate("rangeForOffsets", new Class<?>[] {int.class, int.class, InputFile.class, int[].class},
                -1, 0, inputFile, new int[] {0})).isNull();
        assertThat(invokePrivate("rangeForOffsets", new Class<?>[] {int.class, int.class, InputFile.class, int[].class},
                0, 6, throwingFile, new int[] {0})).isNull();

        // scanner guards and CRLF support.
        assertThat(invokePrivate("scanComment", new Class<?>[] {String.class, int.class}, null, 0)).isEqualTo(0);
        assertThat(invokePrivate("scanStringToken", new Class<?>[] {String.class, int.class}, "x", 1)).isEqualTo(1);
        assertThat(invokePrivate("scanStringLiteral", new Class<?>[] {String.class, int.class}, "x", 0)).isEqualTo(0);
        assertThat(invokePrivate("buildLineStarts", new Class<?>[] {String.class}, "")).isEqualTo(new int[] {0});
        assertThat(invokePrivate("buildLineStarts", new Class<?>[] {String.class}, "a\r\nb")).isEqualTo(new int[] {0, 3});

        // keyword guard path.
        assertThat(invokePrivate("isKeyword", new Class<?>[] {String.class}, " ")).isEqualTo(false);

        // highlightRange invalid guard path.
        invokePrivate("highlightRange", new Class<?>[] {InputFile.class, NewHighlighting.class, String.class, int.class, int.class, TypeOfText.class, int[].class},
                inputFile, highlighting, "abc", 3, 1, TypeOfText.KEYWORD, new int[] {0});
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void testExecuteChecksContinuesWhenPerFileSchemaIndexingFails() throws Exception {
        InputFile inputFile = mock(InputFile.class);
        when(inputFile.uri()).thenReturn(URI.create("file:///query.sql"));
        SensorContext context = mock(SensorContext.class);
        TSqlScript root = new TSqlScript();
        TSqlCheck check = mock(TSqlCheck.class);
        ProjectSchemaCatalog projectSchema = mock(ProjectSchemaCatalog.class);
        doThrow(new RuntimeException("index failure")).when(projectSchema).indexScript(root);

        invokePrivate("executeChecks",
                new Class<?>[] {InputFile.class, SensorContext.class, io.github.fupacat.ast.TSqlFragment.class,
                        String.class, List.class, ProjectSchemaCatalog.class, boolean.class},
                inputFile, context, root, "SELECT 1;", List.of(check), projectSchema, true);

        verify(check, times(1)).scan(eq(root), any(TSqlCheckContext.class));
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void testExecuteChecksSkipsPerFileSchemaIndexingWhenSchemaRulesInactive() throws Exception {
        InputFile inputFile = mock(InputFile.class);
        SensorContext context = mock(SensorContext.class);
        TSqlScript root = new TSqlScript();
        TSqlCheck check = mock(TSqlCheck.class);
        ProjectSchemaCatalog projectSchema = mock(ProjectSchemaCatalog.class);

        invokePrivate("executeChecks",
                new Class<?>[] {InputFile.class, SensorContext.class, io.github.fupacat.ast.TSqlFragment.class,
                        String.class, List.class, ProjectSchemaCatalog.class, boolean.class},
                inputFile, context, root, "SELECT 1;", List.of(check), projectSchema, false);

        verify(projectSchema, never()).indexScript(any());
        verify(check, times(1)).scan(eq(root), any(TSqlCheckContext.class));
    }

    // TRACEABILITY_MAPPING: REQ-005
    private Object invokePrivate(String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = TSqlSensor.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(sensor, args);
    }

    // TRACEABILITY_MAPPING: REQ-005
    @Test
    void testCatalogParserRuntimeFailureIsolatedPerFile() throws IOException {
        RuleKey selectAllRuleKey = RuleKey.of(TSqlRulesDefinition.REPOSITORY_KEY, AvoidSelectAllCheck.RULE_KEY);
        RuleKey syntaxRuleKey = RuleKey.of(TSqlRulesDefinition.REPOSITORY_KEY, CheckList.SYNTAX_ERROR_RULE_KEY);

        SensorContext context = mock(SensorContext.class);
        FileSystem fs = mock(FileSystem.class);
        FilePredicates predicates = mock(FilePredicates.class);
        FilePredicate predicate = mock(FilePredicate.class);
        InputFile badFile = mock(InputFile.class);
        InputFile goodFile = mock(InputFile.class);
        ActiveRules activeRules = mock(ActiveRules.class);
        ActiveRule selectAllRule = mock(ActiveRule.class);
        ActiveRule syntaxRule = mock(ActiveRule.class);

        when(context.fileSystem()).thenReturn(fs);
        when(fs.predicates()).thenReturn(predicates);
        when(predicates.hasLanguage(TSqlLanguage.KEY)).thenReturn(predicate);
        when(fs.inputFiles(predicate)).thenReturn(List.of(badFile, goodFile));

        when(badFile.uri()).thenReturn(URI.create("file:///catalog-runtime-failure.sql"));
        when(badFile.contents()).thenReturn(null);
        when(goodFile.uri()).thenReturn(URI.create("file:///valid.sql"));
        when(goodFile.contents()).thenReturn("SELECT * FROM Customers;\n");
        when(goodFile.lines()).thenReturn(1);

        when(context.activeRules()).thenReturn(activeRules);
        when(activeRules.find(selectAllRuleKey)).thenReturn(selectAllRule);
        when(activeRules.find(syntaxRuleKey)).thenReturn(syntaxRule);

        NewIssue issue = mock(NewIssue.class, org.mockito.Mockito.RETURNS_SELF);
        NewIssueLocation location = mock(NewIssueLocation.class, org.mockito.Mockito.RETURNS_SELF);
        when(context.newIssue()).thenReturn(issue);
        when(issue.newLocation()).thenReturn(location);

        sensor.execute(context);

        verify(issue, atLeastOnce()).forRule(selectAllRuleKey);
        verify(issue, atLeastOnce()).forRule(syntaxRuleKey);
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
        assertThat(measureValues).doesNotContainKey(CoreMetrics.STATEMENTS);
        assertThat(measureValues).containsEntry(CoreMetrics.COMPLEXITY, 3);
        assertThat(measureValues).hasSize(3);
    }
}