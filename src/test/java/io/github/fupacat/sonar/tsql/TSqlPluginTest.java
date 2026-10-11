package io.github.fupacat.sonar.tsql;

import io.github.fupacat.sonar.tsql.rules.TSqlQualityProfile;
import io.github.fupacat.sonar.tsql.rules.TSqlRulesDefinition;
import org.junit.jupiter.api.Test;
import org.sonar.api.Plugin;
import org.sonar.api.SonarRuntime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TSqlPluginTest {

    @Test
    void testPluginDefinesExpectedExtensions() {
        SonarRuntime runtime = mock(SonarRuntime.class);
        Plugin.Context context = new Plugin.Context(runtime);

        TSqlPlugin plugin = new TSqlPlugin();
        plugin.define(context);

        assertThat(context.getExtensions()).contains(
                TSqlLanguage.class,
                TSqlRulesDefinition.class,
                TSqlQualityProfile.class,
                TSqlSensor.class
        );
    }
}
