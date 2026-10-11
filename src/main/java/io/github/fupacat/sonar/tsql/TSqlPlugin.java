package io.github.fupacat.sonar.tsql;

import io.github.fupacat.sonar.tsql.rules.TSqlQualityProfile;
import io.github.fupacat.sonar.tsql.rules.TSqlRulesDefinition;
import org.sonar.api.Plugin;

/**
 * SonarQube plugin entry point for Microsoft Transact-SQL (T-SQL) static code analysis.
 */
public class TSqlPlugin implements Plugin {

    // TRACEABILITY_MAPPING: REQ-002
    @Override
    public void define(Context context) {
        context.addExtensions(
                TSqlLanguage.class,
                TSqlRulesDefinition.class,
                TSqlQualityProfile.class,
                TSqlSensor.class
        );
    }
}
