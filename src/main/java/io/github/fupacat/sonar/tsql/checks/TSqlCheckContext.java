package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.TSqlFragment;
import org.sonar.api.batch.fs.InputFile;

/**
 * Context provided to a {@link TSqlCheck} during file analysis.
 */
public interface TSqlCheckContext {

    /**
     * @return the file currently under analysis
     */
    InputFile getFile();

    /**
     * Reports an issue on a specific AST fragment.
     *
     * @param check the check reporting the issue
     * @param fragment the AST fragment where the violation occurred
     * @param message human-readable issue message
     */
    void reportIssue(TSqlCheck check, TSqlFragment fragment, String message);

    /**
     * Reports an issue on a specific line of the file.
     *
     * @param check the check reporting the issue
     * @param line 1-indexed line number
     * @param message human-readable issue message
     */
    void reportIssue(TSqlCheck check, int line, String message);
}
