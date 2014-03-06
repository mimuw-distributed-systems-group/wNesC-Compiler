package pl.edu.mimuw.nesc.lexer;

import org.anarres.cpp.PreprocessorCommand;
import org.anarres.cpp.Token;
import pl.edu.mimuw.nesc.preprocessor.directive.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Helper class for building preprocessor directive from list of tokens.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
final class PreprocessorDirectiveHelper {

    /*
     * Assertions are used not to affect performance.
     */

    public PreprocessorDirectiveHelper() {
    }

    /**
     * Converts preprocessor object into frontend object representing
     * preprocessor instruction.
     *
     * @param directive  preprocessor directive (from parser)
     * @param sourceFile source file
     * @return preprocessor directive (frontend)
     */
    public PreprocessorDirective buildPreprocessorDirective(final org.anarres.cpp.PreprocessorDirective directive,
                                                            String sourceFile) {
        checkNotNull(directive, "directive cannot be null");

        final PreprocessorCommand command = directive.getCommand();
        final boolean isActiveBlock = directive.isActiveBlock();
        final List<Token> tokens = directive.getTokenList();
        final pl.edu.mimuw.nesc.preprocessor.directive.PreprocessorDirective.Builder builder;

        // FIXME: remove
        //System.out.println("DIRECTIVE: '" + directive + "'");
        //System.out.println("Tokens (" + tokens.size() + "):");
        //for (Token token : tokens) {
        //    System.out.println(token);
        //}
        //System.out.println("END OF TOKENS");

        /*
         * Tokens list contains at least two elements: hash and directive name.
         */
        assert (tokens.size() >= 2);
        assert (tokens.get(0).getType() == Token.HASH);
        assert (tokens.get(1).getType() == Token.IDENTIFIER);

        final Token hashToken = tokens.get(0);
        final Token keywordToken = tokens.get(1);

        switch (command) {
            case PP_INCLUDE:
                builder = handleIncludeDirective(directive);
                break;
            case PP_DEFINE:
                builder = handleDefineDirective(directive);
                break;
            case PP_UNDEF:
                builder = handleUndefDirective(directive);
                break;
            case PP_IF:
                builder = handleIfDirective(directive);
                break;
            case PP_IFDEF:
                builder = handleIfdefDirective(directive);
                break;
            case PP_IFNDEF:
                builder = handleIfndefDirective(directive);
                break;
            case PP_ELSE:
                builder = handleElseDirective(directive);
                break;
            case PP_ELIF:
                builder = handleElifDirective(directive);
                break;
            case PP_ENDIF:
                builder = handleEndifDirective(directive);
                break;
            case PP_WARNING:
                builder = handleWarningDirective(directive);
                break;
            case PP_ERROR:
                builder = handleErrorDirective(directive);
                break;
            case PP_PRAGMA:
                builder = handlePragmaDirective(directive);
                break;
            case PP_LINE:
                builder = handleLineDirective(directive);
                break;
            default:
                throw new RuntimeException("not handled preprocessor command " + command);
        }

        /*
         * Set common parameters.
         */

        buildLinesMap(tokens, builder);

        final PreprocessorDirective newDirective = builder.sourceFile(sourceFile)
                .activeBlock(isActiveBlock)
                .hashLocation(hashToken.getLine(), hashToken.getColumn() + 1)
                .keywordLocation(keywordToken.getLine(),
                        keywordToken.getColumn() + 1,
                        keywordToken.getText().length())
                .build();

        return newDirective;
    }

    private void buildLinesMap(List<Token> tokens,
                               pl.edu.mimuw.nesc.preprocessor.directive.PreprocessorDirective.Builder builder) {
        final Map<Integer, Integer> linesMap = new HashMap<>();

        int currentLine = tokens.get(0).getLine();
        int currentColumn = tokens.get(0).getColumn() + 1;
        for (Token token : tokens) {
            final int tokenLine = token.getLine();
            final int tokenColumn = token.getColumn() + 1;

            assert ((currentLine == tokenLine && currentColumn <= tokenColumn) || (currentLine < tokenLine));

            if (tokenLine == currentLine) {
                currentColumn = tokenColumn;
            } else {
                linesMap.put(currentLine, currentColumn);
                currentLine = tokenLine;
            }
        }
        linesMap.put(currentLine, currentColumn);

        builder.addLines(linesMap);
    }

    private IncludeDirective.Builder handleIncludeDirective(final org.anarres.cpp.PreprocessorDirective directive) {
        assert (directive.getTokenList().size() >= 3);

        final IncludeDirective.Builder builder = IncludeDirective.builder();
        final Token argToken = directive.getTokenList().get(2);

        switch (argToken.getType()) {
            case Token.STRING:
                final String fileNameId = (String) argToken.getValue();
                /* Value in STRING token has already removed quotes. */
                builder.isSystem(false)
                        .fileName(fileNameId);
                break;
            case Token.HEADER:
                final String fileNameH = (String) argToken.getValue();
                builder.isSystem(true)
                        .fileName(fileNameH);
                break;
            default:
                throw new RuntimeException("not handled token type " + argToken);
        }

        builder.argumentLocation(argToken.getLine(),
                argToken.getColumn() + 1,
                argToken.getText().length());

        return builder;
    }

