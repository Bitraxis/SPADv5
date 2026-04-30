package main;

import java.util.List;

// A Program is the root container for all statements in a SPAD file.
class Program {
    public final List<Stmt> statements;

    // The parser builds the full statement list before later phases inspect it.
    public Program(List<Stmt> statements) {
        this.statements = statements;
    }
}

// All executable and declarative constructs derive from Stmt.
abstract class Stmt {
}

// Variable declarations introduce a name, optional explicit type, and
// initializer.
class VarDecl extends Stmt {
    public final String name;
    public final String explicitType;
    public final Expr initializer;

    // Variables are immutable AST records once parsed.
    public VarDecl(String name, String explicitType, Expr initializer) {
        this.name = name;
        this.explicitType = explicitType;
        this.initializer = initializer;
    }
}

// Imports record external modules, version constraints, and source locations.
class ImportStmt extends Stmt {
    public final String module;
    public final String versionConstraint;
    public final String source;

    // The compiler later resolves these fields into toolkit metadata.
    public ImportStmt(String module, String versionConstraint, String source) {
        this.module = module;
        this.versionConstraint = versionConstraint;
        this.source = source;
    }
}

// Dragon use statements enable package-set selection for builds and tooling.
class DragonUseStmt extends Stmt {
    public final List<String> packages;

    // The list is preserved in source order for predictable tooling output.
    public DragonUseStmt(List<String> packages) {
        this.packages = packages;
    }
}

// Function declarations contain a name, typed parameters, a return type, and a
// body.
class FuncDecl extends Stmt {
    public final String name;
    public final List<Parameter> parameters;
    public final String returnType;
    public final List<Stmt> body;

    // Functions are modeled directly so the emitter can translate them to Java
    // methods.
    public FuncDecl(String name, List<Parameter> parameters, String returnType, List<Stmt> body) {
        this.name = name;
        this.parameters = parameters;
        this.returnType = returnType;
        this.body = body;
    }
}

// Project blocks group related declarations and configuration together.
class ProjectDecl extends Stmt {
    public final String name;
    public final List<Stmt> body;

    // The body is stored as a nested statement list so the compiler can recurse
    // naturally.
    public ProjectDecl(String name, List<Stmt> body) {
        this.name = name;
        this.body = body;
    }
}

// Projection declarations bind a symbolic name to a compile-time value.
class ProjectionDecl extends Stmt {
    public final String name;
    public final Expr value;

    // Projections are treated as structured metadata rather than runtime
    // statements.
    public ProjectionDecl(String name, Expr value) {
        this.name = name;
        this.value = value;
    }
}

// Automate blocks represent reusable automation entrypoints that compile like
// functions.
class AutomateDecl extends Stmt {
    public final String name;
    public final List<Parameter> parameters;
    public final String returnType;
    public final List<Stmt> body;

    // Automation blocks reuse the same parameter and return-type model as
    // functions.
    public AutomateDecl(String name, List<Parameter> parameters, String returnType, List<Stmt> body) {
        this.name = name;
        this.parameters = parameters;
        this.returnType = returnType;
        this.body = body;
    }
}

// Export blocks package a nested namespace of declarations for external use.
class ExportDecl extends Stmt {
    public final String name;
    public final List<Stmt> body;

    // The nested body is kept intact so exporters can mirror the source structure.
    public ExportDecl(String name, List<Stmt> body) {
        this.name = name;
        this.body = body;
    }
}

// Directive statements are side-effecting control instructions for the
// toolchain.
class DirectiveStmt extends Stmt {
    public final String name;
    public final List<Expr> arguments;

    // Arguments remain generic because directives are intentionally open-ended.
    public DirectiveStmt(String name, List<Expr> arguments) {
        this.name = name;
        this.arguments = arguments;
    }
}

// Parameters capture the name and declared type used by callable declarations.
class Parameter {
    public final String name;
    public final String type;

    // Parameters are simple value objects used by both parsing and type checking.
    public Parameter(String name, String type) {
        this.name = name;
        this.type = type;
    }
}

// Return statements optionally carry a value depending on the function's
// declared type.
class ReturnStmt extends Stmt {
    public final Expr value;

    // A null value means the statement is a bare return.
    public ReturnStmt(Expr value) {
        this.value = value;
    }
}

// Expression statements evaluate for side effects or to feed a pipeline.
class ExprStmt extends Stmt {
    public final Expr expression;

    // The AST preserves the expression verbatim for later checks and emission.
    public ExprStmt(Expr expression) {
        this.expression = expression;
    }
}

// Blocks create a new lexical scope and hold a list of nested statements.
class BlockStmt extends Stmt {
    public final List<Stmt> statements;

    // Statements stay in source order so execution and diagnostics remain
    // predictable.
    public BlockStmt(List<Stmt> statements) {
        this.statements = statements;
    }
}

// Conditional statements store a condition plus then/else branches.
class IfStmt extends Stmt {
    public final Expr condition;
    public final Stmt thenBranch;
    public final Stmt elseBranch;

    // The else branch may itself be another IfStmt after elif desugaring.
    public IfStmt(Expr condition, Stmt thenBranch, Stmt elseBranch) {
        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }
}

// While statements repeat a body while their condition remains truthy.
class WhileStmt extends Stmt {
    public final Expr condition;
    public final Stmt body;

