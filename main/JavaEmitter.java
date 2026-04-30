package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// The emitter converts the checked SPAD AST into Java source code.
class JavaEmitter {
    private int tempCounter = 0;
    private final Map<String, String> knownFunctionReturnTypes = new HashMap<>();

    // Emit a program using the default package name.
    public String emitProgram(Program program, String className) {
        return emitProgram(program, className, null);
    }

    // Emit a complete Java compilation unit from the SPAD program.
    public String emitProgram(Program program, String className, String packageName) {
        knownFunctionReturnTypes.clear();
        seedKnownFunctionReturnTypes(program.statements);

        StringBuilder out = new StringBuilder();
        if (packageName != null && !packageName.isBlank()) {
            out.append("package ").append(packageName).append(";\n\n");
        }
        out.append("import java.util.*;\n\n");
        out.append("public class ").append(className).append(" {\n");
        out.append(indent(1)).append("static {\n");
        out.append(indent(2)).append("DragonToolkit.bootstrap(\"SPAD\", \"0.1.0\");\n");
        out.append(indent(1)).append("}\n\n");

        List<Stmt> topLevel = new ArrayList<>();
        for (Stmt stmt : program.statements) {
            if (stmt instanceof FuncDecl) {
                emitFunction((FuncDecl) stmt, out, 1);
                out.append("\n");
            } else if (stmt instanceof AutomateDecl) {
                emitAutomate((AutomateDecl) stmt, out, 1);
                out.append("\n");
            } else if (stmt instanceof ProjectDecl) {
                emitProject((ProjectDecl) stmt, out, 1);
                out.append("\n");
            } else if (stmt instanceof ExportDecl) {
                emitExport((ExportDecl) stmt, out, 1);
                out.append("\n");
            } else {
                topLevel.add(stmt);
            }
        }

        out.append(indent(1)).append("public static void main(String[] args) {\n");
        for (Stmt stmt : topLevel) {
            emitStatement(stmt, out, 2);
        }
        out.append(indent(1)).append("}\n");
        out.append("}\n");
        return out.toString();
    }

    // Projects become nested static container classes.
    private void emitProject(ProjectDecl project, StringBuilder out, int level) {
        out.append(indent(level)).append("public static class Project_").append(project.name).append(" {\n");
        for (Stmt stmt : project.body) {
            if (stmt instanceof FuncDecl) {
                emitFunction((FuncDecl) stmt, out, level + 1);
                out.append("\n");
            } else if (stmt instanceof AutomateDecl) {
                emitAutomate((AutomateDecl) stmt, out, level + 1);
                out.append("\n");
            } else if (stmt instanceof ProjectionDecl) {
                ProjectionDecl p = (ProjectionDecl) stmt;
                out.append(indent(level + 1))
                        .append("public static final Object ")
                        .append(p.name)
                        .append(" = ")
                        .append(emitExpr(p.value))
                        .append(";\n");
            } else {
                emitStatement(stmt, out, level + 1);
            }
        }
        out.append(indent(level)).append("}\n");
    }

    // Automation declarations are emitted as Java static methods.
    private void emitAutomate(AutomateDecl auto, StringBuilder out, int level) {
        String params = auto.parameters.stream()
                .map(p -> mapType(p.type) + " " + p.name)
                .collect(Collectors.joining(", "));

        out.append(indent(level))
                .append("public static ")
                .append(mapType(auto.returnType))
                .append(" ")
                .append(auto.name)
                .append("(")
                .append(params)
                .append(") {\n");

        for (Stmt stmt : auto.body) {
            emitStatement(stmt, out, level + 1);
        }

        if ("void".equals(mapType(auto.returnType))) {
            out.append(indent(level + 1)).append("return;\n");
        }

        out.append(indent(level)).append("}\n");
    }

