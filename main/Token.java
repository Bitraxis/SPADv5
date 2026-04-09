package main;

class Token {
    public final TokenType type;
    public final String lexeme;
    public final Object literal;
    public final int line;
    public final int column;

    public Token(TokenType type, String lexeme, Object literal, int line, int column) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
        this.column = column;
    }

    @Override
    public String toString() {
        return type + " '" + lexeme + "' at " + line + ":" + column;
    }
}

enum TokenType {
    LEFT_PAREN,
    RIGHT_PAREN,
    LEFT_BRACE,
    RIGHT_BRACE,
    LEFT_BRACKET,
    RIGHT_BRACKET,
    COMMA,
    DOT,
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
    IDENTIFIER,
    STRING,
    INTEGER,
    FLOAT,
    VAR,
    IMPORT,
    FROM,
    DRAGON,
    FUNC,
    PROJECT,
    PROJECTION,
    DIRECTIVE,
    AUTOMATE,
    RETURN,
    IF,
    ELSE,
    WHILE,
    TRUE,
    FALSE,
    NULL,
    AS,
    EOF
}