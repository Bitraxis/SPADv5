package main;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

class JavaEmitter {
    public String emitProgram(Program program, String className) {
        StringBuilder out = new StringBuilder();
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
        if (stmt instanceof FuncDecl) {
            emitFunction((FuncDecl) stmt, out, level);
            return;
        }
        if (stmt instanceof AutomateDecl) {
            emitAutomate((AutomateDecl) stmt, out, level);
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
                    return "System.out.println(" + arg + ")";
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
        if (expr instanceof GetExpr) {
            GetExpr get = (GetExpr) expr;
            return emitExpr(get.object) + "." + get.name;
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
}