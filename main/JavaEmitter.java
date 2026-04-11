package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class JavaEmitter {
    private int tempCounter = 0;
    private final Map<String, String> knownFunctionReturnTypes = new HashMap<>();

    public String emitProgram(Program program, String className) {
        return emitProgram(program, className, null);
    }

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

    private String emitExpr(Expr expr) {
        if (expr instanceof LiteralExpr) {
            Object value = ((LiteralExpr) expr).value;
            if (value == null) return "null";
            if (value instanceof String) return quote((String) value);
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
            return "(" + emitExpr(binary.left) + " " + binary.operator.lexeme + " " + emitExpr(binary.right) + ")";
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

    private String mapType(String type) {
        switch (type) {
            case "Int": return "int";
            case "Float": return "double";
            case "String": return "String";
            case "Bool": return "boolean";
            case "List": return "List<Object>";
            case "Dict": return "Map<String, Object>";
            case "Void": return "void";
            default: return "Object";
        }
    }

    private String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private String indent(int level) {
        return "    ".repeat(Math.max(0, level));
    }

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

    private String nextTemp(String prefix) {
        tempCounter++;
        return "__" + prefix + "_" + tempCounter;
    }

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
            return inferExprType(((UnaryExpr) expr).right);
        }
        if (expr instanceof BinaryExpr) {
            BinaryExpr binary = (BinaryExpr) expr;
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

    private void seedKnownFunctionReturnTypes(List<Stmt> statements) {
        knownFunctionReturnTypes.put("print", "Object");
        knownFunctionReturnTypes.put("toInt", "int");
        knownFunctionReturnTypes.put("toFloat", "double");
        knownFunctionReturnTypes.put("toString", "String");
        knownFunctionReturnTypes.put("dijkstra", "Map<String, Object>");
        collectFunctionReturnTypes(statements);
    }

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