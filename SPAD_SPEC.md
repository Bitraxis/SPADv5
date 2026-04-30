# The SPAD Standard (TSS)

This document defines the language and build standards for SPAD and Dragon.

## Index

1. TSS-0001: SPAD Language Core Specification
2. TSS-0002: Dragon Toolkit and Build Specification

---

## TSS-0001: SPAD Language Core Specification

## Header

- TSS Number: 0001
- Title: SPAD Language Core
- Status: Active
- Type: Standards Track
- Created: 2026-04-11
- Requires: None
- Supersedes: Informal SPAD language direction notes

## Abstract

SPAD (Simple Projecting Automating Directive) is a statically-checked, opinionated, yet practical language designed for automation, transpilation to the JVM, and readable project-oriented code. The language is a deliberate hybrid inspired by Lua and Python with a consistent, explicit type system. This document defines the normative syntax, typing rules, and runtime model.

## Motivation

SPAD aims to be a small, predictable language that maps cleanly to Java while remaining friendly for scripting and configuration tasks. Readability, tooling, and static checking are prioritized to make SPAD suitable for real projects and libraries.

## Design Goals

1. Statically checked by default with optional type annotations.
2. Minimal, readable surface syntax borrowed from Lua and Python.
3. Deterministic semantics for transpilation to Java.
4. Clear interop with Java standard library and selected JVM libraries.
5. Small core with composable libraries in the prelude.

## High-level choices

- Block delimiters: braces `{ ... }` are the canonical block form. Parentheses may be used for grouping expressions.
- Declarations: `var` for variables, `func` and `def` for functions (both accepted); type hints use `: Type` syntax.
- Returns: functions MUST use explicit `return` to return values. Implicit last-expression returns are not part of the normative core.
- Error handling: `try / except` and `finally` are the standard mechanism.
- String concatenation uses the `..` operator (Lua-style). No interpolation syntax is defined in the core; a future string interpolation feature may be specified separately.

## How To Read The Examples

Every code block below is annotated with short comments that explain the purpose of the snippet. The comments are documentation only; they are there so readers can understand the examples without already knowing the syntax.

## Language Syntax (Normative)

### Modules and Imports

```spad
// Import a Java type and a project module.
import java.time.Instant
import util.logging

// A project block groups related declarations and metadata.
project MyProject {
  // projection metadata
}
```

### Declarations

- Variable:

```spad
// A plain variable can be inferred from its initializer.
var x = 1

// An explicit type annotation keeps the declaration unambiguous.
var count: Int = 0
```

- Function (two accepted keywords, identical semantics):

```spad
// `function` and `def` are synonyms in the normative core.
function add(a: Int, b: Int): Int {
  return a + b
}

// The body still uses braces so the block is visually explicit.
def multiply(x: Int, y: Int): Int {
  return x * y
}
```

- Class (optional, small object model):

```spad
// Classes are optional and intended for small object models.
class Person {
  // Fields are regular declarations inside the class body.
  var name: String

  // Methods use the same function syntax as top-level functions.
  function init(self, name: String) {
    self.name = name
  }
}
```

### Expressions and Literals

- Numbers: `123`, `3.14`
- Strings: double-quoted only: `"hello"`
- Lists: `[1, 2, 3]`
- Dicts (maps): `{ name = "x", value = 3 }`
- Grouping: `(expr)`
- Assignment: `name = expr` (assignment is a statement and not an expression in the normative core)

The bullet list above names the core literal and grouping forms. The parser treats each of them as a distinct expression shape, which keeps the grammar small and the type checker predictable.

### Operators

- Arithmetic: `+`, `-`, `*`, `/`, `%` (numeric types)
- Concatenation: `..` (string concatenation)
- Comparison: `==`, `!=`, `<`, `>`, `<=`, `>=`
- Logical: `and`, `or`, `not`

These operators map to a small set of type rules so the transpiler can generate straightforward Java code instead of relying on hidden coercions.

### Control Flow

If/else and `elif` (Python-style alternative to `elseif`):

```spad
// `elif` keeps the branch chain readable without extra nesting.
if (score > 10) {
  print("high")
} elif (score > 5) {
  print("medium")
} else {
  print("low")
}
```

