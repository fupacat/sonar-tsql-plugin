# MCSWE v3 Protocol

This repository is initialized with the **MCSWE v3** (Mission Control Software Engineering) gate-driven SDLC pipeline.

## Pipeline Workflow

```text
INIT --triage--> TRIAGED --spec--> SPEC_LOCKED --architect--> ARCH_LOCKED --audit--> AUDITED
                                                                     \--(anomaly)--> HEALING --audit--> ...
```

1. **Gate 0: Triage**
   - Resolve decisions in `pre_protocol/DECISIONS.md` (remove any `[UNRESOLVED]` markers).
   - Document any research bounds in `pre_protocol/RESEARCH_BOUNDS.md`.
   - Run: `mcswe-v3 triage`

2. **Gate 1: Specification**
   - Author requirements in `context/SRS.md` using `[REQ-001] ... SHALL ...` syntax (avoid banned terms: fast, efficient, secure, user-friendly, scalable).
   - Provide exactly 1 fenced JSON acceptance record per requirement:
     ```json
     {"requirement":"REQ-001","kind":"categorical","precondition":"...","action":"...","expected":"...","method":"junit","expected_output":"..."}
     ```
   - Run: `mcswe-v3 spec`

3. **Gate 2: Architecture**
   - Author interface definitions in `context/ICD.md` with qualified symbols and exact AST-normalized signatures:
     ```json
     {"kind":"interface","id":"IF-001","path":"src/...","symbol":"...","signature":{"parameters":[],"return":"...","async":false},"dependencies":[],"exchanges":[]}
     ```
   - Run: `mcswe-v3 architect`

4. **Implementation & Guard**
   - Application source and test files must NOT be written unless `mcswe-v3 guard` passes.
   - Claude Code PreToolUse hook (`.claude/settings.json`) enforces `mcswe-v3 hook`.
   - Every function requires `// TRACEABILITY_MAPPING: REQ-NNN` within 3 lines above its declaration.

5. **Gate 3: Audit & Verify**
   - Run: `mcswe-v3 audit` to execute tests, generate `context/RTM.md`, and advance to `AUDITED` (or enter `HEALING`).
   - Run: `mcswe-v3 verify` for a stateless full check across all gates without mutating `state.json`.

## Note on Project Language & Adapters
`sonar-tsql-plugin` is a Java/Maven repository powered by SonarQube Plugin API and `tsql-scriptdom-java`.
MCSWE v3 is configured to use native `java-ast-v1` and `maven-surefire-v1` adapters.
