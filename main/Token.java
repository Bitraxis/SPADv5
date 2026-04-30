package main;

// A token is the smallest unit the lexer hands to the parser: it records the
// kind of symbol, the original text, any literal value, and its source location.
class Token {
    public final TokenType type;
    public final String lexeme;
    public final Object literal;
    public final int line;
    public final int column;

    // Tokens are immutable so downstream phases can safely inspect them.
    public Token(TokenType type, String lexeme, Object literal, int line, int column) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
        this.column = column;
    }

    @Override
    // Human-readable debug form used in diagnostics and tracing.
    public String toString() {
        return type + " '" + lexeme + "' at " + line + ":" + column;
    }
}

// The token set defines every punctuation, operator, keyword, and literal form
// recognized by the language front-end.
enum TokenType {
    LEFT_PAREN,
    RIGHT_PAREN,
    LEFT_BRACE,
    RIGHT_BRACE,
    LEFT_BRACKET,
    RIGHT_BRACKET,
    COMMA,
    DOT,
    RANGE,
    COLON,
    DOUBLE_COLON,
    SEMICOLON,
    NEWLINE,
    PLUS,
    MINUS,
    STAR,
    SLASH,
    PERCENT,
    BANG,
    BANG_EQUAL,
    EQUAL,
    EQUAL_EQUAL,
    GREATER,
    GREATER_EQUAL,
    LESS,
    LESS_EQUAL,
    PIPE_GT,
    FAT_ARROW,
    IDENTIFIER,
    STRING,
    INTEGER,
    FLOAT,
    AND,
    OR,
    NOT,
    VAR,
    IMPORT,
    FROM,
    DRAGON,
    FUNC,
    TRY,
    EXCEPT,
    FINALLY,
    PROJECT,
    PROJECTION,
    DIRECTIVE,
    AUTOMATE,
    EXPORT,
    RETURN,
    IF,
    ELIF,
    ELSE,
    WHILE,
    FOR,
    IN,
    MATCH,
    WHEN,
    TRUE,
    FALSE,
    NULL,
    AS,
    EOF
}