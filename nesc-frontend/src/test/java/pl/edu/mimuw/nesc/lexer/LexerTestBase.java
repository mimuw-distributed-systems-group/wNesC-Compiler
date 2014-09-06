package pl.edu.mimuw.nesc.lexer;

import com.google.common.collect.ImmutableList;
import pl.edu.mimuw.nesc.parser.Parser;
import pl.edu.mimuw.nesc.parser.Symbol;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class LexerTestBase {

    protected Lexer lexer;

    @SuppressWarnings("ConstantConditions")
    protected List<Symbol> readSymbols(String resourcePath, LexerListener lexerListener) throws IOException {
        final String filePath = Thread.currentThread().getContextClassLoader()
                .getResource(resourcePath)
                .getFile();

        lexer = NescLexer.builder()
                .mainFile(filePath)
                .build();
        lexer.setListener(lexerListener);
        lexer.start();

        final List<Symbol> actual = readSymbols(lexer);
        lexer.close();
        return actual;
    }

    protected List<Symbol> readSymbols(final Lexer lexer) {
        final List<Symbol> result = new ArrayList<>();

        Symbol symbol = lexer.nextToken();
        while (symbol.getSymbolCode() != Parser.Lexer.EOF) {
            result.add(symbol);
            symbol = lexer.nextToken();
        }

        return ImmutableList.<Symbol>builder().addAll(result).build();
    }
}
