package pl.edu.mimuw.nesc.integration;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.junit.Before;
import org.junit.Test;
import pl.edu.mimuw.nesc.ContextRef;
import pl.edu.mimuw.nesc.FileData;
import pl.edu.mimuw.nesc.Frontend;
import pl.edu.mimuw.nesc.NescFrontend;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.exception.InvalidOptionsException;
import pl.edu.mimuw.nesc.token.KeywordToken;
import pl.edu.mimuw.nesc.token.PunctuationToken;
import pl.edu.mimuw.nesc.token.Token;

import java.io.File;
import java.util.Collection;
import java.util.List;

import static org.fest.assertions.Assertions.*;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class FileResultTest {

    /*
     * FIXME: fix tests when identifiers are collected
     */

    private Frontend frontend;

    @Before
    public void setUp() throws Exception {
        frontend = NescFrontend.builder()
                .standalone(false)
                .build();
    }

    @Test
    public void testSimpleInterface() throws Exception {
        final String resourcePath = "integration/interface/Simple.nc";
        final FileData fileData = getFileData(resourcePath, "Simple", resourcePath);
        assertThat(fileData).isNotNull();

        /* check tokens */
        final Multimap<Integer, Token> tokens = fileData.getTokens();
        assertThat(tokens).isNotNull();

        // lines 2, 4, 6, 8, 9
        checkEquals(Lists.newArrayList(), tokens.get(2));
        checkEquals(Lists.newArrayList(), tokens.get(4));
        checkEquals(Lists.newArrayList(), tokens.get(6));
        checkEquals(Lists.newArrayList(), tokens.get(8));
        checkEquals(Lists.newArrayList(), tokens.get(9));

        // line 1
        final List<? extends Token> expectedFirstLineTokens = Lists.newArrayList(
                new KeywordToken(new Location("", 1, 1), new Location("", 1, 7), "typedef"),
                new KeywordToken(new Location("", 1, 9), new Location("", 1, 11), "int"),
                new PunctuationToken(new Location("", 1, 21), new Location("", 1, 21))
        );
        checkEquals(expectedFirstLineTokens, tokens.get(1));


        // line 3
        final List<? extends Token> expectedThirdLineTokens = Lists.newArrayList(
                new KeywordToken(new Location("", 3, 1), new Location("", 3, 9), "interface")
        );
        checkEquals(expectedThirdLineTokens, tokens.get(3));

        // line 5
        final List<? extends Token> expectedFifthLineTokens = Lists.newArrayList(
                new KeywordToken(new Location("", 5, 5), new Location("", 5, 11), "command"),
                new KeywordToken(new Location("", 5, 27), new Location("", 5, 29), "int"),
                new PunctuationToken(new Location("", 5, 38), new Location("", 5, 38)),
                new KeywordToken(new Location("", 5, 40), new Location("", 5, 42), "int"),
                new PunctuationToken(new Location("", 5, 50), new Location("", 5, 50)),
                new KeywordToken(new Location("", 5, 52), new Location("", 5, 55), "void"),
                new PunctuationToken(new Location("", 5, 62), new Location("", 5, 62))
        );
        checkEquals(expectedFifthLineTokens, tokens.get(5));

        // line 7
        final List<? extends Token> expectedSeventhLineTokens = Lists.newArrayList(
                new KeywordToken(new Location("", 7, 5), new Location("", 7, 9), "event"),
                new KeywordToken(new Location("", 7, 29), new Location("", 7, 32), "void"),
                new PunctuationToken(new Location("", 7, 38), new Location("", 7, 38)),
                new PunctuationToken(new Location("", 7, 57), new Location("", 7, 57))
        );
        checkEquals(expectedSeventhLineTokens, tokens.get(7));
    }

    private <T> void checkEquals(Collection<? extends T> expected, Collection<? extends T> actual) {
        assertThat(actual.size()).isEqualTo(expected.size());
        for (T object : expected) {
            assertThat(Iterables.contains(actual, object)).isTrue();
        }
    }

    private FileData getFileData(String resourcePath, String mainEntity, String testEntityPath)
            throws InvalidOptionsException {
        final String filePath = getResourceDirectory(resourcePath);
        final String dirPath = getParent(filePath);
        final String[] args = new String[]{"-p", dirPath, "-m", mainEntity};
        final ContextRef contextRef = frontend.createContext(args);
        return frontend.update(contextRef, getResourceDirectory(testEntityPath));
    }

    private String getResourceDirectory(String resourcePath) {
        return Thread.currentThread().getContextClassLoader()
                .getResource(resourcePath)
                .getPath();
    }

    private String getParent(String filePath) {
        return new File(filePath).getParent();
    }

}
