package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.TSqlParserToken;
import io.github.fupacat.ast.TableHint;
import io.github.fupacat.ast.TSqlFragment;

import java.util.List;

/**
 * Rule: Avoid using NOLOCK or READUNCOMMITTED table hints.
 * The NOLOCK hint allows reading dirty, uncommitted data and can lead to skipped or duplicated rows.
 */
public class AvoidNoLockHintCheck extends TSqlCheck {

    public static final String RULE_KEY = "AvoidNoLockHint";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(TableHint node) {
        if (isNoLockHint(node)) {
            reportIssue(node, 
                    "Avoid using 'NOLOCK' or 'READUNCOMMITTED' table hints. They cause dirty reads and data inconsistency.");
        }
        super.visit(node);
    }

    private boolean isNoLockHint(TableHint node) {
        if (node.HintKind != null) {
            String kindName = node.HintKind.name().toUpperCase();
            if (kindName.contains("NOLOCK") || kindName.contains("READUNCOMMITTED")) {
                return true;
            }
        }
        TSqlFragment root = getRoot();
        List tokenStream = (node.ScriptTokenStream != null && !node.ScriptTokenStream.isEmpty())
                ? node.ScriptTokenStream
                : (root != null ? root.ScriptTokenStream : null);

        if (tokenStream != null && node.firstTokenIndex >= 0 && node.firstTokenIndex < tokenStream.size()) {
            Object tokenObj = tokenStream.get(node.firstTokenIndex);
            if (tokenObj instanceof TSqlParserToken token) {
                String txt = token.getText();
                if (txt != null && (txt.equalsIgnoreCase("NOLOCK") || txt.equalsIgnoreCase("READUNCOMMITTED"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