    private DefineDirective.Builder handleDefineDirective(final org.anarres.cpp.PreprocessorDirective directive) {
        assert (directive.getTokenList().size() >= 3);
        assert (directive.getTokenList().get(2).getType() == Token.IDENTIFIER);

        final DefineDirective.Builder builder = DefineDirective.builder();
        final Token nameToken = directive.getTokenList().get(2);

        builder.name(nameToken.getText())
                .nameLocation(nameToken.getLine(),
                        nameToken.getColumn() + 1,
                        nameToken.getText().length());

        // TODO: retrieve args

        return builder;
    }

    private UndefDirective.Builder handleUndefDirective(final org.anarres.cpp.PreprocessorDirective directive) {
        assert (directive.getTokenList().size() == 3);
        assert (directive.getTokenList().get(2).getType() == Token.IDENTIFIER);

        final UndefDirective.Builder builder = UndefDirective.builder();
        final Token nameToken = directive.getTokenList().get(2);

        builder.name(nameToken.getText())
                .nameLocation(nameToken.getLine(),
                        nameToken.getColumn() + 1,
                        nameToken.getText().length());
        return builder;
    }

    private IfDirective.Builder handleIfDirective(final org.anarres.cpp.PreprocessorDirective directive) {
        return IfDirective.builder();
    }

    private IfdefDirective.Builder handleIfdefDirective(final org.anarres.cpp.PreprocessorDirective directive) {
        return (IfdefDirective.Builder) handleConditionalMacroDirective(directive, IfdefDirective.builder());
    }

    private IfndefDirective.Builder handleIfndefDirective(final org.anarres.cpp.PreprocessorDirective directive) {
        return (IfndefDirective.Builder) handleConditionalMacroDirective(directive, IfndefDirective.builder());
    }

    private ConditionalMacroDirective.Builder handleConditionalMacroDirective(
            final org.anarres.cpp.PreprocessorDirective directive, final ConditionalMacroDirective.Builder builder) {
        assert (directive.getTokenList().size() == 3);
        assert (directive.getTokenList().get(2).getType() == Token.IDENTIFIER);

        // FIXME: set proper macro name and location, currently preprocessor
        // contains some bugs and do not return macro name.
        //final Token macroToken = directive.getTokenList().get(2);

        builder.macro("eee")//macro(macroToken.getText())
                .macroLocation(1, 1, 2);
        //        .macroLocation(macroToken.getLine(),
        //                macroToken.getColumn() + 1,
        //                macroToken.getText().length());
        return builder;
    }

    private ElseDirective.Builder handleElseDirective(final org.anarres.cpp.PreprocessorDirective directive) {
        return ElseDirective.builder();
    }

    private ElifDirective.Builder handleElifDirective(final org.anarres.cpp.PreprocessorDirective directive) {
        return ElifDirective.builder();
    }

    private EndifDirective.Builder handleEndifDirective(final org.anarres.cpp.PreprocessorDirective directive) {
        return EndifDirective.builder();
    }

    private WarningDirective.Builder handleWarningDirective(final org.anarres.cpp.PreprocessorDirective directive) {
        final WarningDirective.Builder builder = WarningDirective.builder();
        final List<Token> tokens = directive.getTokenList();

        /* Build warning message. */
        final StringBuilder stringBuilder = new StringBuilder();
        /* Skip hash and warning token. */
        for (int i = 2; i < tokens.size(); ++i) {
            stringBuilder.append(tokens.get(i).getText());
        }

        builder.message(stringBuilder.toString());
        return builder;
    }

    private ErrorDirective.Builder handleErrorDirective(final org.anarres.cpp.PreprocessorDirective directive) {
        final ErrorDirective.Builder builder = ErrorDirective.builder();
        final List<Token> tokens = directive.getTokenList();

        /* Build error message. */
        final StringBuilder stringBuilder = new StringBuilder();
        /* Skip hash and error token. */
        for (int i = 2; i < tokens.size(); ++i) {
            stringBuilder.append(tokens.get(i).getText());
        }

        builder.message(stringBuilder.toString());
        return builder;
    }

    private PragmaDirective.Builder handlePragmaDirective(final org.anarres.cpp.PreprocessorDirective directive) {
        // TODO
        return PragmaDirective.builder();
    }

    private LineDirective.Builder handleLineDirective(final org.anarres.cpp.PreprocessorDirective directive) {
        // TODO
        return LineDirective.builder();
    }

}
