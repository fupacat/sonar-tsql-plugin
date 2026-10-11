package io.github.fupacat.sonar.tsql;

import org.junit.jupiter.api.Test;
import org.sonar.api.config.internal.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;

class TSqlLanguageTest {

    @Test
    void testLanguageDefaults() {
        TSqlLanguage language = new TSqlLanguage(new MapSettings().asConfig());
        assertThat(language.getKey()).isEqualTo("tsql");
        assertThat(language.getName()).isEqualTo("T-SQL");
        assertThat(language.getFileSuffixes()).containsExactly(".sql", ".tsql");
    }

    @Test
    void testCustomFileSuffixes() {
        MapSettings settings = new MapSettings();
        settings.setProperty(TSqlLanguage.FILE_SUFFIXES_KEY, ".prc, .fnc, .sql");
        TSqlLanguage language = new TSqlLanguage(settings.asConfig());

        assertThat(language.getFileSuffixes()).containsExactly(".prc", ".fnc", ".sql");
    }

    @Test
    void testNullConfiguration() {
        TSqlLanguage language = new TSqlLanguage(null);
        assertThat(language.getFileSuffixes()).containsExactly(".sql", ".tsql");
    }
}
