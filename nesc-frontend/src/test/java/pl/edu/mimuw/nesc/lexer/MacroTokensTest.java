package pl.edu.mimuw.nesc.lexer;

import com.google.common.base.Optional;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.parser.Symbol;
import pl.edu.mimuw.nesc.token.MacroToken;

import static org.junit.Assert.*;
import static pl.edu.mimuw.nesc.parser.Parser.Lexer.*;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class MacroTokensTest extends LexerTestBase {
    private static final String TEST_FILENAME = "lexer/macros/macros.nc";
    private Lexer lexer;

    @Before
    public void resetLexer() throws IOException {
        lexer = NescLexer.builder()
                .mainFile(getTestFilePath(TEST_FILENAME))
                .build();
    }

    @Test
    public void testLocations() {
        final List<Symbol> actualSymbols = readSymbols(lexer);
        final Symbol.Builder symbolBuilder = Symbol.builder();
        symbolBuilder.file(getTestFilePath(TEST_FILENAME));

        final List<Symbol> expectedSymbols = Collections.unmodifiableList(Arrays.asList(
                // line 8
                symbolBuilder
                        .symbolCode(INTEGER_LITERAL)
                        .line(8).column(6)
                        .endLine(8).endColumn(9)
                        .value("0")
                        .build(),
                symbolBuilder
                        .symbolCode(PLUS)
                        .line(8).column(1)
                        .endLine(8).endColumn(4)
                        .value("+")
                        .build(),
                symbolBuilder
                        .symbolCode(INTEGER_LITERAL)
                        .line(8).column(12)
                        .endLine(8).endColumn(12)
                        .value("1")
                        .build(),
                symbolBuilder
                        .symbolCode(SEMICOLON)
                        .line(8).column(14)
                        .endLine(8).endColumn(14)
                        .value(";")
                        .build(),
                // line 9
                symbolBuilder
                        .symbolCode(INTEGER_LITERAL)
                        .line(9).column(6)
                        .endLine(9).endColumn(9)
                        .value("0")
                        .build(),
                symbolBuilder
                        .symbolCode(PLUS)
                        .line(9).column(1)
                        .endLine(9).endColumn(4)
                        .value("+")
                        .build(),
                symbolBuilder
                        .symbolCode(INTEGER_LITERAL)
                        .line(9).column(12)
                        .endLine(9).endColumn(12)
                        .value("1")
                        .build(),
                symbolBuilder
                        .symbolCode(PLUS)
                        .line(9).column(1)
                        .endLine(9).endColumn(4)
                        .value("+")
                        .build(),
                symbolBuilder
                        .symbolCode(INTEGER_LITERAL)
                        .line(9).column(15)
                        .endLine(9).endColumn(15)
                        .value("2")
                        .build(),
                symbolBuilder
                        .symbolCode(SEMICOLON)
                        .line(9).column(17)
                        .endLine(9).endColumn(17)
                        .value(";")
                        .build(),
                // line 10
                symbolBuilder
                        .symbolCode(INTEGER_LITERAL)
                        .line(10).column(6)
                        .endLine(10).endColumn(6)
                        .value("1")
                        .build(),
                symbolBuilder
                        .symbolCode(PLUS)
                        .line(10).column(1)
                        .endLine(10).endColumn(4)
                        .value("+")
                        .build(),
                symbolBuilder
                        .symbolCode(INTEGER_LITERAL)
                        .line(10).column(13)
                        .endLine(10).endColumn(16)
                        .value("0")
                        .build(),
                symbolBuilder
                        .symbolCode(MINUS)
                        .line(10).column(9)
                        .endLine(10).endColumn(11)
                        .value("-")
                        .build(),
                symbolBuilder
                        .symbolCode(INTEGER_LITERAL)
                        .line(10).column(19)
                        .endLine(10).endColumn(19)
                        .value("1")
                        .build(),
                symbolBuilder
                        .symbolCode(SEMICOLON)
                        .line(10).column(22)
                        .endLine(10).endColumn(22)
                        .value(";")
                        .build(),
                symbolBuilder
                        .symbolCode(INTEGER_LITERAL)
                        .line(11).column(6)
                        .endLine(11).endColumn(6)
                        .value("1")
                        .build(),
                symbolBuilder
                        .symbolCode(PLUS)
                        .line(11).column(1)
                        .endLine(11).endColumn(4)
                        .value("+")
                        .build(),
                symbolBuilder
                        .symbolCode(INTEGER_LITERAL)
                        .line(11).column(14)
                        .endLine(11).endColumn(17)
                        .value("0")
                        .build(),
                symbolBuilder
                        .symbolCode(PLUS)
                        .line(11).column(9)
                        .endLine(11).endColumn(12)
                        .value("+")
                        .build(),
                symbolBuilder
                        .symbolCode(INTEGER_LITERAL)
                        .line(11).column(20)
                        .endLine(11).endColumn(20)
                        .value("1")
                        .build(),
                symbolBuilder
                        .symbolCode(SEMICOLON)
                        .line(11).column(23)
                        .endLine(11).endColumn(23)
                        .value(";")
                        .build(),
                // line 12
                symbolBuilder
                        .symbolCode(STRING_LITERAL)
                        .line(12).column(1)
                        .endLine(12).endColumn(3)
                        .value("s")
                        .build(),
                symbolBuilder
                        .symbolCode(SEMICOLON)
                        .line(12).column(4)
                        .endLine(12).endColumn(4)
                        .value(";")
                        .build()

        ));

        // Check size of the arrays
        assertEquals(expectedSymbols.size(), actualSymbols.size());

        // Check whether appropriate symbols are equal
        for (int i = 0; i < expectedSymbols.size(); ++i) {
            assertNotNull("Symbol " + (i + 1) + " is null!", expectedSymbols.get(i));
            assertEquals("Symbols " + (i + 1) + " differ!\n" + expectedSymbols.get(i).print()
                    + "\n" + actualSymbols.get(i).print(), expectedSymbols.get(i), actualSymbols.get(i));
        }
    }

    @Test
    public void testListener() {
        // An auxiliary class.
        class MacroInfo {
            public final String name;
            public final Optional<Location> definitionLocation;

            public MacroInfo(String name, Optional<Location> definitionLocation) {
                this.name = name;
                this.definitionLocation = definitionLocation;
            }
        }

        final String testFileFullPath = getTestFilePath(TEST_FILENAME);
        final MacroInfo add2Info = new MacroInfo("ADD2", Optional.of(new Location(testFileFullPath, 1, 9))),
                        add3Info = new MacroInfo("ADD3", Optional.of(new Location(testFileFullPath, 2, 9))),
                        subInfo = new MacroInfo("SUB", Optional.of(new Location(testFileFullPath, 3, 9))),
                        zeroInfo = new MacroInfo("ZERO", Optional.of(new Location(testFileFullPath, 4, 9))),
                        strInfo = new MacroInfo("STR", Optional.of(new Location(testFileFullPath, 5, 9))),
                        emptyInfo = new MacroInfo("EMPTY", Optional.of(new Location(testFileFullPath, 6, 9)));

        final List<MacroToken> expectedMacroTokens = Collections.unmodifiableList(Arrays.asList(
            // line 8
            new MacroToken(new Location(testFileFullPath, 8, 6), new Location(testFileFullPath, 8, 9),
                           zeroInfo.name, zeroInfo.definitionLocation),
            new MacroToken(new Location(testFileFullPath, 8, 1), new Location(testFileFullPath, 8, 4),
                           add2Info.name, add2Info.definitionLocation),
            // line 9
            new MacroToken(new Location(testFileFullPath, 9, 6), new Location(testFileFullPath, 9, 9),
                           zeroInfo.name, zeroInfo.definitionLocation),
            new MacroToken(new Location(testFileFullPath, 9, 1), new Location(testFileFullPath, 9, 4),
                           add3Info.name, add3Info.definitionLocation),
            // line 10
            new MacroToken(new Location(testFileFullPath, 10, 13), new Location(testFileFullPath, 10, 16),
                        zeroInfo.name, zeroInfo.definitionLocation),
            new MacroToken(new Location(testFileFullPath, 10, 9), new Location(testFileFullPath, 10, 11),
                        subInfo.name, subInfo.definitionLocation),
            new MacroToken(new Location(testFileFullPath, 10, 1), new Location(testFileFullPath, 10, 4),
                        add2Info.name, add2Info.definitionLocation),
            // line 11
            new MacroToken(new Location(testFileFullPath, 11, 14), new Location(testFileFullPath, 11, 17),
                        zeroInfo.name, zeroInfo.definitionLocation),
            new MacroToken(new Location(testFileFullPath, 11, 9), new Location(testFileFullPath, 11, 12),
                        add2Info.name, add2Info.definitionLocation),
            new MacroToken(new Location(testFileFullPath, 11, 1), new Location(testFileFullPath, 11, 4),
                        add2Info.name, add2Info.definitionLocation),
            // line 12
            new MacroToken(new Location(testFileFullPath, 12, 1), new Location(testFileFullPath, 12, 3),
                        strInfo.name, strInfo.definitionLocation),
            // line 13
            new MacroToken(new Location(testFileFullPath, 13, 1), new Location(testFileFullPath, 13, 5),
                        emptyInfo.name, emptyInfo.definitionLocation)
        ));

        // Create and set listener
        MacroTokensListener listener = new MacroTokensListener(expectedMacroTokens);
        lexer.setListener(listener);

        // Result is irrelevant in this test
        readSymbols(lexer);

        // Check the number of tokens that has been received by the listener
        assertEquals(listener.getCounterValue(), 12);
    }

    private static String getTestFilePath(String relativePath) {
        return ClassLoader.getSystemResource(TEST_FILENAME).getFile();
    }

    /**
     * Listener that is responsible of checking all macro tokens it gets. It
     * compares them to the expected tokens set in the constructor. If
     * a token differs, it reports an error.
     */
    private static class MacroTokensListener extends TestLexerListener {
        private final List<MacroToken> expectedMacroTokens;

        /**
         * Index of the next macro token from the <code>expectedMacroTokens</code> list
         * to check.
         */
        private int counter = 0;

        private MacroTokensListener(List<MacroToken> expectedMacroTokens) {
            this.expectedMacroTokens = expectedMacroTokens;
        }

        @Override
        public void macroInstantiation(MacroToken macroToken) {
            assertTrue(counter < expectedMacroTokens.size());
            assertEquals(expectedMacroTokens.get(counter),  macroToken);
            ++counter;
        }

        private int getCounterValue() {
            return counter;
        }
    }
}