While loop:

```spad
// The loop checks its condition at the top of each pass.
while (hp > 0) {
  hp = hp - 1
}
```

For-range loop (inclusive upper bound semantics opt-in):

```spad
// The range form reads naturally for inclusive iteration.
for i in 0..10 {
  print(i)
}
```

Try / except / finally (error handling):

```spad
// Exception handling separates the risky work from cleanup.
try {
  riskyOperation()
} except (e) {
  handle(e)
} finally {
  cleanup()
}
```

### Match expressions

Match arms, guards, and wildcard `_` are supported. Guards must evaluate to `Bool`.

```spad
// Match returns a value based on the first arm that matches.
var modeCode = match mode {
  "dev" => 1,
  "prod" => 2,
  _ => 0
}
```

Rules:

1. `_` is a wildcard pattern.
2. `when (expr)` is a guard and must evaluate to `Bool`.
3. A catch-all wildcard arm must be last.
4. Matches must be exhaustive (include `_` or exhaust finite options).

### Comprehensions and pipelines

SPAD includes a lightweight pipeline operator for readability:

```spad
// Pipelines make sequential transformations read left to right.
var out = data |> normalize() |> summarize()
```

List comprehensions are a future-facing feature and may be specified in a later minor release.

## Type System (Normative)

The type system is deliberately small and maps directly to Java types for transpilation. Types are capitalized in the normative document.

Core types:

- `Int` (maps to Java `int`)
- `Long` (maps to Java `long`)
- `Double` (maps to Java `double`)
- `Float` (maps to Java `float`)
- `String` (maps to Java `String`)
- `Bool` (maps to Java `boolean`)
- `List<T>` (generic list)
- `Dict<K,V>` (map/dictionary)
- `Void` (no value)

Type rules:

1. Arithmetic operators require numeric operands (`Int`, `Long`, `Float`, `Double`).
2. `+` is overloaded for numeric addition and string concatenation when either operand is `String` (concatenation uses `..` as preferred form).
3. Comparison operators require comparable types; relational operators require numeric types.
4. Assignment must preserve declared type or be allowed by an explicit cast.
5. Functions must declare a return type; the transpiler enforces that `return` expressions match declared return type.

Type annotations use `: Type` syntax:

```spad
var count: Int = 0
def add(a: Int, b: Int): Int {
  return a + b
}
```

Type inference: local variables without annotations are inferred from their initializers where possible. Inference is conservative and the static checker may require annotations in ambiguous cases.

## Runtime and Transpilation Model

- SPAD source is transpiled to Java source files; the transpiler generates idiomatic Java and prelude helpers.
- The prelude provides common functions such as `print`, `toInt`, `toString`, and basic collections helpers.
- Java interop allows calling fully-qualified Java APIs; dotted `java.` imports are supported.

## Standard Library and Prelude

The prelude is a minimal runtime bundled with the transpiler and includes:

- `print(x)`
- `toInt(x)`, `toDouble(x)`, `toString(x)`
- Collection helpers: `List`, `Dict` builders
- Math helpers and algorithm helpers (optional)

## Optional Terminators

Statements may end with newline or optional semicolon. Semicolons are not required.

## Compatibility and Migration Notes

- Older documents that introduced legacy compatibility constructs (e.g., `unless`, `yield`, symbol literals, interpolation shortcuts, and `begin/rescue/ensure`) are deprecated for the language core and will be removed. Implementations may offer optional compatibility layers, but such layers are not part of this normative specification.
- Where prior examples used interpolation shortcuts, migrate to explicit concatenation or library helper functions.

---

## TSS-0002: Dragon Toolkit and Build Specification

## Dragon Toolkit Header

- TSS Number: 0002
- Title: Dragon Toolkit, Resolution, and Build Profiles
- Status: Active
- Type: Standards Track
- Created: 2026-04-11
- Requires: TSS-0001
- Supersedes: Informal Dragon toolkit notes

## Dragon Toolkit Abstract

