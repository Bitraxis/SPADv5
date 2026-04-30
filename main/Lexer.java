package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// The lexer turns raw source text into a linear stream of tokens for the parser.
class Lexer {
    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();

    // Keyword lookup keeps the scanner fast and makes the language surface
    // explicit.
    static {
        KEYWORDS.put("var", TokenType.VAR);
        KEYWORDS.put("import", TokenType.IMPORT);
        KEYWORDS.put("from", TokenType.FROM);
        KEYWORDS.put("dragon", TokenType.DRAGON);
        KEYWORDS.put("func", TokenType.FUNC);
        KEYWORDS.put("function", TokenType.FUNC);
        KEYWORDS.put("def", TokenType.FUNC);
        KEYWORDS.put("project", TokenType.PROJECT);
        KEYWORDS.put("projection", TokenType.PROJECTION);
        KEYWORDS.put("directive", TokenType.DIRECTIVE);
        KEYWORDS.put("automate", TokenType.AUTOMATE);
        KEYWORDS.put("export", TokenType.EXPORT);
        KEYWORDS.put("return", TokenType.RETURN);
        KEYWORDS.put("if", TokenType.IF);
        KEYWORDS.put("elif", TokenType.ELIF);
        KEYWORDS.put("else", TokenType.ELSE);
        KEYWORDS.put("while", TokenType.WHILE);
        KEYWORDS.put("for", TokenType.FOR);
        KEYWORDS.put("in", TokenType.IN);
        KEYWORDS.put("match", TokenType.MATCH);
        KEYWORDS.put("when", TokenType.WHEN);
        KEYWORDS.put("try", TokenType.TRY);
        KEYWORDS.put("except", TokenType.EXCEPT);
        KEYWORDS.put("finally", TokenType.FINALLY);
        KEYWORDS.put("true", TokenType.TRUE);
        KEYWORDS.put("false", TokenType.FALSE);
        KEYWORDS.put("null", TokenType.NULL);
        KEYWORDS.put("as", TokenType.AS);
        KEYWORDS.put("and", TokenType.AND);
        KEYWORDS.put("or", TokenType.OR);
        KEYWORDS.put("not", TokenType.NOT);
    }

    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;
    private int lineStartIndex = 0;

    // The source string is scanned once from left to right.
    public Lexer(String source) {
        this.source = source;
    }

    // Lex the whole source file and append an EOF sentinel token at the end.
    public List<Token> lex() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "", null, line, column()));
        return tokens;
    }

    // Decode the next character or multicharacter operator into a token.
    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(':
                addToken(TokenType.LEFT_PAREN);
                break;
            case ')':
                addToken(TokenType.RIGHT_PAREN);
                break;
            case '{':
                addToken(TokenType.LEFT_BRACE);
                break;
            case '}':
                addToken(TokenType.RIGHT_BRACE);
                break;
            case '[':
                addToken(TokenType.LEFT_BRACKET);
                break;
            case ']':
                addToken(TokenType.RIGHT_BRACKET);
                break;
            case ',':
                addToken(TokenType.COMMA);
                break;
            case '.':
                if (match('.')) {
                    addToken(TokenType.RANGE); // '..' used for range and string concatenation
                } else {
                    addToken(TokenType.DOT);
                }
                break;
            case ':':
                if (match(':')) {
                    addToken(TokenType.DOUBLE_COLON);
                } else {
                    addToken(TokenType.COLON);
                }
                break;
            case ';':
                addToken(TokenType.SEMICOLON);
                break;
            case '+':
                addToken(TokenType.PLUS);
                break;
            case '-':
                addToken(TokenType.MINUS);
                break;
            case '*':
                addToken(TokenType.STAR);
                break;
            case '%':
                addToken(TokenType.PERCENT);
                break;
            case '!':
                addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG);
                break;
            case '=':
                if (match('=')) {
                    addToken(TokenType.EQUAL_EQUAL);
                } else if (match('>')) {
                    addToken(TokenType.FAT_ARROW);
                } else {
                    addToken(TokenType.EQUAL);
                }
                break;
            case '<':
                addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
                break;
            case '>':
                addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
                break;
            case '|':
                if (match('>')) {
                    addToken(TokenType.PIPE_GT);
                } else {
                    throw error("Unexpected '|'");
                }
                break;
            case '/':
                if (match('/')) {
                    while (peek() != '\n' && !isAtEnd()) {
                        advance();
                    }
                } else {
                    addToken(TokenType.SLASH);
                }
                break;
            case ' ':
            case '\r':
            case '\t':
                break;
            case '\n':
                addToken(TokenType.NEWLINE);
                line++;
                lineStartIndex = current;
                break;
            case '"':
                string();
                break;
            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    throw error("Unexpected character: '" + c + "'");
                }
        }
    }

    // Identifiers are scanned as runs of letters, digits, and underscores.
    private void identifier() {
        while (isAlphaNumeric(peek())) {
            advance();
        }
        String text = source.substring(start, current);
        TokenType type = KEYWORDS.getOrDefault(text, TokenType.IDENTIFIER);
        addToken(type);
    }

    // Numbers support both integer and floating-point literals.
    private void number() {
        while (isDigit(peek())) {
            advance();
        }
        boolean isFloat = false;
        if (peek() == '.' && isDigit(peekNext())) {
            isFloat = true;
            advance();
            while (isDigit(peek())) {
                advance();
            }
        }

        String text = source.substring(start, current);
        if (isFloat) {
            addToken(TokenType.FLOAT, Double.parseDouble(text));
        } else {
            addToken(TokenType.INTEGER, Long.parseLong(text));
        }
    }

    // Strings continue until the matching quote, tracking line numbers along the
    // way.
    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') {
                line++;
                lineStartIndex = current + 1;
            }
            advance();
        }

        if (isAtEnd()) {
            throw error("Unterminated string literal");
        }

        advance();
        String value = source.substring(start + 1, current - 1);
        addToken(TokenType.STRING, value);
    }

    // Match consumes the next character only when it equals the expected character.
    private boolean match(char expected) {
        if (isAtEnd()) {
            return false;
        }
        if (source.charAt(current) != expected) {
            return false;
        }
        current++;
        return true;
    }

    // Peek reads the current character without advancing the scanner.
    private char peek() {
        if (isAtEnd()) {
            return '\0';
        }
        return source.charAt(current);
    }

    // Peek-next is used when the lexer needs one character of lookahead.
    private char peekNext() {
        if (current + 1 >= source.length()) {
            return '\0';
        }
        return source.charAt(current + 1);
    }

    // Alphabetic checks define the identifier character set.
    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z')
                || (c >= 'A' && c <= 'Z')
                || c == '_';
    }

    // Identifiers may contain letters, digits, and underscores after the first
    // character.
    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    // Digit detection is kept separate so numeric scanning stays easy to read.
    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private char advance() {
        return source.charAt(current++);
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String lexeme = source.substring(start, current);
        tokens.add(new Token(type, lexeme, literal, line, column()));
    }

    private int column() {
        return (start - lineStartIndex) + 1;
    }

    private RuntimeException error(String message) {
        return new RuntimeException("Lex error at " + line + ":" + column() + " -> " + message);
    }
}