    // Export blocks also become nested static container classes.
    private void emitExport(ExportDecl export, StringBuilder out, int level) {
        out.append(indent(level)).append("public static class Export_").append(export.name).append(" {\n");
        for (Stmt stmt : export.body) {
            if (stmt instanceof FuncDecl) {
                emitFunction((FuncDecl) stmt, out, level + 1);
                out.append("\n");
            } else if (stmt instanceof AutomateDecl) {
                emitAutomate((AutomateDecl) stmt, out, level + 1);
                out.append("\n");
            } else if (stmt instanceof ProjectionDecl) {
                ProjectionDecl p = (ProjectionDecl) stmt;
                out.append(indent(level + 1))
                        .append("public static final Object ")
                        .append(p.name)
                        .append(" = ")
                        .append(emitExpr(p.value))
                        .append(";\n");
            } else {
                emitStatement(stmt, out, level + 1);
            }
        }
        out.append(indent(level)).append("}\n");
    }

    // Standard functions map directly to Java static methods.
    private void emitFunction(FuncDecl fn, StringBuilder out, int level) {
        String params = fn.parameters.stream()
                .map(p -> mapType(p.type) + " " + p.name)
                .collect(Collectors.joining(", "));

        out.append(indent(level))
                .append("public static ")
                .append(mapType(fn.returnType))
                .append(" ")
                .append(fn.name)
                .append("(")
                .append(params)
                .append(") {\n");

        for (Stmt stmt : fn.body) {
            emitStatement(stmt, out, level + 1);
        }

        if ("void".equals(mapType(fn.returnType))) {
            out.append(indent(level + 1)).append("return;\n");
        }

        out.append(indent(level)).append("}\n");
    }

