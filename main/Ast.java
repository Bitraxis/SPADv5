package main;

import java.util.List;

class Program {
    public final List<Stmt> statements;

    public Program(List<Stmt> statements) {
        this.statements = statements;
    }
}

abstract class Stmt {}

class VarDecl extends Stmt {
    public final String name;
    public final String explicitType;
    public final Expr initializer;

    public VarDecl(String name, String explicitType, Expr initializer) {
        this.name = name;
        this.explicitType = explicitType;
        this.initializer = initializer;
    }
}

class ImportStmt extends Stmt {
    public final String module;
    public final String versionConstraint;
    public final String source;

    public ImportStmt(String module, String versionConstraint, String source) {
        this.module = module;
        this.versionConstraint = versionConstraint;
        this.source = source;
    }
}

class DragonUseStmt extends Stmt {
    public final List<String> packages;

    public DragonUseStmt(List<String> packages) {
        this.packages = packages;
    }
}

class FuncDecl extends Stmt {
    public final String name;
    public final List<Parameter> parameters;
    public final String returnType;
    public final List<Stmt> body;

    public FuncDecl(String name, List<Parameter> parameters, String returnType, List<Stmt> body) {
        this.name = name;
        this.parameters = parameters;
        this.returnType = returnType;
        this.body = body;
    }
}

class ProjectDecl extends Stmt {
    public final String name;
    public final List<Stmt> body;

    public ProjectDecl(String name, List<Stmt> body) {
        this.name = name;
        this.body = body;
    }
}

class ProjectionDecl extends Stmt {
    public final String name;
    public final Expr value;

    public ProjectionDecl(String name, Expr value) {
        this.name = name;
        this.value = value;
    }
}

class AutomateDecl extends Stmt {
    public final String name;
    public final List<Parameter> parameters;
    public final String returnType;
    public final List<Stmt> body;

    public AutomateDecl(String name, List<Parameter> parameters, String returnType, List<Stmt> body) {
        this.name = name;
        this.parameters = parameters;
        this.returnType = returnType;
        this.body = body;
    }
}

class ExportDecl extends Stmt {
    public final String name;
    public final List<Stmt> body;

    public ExportDecl(String name, List<Stmt> body) {
        this.name = name;
        this.body = body;
    }
}

class DirectiveStmt extends Stmt {
    public final String name;
    public final List<Expr> arguments;

    public DirectiveStmt(String name, List<Expr> arguments) {
        this.name = name;
        this.arguments = arguments;
    }
}

class Parameter {
    public final String name;
    public final String type;

    public Parameter(String name, String type) {
        this.name = name;
        this.type = type;
    }
}

class ReturnStmt extends Stmt {
    public final Expr value;

    public ReturnStmt(Expr value) {
        this.value = value;
    }
}

class ExprStmt extends Stmt {
    public final Expr expression;

    public ExprStmt(Expr expression) {
        this.expression = expression;
    }
}

class BlockStmt extends Stmt {
    public final List<Stmt> statements;

    public BlockStmt(List<Stmt> statements) {
        this.statements = statements;
    }
}

class IfStmt extends Stmt {
    public final Expr condition;
    public final Stmt thenBranch;
    public final Stmt elseBranch;

    public IfStmt(Expr condition, Stmt thenBranch, Stmt elseBranch) {
        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }
}

class WhileStmt extends Stmt {
    public final Expr condition;
    public final Stmt body;

    public WhileStmt(Expr condition, Stmt body) {
        this.condition = condition;
        this.body = body;
    }
}

class ForRangeStmt extends Stmt {
    public final String variable;
    public final Expr start;
    public final Expr end;
    public final Stmt body;

    public ForRangeStmt(String variable, Expr start, Expr end, Stmt body) {
        this.variable = variable;
        this.start = start;
        this.end = end;
        this.body = body;
    }
}

abstract class Expr {}

class LiteralExpr extends Expr {
    public final Object value;

    public LiteralExpr(Object value) {
        this.value = value;
    }
}

class VariableExpr extends Expr {
    public final String name;

    public VariableExpr(String name) {
        this.name = name;
    }
}

class AssignExpr extends Expr {
    public final String name;
    public final Expr value;

    public AssignExpr(String name, Expr value) {
        this.name = name;
        this.value = value;
    }
}

class UnaryExpr extends Expr {
    public final Token operator;
    public final Expr right;

    public UnaryExpr(Token operator, Expr right) {
        this.operator = operator;
        this.right = right;
    }
}

class BinaryExpr extends Expr {
    public final Expr left;
    public final Token operator;
    public final Expr right;

    public BinaryExpr(Expr left, Token operator, Expr right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }
}

class GroupingExpr extends Expr {
    public final Expr expression;

    public GroupingExpr(Expr expression) {
        this.expression = expression;
    }
}

class CallExpr extends Expr {
    public final Expr callee;
    public final List<Expr> arguments;

    public CallExpr(Expr callee, List<Expr> arguments) {
        this.callee = callee;
        this.arguments = arguments;
    }
}

class GetExpr extends Expr {
    public final Expr object;
    public final String name;

    public GetExpr(Expr object, String name) {
        this.object = object;
        this.name = name;
    }
}

class ListExpr extends Expr {
    public final List<Expr> elements;

    public ListExpr(List<Expr> elements) {
        this.elements = elements;
    }
}

class DictExpr extends Expr {
    public final List<DictEntry> entries;

    public DictExpr(List<DictEntry> entries) {
        this.entries = entries;
    }
}

class DictEntry {
    public final String key;
    public final Expr value;

    public DictEntry(String key, Expr value) {
        this.key = key;
        this.value = value;
    }
}

class MatchExpr extends Expr {
    public final Expr value;
    public final List<MatchArm> arms;

    public MatchExpr(Expr value, List<MatchArm> arms) {
        this.value = value;
        this.arms = arms;
    }
}

class MatchArm {
    public final boolean wildcard;
    public final Expr pattern;
    public final Expr guard;
    public final Expr result;

    public MatchArm(boolean wildcard, Expr pattern, Expr guard, Expr result) {
        this.wildcard = wildcard;
        this.pattern = pattern;
        this.guard = guard;
        this.result = result;
    }
}

class JavaInteropExpr extends Expr {
    public final String qualifiedName;

    public JavaInteropExpr(String qualifiedName) {
        this.qualifiedName = qualifiedName;
    }
}

