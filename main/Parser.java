package main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// The parser consumes tokens and builds the abstract syntax tree used by later phases.
class Parser {
    private final List<Token> tokens;
    private int current = 0;

    // Parsing is token-driven; the parser keeps a moving cursor through the list.
    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    // Parse a complete source file into a Program node.
    public Program parseProgram() {
        List<Stmt> statements = new ArrayList<>();
        skipSeparators();
        while (!isAtEnd()) {
            statements.add(parseDeclaration());
            skipSeparators();
        }
        return new Program(statements);
    }

    // Declarations are recognized first so top-level forms stay explicit.
    private Stmt parseDeclaration() {
        if (match(TokenType.IMPORT)) {
            return parseImportDecl();
        }
        if (match(TokenType.DRAGON)) {
            return parseDragonUseDecl();
        }
        if (match(TokenType.VAR)) {
            return parseVarDecl();
        }
        if (match(TokenType.FUNC)) {
            return parseFuncDecl();
        }
        if (match(TokenType.PROJECT)) {
            return parseProjectDecl();
        }
        if (match(TokenType.PROJECTION)) {
            return parseProjectionDecl();
        }
        if (match(TokenType.AUTOMATE)) {
            return parseAutomateDecl();
        }
        if (match(TokenType.EXPORT)) {
            return parseExportDecl();
        }
        return parseStatement();
    }

    // Imports record module metadata and optional source/version information.
    private Stmt parseImportDecl() {
        Token module = consume(TokenType.IDENTIFIER, "Expected module name after import");
        String versionConstraint = "*";
        if (match(TokenType.EQUAL)) {
            Token version = consume(TokenType.STRING, "Expected version constraint string after '='");
            versionConstraint = String.valueOf(version.literal);
        }
        String source = "ftp://central";
        if (match(TokenType.FROM)) {
            Token sourceToken = consumeAny("Expected source after from", TokenType.STRING, TokenType.IDENTIFIER);
            source = sourceToken.type == TokenType.STRING
                    ? String.valueOf(sourceToken.literal)
                    : sourceToken.lexeme;
        }
        return new ImportStmt(module.lexeme, versionConstraint, source);
    }

    // Dragon use declarations carry a set of package names.
    private Stmt parseDragonUseDecl() {
        consume(TokenType.EQUAL, "Expected '=' after dragon");
        consume(TokenType.LEFT_BRACE, "Expected '{' after dragon =");
        List<String> packages = new ArrayList<>();
        if (!check(TokenType.RIGHT_BRACE)) {
            do {
                Token packageName = consume(TokenType.IDENTIFIER, "Expected package name in dragon package list");
                packages.add(packageName.lexeme);
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.RIGHT_BRACE, "Expected '}' after dragon package list");
        return new DragonUseStmt(packages);
    }

    // Variable declarations allow optional inline type annotations.
    private Stmt parseVarDecl() {
        Token name = consume(TokenType.IDENTIFIER, "Expected variable name");
        String explicitType = null;

        // Check for type annotation: var name: Type = value
        if (check(TokenType.COLON)) {
            advance(); // consume ':'
            explicitType = consume(TokenType.IDENTIFIER, "Expected type name after ':'").lexeme;
        }

        consume(TokenType.EQUAL, "Expected '=' after variable name or type");
        Expr initializer = parseExpression();
        return new VarDecl(name.lexeme, explicitType, initializer);
    }

    // Functions and defs share the same grammar and AST node.
    private Stmt parseFuncDecl() {
        Token name = consume(TokenType.IDENTIFIER, "Expected function name");
        consume(TokenType.LEFT_PAREN, "Expected '(' after function name");

        List<Parameter> parameters = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                Token paramName = consume(TokenType.IDENTIFIER, "Expected parameter name");
                if (match(TokenType.COLON)) {
                    Token paramType = consume(TokenType.IDENTIFIER, "Expected parameter type");
                    parameters.add(new Parameter(paramName.lexeme, paramType.lexeme));
                } else if (match(TokenType.EQUAL)) {
                    Token paramType = consume(TokenType.IDENTIFIER, "Expected parameter type");
                    parameters.add(new Parameter(paramName.lexeme, paramType.lexeme));
                } else {
                    throw error(peek(), "Expected ':' or '=' after parameter name");
                }
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.RIGHT_PAREN, "Expected ')' after parameters");

        String returnType = "Void";
        if (match(TokenType.EQUAL)) {
            returnType = consume(TokenType.IDENTIFIER, "Expected return type after '='").lexeme;
        }

        consume(TokenType.LEFT_BRACE, "Expected '{' before function body");
        List<Stmt> body = parseBlockStatements();
        return new FuncDecl(name.lexeme, parameters, returnType, body);
    }