    // The body is stored as a single statement so either a block or a single line
    // can be used.
    public WhileStmt(Expr condition, Stmt body) {
        this.condition = condition;
        this.body = body;
    }
}

// For-range statements represent inclusive numeric iteration.
class ForRangeStmt extends Stmt {
    public final String variable;
    public final Expr start;
    public final Expr end;
    public final Stmt body;

    // The parser keeps the loop variable name separate so the emitter can generate
    // Java locals.
    public ForRangeStmt(String variable, Expr start, Expr end, Stmt body) {
        this.variable = variable;
        this.start = start;
        this.end = end;
        this.body = body;
    }
}

// Expressions are the value-producing half of the AST.
abstract class Expr {
}

// Literal expressions store parsed constants such as numbers, strings, and
// booleans.
class LiteralExpr extends Expr {
    public final Object value;

    // The literal value is preserved exactly as parsed.
    public LiteralExpr(Object value) {
        this.value = value;
    }
}

// Variable expressions refer to previously declared names.
class VariableExpr extends Expr {
    public final String name;

    // A variable node is just the resolved name.
    public VariableExpr(String name) {
        this.name = name;
    }
}

// Assignment expressions update a previously declared variable.
class AssignExpr extends Expr {
    public final String name;
    public final Expr value;

    // The assignment target is kept as a name for straightforward code generation.
    public AssignExpr(String name, Expr value) {
        this.name = name;
        this.value = value;
    }
}

// Unary expressions apply a single operator to one operand.
class UnaryExpr extends Expr {
    public final Token operator;
    public final Expr right;

    // The token is retained so the emitter can preserve the original operator
    // choice.
    public UnaryExpr(Token operator, Expr right) {
        this.operator = operator;
        this.right = right;
    }
}

// Binary expressions represent operators with a left and right operand.
class BinaryExpr extends Expr {
    public final Expr left;
    public final Token operator;
    public final Expr right;

    // Binary nodes are the backbone of arithmetic, comparison, logical, and concat
    // operations.
    public BinaryExpr(Expr left, Token operator, Expr right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }
}

// Grouping expressions preserve explicit parentheses from source code.
class GroupingExpr extends Expr {
    public final Expr expression;

    // Parentheses are semantic no-ops here, but the AST still records them.
    public GroupingExpr(Expr expression) {
        this.expression = expression;
    }
}

// Call expressions invoke a callee with a list of arguments.
class CallExpr extends Expr {
    public final Expr callee;
    public final List<Expr> arguments;

    // The callee may be a variable, a field access, or another expression.
    public CallExpr(Expr callee, List<Expr> arguments) {
        this.callee = callee;
        this.arguments = arguments;
    }
}

// Get expressions model dotted property access.
class GetExpr extends Expr {
    public final Expr object;
    public final String name;

    // The name is stored separately so the emitter can render object.member.
    public GetExpr(Expr object, String name) {
        this.object = object;
        this.name = name;
    }
}

// List expressions keep element order intact.
class ListExpr extends Expr {
    public final List<Expr> elements;

    // Lists are captured as ordered expression sequences.
    public ListExpr(List<Expr> elements) {
        this.elements = elements;
    }
}

// Dict expressions store key/value pairs as parsed.
class DictExpr extends Expr {
    public final List<DictEntry> entries;

    // Dictionary entries are preserved exactly so the emitter can rebuild the
    // literal.
    public DictExpr(List<DictEntry> entries) {
        this.entries = entries;
    }
}

// A dictionary entry is a single string key paired with a value expression.
class DictEntry {
    public final String key;
    public final Expr value;

    // Keys are normalized to strings during parsing.
    public DictEntry(String key, Expr value) {
        this.key = key;
        this.value = value;
    }
}

// Match expressions select one result from several pattern arms.
class MatchExpr extends Expr {
    public final Expr value;
    public final List<MatchArm> arms;

    // The subject expression is stored once and shared by all arms.
    public MatchExpr(Expr value, List<MatchArm> arms) {
        this.value = value;
        this.arms = arms;
    }
}

// Each match arm records its pattern, optional guard, and result expression.
class MatchArm {
    public final boolean wildcard;
    public final Expr pattern;
    public final Expr guard;
    public final Expr result;

    // Wildcards are tracked explicitly so the type checker can enforce arm order.
    public MatchArm(boolean wildcard, Expr pattern, Expr guard, Expr result) {
        this.wildcard = wildcard;
        this.pattern = pattern;
        this.guard = guard;
        this.result = result;
    }
}

// Try statements carry separate bodies for the protected block, exception
// block, and finally block.
class TryStmt extends Stmt {
    public final List<Stmt> tryBody;
    public final String exceptionName; // may be null
    public final List<Stmt> exceptBody; // may be null
    public final List<Stmt> finallyBody; // may be null

    // The nullable fields model optional except/finally sections without extra
    // wrapper nodes.
    public TryStmt(List<Stmt> tryBody, String exceptionName, List<Stmt> exceptBody, List<Stmt> finallyBody) {
        this.tryBody = tryBody;
        this.exceptionName = exceptionName;
        this.exceptBody = exceptBody;
        this.finallyBody = finallyBody;
    }
}

// Java interop expressions point directly at qualified Java symbols.
class JavaInteropExpr extends Expr {
    public final String qualifiedName;

    // The emitter can inject the qualified Java name as-is.
    public JavaInteropExpr(String qualifiedName) {
        this.qualifiedName = qualifiedName;
    }
}
