package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.ForceSeekTableHint;
import io.github.fupacat.ast.IndexTableHint;
import io.github.fupacat.ast.LiteralTableHint;
import io.github.fupacat.ast.TableHint;
import io.github.fupacat.ast.TableHintKind;

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
            if (node.HintKind == TableHintKind.HoldLock || node.HintKind == TableHintKind.Index || node.HintKind == TableHintKind.ForceSeek) {
                return false;
            }
            String kindName = node.HintKind.name().toUpperCase();
            return kindName.contains("NOLOCK") || kindName.contains("READUNCOMMITTED");
        }
        // In ScriptDOM, simple identifier hints (such as NOLOCK and READUNCOMMITTED)
        // are parsed as basic TableHint instances without specialized sub-types
        return !(node instanceof IndexTableHint)
                && !(node instanceof ForceSeekTableHint)
                && !(node instanceof LiteralTableHint);
    }
}
