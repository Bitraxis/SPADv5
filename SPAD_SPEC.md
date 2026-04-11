# The SPAD Standard (TSS)

This document defines standards for SPAD and Dragon in a format inspired by PEP-style specifications.

## Index

1. TSS-0001: SPAD Language Core Specification
2. TSS-0002: Dragon Toolkit and Build Specification

---

# TSS-0001: SPAD Language Core Specification

## Header

- TSS Number: 0001
- Title: SPAD Language Core
- Status: Active
- Type: Standards Track
- Created: 2026-04-11
- Requires: None
- Supersedes: Informal SPAD language direction notes

## Abstract

SPAD (Simple Projecting Automating Directive) is a statically checked, expression-forward language with Java-oriented structure, simplified assignment syntax, and automation-oriented declarations. This specification defines the core syntax, type behavior, and runtime model.

## Motivation

SPAD exists to provide a small, readable language for project automation and directive-driven workflows while remaining JVM-friendly. The syntax intentionally favors `=`, optional semicolons, and block-based forms using `()` and `{}`.

## Design Goals

1. Static checking by default.
2. Expression-friendly grammar.
3. Predictable operators and types.
4. JVM interoperability and transpilation.
5. Clear, compact syntax with minimal punctuation overhead.

## Language Syntax (Normative)

### Declarations and Directives

- `import presentation from "ftp://central"`
- `import presentation = ">=1.2.0" from "ftp://central"`
- `dragon = {presentation, graph, automation}`
- `var name = expr`
- `func name(a = Int, b = Int) = Int { ... }`
- `automate name(args...) = Type { ... }`
- `project Name { ... }`
- `projection key = expr`
- `export toolkit { ... }`
- `directive name(args...)`

### Core Expressions and Literals

- Lists: `[1, 2, 3]`
- Dictionaries: `{name = "x", level = 3}`
- Grouping: `(expr)`
- Assignment expression: `name = expr`
- Pipeline: `value |> normalize() |> summarize()`

### Control Flow

```spad
if (score > 10) {
  print("high")
} else {
  print("low")
}

while (hp > 0) {
  hp = hp - 1
}

for i in 0..10 {
  print(i)
}
```

### Match Expressions

SPAD match supports literal arms, wildcard arms, and guarded arms.

```spad
var modeCode = match mode {
  "dev" => 1,
  "prod" => 2,
  _ => 0
}

var weird = match points {
  _ when (points > 9000) => "over",
  _ => "normal"
}
```

Rules:

1. `_` is a wildcard pattern.
2. `when (expr)` is a guard and must evaluate to `Bool`.
3. A catch-all wildcard arm (`_` without guard) must be last.
4. Matches must be exhaustive:
   - either include a catch-all wildcard arm, or
   - fully cover known finite boolean cases (`true` and `false`).

### Java Interop Short Form

```spad
var now = java::java.time.Instant.now()
```

## Type System (Normative)

Supported named types:

- `Int`
- `Float`
- `String`
- `Bool`
- `List`
- `Dict`
- `Void`

Static checker requirements:

1. Arithmetic operators (`+`, `-`, `*`, `/`, `%`) require numeric operands, except string concatenation with `+`.
2. Comparison operators (`>`, `>=`, `<`, `<=`) require numeric operands.
3. Unary `-` requires numeric operands.
4. Unary `!` requires `Bool`.
5. Assignment must preserve type compatibility.
6. Function return values must match declared return type.
7. Match guard expressions must be `Bool`.
8. `for i in a..b` requires `Int..Int` bounds.

## Runtime Model

SPAD is transpiled to Java source.

Runtime helpers are provided by the prelude and include:

- `print(x)`
- `toInt(x)`
- `toFloat(x)`
- `toString(x)`
- `dijkstra(graph, start)`
- `directive(name, ...args)`

## Optional Terminators

Statements may terminate by newline or semicolon. Semicolons are optional.

## Compatibility

This standard preserves compatibility with SPAD constructs already accepted by current compiler implementations.

---

# TSS-0002: Dragon Toolkit and Build Specification

## Header

- TSS Number: 0002
- Title: Dragon Toolkit, Resolution, and Build Profiles
- Status: Active
- Type: Standards Track
- Created: 2026-04-11
- Requires: TSS-0001
- Supersedes: Informal Dragon toolkit notes

## Abstract

Dragon is the SPAD project manager and build orchestrator. This specification defines configuration, lockfile behavior, package resolution, profile discovery, and command-line execution semantics.

## Motivation

Dragon standardizes repeatable SPAD builds, import resolution, local/remote package source behavior, and profile-driven compilation.

## Configuration Files (Normative)

### `spad.toml`

Standard sections:

1. `[spad]` language and version.
2. `[repos]` source repositories and local source paths.
3. `[packages]` package set filters.
4. `[extensions]` supported extension implementation languages.

Example:

```toml
[spad]
language = "SPAD"
version = "5.0.0"

[repos]
sources = ["ftp://central", "https://repo.spad-lang.org/ftp", "packages-local"]
local_sources = ["packages-local"]

[packages]
sets = ["T", "S", "P", "A", "D", "L"]

[extensions]
languages = ["spad", "java", "lua"]
```

### `dragon.lock`

`dragon.lock` pins module versions and source origins.

## Package Resolution (Normative)

Dragon resolves imports using:

1. Module constraints from source imports.
2. Lockfile pins when semver-compatible.
3. Configured repositories and local directories.
4. Package-set allowlist filtering.
5. Local cache materialization under `.dragon/cache`.

Repository roots expose `PACKAGES.toml` and package sets `T`, `S`, `P`, `A`, `D`, `L`.

## Build Profiles and Projections

Dragon supports project/projection discovery from SPAD source and profile-targeted compile operations.

Projection profile behavior:

1. `project Name { projection key = value }` defines a profile entry.
2. Dragon can list discovered profiles.
3. Dragon can activate a selected profile during compile/package flows.
4. Active projection metadata is propagated to compilation as system properties.

## CLI Commands (Normative)

Required commands:

1. `resolve`
2. `compile`
3. `package`
4. `profiles`
5. `list-packages`
6. `install-spad`

Common options:

- `--workspace, -w`
- `--source, -s`
- `--out, -o`
- `--class, -c`
- `--run`
- `--jre`
- `--jre-out`
- `--projection, -p`
- `--list-profiles`
- `--verbose, -v`

## Build and JVM Targeting

1. Dragon invokes SPAD compilation to Java source.
2. Generated Java compiles with `javac`.
3. Outputs include classes and optional `.jre` bundle.
4. `.jre` uses jar layout with `Main-Class` manifest.

## Security and Integrity Notes

1. Source repository trust is implementation-defined and SHOULD be explicit.
2. Lockfile pinning SHOULD be used for reproducibility.
3. Local cache SHOULD be treated as derived artifacts.

## Reference Implementation Notes

Current implementation roots:

1. SPAD compiler: `main/`
2. Dragon toolkit: `dragon/`

---

## Conformance Summary

An implementation is considered TSS-conformant when:

1. It accepts and processes TSS-0001 syntax and static type requirements.
2. It provides Dragon behavior as defined by TSS-0002.
3. It preserves the SPAD design center: assignment-first style with `=`, parenthesis-heavy expressions, and brace-delimited blocks.

## Build Commands

- Compile all Java sources: `javac main/*.java dragon/*.java`
- Run SPAD compiler: `java main.main <file.spad>`
- Run Dragon CLI: `java dragon.DragonMain help`
