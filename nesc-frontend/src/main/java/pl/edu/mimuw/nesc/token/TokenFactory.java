package pl.edu.mimuw.nesc.token;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.parser.Symbol;

import static pl.edu.mimuw.nesc.parser.Parser.Lexer.*;

/**
 * Provides methods for creating token instances.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class TokenFactory {

    /**
     * Converts {@link pl.edu.mimuw.nesc.parser.Symbol} object into concrete
     * {@link pl.edu.mimuw.nesc.token.Token} instance. Only part of symbols
     * is converted. Tokens that should carry some semantic information should be
     * created in different way.
     *
     * @param symbol symbol passed from lexer to parser
     * @return token instance
     */
    public static Optional<? extends Token> of(Symbol symbol) {
        /* Don't create tokens from symbols that come from macros expansions */
        if (symbol.isExpanded()) {
            return Optional.absent();
        }

        final int symbolCode = symbol.getSymbolCode();

        /*
         * keywords
         *
         * To increase performance check only if code is in specific range.
         * Bison parser generator simply assigns consecutive codes to tokens
         * in order of their appearance in bison grammar definition.
         */
        if (symbolCode >= VOID && symbolCode <= TARGET_DEF) {
            return Optional.of(new KeywordToken(symbol.getLocation(), symbol.getEndLocation(), symbol.getValue()));
        }
        /* punctuation mark */
        else if (symbolCode == SEMICOLON || symbolCode == COMMA) {
            return Optional.of(new PunctuationToken(symbol.getLocation(), symbol.getEndLocation()));
        }
        /* number */
        else if (symbolCode == INTEGER_LITERAL || symbolCode == FLOATING_POINT_LITERAL) {
            return Optional.of(new NumberToken(symbol.getLocation(), symbol.getEndLocation(), symbol.getValue()));
        }
        /* string */
        else if (symbolCode == STRING_LITERAL) {
            return Optional.of(new StringToken(symbol.getLocation(), symbol.getEndLocation(), symbol.getValue()));
        }
        /* character */
        else if (symbolCode == CHARACTER_LITERAL) {
            return Optional.of(new CharacterToken(symbol.getLocation(), symbol.getEndLocation(), symbol.getValue()));
        }
        return Optional.absent();
    }

    private TokenFactory() {
    }
}
