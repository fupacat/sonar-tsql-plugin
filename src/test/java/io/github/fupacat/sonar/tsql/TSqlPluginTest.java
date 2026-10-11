package io.github.fupacat.sonar.tsql;

import io.github.fupacat.sonar.tsql.rules.TSqlQualityProfile;
import io.github.fupacat.sonar.tsql.rules.TSqlRulesDefinition;
import org.junit.jupiter.api.Test;
import org.sonar.api.Plugin;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.utils.Version;

import static org.assertj.core.api.Assertions.assertThat;

class TSqlPluginTest {

    @Test
    void testPluginDefinesExpectedExtensions() {
        SonarRuntime runtime = SonarRuntimeImpl.forPR(Version.create(10, 4), SonarQubeSide.SCANNER, SonarEdition.COMMUNITY);
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