Dragon is the SPAD project manager and build orchestrator. This specification defines configuration, lockfile behavior, package resolution, profile discovery, and command-line execution semantics.

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
sources = ["https://repo.spad-lang.org/", "packages-local"]
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
3. It preserves the SPAD design center: assignment-first style with `=`, explicit `return` semantics, and brace-delimited blocks.

## Build Commands

- Compile all Java sources: `javac main/*.java dragon/*.java`
- Run SPAD compiler: `java main.main <file.spad>`
- Run Dragon CLI: `java dragon.DragonMain help`

---

## Lexical Grammar and Tokens (Normative)

This section specifies the lexical tokens the SPAD lexer must recognize. The lexer is line/column aware and provides tokens with type and literal value to the parser.

- Whitespace: spaces, tabs, carriage returns and newlines separate tokens; newlines are significant only for optional statement termination.
- Comments: `--` to end of line. Block comments use `--[[` ... `]]` (optional).

Tokens:

- IDENTIFIER: `[A-Za-z_][A-Za-z0-9_]*`
- INT_LITERAL: `[0-9]+`
- FLOAT_LITERAL: `[0-9]+\.[0-9]+`
- STRING_LITERAL: `"(\\.|[^"])*"` (double-quoted only)
- KEYWORDS: `var, function, def, if, elif, else, for, while, try, except, finally, return, import, project, match, in, true, false, null`
- OPERATORS / PUNCT: `+ - * / % == != < > <= >= = .. |> ( ) { } [ ] , : ..` and `..` for concatenation

The lexer SHOULD produce error tokens for illegal characters and report unterminated string literals with line/column locations.

## Concrete Syntax (EBNF, normative)

The following is a compact EBNF for the core language. Implementations may extend but must accept this core.

program        ::= { top_level_decl }
top_level_decl ::= import_decl | project_decl | declaration
import_decl    ::= 'import' qualified_name
project_decl   ::= 'project' IDENTIFIER '{' { projection_entry } '}'
projection_entry ::= IDENTIFIER '=' expression
declaration    ::= var_decl | func_decl
var_decl       ::= 'var' IDENTIFIER [ ':' type ] [ '=' expression ]
func_decl      ::= ( 'function' | 'def' ) IDENTIFIER '(' [ param_list ] ')' [ ':' type ] block
param_list     ::= param { ',' param }
param          ::= IDENTIFIER [ ':' type ]
block          ::= '{' { statement } '}'
statement      ::= declaration | expr_stmt | if_stmt | while_stmt | for_stmt | return_stmt | try_stmt
expr_stmt      ::= expression
if_stmt        ::= 'if' '(' expression ')' block { 'elif' '(' expression ')' block } [ 'else' block ]
while_stmt     ::= 'while' '(' expression ')' block
for_stmt       ::= 'for' IDENTIFIER 'in' range_expr block
range_expr     ::= expression '..' expression
return_stmt    ::= 'return' [ expression ]
try_stmt       ::= 'try' block 'except' '(' IDENTIFIER ')' block [ 'finally' block ]
match_expr     ::= 'match' expression '{' { match_arm } '}'
match_arm      ::= pattern '=>' expression [ ',' ]
pattern        ::= literal | IDENTIFIER | '_' | '{' pattern_fields '}'
expression     ::= assignment
assignment     ::= logical_or [ '=' expression ]
logical_or     ::= logical_and { 'or' logical_and }
logical_and    ::= equality { 'and' equality }
equality       ::= comparison { ( '==' | '!=' ) comparison }
comparison     ::= additive { ( '<' | '>' | '<=' | '>=' ) additive }
additive       ::= multiplicative { ( '+' | '-' ) multiplicative }
multiplicative ::= unary { ( '*' | '/' | '%' ) unary }
unary          ::= ( 'not' | '-' ) unary | primary
primary        ::= literal | IDENTIFIER | '(' expression ')' | list_literal | dict_literal | call_expr | match_expr
call_expr      ::= primary '(' [ arg_list ] ')'
arg_list       ::= expression { ',' expression }
list_literal   ::= '[' [ expression { ',' expression } ] ']'
dict_literal   ::= '{' [ dict_entry { ',' dict_entry } ] '}'
dict_entry     ::= IDENTIFIER '=' expression
type           ::= IDENTIFIER | IDENTIFIER '<' type_list '>'
type_list      ::= type { ',' type }

