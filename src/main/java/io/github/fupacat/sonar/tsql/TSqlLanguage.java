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

    @Override
    public String[] getFileSuffixes() {
        if (configuration != null) {
            Optional<String[]> suffixes = configuration.getStringArray(FILE_SUFFIXES_KEY);
            if (suffixes.isPresent() && suffixes.get().length > 0) {
                return Arrays.stream(suffixes.get())
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toArray(String[]::new);
            }
        }
        return DEFAULT_FILE_SUFFIXES;
    }
}
