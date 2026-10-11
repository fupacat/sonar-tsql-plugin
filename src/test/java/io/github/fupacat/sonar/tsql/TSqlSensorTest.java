package io.github.fupacat.sonar.tsql;

import io.github.fupacat.sonar.tsql.checks.AvoidSelectAllCheck;
import io.github.fupacat.sonar.tsql.checks.CheckList;
import io.github.fupacat.sonar.tsql.rules.TSqlRulesDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.rule.RuleKey;

import java.io.File;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TSqlSensorTest {

    @TempDir
    Path tempDir;

    private SensorContextTester context;
    private TSqlSensor sensor;

    @BeforeEach
    void setUp() {
        context = SensorContextTester.create(tempDir);
        sensor = new TSqlSensor();
    }

    @Test
    void testDescriptor() {
        DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();
        sensor.describe(descriptor);

        assertThat(descriptor.name()).isEqualTo("T-SQL ScriptDOM Sensor");
        assertThat(descriptor.languages()).containsExactly(TSqlLanguage.KEY);
    }

    @Test
    void testExecutesActiveRuleOnValidSql() {
        // Activate rule
        RuleKey ruleKey = RuleKey.of(TSqlRulesDefinition.REPOSITORY_KEY, AvoidSelectAllCheck.RULE_KEY);
        context.setActiveRules(new org.sonar.api.batch.rule.internal.ActiveRulesBuilder()
                .create(ruleKey)
                .setName("Avoid Select All")
                .build());

        String sql = "SELECT * FROM Customers;\n";
        context.fileSystem().add(TestInputFileBuilder.create("module", new File(tempDir.toFile(), "sample.sql"))
                .setLanguage(TSqlLanguage.KEY)
                .setContents(sql)
                .build());

        sensor.execute(context);

        assertThat(context.allIssues()).hasSize(1);
        Issue issue = context.allIssues().iterator().next();
        assertThat(issue.ruleKey()).isEqualTo(ruleKey);
        assertThat(issue.primaryLocation().message()).contains("SELECT *");
    }

    @Test
    void testReportsSyntaxError() {
        // Activate syntax error rule
        RuleKey syntaxRuleKey = RuleKey.of(TSqlRulesDefinition.REPOSITORY_KEY, CheckList.SYNTAX_ERROR_RULE_KEY);
        context.setActiveRules(new org.sonar.api.batch.rule.internal.ActiveRulesBuilder()
                .create(syntaxRuleKey)
                .setName("Syntax Error")
                .build());

        String malformedSql = "SELECT FROM WHERE;\n";
        context.fileSystem().add(TestInputFileBuilder.create("module", new File(tempDir.toFile(), "broken.sql"))
                .setLanguage(TSqlLanguage.KEY)
                .setContents(malformedSql)
                .build());

        sensor.execute(context);

        assertThat(context.allIssues()).isNotEmpty();
        Issue issue = context.allIssues().iterator().next();
        assertThat(issue.ruleKey()).isEqualTo(syntaxRuleKey);
    }
}