    // Project blocks are parsed as nested statement lists.
    private Stmt parseProjectDecl() {
        Token name = consume(TokenType.IDENTIFIER, "Expected project name");
        consume(TokenType.LEFT_BRACE, "Expected '{' before project body");
        List<Stmt> body = parseBlockStatements();
        return new ProjectDecl(name.lexeme, body);
    }

    // Projection declarations bind names to expression values.
    private Stmt parseProjectionDecl() {
        Token name = consume(TokenType.IDENTIFIER, "Expected projection name");
        consume(TokenType.EQUAL, "Expected '=' after projection name");
        Expr value = parseExpression();
        return new ProjectionDecl(name.lexeme, value);
    }

    // Automate declarations reuse the same shape as functions but mean automation
    // entrypoints.
    private Stmt parseAutomateDecl() {
        Token name = consume(TokenType.IDENTIFIER, "Expected automation block name");
        consume(TokenType.LEFT_PAREN, "Expected '(' after automation block name");

        List<Parameter> parameters = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                Token paramName = consume(TokenType.IDENTIFIER, "Expected parameter name");
                consume(TokenType.EQUAL, "Expected '=' after parameter name");
                Token paramType = consume(TokenType.IDENTIFIER, "Expected parameter type");
                parameters.add(new Parameter(paramName.lexeme, paramType.lexeme));
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.RIGHT_PAREN, "Expected ')' after parameters");

        String returnType = "Void";
        if (match(TokenType.EQUAL)) {
            returnType = consume(TokenType.IDENTIFIER, "Expected return type after '='").lexeme;
        }

