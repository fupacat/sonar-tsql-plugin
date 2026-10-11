package io.github.fupacat.sonar.tsql;

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
import org.sonar.api.rule.RuleKey;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TSqlSensorTest {

    private TSqlSensor sensor;

    @BeforeEach
    void setUp() {
        sensor = new TSqlSensor();
    }

    @Test
    void testDescriptor() {
        SensorDescriptor descriptor = mock(SensorDescriptor.class, org.mockito.Mockito.RETURNS_SELF);
        sensor.describe(descriptor);

        verify(descriptor).name("T-SQL ScriptDOM Sensor");
        verify(descriptor).onlyOnLanguage(TSqlLanguage.KEY);
    }

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

        List<CapturedIssue> reportedIssues = new ArrayList<>();
        when(context.newIssue()).thenAnswer(inv -> new MockNewIssue(reportedIssues));

        sensor.execute(context);

        assertThat(reportedIssues).hasSize(1);
        assertThat(reportedIssues.get(0).ruleKey).isEqualTo(ruleKey);
        assertThat(reportedIssues.get(0).message).contains("SELECT *");
    }

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

        List<CapturedIssue> reportedIssues = new ArrayList<>();
        when(context.newIssue()).thenAnswer(inv -> new MockNewIssue(reportedIssues));

        sensor.execute(context);

        assertThat(reportedIssues).isNotEmpty();
        assertThat(reportedIssues.get(0).ruleKey).isEqualTo(syntaxRuleKey);
    }

    private static class CapturedIssue {
        RuleKey ruleKey;
        String message;
    }

    private static class MockNewIssue implements NewIssue {
        private final List<CapturedIssue> sink;
        private final CapturedIssue current = new CapturedIssue();

        MockNewIssue(List<CapturedIssue> sink) {
            this.sink = sink;
        }

        @Override
        public NewIssue forRule(RuleKey ruleKey) {
            current.ruleKey = ruleKey;
            return this;
        }

        @Override
        public NewIssueLocation newLocation() {
            return new MockNewIssueLocation(current);
        }

        @Override
        public NewIssue at(NewIssueLocation newIssueLocation) {
            return this;
        }

        @Override
        public NewIssue addLocation(NewIssueLocation newIssueLocation) {
            return this;
        }

        @Override
        public NewIssue setGap(Double aDouble) {
            return this;
        }

        @Override
        public NewIssue overrideSeverity(org.sonar.api.batch.rule.Severity severity) {
            return this;
        }

        @Override
        public void save() {
            sink.add(current);
        }
    }

    private static class MockNewIssueLocation implements NewIssueLocation {
        private final CapturedIssue current;

        MockNewIssueLocation(CapturedIssue current) {
            this.current = current;
        }

        @Override
        public NewIssueLocation on(InputFile inputFile) {
            return this;
        }

        @Override
        public NewIssueLocation at(TextRange textRange) {
            return this;
        }

        @Override
        public NewIssueLocation message(String s) {
            current.message = s;
            return this;
        }
    }
}
