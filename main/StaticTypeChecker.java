package main;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class StaticTypeChecker {
    private final Deque<Map<String, SpadType>> scopes = new ArrayDeque<>();

    public void check(Program program) {
        scopes.clear();
        pushScope();

        Map<String, FunctionSig> builtins = createBuiltinFunctions();
        Map<String, FunctionSig> topFunctions = new HashMap<>(builtins);
        collectFunctionSignatures(program.statements, topFunctions);

        for (Stmt stmt : program.statements) {
            checkStmt(stmt, topFunctions, SpadType.VOID);
        }

        popScope();
    }

    private Map<String, FunctionSig> createBuiltinFunctions() {
        Map<String, FunctionSig> builtins = new HashMap<>();
        builtins.put("print", new FunctionSig("print", List.of(SpadType.ANY), SpadType.ANY));
        builtins.put("toInt", new FunctionSig("toInt", List.of(SpadType.ANY), SpadType.INT));
        builtins.put("toFloat", new FunctionSig("toFloat", List.of(SpadType.ANY), SpadType.FLOAT));
        builtins.put("toString", new FunctionSig("toString", List.of(SpadType.ANY), SpadType.STRING));
        builtins.put("dijkstra", new FunctionSig("dijkstra", List.of(SpadType.DICT, SpadType.STRING), SpadType.DICT));
        return builtins;
    }

    private void collectFunctionSignatures(List<Stmt> statements, Map<String, FunctionSig> target) {
        for (Stmt stmt : statements) {
            if (stmt instanceof FuncDecl fn) {
                target.put(fn.name, createSignature(fn.name, fn.parameters, fn.returnType));
            } else if (stmt instanceof AutomateDecl auto) {
                target.put(auto.name, createSignature(auto.name, auto.parameters, auto.returnType));
            }
        }
    }

    private FunctionSig createSignature(String name, List<Parameter> params, String returnTypeName) {
        List<SpadType> args = new ArrayList<>();
        for (Parameter p : params) {
            args.add(fromTypeName(p.type));
        }
        return new FunctionSig(name, args, fromTypeName(returnTypeName));
    }

    private void checkStmt(Stmt stmt, Map<String, FunctionSig> functions, SpadType expectedReturn) {
        if (stmt instanceof ImportStmt || stmt instanceof DragonUseStmt || stmt instanceof DirectiveStmt) {
            return;
        }
        if (stmt instanceof VarDecl decl) {
            SpadType initType = inferExprType(decl.initializer, functions);
            SpadType declared = decl.explicitType == null ? initType : fromTypeName(decl.explicitType);
            if (!isAssignable(declared, initType)) {
                throw error("Variable '" + decl.name + "' expects " + declared + " but got " + initType);
            }
            define(decl.name, declared);
            return;
        }
        if (stmt instanceof ProjectionDecl projection) {
            inferExprType(projection.value, functions);
            return;
        }
        if (stmt instanceof ReturnStmt ret) {
            if (expectedReturn == SpadType.VOID) {
                if (ret.value != null) {
                    throw error("Void function cannot return a value");
                }
                return;
            }
            if (ret.value == null) {
                throw error("Missing return value for function returning " + expectedReturn);
            }
            SpadType valueType = inferExprType(ret.value, functions);
            if (!isAssignable(expectedReturn, valueType)) {
                throw error("Return type mismatch: expected " + expectedReturn + " but got " + valueType);
            }
            return;
        }
        if (stmt instanceof ExprStmt exprStmt) {
            inferExprType(exprStmt.expression, functions);
            return;
        }
        if (stmt instanceof BlockStmt block) {
            pushScope();
            for (Stmt inner : block.statements) {
                checkStmt(inner, functions, expectedReturn);
            }
            popScope();
            return;
        }
        if (stmt instanceof IfStmt ifStmt) {
            ensureBool(inferExprType(ifStmt.condition, functions), "if condition must be Bool");
            checkStmt(ifStmt.thenBranch, functions, expectedReturn);
            if (ifStmt.elseBranch != null) {
                checkStmt(ifStmt.elseBranch, functions, expectedReturn);
            }
            return;
        }
        if (stmt instanceof WhileStmt whileStmt) {
            ensureBool(inferExprType(whileStmt.condition, functions), "while condition must be Bool");
            checkStmt(whileStmt.body, functions, expectedReturn);
            return;
        }
        if (stmt instanceof ForRangeStmt forRange) {
            SpadType startType = inferExprType(forRange.start, functions);
            SpadType endType = inferExprType(forRange.end, functions);
            if (startType != SpadType.INT || endType != SpadType.INT) {
                throw error("for range bounds must be Int..Int but got " + startType + ".." + endType);
            }
            pushScope();
            define(forRange.variable, SpadType.INT);
            checkStmt(forRange.body, functions, expectedReturn);
            popScope();
            return;
        }
        if (stmt instanceof FuncDecl fn) {
            checkCallable(fn.parameters, fn.returnType, fn.body, functions);
            return;
        }
        if (stmt instanceof AutomateDecl auto) {
            checkCallable(auto.parameters, auto.returnType, auto.body, functions);
            return;
        }
        if (stmt instanceof ProjectDecl project) {
            Map<String, FunctionSig> localFunctions = new HashMap<>(functions);
            collectFunctionSignatures(project.body, localFunctions);
            pushScope();
            for (Stmt inner : project.body) {
                checkStmt(inner, localFunctions, expectedReturn);
            }
            popScope();
            return;
        }
        if (stmt instanceof ExportDecl exportDecl) {
            Map<String, FunctionSig> localFunctions = new HashMap<>(functions);
            collectFunctionSignatures(exportDecl.body, localFunctions);
            pushScope();
            for (Stmt inner : exportDecl.body) {
                checkStmt(inner, localFunctions, expectedReturn);
            }
            popScope();
            return;
        }

        throw error("Unsupported statement in type checker: " + stmt.getClass().getSimpleName());
    }

    private void checkCallable(
            List<Parameter> parameters,
            String returnTypeName,
            List<Stmt> body,
            Map<String, FunctionSig> functions
    ) {
        SpadType expectedReturn = fromTypeName(returnTypeName);
        pushScope();
        for (Parameter parameter : parameters) {
            define(parameter.name, fromTypeName(parameter.type));
        }

        boolean sawReturn = false;
        for (Stmt inner : body) {
            if (inner instanceof ReturnStmt) {
                sawReturn = true;
            }
            checkStmt(inner, functions, expectedReturn);
        }

        if (expectedReturn != SpadType.VOID && !sawReturn) {
            throw error("Function with return type " + expectedReturn + " must include at least one return statement");
        }

        popScope();
    }

    private SpadType inferExprType(Expr expr, Map<String, FunctionSig> functions) {
        if (expr instanceof LiteralExpr lit) {
            if (lit.value == null) return SpadType.NULL;
            if (lit.value instanceof String) return SpadType.STRING;
            if (lit.value instanceof Boolean) return SpadType.BOOL;
            if (lit.value instanceof Double || lit.value instanceof Float) return SpadType.FLOAT;
            if (lit.value instanceof Number) return SpadType.INT;
            return SpadType.ANY;
        }
        if (expr instanceof VariableExpr var) {
            SpadType resolved = resolve(var.name);
            if (resolved == null) {
                throw error("Unknown variable '" + var.name + "'");
            }
            return resolved;
        }
        if (expr instanceof AssignExpr assign) {
            SpadType existing = resolve(assign.name);
            if (existing == null) {
                throw error("Cannot assign to unknown variable '" + assign.name + "'");
            }
            SpadType valueType = inferExprType(assign.value, functions);
            if (!isAssignable(existing, valueType)) {
                throw error("Cannot assign " + valueType + " to variable '" + assign.name + "' of type " + existing);
            }
            return existing;
        }
        if (expr instanceof UnaryExpr unary) {
            SpadType right = inferExprType(unary.right, functions);
            if (unary.operator.type == TokenType.BANG) {
                ensureBool(right, "'!' requires Bool");
                return SpadType.BOOL;
            }
            if (unary.operator.type == TokenType.MINUS) {
                ensureNumeric(right, "Unary '-' requires Int or Float");
                return right;
            }
            return SpadType.ANY;
        }
        if (expr instanceof BinaryExpr bin) {
            SpadType left = inferExprType(bin.left, functions);
            SpadType right = inferExprType(bin.right, functions);
            return inferBinaryType(bin.operator.type, left, right);
        }
        if (expr instanceof GroupingExpr grouping) {
            return inferExprType(grouping.expression, functions);
        }
        if (expr instanceof ListExpr list) {
            for (Expr element : list.elements) {
                inferExprType(element, functions);
            }
            return SpadType.LIST;
        }
        if (expr instanceof DictExpr dict) {
            for (DictEntry e : dict.entries) {
                inferExprType(e.value, functions);
            }
            return SpadType.DICT;
        }
        if (expr instanceof GetExpr) {
            return SpadType.ANY;
        }
        if (expr instanceof JavaInteropExpr) {
            return SpadType.ANY;
        }
        if (expr instanceof CallExpr call) {
            if (call.callee instanceof VariableExpr calleeVar) {
                FunctionSig sig = functions.get(calleeVar.name);
                if (sig == null) {
                    return SpadType.ANY;
                }
                if (sig.argTypes.size() != call.arguments.size()) {
                    throw error("Function '" + sig.name + "' expects " + sig.argTypes.size()
                            + " args but got " + call.arguments.size());
                }
                for (int i = 0; i < call.arguments.size(); i++) {
                    SpadType actual = inferExprType(call.arguments.get(i), functions);
                    SpadType expected = sig.argTypes.get(i);
                    if (!isAssignable(expected, actual)) {
                        throw error("Argument " + (i + 1) + " for function '" + sig.name + "' expects "
                                + expected + " but got " + actual);
                    }
                }
                return sig.returnType;
            }
            for (Expr argument : call.arguments) {
                inferExprType(argument, functions);
            }
            return SpadType.ANY;
        }
        if (expr instanceof MatchExpr matchExpr) {
            return inferMatchType(matchExpr, functions);
        }

        return SpadType.ANY;
    }

    private SpadType inferMatchType(MatchExpr matchExpr, Map<String, FunctionSig> functions) {
        SpadType valueType = inferExprType(matchExpr.value, functions);
        if (matchExpr.arms.isEmpty()) {
            throw error("match expression must have at least one arm");
        }

        Set<Object> boolPatterns = new HashSet<>();
        boolean hasExhaustiveWildcard = false;
        boolean seenWildcard = false;
        SpadType resultType = null;

        pushScope();
        define("it", valueType == SpadType.NULL ? SpadType.ANY : valueType);

        for (int i = 0; i < matchExpr.arms.size(); i++) {
            MatchArm arm = matchExpr.arms.get(i);
            if (arm.wildcard) {
                seenWildcard = true;
                if (arm.guard == null && i != matchExpr.arms.size() - 1) {
                    throw error("Catch-all wildcard arm '_' must be the last arm");
                }
            } else {
                SpadType patternType = inferExprType(arm.pattern, functions);
                if (!isComparable(valueType, patternType)) {
                    throw error("Match pattern type " + patternType + " is incompatible with matched type " + valueType);
                }
                if (valueType == SpadType.BOOL && arm.pattern instanceof LiteralExpr lit && lit.value instanceof Boolean b) {
                    boolPatterns.add(b);
                }
            }

            if (arm.guard != null) {
                ensureBool(inferExprType(arm.guard, functions), "match guard must be Bool");
            }

            if (arm.wildcard && arm.guard == null) {
                hasExhaustiveWildcard = true;
            }

            SpadType armResultType = inferExprType(arm.result, functions);
            resultType = (resultType == null) ? armResultType : unify(resultType, armResultType);
        }

        popScope();

        if (!hasExhaustiveWildcard) {
            boolean boolCovered = valueType == SpadType.BOOL && boolPatterns.contains(Boolean.TRUE) && boolPatterns.contains(Boolean.FALSE);
            if (!boolCovered) {
                throw error("Non-exhaustive match: add '_' arm or cover all Bool cases");
            }
        }

        return resultType == null ? SpadType.ANY : resultType;
    }

    private SpadType inferBinaryType(TokenType operator, SpadType left, SpadType right) {
        switch (operator) {
            case PLUS:
                if (left == SpadType.STRING || right == SpadType.STRING) {
                    return SpadType.STRING;
                }
                ensureNumeric(left, "'+' requires numeric types unless string concatenation is used");
                ensureNumeric(right, "'+' requires numeric types unless string concatenation is used");
                return promoteNumeric(left, right);
            case MINUS:
            case STAR:
            case SLASH:
            case PERCENT:
                ensureNumeric(left, "Arithmetic operators require Int or Float");
                ensureNumeric(right, "Arithmetic operators require Int or Float");
                return promoteNumeric(left, right);
            case GREATER:
            case GREATER_EQUAL:
            case LESS:
            case LESS_EQUAL:
                ensureNumeric(left, "Comparison operators require Int or Float");
                ensureNumeric(right, "Comparison operators require Int or Float");
                return SpadType.BOOL;
            case EQUAL_EQUAL:
            case BANG_EQUAL:
                if (!isComparable(left, right)) {
                    throw error("Cannot compare " + left + " with " + right);
                }
                return SpadType.BOOL;
            default:
                return SpadType.ANY;
        }
    }

    private SpadType unify(SpadType first, SpadType second) {
        if (first == second) {
            return first;
        }
        if (isNumeric(first) && isNumeric(second)) {
            return SpadType.FLOAT;
        }
        if (first == SpadType.NULL) {
            return second;
        }
        if (second == SpadType.NULL) {
            return first;
        }
        return SpadType.ANY;
    }

    private boolean isAssignable(SpadType expected, SpadType actual) {
        if (expected == SpadType.ANY || actual == SpadType.ANY) {
            return true;
        }
        if (expected == actual) {
            return true;
        }
        if (expected == SpadType.FLOAT && actual == SpadType.INT) {
            return true;
        }
        if (actual == SpadType.NULL && expected != SpadType.INT && expected != SpadType.FLOAT && expected != SpadType.BOOL) {
            return true;
        }
        return false;
    }

    private boolean isComparable(SpadType left, SpadType right) {
        if (left == SpadType.ANY || right == SpadType.ANY) {
            return true;
        }
        if (left == right) {
            return true;
        }
        return isNumeric(left) && isNumeric(right);
    }

    private void ensureBool(SpadType value, String message) {
        if (value != SpadType.BOOL && value != SpadType.ANY) {
            throw error(message + " (got " + value + ")");
        }
    }

    private void ensureNumeric(SpadType value, String message) {
        if (!isNumeric(value) && value != SpadType.ANY) {
            throw error(message + " (got " + value + ")");
        }
    }

    private boolean isNumeric(SpadType value) {
        return value == SpadType.INT || value == SpadType.FLOAT;
    }

    private SpadType promoteNumeric(SpadType left, SpadType right) {
        if (left == SpadType.FLOAT || right == SpadType.FLOAT) {
            return SpadType.FLOAT;
        }
        return SpadType.INT;
    }

    private SpadType fromTypeName(String name) {
        if (name == null) {
            return SpadType.ANY;
        }
        return switch (name) {
            case "Int" -> SpadType.INT;
            case "Float" -> SpadType.FLOAT;
            case "String" -> SpadType.STRING;
            case "Bool" -> SpadType.BOOL;
            case "List" -> SpadType.LIST;
            case "Dict" -> SpadType.DICT;
            case "Void" -> SpadType.VOID;
            default -> SpadType.ANY;
        };
    }

    private void define(String name, SpadType type) {
        scopes.peek().put(name, type);
    }

    private SpadType resolve(String name) {
        for (Map<String, SpadType> scope : scopes) {
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        return null;
    }

    private void pushScope() {
        scopes.push(new HashMap<>());
    }

    private void popScope() {
        scopes.pop();
    }

    private RuntimeException error(String message) {
        return new RuntimeException("Type error -> " + message);
    }

    private enum SpadType {
        ANY,
        INT,
        FLOAT,
        STRING,
        BOOL,
        LIST,
        DICT,
        VOID,
        NULL
    }

    private static final class FunctionSig {
        private final String name;
        private final List<SpadType> argTypes;
        private final SpadType returnType;

        private FunctionSig(String name, List<SpadType> argTypes, SpadType returnType) {
            this.name = name;
            this.argTypes = argTypes;
            this.returnType = returnType;
        }
    }
}
