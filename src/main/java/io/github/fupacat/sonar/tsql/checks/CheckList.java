package io.github.fupacat.sonar.tsql.checks;

import java.util.List;

/**
 * Registry of all available T-SQL rule checks.
 */
public final class CheckList {

    public static final String SYNTAX_ERROR_RULE_KEY = "SyntaxError";

    private static final List<Class<? extends TSqlCheck>> CHECKS = List.of(
            AvoidSelectAllCheck.class,
            AvoidLeadingWildcardInLikeCheck.class,
            AvoidNoLockHintCheck.class,
            AvoidDeprecatedTypesCheck.class,
            DeleteAndUpdateWithoutWhereCheck.class,
            NotInSubqueryCheck.class,
            NullComparisonCheck.class,
            TopWithoutOrderByCheck.class,
            CaseInputInvariantCheck.class,
            UnusedLocalVariableCheck.class,
            UnusedParameterCheck.class,
            UnusedLabelCheck.class,
            UnreachableCodeCheck.class,
            SingleIterationLoopCheck.class,
            SelfAssignmentCheck.class,
            CatchClauseShouldDoMoreThanRethrowCheck.class,
            RedundantJumpCheck.class
    );

    private CheckList() {
    }

    // TRACEABILITY_MAPPING: REQ-006
    public static List<Class<? extends TSqlCheck>> getChecks() {
        return CHECKS;
    }

    // TRACEABILITY_MAPPING: REQ-006
    public static List<TSqlCheck> instantiateChecks() {
        return List.of(
                new AvoidSelectAllCheck(),
                new AvoidLeadingWildcardInLikeCheck(),
                new AvoidNoLockHintCheck(),
                new AvoidDeprecatedTypesCheck(),
                new DeleteAndUpdateWithoutWhereCheck(),
                new NotInSubqueryCheck(),
                new NullComparisonCheck(),
                new TopWithoutOrderByCheck(),
                new CaseInputInvariantCheck(),
                new UnusedLocalVariableCheck(),
                new UnusedParameterCheck(),
                new UnusedLabelCheck(),
                new UnreachableCodeCheck(),
                new SingleIterationLoopCheck(),
                new SelfAssignmentCheck(),
                new CatchClauseShouldDoMoreThanRethrowCheck(),
                new RedundantJumpCheck()
        );
    }
}
