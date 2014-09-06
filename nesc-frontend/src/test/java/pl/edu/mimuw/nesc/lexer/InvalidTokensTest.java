package pl.edu.mimuw.nesc.lexer;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import org.junit.Test;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.parser.Parser;
import pl.edu.mimuw.nesc.parser.Symbol;
import pl.edu.mimuw.nesc.problem.NescError;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class InvalidTokensTest extends LexerTestBase {

    private static final class InvalidTokensLexerListener extends TestLexerListener {

        private final List<NescError> errorList;
        private final List<Comment> commentList;

        public InvalidTokensLexerListener() {
            this.errorList = Lists.newArrayList();
            this.commentList = Lists.newArrayList();
        }

        @Override
        public void error(String fileName, int startLine, int startColumn,
                          Optional<Integer> endLine, Optional<Integer> endColumn, String message) {
            final Location startLocation = new Location(fileName, startLine, startColumn);
            final Optional<Location> endLocation;
            if (endLine.isPresent()) {
                endLocation = Optional.of(new Location(fileName, endLine.get(), endColumn.get()));
            } else {
                endLocation = Optional.absent();
            }
            final NescError error = new NescError(startLocation, endLocation, message);
            errorList.add(error);
        }

        @Override
        public void comment(Comment comment) {
            commentList.add(comment);
        }

        public List<NescError> getErrorList() {
            return errorList;
        }

        public List<Comment> getCommentList() {
            return commentList;
        }
    }

    @Test
    public void testInvalidNumber() throws Exception {
        final InvalidTokensLexerListener listener = new InvalidTokensLexerListener();
        final List<Symbol> actual = readSymbols("lexer/invalidtokens/InvalidNumber.nc", listener);
        assertThat(actual).isNotNull();
        assertThat(actual).isNotEmpty();
        assertThat(listener.getErrorList()).isNotEmpty();

        final Symbol expectedToken = Symbol.builder()
                .symbolCode(Parser.Lexer.FLOATING_POINT_LITERAL)
                .file("")   // not important
                .line(3).column(21)
                .endLine(3).endColumn(28)
                .value("1234.5K;")
                .invalid(true)
                .isExpanded(false)
                .build();
        final NescError expectedError = new NescError(new Location("", 3, 21), Optional.of(new Location("", 3, 28)),
                "Invalid suffix \"K\" on numeric constant");

        assertThat(actual).contains(expectedToken);
        assertThat(listener.getErrorList()).containsExactly(expectedError);
    }

    @Test
    public void testInvalidStringEOL() throws Exception {
        final InvalidTokensLexerListener listener = new InvalidTokensLexerListener();
        final List<Symbol> actual = readSymbols("lexer/invalidtokens/InvalidString.nc", listener);
        assertThat(actual).isNotNull();
        assertThat(actual).isNotEmpty();
        assertThat(listener.getErrorList()).isNotEmpty();

        final Symbol expectedToken = Symbol.builder()
                .symbolCode(Parser.Lexer.STRING_LITERAL)
                .file("")   // not important
                .line(3).column(21)
                .endLine(3).endColumn(41)
                .value("unterminated string;")
                .invalid(true)
                .isExpanded(false)
                .build();
        final NescError expectedError = new NescError(new Location("", 3, 21), Optional.of(new Location("", 3, 41)),
                "Unterminated string literal after unterminated string;");

        assertThat(actual).contains(expectedToken);
        assertThat(listener.getErrorList()).containsExactly(expectedError);
    }

    @Test
    public void testInvalidStringEOF() throws Exception {
        final InvalidTokensLexerListener listener = new InvalidTokensLexerListener();
        final List<Symbol> actual = readSymbols("lexer/invalidtokens/InvalidStringEOF.nc", listener);
        assertThat(actual).isNotNull();
        assertThat(actual).isNotEmpty();
        assertThat(listener.getErrorList()).isNotEmpty();

        final Symbol expectedToken = Symbol.builder()
                .symbolCode(Parser.Lexer.STRING_LITERAL)
                .file("")   // not important
                .line(3).column(21)
                .endLine(3).endColumn(40)
                .value("unterminated string")
                .invalid(true)
                .isExpanded(false)
                .build();
        final NescError expectedError = new NescError(new Location("", 3, 21), Optional.of(new Location("", 3, 40)),
                "End of file in string literal after unterminated string");

        assertThat(actual).contains(expectedToken);
        assertThat(listener.getErrorList()).containsExactly(expectedError);
    }

    @Test
    public void testInvalidCharacter() throws Exception {
        final InvalidTokensLexerListener listener = new InvalidTokensLexerListener();
        final List<Symbol> actual = readSymbols("lexer/invalidtokens/InvalidCharacter.nc", listener);
        assertThat(actual).isNotNull();
        assertThat(actual).isNotEmpty();
        assertThat(listener.getErrorList()).isNotEmpty();

        final Symbol expectedToken = Symbol.builder()
                .symbolCode(Parser.Lexer.CHARACTER_LITERAL)
                .file("")   // not important
                .line(3).column(20)
                .endLine(3).endColumn(22)
                .value("a;")
                .invalid(true)
                .isExpanded(false)
                .build();
        final NescError expectedError = new NescError(new Location("", 3, 20), Optional.of(new Location("", 3, 22)),
                "Unterminated character literal after a;");

        assertThat(actual).contains(expectedToken);
        assertThat(listener.getErrorList()).containsExactly(expectedError);
    }

    @Test
    public void testInvalidCharacterEOF() throws Exception {
        final InvalidTokensLexerListener listener = new InvalidTokensLexerListener();
        final List<Symbol> actual = readSymbols("lexer/invalidtokens/InvalidCharacterEOF.nc", listener);
        assertThat(actual).isNotNull();
        assertThat(actual).isNotEmpty();
        assertThat(listener.getErrorList()).isNotEmpty();

        final Symbol expectedToken = Symbol.builder()
                .symbolCode(Parser.Lexer.CHARACTER_LITERAL)
                .file("")   // not important
                .line(3).column(20)
                .endLine(3).endColumn(21)
                .value("a")
                .invalid(true)
                .isExpanded(false)
                .build();
        final NescError expectedError = new NescError(new Location("", 3, 20), Optional.of(new Location("", 3, 21)),
                "End of file in character literal after a");

        assertThat(actual).contains(expectedToken);
        assertThat(listener.getErrorList()).containsExactly(expectedError);
    }

    @Test
    public void testInvalidCommentEOF() throws Exception {
        final InvalidTokensLexerListener listener = new InvalidTokensLexerListener();
        final List<Symbol> actual = readSymbols("lexer/invalidtokens/InvalidCommentEOF.nc", listener);
        assertThat(actual).isNotNull();
        assertThat(listener.getCommentList()).isNotEmpty();
        assertThat(listener.getErrorList()).isNotEmpty();

        final Comment expectedComment = Comment.builder()
                .file("")
                .line(3)
                .column(1)
                .isC(true)
                .body("/* this is\n" +
                        " * an unterminated\n" +
                        " * comment")
                .invalid(true)
                .build();
        final NescError expectedError = new NescError(new Location("", 3, 1), Optional.<Location>absent(),
                "End of file in a multiline comment");

        assertThat(listener.getCommentList()).contains(expectedComment);
        assertThat(listener.getErrorList()).containsExactly(expectedError);
    }
}