        consume(TokenType.LEFT_BRACE, "Expected '{' before automate body");
        List<Stmt> body = parseBlockStatements();
        return new AutomateDecl(name.lexeme, parameters, returnType, body);
    }

    // Export blocks are containers for nested declarations.
    private Stmt parseExportDecl() {
        Token name = consume(TokenType.IDENTIFIER, "Expected export block name");
        consume(TokenType.LEFT_BRACE, "Expected '{' before export block body");
        List<Stmt> body = parseBlockStatements();
        return new ExportDecl(name.lexeme, body);
    }

    // Parse a brace-delimited statement list until the closing brace appears.
    private List<Stmt> parseBlockStatements() {
        List<Stmt> statements = new ArrayList<>();
        skipSeparators();
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            statements.add(parseDeclaration());
            skipSeparators();
        }
        consume(TokenType.RIGHT_BRACE, "Expected '}' after block");
        return statements;
    }

    // Statements include control flow, directives, blocks, and plain expression
    // statements.
    private Stmt parseStatement() {
        if (match(TokenType.IF)) {
            return parseIfStmt();
        }
        if (match(TokenType.WHILE)) {
            return parseWhileStmt();
        }
        if (match(TokenType.FOR)) {
            return parseForRangeStmt();
        }
        if (match(TokenType.TRY)) {
            return parseTryStmt();
        }
        if (match(TokenType.DIRECTIVE)) {
            return parseDirectiveStmt();
        }
        if (match(TokenType.RETURN)) {
            if (isStatementEnd(peek())) {
                return new ReturnStmt(null);
            }
            return new ReturnStmt(parseExpression());
        }
        if (match(TokenType.LEFT_BRACE)) {
            return new BlockStmt(parseBlockStatements());
        }
        return new ExprStmt(parseExpression());
    }

    // Directives are parsed as named calls with positional arguments.
    private Stmt parseDirectiveStmt() {
        Token name = consume(TokenType.IDENTIFIER, "Expected directive name");
        consume(TokenType.LEFT_PAREN, "Expected '(' after directive name");
        List<Expr> args = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                args.add(parseExpression());
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.RIGHT_PAREN, "Expected ')' after directive arguments");
        return new DirectiveStmt(name.lexeme, args);
    }

    // Chained if/elif/else expressions are normalized into nested IfStmt nodes.
    private Stmt parseIfStmt() {
        consume(TokenType.LEFT_PAREN, "Expected '(' after if");
        Expr condition = parseExpression();
        consume(TokenType.RIGHT_PAREN, "Expected ')' after if condition");
        Stmt thenBranch = parseStatement();
        Stmt elseBranch = null;
        while (match(TokenType.ELIF)) {
            // Desugar chained elif into nested if-else for AST simplicity
            consume(TokenType.LEFT_PAREN, "Expected '(' after elif");
            Expr elifCond = parseExpression();
            consume(TokenType.RIGHT_PAREN, "Expected ')' after elif condition");
            Stmt elifThen = parseStatement();
            thenBranch = new IfStmt(condition, thenBranch, new IfStmt(elifCond, elifThen, null));
            condition = null; // we've consumed the original condition
        }
        if (match(TokenType.ELSE)) {
            elseBranch = parseStatement();
        }
        return new IfStmt(condition, thenBranch, elseBranch);
    }

    // Try blocks support optional except and finally sections.
    private Stmt parseTryStmt() {
        // try { ... } except (e) { ... } [ finally { ... } ]
        consume(TokenType.LEFT_BRACE, "Expected '{' after try");
        List<Stmt> tryBody = parseBlockStatements();
        String exceptionName = null;
        List<Stmt> exceptBody = null;
        List<Stmt> finallyBody = null;
        if (match(TokenType.EXCEPT)) {
            consume(TokenType.LEFT_PAREN, "Expected '(' after except");
            Token ex = consume(TokenType.IDENTIFIER, "Expected exception identifier in except");
            exceptionName = ex.lexeme;
            consume(TokenType.RIGHT_PAREN, "Expected ')' after except identifier");
            consume(TokenType.LEFT_BRACE, "Expected '{' before except body");
            exceptBody = parseBlockStatements();
        }
        if (match(TokenType.FINALLY)) {
            consume(TokenType.LEFT_BRACE, "Expected '{' before finally body");
            finallyBody = parseBlockStatements();
        }
        return new TryStmt(tryBody, exceptionName, exceptBody, finallyBody);
    }

    // While loops store a condition and a single body statement.
    private Stmt parseWhileStmt() {
        consume(TokenType.LEFT_PAREN, "Expected '(' after while");
        Expr condition = parseExpression();
        consume(TokenType.RIGHT_PAREN, "Expected ')' after while condition");
        Stmt body = parseStatement();
        return new WhileStmt(condition, body);
    }

    // Range loops capture the loop bounds so the emitter can build an integer loop.
    private Stmt parseForRangeStmt() {
        Token variable = consume(TokenType.IDENTIFIER, "Expected loop variable after for");
        consume(TokenType.IN, "Expected 'in' after loop variable");
        Expr start = parseExpression();
        consume(TokenType.RANGE, "Expected '..' in for range");
        Expr end = parseExpression();
        Stmt body = parseStatement();
        return new ForRangeStmt(variable.lexeme, start, end, body);
    }

    // Expression parsing starts at the lowest-precedence assignment level.
    private Expr parseExpression() {
        return parseAssignment();
    }

    // Assignment is right-associative and only permits variable targets.
    private Expr parseAssignment() {
        Expr expr = parsePipeline();
        if (match(TokenType.EQUAL)) {
            Token equals = previous();
            Expr value = parseAssignment();
            if (expr instanceof VariableExpr) {
                return new AssignExpr(((VariableExpr) expr).name, value);
            }
            throw error(equals, "Invalid assignment target");
        }
        return expr;
    }

    // Pipelines are lowered to function-call chaining for readability.
    private Expr parsePipeline() {
        Expr expr = parseOr();
        while (match(TokenType.PIPE_GT)) {
            Expr stage = parseCall();
            if (stage instanceof CallExpr) {
                CallExpr call = (CallExpr) stage;
                List<Expr> args = new ArrayList<>();
                args.add(expr);
                args.addAll(call.arguments);
                expr = new CallExpr(call.callee, args);
            } else {
                expr = new CallExpr(stage, Collections.singletonList(expr));
            }
        }
        return expr;
    }

    // Equality sits above comparison in the precedence ladder.
    private Expr parseEquality() {
        Expr expr = parseComparison();
        while (match(TokenType.EQUAL_EQUAL, TokenType.BANG_EQUAL)) {
            Token op = previous();
            Expr right = parseComparison();
            expr = new BinaryExpr(expr, op, right);
        }
        return expr;
    }

    private Expr parseOr() {
        Expr expr = parseAnd();
        while (match(TokenType.OR)) {
            Token op = previous();
            Expr right = parseAnd();
            expr = new BinaryExpr(expr, op, right);
        }
        return expr;
    }

    private Expr parseAnd() {
        Expr expr = parseEquality();
        while (match(TokenType.AND)) {
            Token op = previous();
            Expr right = parseEquality();
            expr = new BinaryExpr(expr, op, right);
        }
        return expr;
    }

    private Expr parseComparison() {
        Expr expr = parseTerm();
        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            Token op = previous();
            Expr right = parseTerm();
            expr = new BinaryExpr(expr, op, right);
        }
        return expr;
    }

    private Expr parseTerm() {
        Expr expr = parseConcat();
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            Token op = previous();
            Expr right = parseFactor();
            expr = new BinaryExpr(expr, op, right);
        }
        return expr;
    }

    private Expr parseConcat() {
        Expr expr = parseFactor();
        while (match(TokenType.RANGE)) { // '..' used as concat when operands are strings
            Token op = previous();
            Expr right = parseFactor();
            expr = new BinaryExpr(expr, op, right);
        }
        return expr;
    }

    private Expr parseFactor() {
        Expr expr = parseUnary();
        while (match(TokenType.STAR, TokenType.SLASH, TokenType.PERCENT)) {
            Token op = previous();
            Expr right = parseUnary();
            expr = new BinaryExpr(expr, op, right);
        }
        return expr;
    }

    private Expr parseUnary() {
        if (match(TokenType.BANG, TokenType.MINUS, TokenType.NOT)) {
            Token op = previous();
            Expr right = parseUnary();
            return new UnaryExpr(op, right);
        }
        return parseCall();
    }

    private Expr parseCall() {
        Expr expr = parsePrimary();
        while (true) {
            if (match(TokenType.LEFT_PAREN)) {
                List<Expr> args = new ArrayList<>();
                if (!check(TokenType.RIGHT_PAREN)) {
                    do {
                        args.add(parseExpression());
                    } while (match(TokenType.COMMA));
                }
                consume(TokenType.RIGHT_PAREN, "Expected ')' after arguments");
                expr = new CallExpr(expr, args);
            } else if (match(TokenType.DOT)) {
                Token name = consume(TokenType.IDENTIFIER, "Expected member name after '.'");
                expr = new GetExpr(expr, name.lexeme);
            } else {
                break;
            }
        }
        return expr;
    }

    private Expr parsePrimary() {
        if (check(TokenType.IDENTIFIER)
                && "java".equals(peek().lexeme)
                && checkNext(TokenType.DOUBLE_COLON)) {
            advance();
            consume(TokenType.DOUBLE_COLON, "Expected '::' after java");
            StringBuilder qualified = new StringBuilder();
            qualified.append(consume(TokenType.IDENTIFIER, "Expected Java package or class name").lexeme);
            while (match(TokenType.DOT)) {
                qualified.append('.');
                qualified.append(consume(TokenType.IDENTIFIER, "Expected Java member name").lexeme);
            }
            return new JavaInteropExpr(qualified.toString());
        }
        if (match(TokenType.MATCH)) {
            return parseMatchExpr();
        }
        if (match(TokenType.FALSE))
            return new LiteralExpr(false);
        if (match(TokenType.TRUE))
            return new LiteralExpr(true);
        if (match(TokenType.NULL))
            return new LiteralExpr(null);
        if (match(TokenType.INTEGER, TokenType.FLOAT, TokenType.STRING)) {
            return new LiteralExpr(previous().literal);
        }
        if (match(TokenType.IDENTIFIER)) {
            return new VariableExpr(previous().lexeme);
        }
        if (match(TokenType.LEFT_PAREN)) {
            Expr expr = parseExpression();
            consume(TokenType.RIGHT_PAREN, "Expected ')' after expression");
            return new GroupingExpr(expr);
        }
        if (match(TokenType.LEFT_BRACKET)) {
            List<Expr> elements = new ArrayList<>();
            if (!check(TokenType.RIGHT_BRACKET)) {
                do {
                    elements.add(parseExpression());
                } while (match(TokenType.COMMA));
            }
            consume(TokenType.RIGHT_BRACKET, "Expected ']' after list literal");
            return new ListExpr(elements);
        }
        if (match(TokenType.LEFT_BRACE)) {
            List<DictEntry> entries = new ArrayList<>();
            if (!check(TokenType.RIGHT_BRACE)) {
                do {
                    Token keyToken = consumeAny("Expected dictionary key", TokenType.IDENTIFIER, TokenType.STRING);
                    String key = keyToken.type == TokenType.STRING ? (String) keyToken.literal : keyToken.lexeme;
                    consume(TokenType.EQUAL, "Expected '=' after dictionary key");
                    Expr value = parseExpression();
                    entries.add(new DictEntry(key, value));
                } while (match(TokenType.COMMA));
            }
            consume(TokenType.RIGHT_BRACE, "Expected '}' after dictionary literal");
            return new DictExpr(entries);
        }

        throw error(peek(), "Expected expression");
    }

    private Expr parseMatchExpr() {
        Expr value = parseExpression();
        consume(TokenType.LEFT_BRACE, "Expected '{' after match expression value");

        List<MatchArm> arms = new ArrayList<>();
        skipSeparators();
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            boolean wildcard = false;
            Expr pattern = null;
            if (check(TokenType.IDENTIFIER) && "_".equals(peek().lexeme)) {
                wildcard = true;
                advance();
            } else {
                pattern = parseExpression();
            }

            Expr guard = null;
            if (match(TokenType.WHEN)) {
                consume(TokenType.LEFT_PAREN, "Expected '(' after when");
                guard = parseExpression();
                consume(TokenType.RIGHT_PAREN, "Expected ')' after match guard");
            }

            consume(TokenType.FAT_ARROW, "Expected '=>' after match pattern");
            Expr result = parseExpression();
            arms.add(new MatchArm(wildcard, pattern, guard, result));
            if (!match(TokenType.COMMA)) {
                skipSeparators();
            }
            skipSeparators();
        }
        consume(TokenType.RIGHT_BRACE, "Expected '}' after match expression");
        return new MatchExpr(value, arms);
    }

    private boolean isStatementEnd(Token token) {
        return token.type == TokenType.SEMICOLON
                || token.type == TokenType.NEWLINE
                || token.type == TokenType.RIGHT_BRACE
                || token.type == TokenType.EOF;
    }

    private void skipSeparators() {
        while (match(TokenType.SEMICOLON, TokenType.NEWLINE)) {
            // Optional line terminators support both terse and explicit styles.
        }
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) {
            return advance();
        }
        throw error(peek(), message);
    }

    private Token consumeAny(String message, TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                return advance();
            }
        }
        throw error(peek(), message);
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) {
            return type == TokenType.EOF;
        }
        return peek().type == type;
    }

    private boolean checkNext(TokenType type) {
        if (current + 1 >= tokens.size()) {
            return false;
        }
        return tokens.get(current + 1).type == type;
    }

    private Token advance() {
        if (!isAtEnd()) {
            current++;
        }
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private RuntimeException error(Token token, String message) {
        return new RuntimeException("Parse error at " + token.line + ":" + token.column + " -> " + message);
    }
}