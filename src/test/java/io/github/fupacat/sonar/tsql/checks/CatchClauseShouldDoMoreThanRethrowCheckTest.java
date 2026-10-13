package io.github.fupacat.sonar.tsql.checks;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CatchClauseShouldDoMoreThanRethrowCheckTest {

    private final CatchClauseShouldDoMoreThanRethrowCheck check = new CatchClauseShouldDoMoreThanRethrowCheck();

    @Test
    void testFlagsCatchThatOnlyRethrows() {
        String sql = "BEGIN TRY SELECT 1/0; END TRY BEGIN CATCH THROW; END CATCH;";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0)).contains("rethrow");
    }

    @Test
    void testAllowsCatchThatLogsAndRethrows() {
        String sql = "BEGIN TRY SELECT 1/0; END TRY BEGIN CATCH INSERT INTO Errors VALUES (ERROR_MESSAGE()); THROW; END CATCH;";
        List<String> issues = CheckTestUtils.scan(check, sql);
        assertThat(issues).isEmpty();
    }
}
