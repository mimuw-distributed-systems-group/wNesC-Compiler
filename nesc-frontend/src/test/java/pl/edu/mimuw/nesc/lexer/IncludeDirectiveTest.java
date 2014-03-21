package pl.edu.mimuw.nesc.lexer;

import org.junit.Test;
import pl.edu.mimuw.nesc.preprocessor.directive.IncludeDirective;
import pl.edu.mimuw.nesc.preprocessor.directive.PreprocessorDirective;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class IncludeDirectiveTest extends LexerTestBase {

    @Test
    public void testExistingFileInclude() throws Exception {
        final String filePath = Thread.currentThread().getContextClassLoader()
                .getResource("lexer/include/IncludeFound.nc")
                .getFile();

        final Lexer lexer = NescLexer.builder()
                .mainFile(filePath)
                .build();
        lexer.setListener(new TestLexerListener() {
            @Override
            public void preprocessorDirective(PreprocessorDirective preprocessorDirective) {
                assertThat(preprocessorDirective).isInstanceOf(IncludeDirective.class);
                final IncludeDirective include = (IncludeDirective) preprocessorDirective;
                assertThat(include.getFilePath().isPresent()).isTrue();
                assertThat(include.getFilePath().get()).endsWith("lexer/include/header_file.h");

            }
        });
        lexer.start();
        readSymbols(lexer);
        lexer.close();
    }

    @Test
    public void testNonexistentFileInclude() throws Exception {
        final String filePath = Thread.currentThread().getContextClassLoader()
                .getResource("lexer/include/IncludeNotFound.nc")
                .getFile();

        final Lexer lexer = NescLexer.builder()
                .mainFile(filePath)
                .build();
        lexer.setListener(new TestLexerListener() {
            @Override
            public void preprocessorDirective(PreprocessorDirective preprocessorDirective) {
                assertThat(preprocessorDirective).isInstanceOf(IncludeDirective.class);
                final IncludeDirective include = (IncludeDirective) preprocessorDirective;
                assertThat(include.getFilePath().isPresent()).isFalse();

            }
        });
        lexer.start();
        readSymbols(lexer);
        lexer.close();
    }

}