    // Each statement kind is rendered into the closest Java equivalent.
    private void emitStatement(Stmt stmt, StringBuilder out, int level) {
        if (stmt instanceof ImportStmt) {
            ImportStmt imp = (ImportStmt) stmt;
            out.append(indent(level))
                    .append("DragonToolkit.importModule(")
                    .append(quote(imp.module))
                    .append(", ")
                    .append(quote(imp.versionConstraint))
                    .append(", ")
                    .append(quote(imp.source))
                    .append(");\n");
            return;
        }
        if (stmt instanceof TryStmt) {
            TryStmt t = (TryStmt) stmt;
            out.append(indent(level)).append("try {\n");
            for (Stmt s : t.tryBody) {
                emitStatement(s, out, level + 1);
            }
            out.append(indent(level)).append("}\n");
            if (t.exceptBody != null) {
                String exName = t.exceptionName == null ? "e" : t.exceptionName;
                out.append(" catch (Exception ").append(exName).append(") {\n");
                for (Stmt s : t.exceptBody) {
                    emitStatement(s, out, level + 1);
                }
                out.append(indent(level)).append("}\n");
            }
            if (t.finallyBody != null) {
                out.append(" finally {\n");
                for (Stmt s : t.finallyBody) {
                    emitStatement(s, out, level + 1);
                }
                out.append(indent(level)).append("}\n");
            } else {
                out.append("\n");
            }
            return;
        }
        if (stmt instanceof DragonUseStmt) {
            DragonUseStmt dragonUse = (DragonUseStmt) stmt;
            String args = dragonUse.packages.stream().map(this::quote).collect(Collectors.joining(", "));
            out.append(indent(level))
                    .append("DragonToolkit.usePackages(Arrays.asList(")
                    .append(args)
                    .append("));\n");
            return;
        }
        if (stmt instanceof VarDecl) {
            VarDecl decl = (VarDecl) stmt;
            String type = decl.explicitType == null ? "var" : mapType(decl.explicitType);
            out.append(indent(level))
                    .append(type)
                    .append(" ")
                    .append(decl.name)
                    .append(" = ")
                    .append(emitExpr(decl.initializer))
                    .append(";\n");
            return;
        }
        if (stmt instanceof ProjectionDecl) {
            ProjectionDecl projection = (ProjectionDecl) stmt;
            out.append(indent(level))
                    .append("final var ")
                    .append(projection.name)
                    .append(" = ")
                    .append(emitExpr(projection.value))
                    .append(";\n");
            return;
        }
        if (stmt instanceof DirectiveStmt) {
            DirectiveStmt directive = (DirectiveStmt) stmt;
            String args = directive.arguments.stream().map(this::emitExpr).collect(Collectors.joining(", "));
            out.append(indent(level))
                    .append("SpadPrelude.directive(")
                    .append(quote(directive.name));
            if (!args.isEmpty()) {
                out.append(", ").append(args);
            }
            out.append(");\n");
            return;
        }
        if (stmt instanceof ReturnStmt) {
            ReturnStmt ret = (ReturnStmt) stmt;
            out.append(indent(level)).append("return");
            if (ret.value != null) {
                out.append(" ").append(emitExpr(ret.value));
            }
            out.append(";\n");
            return;
        }
        if (stmt instanceof ExprStmt) {
            out.append(indent(level)).append(emitExpr(((ExprStmt) stmt).expression)).append(";\n");
            return;
        }
        if (stmt instanceof BlockStmt) {
            out.append(indent(level)).append("{\n");
            for (Stmt inner : ((BlockStmt) stmt).statements) {
                emitStatement(inner, out, level + 1);
            }
            out.append(indent(level)).append("}\n");
            return;
        }
        if (stmt instanceof IfStmt) {
            IfStmt ifStmt = (IfStmt) stmt;
            out.append(indent(level)).append("if (SpadPrelude.truthy(")
                    .append(emitExpr(ifStmt.condition))
                    .append(")) ");
            emitBranchBody(ifStmt.thenBranch, out, level);
            if (ifStmt.elseBranch != null) {
                out.append(indent(level)).append("else ");
                emitBranchBody(ifStmt.elseBranch, out, level);
            }
            return;
        }
        if (stmt instanceof WhileStmt) {
            WhileStmt whileStmt = (WhileStmt) stmt;
            out.append(indent(level)).append("while (SpadPrelude.truthy(")
                    .append(emitExpr(whileStmt.condition))
                    .append(")) ");
            emitBranchBody(whileStmt.body, out, level);
            return;
        }
        if (stmt instanceof ForRangeStmt) {
            ForRangeStmt forStmt = (ForRangeStmt) stmt;
            String startVar = nextTemp("start");
            String endVar = nextTemp("end");
            String stepVar = nextTemp("step");
            out.append(indent(level)).append("{\n");
            out.append(indent(level + 1)).append("int ").append(startVar).append(" = SpadPrelude.toInt(")
                    .append(emitExpr(forStmt.start)).append(");\n");
            out.append(indent(level + 1)).append("int ").append(endVar).append(" = SpadPrelude.toInt(")
                    .append(emitExpr(forStmt.end)).append(");\n");
            out.append(indent(level + 1)).append("int ").append(stepVar).append(" = ")
                    .append(startVar).append(" <= ").append(endVar).append(" ? 1 : -1;\n");
            out.append(indent(level + 1)).append("for (int ").append(forStmt.variable).append(" = ").append(startVar)
                    .append("; ")
                    .append(stepVar).append(" > 0 ? ").append(forStmt.variable).append(" <= ").append(endVar)
                    .append(" : ").append(forStmt.variable).append(" >= ").append(endVar)
                    .append("; ").append(forStmt.variable).append(" += ").append(stepVar).append(") ");
            emitBranchBody(forStmt.body, out, level + 1);
            out.append(indent(level)).append("}\n");
            return;
        }
        if (stmt instanceof FuncDecl) {
            emitFunction((FuncDecl) stmt, out, level);
            return;
        }
        if (stmt instanceof AutomateDecl) {
            emitAutomate((AutomateDecl) stmt, out, level);
            return;
        }
        if (stmt instanceof ExportDecl) {
            emitExport((ExportDecl) stmt, out, level);
            return;
        }

        out.append(indent(level)).append("/* Unsupported statement */\n");
    }

