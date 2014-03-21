package pl.edu.mimuw.nesc.lexer;

import com.google.common.collect.ImmutableList;
import pl.edu.mimuw.nesc.parser.Parser;
import pl.edu.mimuw.nesc.parser.Symbol;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class LexerTestBase {

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
