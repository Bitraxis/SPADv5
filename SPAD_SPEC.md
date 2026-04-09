# SPAD Language Direction (Simple Projecting Automating Directive)

SPAD stands for: Simple Projecting Automating Directive.
Dragon is the main compiler and toolkit project for SPAD.

## Core Design
- Java-like structure and static checking by default.
- Simplified syntax: `=` preferred, optional `;`, expression-friendly style.
- Parenthesis-heavy expression grammar.
- Strong data types with focused operations per type.
- Controlled dynamic behavior via typed conversion helpers.

## Supported Constructs (Current Prototype)
- `import presentation from "ftp://central"`
- `dragon = {presentation, graph}`
- `var name = expr`
- `func name(a = Int, b = Int) = Int { ... }`
- `project Name { ... }`
- `projection key = expr`
- `directive name(args...)`
- `automate name(args...) = Type { ... }`
- Lists: `[1, 2, 3]`
- Dictionaries: `{name = "x", level = 3}`
- Optional semicolons (newline or `;`)

## JVM Targeting
- SPAD is transpiled to Java source.
- Java output can be compiled with `javac` and run on the JVM.
- Runtime helpers are provided through a prelude class.
- Dragon runtime metadata is included in generated Java classes.

## Dragon Toolkit Focus
- Compiler/tooling project name: `dragon`.
- Dragon is a separate program under `dragon/` and acts as SPAD manager (similar role to Maven/Gradle for SPAD projects).
- Tracks SPAD language versions (current bootstrap example: `0.1.0`).
- Package import model with central repositories:
  - `ftp://central`
  - `https://repo.spad-lang.org/ftp`
- Package usage inside SPAD uses:
  - `dragon = {presentation, graph, automation}`
- Module import syntax:
  - `import presentation from "ftp://central"`
  - `import presentation = ">=1.2.0" from "ftp://central"`
- Extension implementation languages currently enabled:
  - `spad`
  - `java`
  - `lua`

## Dragon Config + Lock
- `spad.toml` stores language/version, repositories, and extension languages.
- `dragon.lock` pins module versions and sources.
- Repository roots expose `PACKAGES.toml` and package sets: `T`, `S`, `P`, `A`, `D`, `L`.
- Set `L` is used to host full SPAD language and Dragon artifacts.
- Dragon resolves SPAD imports using:
  - version constraints from source
  - lockfile pins when compatible with semver constraints
  - configured ftp/http repositories and local package directories
  - local cache at `.dragon/cache` for pulled artifacts

## Dragon Commands (Current Prototype)
- `resolve`: parse config + lock + SPAD imports and print resolved endpoints.
- `compile`: run resolve and trigger SPAD transpile workflow.
- `install-spad`: install/manage SPAD runtime marker files.

## Built-in Runtime Helpers (Current Prototype)
- `print(x)`
- `toInt(x)`
- `toFloat(x)`
- `toString(x)`
- `dijkstra(graph, start)`
- `directive(name, ...args)` routing hook

## "Everything Java/C/Python" Strategy
A complete one-shot implementation is very large; the practical approach is staged while preserving compatibility goals.

### Phase 1
- Complete parser coverage for control-flow (`if`, `else`, `while`, `for`).
- Add type checker pass with strict operator/type rules.
- Expand prelude with string/list/dict/math APIs.
- Add Dragon lockfile and version manifest (`dragon.lock`, `spad.toml`).

### Phase 2
- Java interop layer (`java::package.Class.method(...)`).
- C-style low-level module (manual memory primitives behind safe wrappers).
- Python-style utility batteries (itertools-like and dict/list comprehensions).
- Lua extension packaging ABI for Dragon registry modules.

### Phase 3
- Module/package manager integrated with `project` blocks.
- Projection engine for build profiles (`projection dev = ...`, `projection prod = ...`).
- Directive-driven optimizer and automation pipelines.

## Example
```spad
project GraphLab {
  projection mode = "dev"

  automate shortestPath(graph = Dict, start = String) = Dict {
    directive optimize("hot-path")
    return dijkstra(graph, start)
  }
}
```

## Build
- Compile: `javac main/*.java`
- Run parser/transpiler demo: `java main.main`
