package io.github.fupacat.sonar.tsql;

import io.github.fupacat.sonar.tsql.rules.TSqlQualityProfile;
import io.github.fupacat.sonar.tsql.rules.TSqlRulesDefinition;
import org.junit.jupiter.api.Test;
import org.sonar.api.Plugin;
import org.sonar.api.SonarRuntime;
import org.sonar.api.config.PropertyDefinition;

import java.util.List;

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

        List<PropertyDefinition> propertyDefinitions = context.getExtensions().stream()
                .filter(PropertyDefinition.class::isInstance)
                .map(PropertyDefinition.class::cast)
                .toList();

        assertThat(propertyDefinitions)
                .singleElement()
                .satisfies(property -> {
                    assertThat(property.key()).isEqualTo(TSqlLanguage.FILE_SUFFIXES_KEY);
                    assertThat(property.name()).isEqualTo("File Suffixes");
                    assertThat(property.description()).isEqualTo("List of suffixes for T-SQL files to analyze.");
                    assertThat(property.category()).isEqualTo("T-SQL");
                    assertThat(property.defaultValue()).isEqualTo(String.join(",", TSqlLanguage.DEFAULT_FILE_SUFFIXES));
                    assertThat(property.multiValues()).isTrue();
                });
    }
}
