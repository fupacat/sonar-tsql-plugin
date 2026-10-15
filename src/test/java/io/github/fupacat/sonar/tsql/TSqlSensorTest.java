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
    private Object invokePrivate(String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = TSqlSensor.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(sensor, args);
    }
}