This EBNF is intentionally compact; implementers should add precise precedence rules identical to the expression productions above.

## Semantics and Static Rules

- Assignment: left side must be an lvalue. Type of assigned expression must be assignable to declared variable type.
- Functions: parameters and return types are checked statically. Overloading is not part of the core.
- Scope: lexical, block-scoped. `var` declarations are visible from point of declaration to block end.
- Closures: functions capture immutable bindings by value semantics for primitives and by reference for objects (implementation detail documented in transpiler runtime).
- Garbage: runtime follows Java GC semantics; SPAD objects become Java objects.

## Standard Library and Prelude (Normative surface)

The prelude is a small set of helpers automatically imported into every module. The following are the required entries and brief signatures. Implementations may provide additional helpers.

- `print(x: Any): Any` — formatted output to stdout; returns the input value.
- `toInt(x: Any): Int` — conversion, throws on invalid.
- `toFloat(x: Any): Double` — floating-point conversion, throws on invalid.
- `toString(x: Any): String` — stringify any value.
- `read(path: String): String` — read entire file contents as a string; throws on I/O error.
- `write(path: String, data: String): Void` — write string to file; creates or overwrites; throws on I/O error.
- `dijkstra(graph: Dict<String, Dict<String, Int>>, start: String): Dict<String, Int>` — shortest-path algorithm.

Stdlib design note: keep the prelude small; prefer libraries for larger features.

## Module, Packaging, and Versioning

- Source files map to modules; a top-level `project` groups multiple modules.
- `spad.toml` defines `language.version` and package coordinates.
- Packaging format: source packages are tar.gz with manifest `spad.toml`. Binary artifacts are standard jars produced by the transpiled Java.
- Versioning: follow semver for packages. Language evolution uses MAJOR.MINOR.PATCH; breaking changes bump MAJOR.

## Tooling (Normative recommendations)

- `spad fmt` — formatter with stable layout rules; the spec documents canonical formatting tokens.
- `spad lint` — linter that enforces style and catches common mistakes.
- `spad build` / `spad compile` — transpiles to Java and then compiles.
- `spad test` — runs test files (test harness is a simple Java runner generated by the transpiler)

Implementations SHOULD expose an API for IDE integration (language server protocol) and provide metadata for symbol indexing.

## Error Model and Diagnostics

- Lexical errors: report with line/column and snippet.
- Parse errors: include expected tokens and recovery hint.
- Type errors: clear messages with expected/actual types and suggestion for quick fixes (e.g., add `: Int`).

Diagnostic examples:

```text
Error: Type mismatch: expected `Int` but found `String` at src/main.spad:12:8
Hint: add `: Int` to the variable or call `toInt(...)` to convert.
```

## Examples (Normative examples to illustrate core features)

Hello world:

```spad
function main(): Void {
  print("Hello, SPAD")
}

main()
```

Simple module and types:

```spad
import java.time.Instant

def add(a: Int, b: Int): Int {
  return a + b
}

var values: List<Int> = List.of(1, 2, 3)

for i in 0..(values.length - 1) {
  print(values[i])
}
```

Match example:

```spad
var result = match status {
  "ok" => "success",
  "err" => "failure",
  _ => "unknown"
}
```

## Migration Guide (brief)

- Remove legacy idioms: `unless`, `yield`, `:symbol` keys and interpolation shortcuts. Replace with explicit constructs:
  - `unless cond then` -> `if (not cond) { ... }`
  - `yield` -> explicit callback parameter and call
  - interpolation -> `toString` and `..` concatenation or `String.format` via Java interop
- Replace `begin/rescue/ensure` with `try/except/finally` blocks.

## Extension Points and Compatibility Layers

Implementations may optionally provide compatibility flags for older syntax, but such flags are not part of the normative standard and behavior under the compatibility layer is implementation-defined.

## Concluding notes

This expanded specification provides the concrete lexical, syntactic, semantic, and tooling guidance required to treat SPAD as a practical language for real projects. Implementers and library authors should follow the normative sections above for compatibility.
