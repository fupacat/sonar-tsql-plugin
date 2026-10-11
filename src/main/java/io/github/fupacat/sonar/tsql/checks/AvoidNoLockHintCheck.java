package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.TSqlParserToken;
import io.github.fupacat.ast.TableHint;

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
    }

    private boolean isNoLockHint(TableHint node) {
        if (node.HintKind != null) {
            String kindName = node.HintKind.name().toUpperCase();
            if (kindName.contains("NOLOCK") || kindName.contains("READUNCOMMITTED")) {
                return true;
            }
        }
        if (node.ScriptTokenStream != null && node.firstTokenIndex >= 0 && node.firstTokenIndex < node.ScriptTokenStream.size()) {
            Object tokenObj = node.ScriptTokenStream.get(node.firstTokenIndex);
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
