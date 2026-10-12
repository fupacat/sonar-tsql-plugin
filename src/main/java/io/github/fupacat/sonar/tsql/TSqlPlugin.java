package io.github.fupacat.sonar.tsql;

import io.github.fupacat.sonar.tsql.rules.TSqlQualityProfile;
import io.github.fupacat.sonar.tsql.rules.TSqlRulesDefinition;
import org.sonar.api.Plugin;
import org.sonar.api.config.PropertyDefinition;

/**
 * SonarQube plugin entry point for Microsoft Transact-SQL (T-SQL) static code analysis.
 */
public class TSqlPlugin implements Plugin {

    @Override
    public void define(Context context) {
        context.addExtensions(
                TSqlLanguage.class,
                PropertyDefinition.builder(TSqlLanguage.FILE_SUFFIXES_KEY)
                        .name("File Suffixes")
                        .description("List of suffixes for T-SQL files to analyze.")
                        .category("T-SQL")
                        .defaultValue(String.join(",", TSqlLanguage.DEFAULT_FILE_SUFFIXES))
                        .multiValues(true)
                        .build(),
                TSqlRulesDefinition.class,
                TSqlQualityProfile.class,
                TSqlSensor.class
        );
    }
}
