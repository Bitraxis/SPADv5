# SPAD

SPAD is a small JVM-targeted language for projects, automation, and directive-driven workflows. The repository contains the compiler, the runtime helpers it emits against, and the Dragon package/build tooling that sits next to the language core.

This README is a practical guide. It explains what each major file does, how the compiler pipeline works, and how to run the common workflows without needing to read the source first.

## What SPAD Is

SPAD is designed around a simple pipeline:

1. Source text is lexed into tokens.
2. Tokens are parsed into an AST.
3. The static checker validates types and control flow.
4. The emitter converts SPAD to Java.
5. `javac` compiles the generated Java into classes.
6. The runtime helpers in `main/SpadPrelude.java` and `main/DragonToolkit.java` provide the shared behavior that emitted programs use.

## Repository Layout

- `main/` contains the compiler front-end, AST, type checker, emitter, and runtime support.
- `dragon/` contains the Dragon package and lockfile tooling.
- `packages-local/` contains local package metadata and example packages.
- `scripts/` contains build and install entry points for Windows and Unix-like systems.
- `SPAD_SPEC.md` is the normative language and tooling specification.

## How The Compiler Fits Together

The compiler entrypoint in `main/main.java` is the best place to understand the executable flow. It does not implement language semantics itself; instead it orchestrates the pipeline.

```java
// The CLI entrypoint only catches top-level failures and prints one short message.
public static void main(String[] args) {
    try {
        run(args);
    } catch (Exception ex) {
        System.err.println("SPAD compile failed: " + ex.getMessage());
        System.exit(1);
    }
}
```

That block keeps user-facing failures readable. The real work happens in the next stage.

```java
// The compiler reads source, parses it, type-checks it, emits Java, compiles Java, and optionally runs the result.
private static void run(String[] args) throws Exception {
    if (args.length == 0 || hasArg(args, "--help") || hasArg(args, "-h")) {
        printUsage();
        return;
    }

    CliOptions options = parseOptions(args);
    String source = Files.readString(options.inputFile);
    CompileResult result = compileSource(source, options.className, options.outDir, options.inputFile);
}
```

The compiler pipeline is intentionally explicit so each step can be debugged separately.

## Quick Start

Build the project first.

```bat
scripts\build-all.cmd
```

Compile a SPAD file to JVM classes.

```bash
java main.main main/main.spad --out build/spad --class SpadProgram
```

Run the compiled program immediately after compilation.

```bash
java main.main main/main.spad --out build/spad --class SpadProgram --run
```

Emit a bundle with a `.jre` extension.

```bash
java main.main main/main.spad --out build/spad --class SpadProgram --jre
```

## CLI Options

The CLI accepts these common flags:

- `--out <dir>` sets the build output directory.
- `--class <Name>` sets the generated Java class name.
- `--run` runs the generated class after compilation.
- `--print-java` prints the generated Java source.
- `--jre` writes a runnable bundle.
- `--jre-out <file.jre>` overrides the bundle path.

The options parser in `main/main.java` is deliberately strict so invalid inputs fail early instead of producing confusing compiler output.

```java
// The parser validates the input file, derives a class name when needed, and stores every selected option in CliOptions.
private static CliOptions parseOptions(String[] args) {
    Path inputFile = null;
    Path outDir = Paths.get("build", "spad");
    String className = null;
    boolean runCompiledClass = false;
    boolean printJava = false;
    boolean emitJreBundle = false;
    Path jreOutputPath = null;
}
```

## Language Examples

### Variables and Types

```spad
// An untyped variable is inferred from its initializer.
var count = 10

// An explicit type annotation makes the intended value domain clear.
var total: Int = 0
```

The first line shows inference, while the second line shows a declaration that is checked against an explicit type.

### Functions

```spad
// The function keyword defines a reusable block with named, typed inputs.
function add(a: Int, b: Int): Int {
  return a + b
}
```

This example demonstrates the usual pattern: typed parameters, a typed return value, and an explicit `return` statement.

### Conditionals

```spad
// elif is the preferred chained-branch form.
if (score > 10) {
  print("high")
} elif (score > 5) {
  print("medium")
} else {
  print("low")
}
```

The language uses braces for blocks so the structure is always visually obvious.

### Loops and Ranges

```spad
// The range operator is inclusive in the standard for-range form.
for i in 0..10 {
  print(i)
}
```

This loop counts through the range and is designed to be easy to read in automation scripts.

### Error Handling

```spad
// try / except / finally gives the language a predictable error-handling shape.
try {
  riskyOperation()
} except (e) {
  handle(e)
} finally {
  cleanup()
}
```

The try block contains the risky code, the except block handles failure, and the finally block performs cleanup.

### Pipelines

```spad
// Pipelines pass the previous result into the next step for readable data flow.
var out = data |> normalize() |> summarize()
```

This pattern is useful when you want project logic to read left-to-right.

## Runtime Helpers

The emitted Java source uses shared helpers from the runtime layer.

```java
// The prelude gives emitted programs a small, reusable standard library.
DragonToolkit.bootstrap("SPAD", "0.1.0");
```

That bootstrap call is one of the first things emitted programs run so the runtime can initialize itself consistently.

The runtime currently provides helpers such as printing, truthiness checks, number conversion, string conversion, and a few collection utilities.

## Generated Output

A SPAD compilation usually produces:

- generated Java source under `build/spad/generated-src/`
- compiled `.class` files under `build/spad/classes/`
- optionally, a `.jre` bundle for distribution

That separation makes the compiler easier to inspect because you can look at the emitted Java before you decide whether to run it.

## Development Notes

- `SPAD_SPEC.md` is the language reference and should stay aligned with the parser, type checker, and emitter.
- `main/StaticTypeChecker.java` enforces language rules before code generation.
- `main/JavaEmitter.java` decides how SPAD maps to Java.
- `main/SpadPrelude.java` contains runtime behavior that emitted programs rely on.

If you are changing the language, update the spec and the implementation together so the docs do not drift away from the compiler behavior.

## Next Steps

1. Read `SPAD_SPEC.md` for the normative grammar and semantics.
2. Inspect `main/main.java` to see the compile pipeline end to end.
3. Run `scripts\build-all.cmd` after changes to verify the whole toolchain still compiles.
