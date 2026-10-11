package io.github.fupacat.sonar.tsql;

import org.junit.jupiter.api.Test;
import org.sonar.api.config.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TSqlLanguageTest {

    // TRACEABILITY_MAPPING: REQ-001
    @Test
    void testLanguageDefaults() {
        Configuration config = mock(Configuration.class);
        TSqlLanguage language = new TSqlLanguage(config);
        assertThat(language.getKey()).isEqualTo("tsql");
        assertThat(language.getName()).isEqualTo("T-SQL");
        assertThat(language.getFileSuffixes()).containsExactly(".sql", ".tsql");
    }

    // TRACEABILITY_MAPPING: REQ-001
    @Test
    void testCustomFileSuffixes() {
        Configuration config = mock(Configuration.class);
        when(config.getStringArray(TSqlLanguage.FILE_SUFFIXES_KEY))
                .thenReturn(new String[]{".prc", ".fnc", ".sql"});
        TSqlLanguage language = new TSqlLanguage(config);

        assertThat(language.getFileSuffixes()).containsExactly(".prc", ".fnc", ".sql");
    }

    // TRACEABILITY_MAPPING: REQ-001
    @Test
    void testNullConfiguration() {
        TSqlLanguage language = new TSqlLanguage(null);
        assertThat(language.getFileSuffixes()).containsExactly(".sql", ".tsql");
    }
}