    // Expressions are emitted recursively so nested operations stay faithful to
    // source order.
    private String emitExpr(Expr expr) {
        if (expr instanceof LiteralExpr) {
            Object value = ((LiteralExpr) expr).value;
            if (value == null)
                return "null";
            if (value instanceof String)
                return quote((String) value);
            return String.valueOf(value);
        }
        if (expr instanceof VariableExpr) {
            return ((VariableExpr) expr).name;
        }
        if (expr instanceof AssignExpr) {
            AssignExpr assign = (AssignExpr) expr;
            return assign.name + " = " + emitExpr(assign.value);
        }
        if (expr instanceof UnaryExpr) {
            UnaryExpr unary = (UnaryExpr) expr;
            if (unary.operator.type == TokenType.BANG) {
                return "!SpadPrelude.truthy(" + emitExpr(unary.right) + ")";
            }
            if (unary.operator.type == TokenType.NOT) {
                return "!SpadPrelude.truthy(" + emitExpr(unary.right) + ")";
            }
            if (unary.operator.type == TokenType.MINUS) {
                return "(-" + emitExpr(unary.right) + ")";
            }
            return unary.operator.lexeme + emitExpr(unary.right);
        }
        if (expr instanceof BinaryExpr) {
            BinaryExpr binary = (BinaryExpr) expr;
            if (binary.operator.type == TokenType.EQUAL_EQUAL) {
                return "SpadPrelude.eq(" + emitExpr(binary.left) + ", " + emitExpr(binary.right) + ")";
            }
            if (binary.operator.type == TokenType.BANG_EQUAL) {
                return "(!SpadPrelude.eq(" + emitExpr(binary.left) + ", " + emitExpr(binary.right) + "))";
            }
            if (binary.operator.type == TokenType.AND) {
                return "(SpadPrelude.truthy(" + emitExpr(binary.left) + ") && SpadPrelude.truthy("
                        + emitExpr(binary.right) + "))";
            }
            if (binary.operator.type == TokenType.OR) {
                return "(SpadPrelude.truthy(" + emitExpr(binary.left) + ") || SpadPrelude.truthy("
                        + emitExpr(binary.right) + "))";
            }
            if (binary.operator.type == TokenType.RANGE) {
                // '..' used as concatenation in expressions
                return "(SpadPrelude.toStringValue(" + emitExpr(binary.left) + ") + SpadPrelude.toStringValue("
                        + emitExpr(binary.right) + "))";
            }
            return "(" + emitExpr(binary.left) + " " + mapOperator(binary.operator) + " " + emitExpr(binary.right)
                    + ")";
        }
        if (expr instanceof GroupingExpr) {
            return "(" + emitExpr(((GroupingExpr) expr).expression) + ")";
        }
        if (expr instanceof CallExpr) {
            CallExpr call = (CallExpr) expr;
            if (call.callee instanceof VariableExpr) {
                String callee = ((VariableExpr) call.callee).name;
                if ("print".equals(callee)) {
                    String arg = call.arguments.isEmpty() ? "\"\"" : emitExpr(call.arguments.get(0));
                    return "SpadPrelude.print(" + arg + ")";
                }
                if ("toInt".equals(callee) && call.arguments.size() == 1) {
                    return "SpadPrelude.toInt(" + emitExpr(call.arguments.get(0)) + ")";
                }
                if ("toFloat".equals(callee) && call.arguments.size() == 1) {
                    return "SpadPrelude.toFloat(" + emitExpr(call.arguments.get(0)) + ")";
                }
                if ("toString".equals(callee) && call.arguments.size() == 1) {
                    return "SpadPrelude.toStringValue(" + emitExpr(call.arguments.get(0)) + ")";
                }
                if ("dijkstra".equals(callee)) {
                    String args = call.arguments.stream().map(this::emitExpr).collect(Collectors.joining(", "));
                    return "SpadPrelude.dijkstra(" + args + ")";
                }
            }
            String args = call.arguments.stream().map(this::emitExpr).collect(Collectors.joining(", "));
            return emitExpr(call.callee) + "(" + args + ")";
        }
        if (expr instanceof JavaInteropExpr) {
            return ((JavaInteropExpr) expr).qualifiedName;
        }
        if (expr instanceof GetExpr) {
            GetExpr get = (GetExpr) expr;
            return emitExpr(get.object) + "." + get.name;
        }
        if (expr instanceof MatchExpr) {
            MatchExpr match = (MatchExpr) expr;
            String matchVar = nextTemp("match");
            String genericType = inferMatchGenericType(match);
            StringBuilder out = new StringBuilder();
            out.append("((java.util.function.Supplier<").append(genericType).append(">) () -> { ");
            out.append("Object ").append(matchVar).append(" = ").append(emitExpr(match.value)).append("; ");
            out.append("Object it = ").append(matchVar).append("; ");
            for (MatchArm arm : match.arms) {
                String armCondition;
                if (arm.wildcard) {
                    armCondition = "true";
                } else {
                    armCondition = "Objects.equals(" + matchVar + ", " + emitExpr(arm.pattern) + ")";
                }
                if (arm.guard != null) {
                    armCondition = "(" + armCondition + " && SpadPrelude.truthy(" + emitExpr(arm.guard) + "))";
                }
                out.append("if (").append(armCondition).append(") return ")
                        .append(emitExpr(arm.result)).append("; ");
            }
            out.append("throw new RuntimeException(\"No match arm for value: \" + ").append(matchVar).append("); ");
            out.append("}).get()");
            return out.toString();
        }
        if (expr instanceof ListExpr) {
            ListExpr list = (ListExpr) expr;
            String args = list.elements.stream().map(this::emitExpr).collect(Collectors.joining(", "));
            return "new ArrayList<>(Arrays.asList(" + args + "))";
        }
        if (expr instanceof DictExpr) {
            DictExpr dict = (DictExpr) expr;
            if (dict.entries.isEmpty()) {
                return "new HashMap<>()";
            }
            String entries = dict.entries.stream()
                    .map(e -> "Map.entry(" + quote(e.key) + ", " + emitExpr(e.value) + ")")
                    .collect(Collectors.joining(", "));
            return "new HashMap<>(Map.ofEntries(" + entries + "))";
        }
        return "/* unsupported expr */ null";
    }

