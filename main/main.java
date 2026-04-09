package main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class main {
    public static void main(String[] args) throws IOException {
        String source;
        if (args.length > 0) {
            source = Files.readString(Path.of(args[0]));
        } else {
            source = String.join("\n",
                    "import presentation = \">=1.2.0\" from \"ftp://central\"",
                    "dragon = {presentation, graph, automation}",
                    "project CoreGraph {",
                    "  projection version = \"0.1\"",
                    "}",
                    "func sum(a = Int, b = Int) = Int {",
                    "  return (a + b)",
                    "}",
                    "automate greet(name = String) = Void {",
                    "  directive optimize(\"warmup\", 3)",
                    "  print(name)",
                    "}",
                    "var numbers = [1, 2, 3]",
                    "var profile = {name = \"Veronika\", level = 7}",
                    "var total = sum(10, 20)",
                    "var num = toInt(\"42\")",
                    "greet(\"SPAD\")",
                    "print(total)");
        }

        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.lex();

        Parser parser = new Parser(tokens);
        Program program = parser.parseProgram();

        JavaEmitter emitter = new JavaEmitter();
        String javaCode = emitter.emitProgram(program, "SpadGenerated");
        System.out.println(javaCode);
    }
}