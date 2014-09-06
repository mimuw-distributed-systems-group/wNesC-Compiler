package pl.edu.mimuw.nesc.lexer;

import com.google.common.collect.ImmutableList;
import org.junit.Ignore;
import org.junit.Test;
import pl.edu.mimuw.nesc.parser.Parser;
import pl.edu.mimuw.nesc.parser.Symbol;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static pl.edu.mimuw.nesc.parser.Parser.Lexer.*;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class TokensLocationsTest extends LexerTestBase {

    @Test
    @Ignore // FIXME
    public void testLocations1() throws IOException {
        @SuppressWarnings("ConstantConditions") final String filePath = Thread.currentThread().getContextClassLoader()
                .getResource("lexer/locations/tokens_locations1.nc")
                .getFile();

        final Lexer lexer = NescLexer.builder()
                .mainFile(filePath)
                .build();
        lexer.start();

        final List<Symbol> actual = readSymbols(lexer);
        lexer.close();

        final List<Symbol> expected = ImmutableList.<Symbol>builder()
                // line 1
                .add(Symbol.builder()
                        .symbolCode(MODULE)
                        .file(filePath)
                        .line(1).column(1)
                        .endLine(1).endColumn(6)
                        .value("module")
                        .build())
                .add(Symbol.builder()
                        .symbolCode(IDENTIFIER)
                        .file(filePath)
                        .line(1).column(8)
                        .endLine(1).endColumn(22)
                        .value("TokensLocations")
                        .build())
                .add(Symbol.builder()
                        .symbolCode(LBRACE)
                        .file(filePath)
                        .line(1).column(24)
                        .endLine(1).endColumn(24)
                        .build())
                // line 2
                .add(Symbol.builder()
                        .symbolCode(USES)
                        .file(filePath)
                        .line(2).column(5)
                        .endLine(2).endColumn(8)
                        .value("uses")
                        .build())
                .add(Symbol.builder()
                        .symbolCode(INTERFACE)
                        .file(filePath)
                        .line(2).column(10)
                        .endLine(2).endColumn(18)
                        .value("interface")
                        .build())
                .add(Symbol.builder()
                        .symbolCode(IDENTIFIER)
                        .file(filePath)
                        .line(2).column(20)
                        .endLine(2).endColumn(22)
                        .value("Foo")
                        .build())
                .add(Symbol.builder()
                        .symbolCode(SEMICOLON)
                        .file(filePath)
                        .line(2).column(23)
                        .endLine(2).endColumn(23)
                        .build())
                // line 3
                .add(Symbol.builder()
                        .symbolCode(RBRACE)
                        .file(filePath)
                        .line(3).column(1)
                        .endLine(3).endColumn(1)
                        .build())
                .add(Symbol.builder()
                        .symbolCode(IMPLEMENTATION)
                        .file(filePath)
                        .line(3).column(3)
                        .endLine(3).endColumn(16)
                        .value("implementation")
                        .build())
                .add(Symbol.builder()
                        .symbolCode(LBRACE)
                        .file(filePath)
                        .line(3).column(18)
                        .endLine(3).endColumn(18)
                        .build())
                // line 4
                .add(Symbol.builder()
                        .symbolCode(CHAR)
                        .file(filePath)
                        .line(4).column(5)
                        .endLine(4).endColumn(8)
                        .value("char")
                        .build())
                .add(Symbol.builder()
                        .symbolCode(STAR)
                        .file(filePath)
                        .line(4).column(10)
                        .endLine(4).endColumn(10)
                        .build())
                .add(Symbol.builder()
                        .symbolCode(IDENTIFIER)
                        .file(filePath)
                        .line(4).column(11)
                        .endLine(4).endColumn(23)
                        .value("broken_string")
                        .build())
                .add(Symbol.builder()
                        .symbolCode(EQ)
                        .file(filePath)
                        .line(4).column(25)
                        .endLine(4).endColumn(25)
                        .build())
                .add(Symbol.builder()
                        .symbolCode(STRING_LITERAL)
                        .file(filePath)
                        .line(4).column(27)
                        .endLine(6).endColumn(33)
                        .value("This string is broken " +
                                "            with backslash " +
                                "            into multiline token")
                        .build())
                // line 6
                .add(Symbol.builder()
                        .symbolCode(SEMICOLON)
                        .file(filePath)
                        .line(6).column(34)
                        .endLine(6).endColumn(34)
                        .build())
                .build();

        assertThat(actual).isEqualTo(expected);
    }

}
