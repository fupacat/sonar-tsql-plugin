package io.github.fupacat.sonar.tsql;

import org.sonar.api.config.Configuration;
import org.sonar.api.resources.AbstractLanguage;

import java.util.Arrays;
import java.util.Optional;

/**
 * Definition of the Transact-SQL (T-SQL) language for SonarQube.
 */
public class TSqlLanguage extends AbstractLanguage {

    public static final String KEY = "tsql";
    public static final String NAME = "T-SQL";
    public static final String FILE_SUFFIXES_KEY = "sonar.tsql.file.suffixes";
    public static final String[] DEFAULT_FILE_SUFFIXES = {".sql", ".tsql"};

    private final Configuration configuration;

    public TSqlLanguage(Configuration configuration) {
        super(KEY, NAME);
        this.configuration = configuration;
    }

    // TRACEABILITY_MAPPING: REQ-001
    @Override
    public String[] getFileSuffixes() {
        if (configuration != null) {
            String[] suffixes = configuration.getStringArray(FILE_SUFFIXES_KEY);
            if (suffixes != null && suffixes.length > 0) {
                return Arrays.stream(suffixes)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toArray(String[]::new);
            }
        }
        return DEFAULT_FILE_SUFFIXES;
    }
}