    // SPAD types are mapped to their Java runtime representation.
    private String mapType(String type) {
        switch (type) {
            case "Int":
                return "int";
            case "Float":
                return "double";
            case "String":
                return "String";
            case "Bool":
                return "boolean";
            case "List":
                return "List<Object>";
            case "Dict":
                return "Map<String, Object>";
            case "Void":
                return "void";
            default:
                return "Object";
        }
    }

    // Java string literals need escaping before insertion into generated code.
    private String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    // Some token lexemes are mapped to Java operator text for cleaner emission.
    private String mapOperator(Token operator) {
        return switch (operator.type) {
            case PLUS -> "+";
            case MINUS -> "-";
            case STAR -> "*";
            case SLASH -> "/";
            case PERCENT -> "%";
            case GREATER -> ">";
            case GREATER_EQUAL -> ">=";
            case LESS -> "<";
            case LESS_EQUAL -> "<=";
            default -> operator.lexeme;
        };
    }

    // Indentation is generated with spaces so the output Java stays readable.
    private String indent(int level) {
        return "    ".repeat(Math.max(0, level));
    }

    // Branch bodies are wrapped consistently whether the source used a block or a
    // single statement.
    private void emitBranchBody(Stmt body, StringBuilder out, int level) {
        if (body instanceof BlockStmt) {
            out.append("{\n");
            for (Stmt inner : ((BlockStmt) body).statements) {
                emitStatement(inner, out, level + 1);
            }
            out.append(indent(level)).append("}\n");
        } else {
            out.append("{\n");
            emitStatement(body, out, level + 1);
            out.append(indent(level)).append("}\n");
        }
    }

    // Temporary names avoid collisions in emitted helper code.
    private String nextTemp(String prefix) {
        tempCounter++;
        return "__" + prefix + "_" + tempCounter;
    }

    // Match expressions need a best-effort Java generic type for the lambda
    // wrapper.
    private String inferMatchGenericType(MatchExpr match) {
        String concrete = null;
        for (MatchArm arm : match.arms) {
            String current = inferExprType(arm.result);
            if ("Object".equals(current)) {
                continue;
            }
            if (concrete == null) {
                concrete = current;
                continue;
            }
            if (!concrete.equals(current)) {
                return "Object";
            }
        }
        if (concrete == null) {
            return "Object";
        }
        return boxType(concrete);
    }

