package pl.edu.mimuw.nesc.integration;

import com.google.common.base.Optional;
import org.junit.Test;
import pl.edu.mimuw.nesc.FileData;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.issue.NescError;
import pl.edu.mimuw.nesc.lexer.Comment;
import pl.edu.mimuw.nesc.token.CharacterToken;
import pl.edu.mimuw.nesc.token.NumberToken;
import pl.edu.mimuw.nesc.token.StringToken;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class InvalidTokensTest extends IntegrationTestBase {

    @Test
    public void testInvalidNumber() throws Exception {
        final String resourcePath = "integration/invalidtokens/InvalidNumber.nc";
        final FileData fileData = getFileData(frontend, resourcePath, "InvalidNumber", resourcePath);
        assertThat(fileData).isNotNull();

        final NumberToken expectedToken = new NumberToken(new Location("", 3, 21), new Location("", 3, 28),
                "1234.5K;");
        assertThat(fileData.getTokens().get(3)).contains(expectedToken);

        final NescError expectedError = new NescError(new Location("", 3, 21), Optional.of(new Location("", 3, 28)),
                "Invalid suffix \"K\" on numeric constant");
        assertThat(fileData.getIssues().get(3)).contains(expectedError);
    }

    @Test
    public void testInvalidStringEOL() throws Exception {
        final String resourcePath = "integration/invalidtokens/InvalidString.nc";
        final FileData fileData = getFileData(frontend, resourcePath, "InvalidString", resourcePath);
        assertThat(fileData).isNotNull();

        final StringToken expectedToken = new StringToken(new Location("", 3, 21), new Location("", 3, 41),
                "unterminated string;");
        assertThat(fileData.getTokens().get(3)).contains(expectedToken);

        final NescError expectedError = new NescError(new Location("", 3, 21), Optional.of(new Location("", 3, 41)),
                "Unterminated string literal after unterminated string;");
        assertThat(fileData.getIssues().get(3)).contains(expectedError);
    }

    @Test
    public void testInvalidStringEOF() throws Exception {
        final String resourcePath = "integration/invalidtokens/InvalidStringEOF.nc";
        final FileData fileData = getFileData(frontend, resourcePath, "InvalidStringEOF", resourcePath);
        assertThat(fileData).isNotNull();

        final StringToken expectedToken = new StringToken(new Location("", 3, 21), new Location("", 3, 40),
                "unterminated string");
        assertThat(fileData.getTokens().get(3)).contains(expectedToken);

        final NescError expectedError = new NescError(new Location("", 3, 21), Optional.of(new Location("", 3, 40)),
                "End of file in string literal after unterminated string");
        assertThat(fileData.getIssues().get(3)).contains(expectedError);
    }

    @Test
    public void testInvalidCharacter() throws Exception {
        final String resourcePath = "integration/invalidtokens/InvalidCharacter.nc";
        final FileData fileData = getFileData(frontend, resourcePath, "InvalidCharacter", resourcePath);
        assertThat(fileData).isNotNull();

        final CharacterToken expectedToken = new CharacterToken(new Location("", 3, 20), new Location("", 3, 22), "a;");
        assertThat(fileData.getTokens().get(3)).contains(expectedToken);

        final NescError expectedError = new NescError(new Location("", 3, 20), Optional.of(new Location("", 3, 22)),
                "Unterminated character literal after a;");
        assertThat(fileData.getIssues().get(3)).contains(expectedError);
    }

    @Test
    public void testInvalidCharacterEOF() throws Exception {
        final String resourcePath = "integration/invalidtokens/InvalidCharacterEOF.nc";
        final FileData fileData = getFileData(frontend, resourcePath, "InvalidCharacterEOF", resourcePath);
        assertThat(fileData).isNotNull();

        final CharacterToken expectedToken = new CharacterToken(new Location("", 3, 20), new Location("", 3, 21), "a");
        assertThat(fileData.getTokens().get(3)).contains(expectedToken);

        final NescError expectedError = new NescError(new Location("", 3, 20), Optional.of(new Location("", 3, 21)),
                "End of file in character literal after a");
        assertThat(fileData.getIssues().get(3)).contains(expectedError);
    }

    @Test
    public void testInvalidCommentEOF() throws Exception {
        final String resourcePath = "integration/invalidtokens/InvalidCommentEOF.nc";
        final FileData fileData = getFileData(frontend, resourcePath, "InvalidCommentEOF", resourcePath);
        assertThat(fileData).isNotNull();

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
        assertThat(fileData.getComments()).containsExactly(expectedComment);

        final NescError expectedError = new NescError(new Location("", 3, 1), Optional.<Location>absent(),
                "End of file in a multiline comment");
        assertThat(fileData.getIssues().get(3)).contains(expectedError);
    }

}
