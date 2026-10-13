package io.github.fupacat.sonar.tsql.checks;

import io.github.fupacat.ast.ColumnDefinition;
import io.github.fupacat.ast.ConstraintDefinition;
import io.github.fupacat.ast.CreateTableStatement;
import io.github.fupacat.ast.TableDefinition;
import io.github.fupacat.ast.UniqueConstraintDefinition;

/**
 * Rule: A primary key should be specified for CREATE TABLE statements.
 */
public class PrimaryKeyRequiredCheck extends TSqlCheck {

    public static final String RULE_KEY = "S1614";

    @Override
    public String getRuleKey() {
        return RULE_KEY;
    }

    @Override
    public void visit(CreateTableStatement node) {
        if (!hasPrimaryKey(node.Definition)) {
            reportIssue(node, "A primary key should be specified during CREATE TABLE statements.");
        }
        super.visit(node);
    }

    private boolean hasPrimaryKey(TableDefinition definition) {
        if (definition == null) {
            return false;
        }

        return hasPrimaryKeyInTableConstraints(definition) || hasPrimaryKeyInColumnConstraints(definition);
    }

    private boolean hasPrimaryKeyInTableConstraints(TableDefinition definition) {
        if (definition.TableConstraints == null) {
            return false;
        }

        for (ConstraintDefinition constraint : definition.TableConstraints) {
            if (isPrimaryKey(constraint)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasPrimaryKeyInColumnConstraints(TableDefinition definition) {
        if (definition.ColumnDefinitions == null) {
            return false;
        }

        for (ColumnDefinition column : definition.ColumnDefinitions) {
            if (column.Constraints == null) {
                continue;
            }
            for (ConstraintDefinition constraint : column.Constraints) {
                if (isPrimaryKey(constraint)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isPrimaryKey(ConstraintDefinition constraint) {
        return constraint instanceof UniqueConstraintDefinition uniqueConstraint && uniqueConstraint.IsPrimaryKey;
    }
}