    // Lightweight type inference helps the emitter choose the best Java operator
    // shape.
    private String inferExprType(Expr expr) {
        if (expr instanceof LiteralExpr) {
            Object value = ((LiteralExpr) expr).value;
            if (value instanceof Long || value instanceof Integer) {
                return "int";
            }
            if (value instanceof Double || value instanceof Float) {
                return "double";
            }
            if (value instanceof Boolean) {
                return "boolean";
            }
            if (value instanceof String) {
                return "String";
            }
            if (value == null) {
                return "Object";
            }
        }
        if (expr instanceof UnaryExpr) {
            UnaryExpr u = (UnaryExpr) expr;
            if (u.operator.type == TokenType.BANG || u.operator.type == TokenType.NOT)
                return "boolean";
            return inferExprType(u.right);
        }
        if (expr instanceof BinaryExpr) {
            BinaryExpr binary = (BinaryExpr) expr;
            if (binary.operator.type == TokenType.AND || binary.operator.type == TokenType.OR) {
                return "boolean";
            }
            if (binary.operator.type == TokenType.RANGE) {
                return "String";
            }
            String left = inferExprType(binary.left);
            String right = inferExprType(binary.right);
            if (binary.operator.type == TokenType.PLUS) {
                if ("String".equals(left) || "String".equals(right)) {
                    return "String";
                }
            }
            if ("double".equals(left) || "double".equals(right)) {
                return "double";
            }
            if ("int".equals(left) && "int".equals(right)) {
                return "int";
            }
            if (binary.operator.type == TokenType.EQUAL_EQUAL || binary.operator.type == TokenType.BANG_EQUAL
                    || binary.operator.type == TokenType.GREATER || binary.operator.type == TokenType.GREATER_EQUAL
                    || binary.operator.type == TokenType.LESS || binary.operator.type == TokenType.LESS_EQUAL) {
                return "boolean";
            }
        }
        if (expr instanceof GroupingExpr) {
            return inferExprType(((GroupingExpr) expr).expression);
        }
        if (expr instanceof AssignExpr) {
            return inferExprType(((AssignExpr) expr).value);
        }
        if (expr instanceof CallExpr) {
            CallExpr call = (CallExpr) expr;
            if (call.callee instanceof VariableExpr) {
                String callee = ((VariableExpr) call.callee).name;
                return knownFunctionReturnTypes.getOrDefault(callee, "Object");
            }
        }
        return "Object";
    }

    // Primitive Java types are boxed when a generic context needs an object type.
    private String boxType(String type) {
        switch (type) {
            case "int":
                return "Integer";
            case "double":
                return "Double";
            case "boolean":
                return "Boolean";
            default:
                return type;
        }
    }

    // Seed the function-return table before walking the AST for nested
    // declarations.
    private void seedKnownFunctionReturnTypes(List<Stmt> statements) {
        knownFunctionReturnTypes.put("print", "Object");
        knownFunctionReturnTypes.put("toInt", "int");
        knownFunctionReturnTypes.put("toFloat", "double");
        knownFunctionReturnTypes.put("toString", "String");
        knownFunctionReturnTypes.put("dijkstra", "Map<String, Object>");
        collectFunctionReturnTypes(statements);
    }

    // Walk nested declarations and remember their Java return types for later
    // emission.
    private void collectFunctionReturnTypes(List<Stmt> statements) {
        for (Stmt stmt : statements) {
            if (stmt instanceof FuncDecl) {
                FuncDecl fn = (FuncDecl) stmt;
                knownFunctionReturnTypes.put(fn.name, mapType(fn.returnType));
            } else if (stmt instanceof AutomateDecl) {
                AutomateDecl auto = (AutomateDecl) stmt;
                knownFunctionReturnTypes.put(auto.name, mapType(auto.returnType));
            } else if (stmt instanceof ProjectDecl) {
                collectFunctionReturnTypes(((ProjectDecl) stmt).body);
            } else if (stmt instanceof ExportDecl) {
                collectFunctionReturnTypes(((ExportDecl) stmt).body);
            }
        }
    }
}