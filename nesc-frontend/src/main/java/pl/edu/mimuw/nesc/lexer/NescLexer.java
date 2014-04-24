package pl.edu.mimuw.nesc.lexer;

import com.google.common.base.Optional;
import org.anarres.cpp.*;
import org.apache.log4j.Logger;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.common.util.file.FileUtils;
import pl.edu.mimuw.nesc.parser.Symbol;
import pl.edu.mimuw.nesc.preprocessor.PreprocessorMacro;
import pl.edu.mimuw.nesc.preprocessor.directive.IncludeDirective;
import pl.edu.mimuw.nesc.token.MacroToken;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static pl.edu.mimuw.nesc.common.util.file.FileUtils.normalizePath;
import static pl.edu.mimuw.nesc.lexer.SymbolFactory.getSymbolCode;
import static pl.edu.mimuw.nesc.parser.Parser.Lexer.*;

/**
 * NesC language lexer.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class NescLexer extends AbstractLexer {

    private static final Logger LOG = Logger.getLogger(NescLexer.class);

    private final Preprocessor preprocessor;
    /**
     * Preprocessor tokens queue.
     */
    private final LinkedList<Token> tokenQueue;
    /**
     * Lexer symbols queue.
     */
    private final LinkedList<Symbol> symbolQueue;
    /**
     * Keeps track of files currently being parsed.
     */
    private final Stack<String> sourceStack;
    /**
     * First file to parse, could be a nesC file or header/C file included
     * by default.
     */
    private final String startFile;

    /**
     * Creates lexer from builder.
     *
     * @param builder builder
     * @throws IOException
     * @throws pl.edu.mimuw.nesc.exception.LexerException
     */
    protected NescLexer(Builder builder) throws IOException, pl.edu.mimuw.nesc.exception.LexerException {
        super(builder.mainFilePath, builder.systemIncludePaths, builder.userIncludePaths, builder.includeFilePaths,
                builder.macros, builder.unparsedMacros);
        final NescPreprocessorListener preprocessorListener = new NescPreprocessorListener();
        this.tokenQueue = new LinkedList<>();
        this.symbolQueue = new LinkedList<>();
        this.sourceStack = new Stack<>();

        /*
         * Init preprocessor. When we want to include some additional header
         * files and we need them to be preprocessed before the main source
         * file, we have to create the preprocessor instance with the first
         * header file as the constructor parameter. The remaining header files
         * should be set using addInput method. Finally, the main source file
         * should be add as the LAST file using addInput.
         */
        final LinkedList<String> filesOrder = new LinkedList<>(this.includeFilePaths);
        filesOrder.add(this.mainFilePath);
        startFile = filesOrder.removeFirst();
        this.preprocessor = new Preprocessor(new File(startFile));
        this.sourceStack.push(startFile);
        for (String filePath : filesOrder) {
            this.preprocessor.addInput(new File(filePath));
        }
        this.preprocessor.setListener(preprocessorListener);
        this.preprocessor.setQuoteIncludePath(userIncludePaths);
        this.preprocessor.setSystemIncludePath(systemIncludePaths);
        this.preprocessor.addFeature(Feature.DIGRAPHS);
        this.preprocessor.addFeature(Feature.TRIGRAPHS);

		/* Add macros. */
        addMacros(this.macros);
        for (Map.Entry<String, String> macroEntry : this.unparsedMacros.entrySet()) {
            final String name = macroEntry.getKey();
            final String value = macroEntry.getValue();
            addMacro(name, value);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void start() {
        sourceStack.push(startFile);
        if (listener != null) {
            listener.fileChanged(Optional.<String>absent(), startFile, true);
        }
    }

    @Override
    public Symbol nextToken() throws pl.edu.mimuw.nesc.exception.LexerException {
        try {
            return popSymbol();
        } catch (LexerException | IOException e) {
            e.printStackTrace();
            final String message = "cannot read next token";
            throw new pl.edu.mimuw.nesc.exception.LexerException(message, e);
        }
    }

    @Override
    public void addMacro(PreprocessorMacro macro) throws pl.edu.mimuw.nesc.exception.LexerException {
        checkNotNull(macro, "macro should not be null");
        try {
            preprocessor.addMacro(macro.getProcessedObject());
        } catch (LexerException e) {
            e.printStackTrace();
            final String message = "cannot add macro " + macro;
            throw new pl.edu.mimuw.nesc.exception.LexerException(message, e);
        }
    }

    @Override
    public void addMacros(Collection<PreprocessorMacro> macros) throws pl.edu.mimuw.nesc.exception.LexerException {
        checkNotNull(macros, "macros collection cannot be null");
        for (PreprocessorMacro macro : macros) {
            addMacro(macro);
        }
    }

    @Override
    public Map<String, PreprocessorMacro> getMacros() {
        final Map<String, Macro> preprocessorMacros = this.preprocessor.getMacros();
        final Map<String, PreprocessorMacro> result = new HashMap<>(preprocessorMacros.size());

        for (Map.Entry<String, Macro> macro : preprocessorMacros.entrySet()) {
            final String macroName = macro.getKey();
            final Macro object = macro.getValue();
            final Optional<String> path = getSourcePath(object.getSource());
            final PreprocessorMacro pm = new PreprocessorMacro(macroName, path, object);
            result.put(macroName, pm);
        }
        return result;
    }

    @Override
    public boolean isOnTopContext() {
        // TODO
        throw new RuntimeException("not implemented");
    }

    @Override
    public void cancel() throws IOException {
        close();
    }

    @Override
    public void close() throws IOException {
        this.preprocessor.close();
    }

    private void addMacro(String name, String value) throws pl.edu.mimuw.nesc.exception.LexerException {
        try {
            this.preprocessor.addMacro(name, value);
        } catch (LexerException e) {
            e.printStackTrace();
            final String message = "cannot add macro " + name + " " + value;
            throw new pl.edu.mimuw.nesc.exception.LexerException(message, e);
        }
    }

    private String getCurrentFile() {
        return this.sourceStack.peek();
    }

    private void pushSymbol(Symbol symbol) {
        this.symbolQueue.addFirst(symbol);
    }

    private Symbol popSymbol() throws IOException, LexerException {
        if (this.symbolQueue.isEmpty()) {
            return lex();
        } else {
            return this.symbolQueue.removeFirst();
        }
    }

    private Symbol lex() throws IOException, LexerException {
        final Symbol.Builder builder = Symbol.builder();

        Token token = popToken();

        /*
         * Handle token which should not produce parser token.
         */
        if (ignoreToken(token)) {
            return lex();
        } else if (isComment(token)) {
            /*
             * NOTICE: INVALID token which was intended to be a comment should
             * be handled in this section.
             */
            handleComment(token, isInvalidComment(token));
            return lex();
        } else if (isHash(token)) {
            // TODO handle hash
            return lex();
        }

        /*
         * Handle token which should produce parser token.
         */
        final Token originalMacroToken = token.getOriginalMacroToken();

        builder.file(getCurrentFile())
                .line(originalMacroToken == null ? token.getLine() : originalMacroToken.getLine())
                .column(1 + (originalMacroToken == null ? token.getColumn() : originalMacroToken.getColumn()))
                .isExpanded(token.isExpanded());
        /*
         * XXX: generally endColumn should looks like:
         *     token.getColumn() +   1 + (token.text().length() - 1)
         *     ^                     ^                  ^
         *     token start position  |                  |
         *               jcpp starts counting from 0    |
         *                both range ends are inclusive, subtract one from token length
         */

        switch (token.getType()) {
            case Token.IDENTIFIER:
                lexIdentifier(token, builder);
                break;
            case Token.NUMBER:
                lexNumber(token, builder, false);
                break;
            case Token.STRING:
                lexString(token, builder, false);
                break;
            case Token.CHARACTER:
                lexCharacter(token, builder, false);
                break;
            case Token.INVALID:
                lexInvalid(token, builder);
                break;
            case Token.EOF:
                builder.symbolCode(EOF)
                        .value("EOF");
                break;
            default:
                lexOtherToken(token, builder);
                break;
        }

        /* Correct the end position if the token has come from a macro expansion */
        if (originalMacroToken != null) {
            builder.endLine(originalMacroToken.getLine())
                    .endColumn(originalMacroToken.getColumn() + originalMacroToken.getText().length());
        }

        final Symbol symbol = builder.build();

        /*
         * Report error for invalid symbols.
         * This is the best place because end location is known.
         */
        if (symbol.isInvalid() && listener != null) {
            final String errorMsg = (String) token.getValue();
            final Location location = symbol.getLocation();
            final Location endLocation = symbol.getEndLocation();
            listener.error(location.getFilePath(), location.getLine(), location.getColumn(),
                    Optional.of(endLocation.getLine()), Optional.of(endLocation.getColumn()),
                    errorMsg);
        }
        return symbol;
    }

    private boolean ignoreToken(Token token) {
        final int type = token.getType();
        return ((type == Token.WHITESPACE) || (type == Token.NL) || (type == Token.LITERAL) || (type == Token.PASTE) ||
                (type == Token.P_LINE));
    }

    private boolean isComment(Token token) {
        final int type = token.getType();
        return (type == Token.CCOMMENT || type == Token.CPPCOMMENT || isInvalidComment(token));
    }

    private boolean isInvalidComment(Token token) {
        final int type = token.getType();
        return (type == Token.INVALID && token.getExpectedType() == Token.CCOMMENT);
    }

    private boolean isHash(Token token) {
        final int type = token.getType();
        return (type == Token.HASH || type == Token.HEADER);
    }

    private void lexIdentifier(Token token, Symbol.Builder builder) {
        final String text = token.getText();

        /*
         * Split '@name' token into two tokens AT(@) and IDENTIFIER (name).
         */
        if (text.charAt(0) != '@') {
            final int code = getSymbolCode(text);
            builder.symbolCode(code)
                    .value(text)
                    .endLine(token.getLine())
                    .endColumn(token.getColumn() + text.length());
        } else {
            builder.symbolCode(AT)
                    .value("@")
                    .endLine(token.getLine())
                    .endColumn(token.getColumn() + 1);

            final String identifier = text.substring(1);
            final Symbol idSymbol = Symbol.builder()
                    .value(identifier)
                    .file(getCurrentFile())
                    .line(token.getLine())
                    .column(token.getColumn() + 1)  // +1 for @
                    .endLine(token.getLine())
                    .endColumn(token.getColumn() + text.length())
                    .build();
            pushSymbol(idSymbol);
        }
    }

    private void lexNumber(Token token, Symbol.Builder builder, boolean invalid) {
        final String numeric = token.getText();
        /*
         * FIXME: how to distinguish between int and float?
         * Simple heuristic is to check if value contains non-numerical
         * characters.
         */
        builder.symbolCode(numeric.matches("[0-9]+") ? INTEGER_LITERAL : FLOATING_POINT_LITERAL)
                .value(numeric)
                .endLine(token.getLine())
                .endColumn(token.getColumn() + token.getText().length())
                .invalid(invalid);
    }

    /**
     * @param text Text from a token that has been stringized.
     * @return Length of the given text before being stringized because of the
     *         macro operator '#'.
     */
    private int computeStringizedAlignment(String text) {
        int result = text.length() - 2;  // subtract 2 because of the quotes addition

        int i = 1;
        while (i < text.length() - 1) {
            final char fst = text.charAt(i);
            final char snd = text.charAt(i + 1);

            if (fst == '\\' && (snd == '\\' || snd == '"' || snd == 'n' || snd == 'r')) {
                --result;
                i += 2;
            } else {
                ++i;
            }
        }

        return result;
    }

    private void lexString(Token token, Symbol.Builder builder, boolean invalid) throws IOException, LexerException {
        /*
         * Remove quotes from string. getValue() may contain error message
         * in case of invalid string token.
         */
        final String string = token.getText().replaceAll("^\"|\"$", "");
        builder.symbolCode(STRING_LITERAL)
                .value(string)
                .endLine(token.getLine())
                .endColumn(token.getColumn()
                        +  (    !token.isStringized()
                             ?  token.getText().length()
                             :  computeStringizedAlignment(token.getText())))
                .invalid(invalid);
        // FIXME: end location, string literal may be broken by backslash!
        // TODO: get next token location and subtract one from column (line the same)
        // currently preprocessor does not handle backslashes properly
    }

    private void lexCharacter(Token token, Symbol.Builder builder, boolean invalid) {
        /*
         * Remove quotes from string. getValue() may contain error message
         * in case of invalid string token.
         */
        final String character = token.getText().replaceAll("^'|'$", "");
        builder.symbolCode(CHARACTER_LITERAL)
                .value(character)
                .endLine(token.getLine())
                .endColumn(token.getColumn() + token.getText().length())
                .invalid(invalid);
    }

    private void lexInvalid(Token token, Symbol.Builder builder) throws IOException, LexerException {
        final int expectedType = token.getExpectedType();
        /* Change token type (we need to create a new object). */
        token = new Token(expectedType, token.getLine(), token.getColumn(), token.getText(), token.getValue());

        switch (expectedType) {
            case Token.STRING:
                lexString(token, builder, true);
                break;
            case Token.CHARACTER:
                lexCharacter(token, builder, true);
                break;
            case Token.NUMBER:
                /*
                 * FIXME: When invalid number token is detected, preprocessor
                 * "consumes the rest of the current line into an invalid".
                 */
                lexNumber(token, builder, true);
                break;
            case Token.HEADER:
                // TODO
                break;
            case Token.CCOMMENT:
                throw new IllegalStateException("invalid C comment should be handled earlier");
            default:
                throw new IllegalArgumentException("unexpected token type " + expectedType);
        }
    }

    private void handleComment(Token token, boolean invalid) {
        final String fileName = getCurrentFile();
        final int line = token.getLine();
        final int column = token.getColumn() + 1;

        final Comment comment = Comment.builder()
                .file(fileName)
                .line(line)
                .column(column)
                .body(token.getText())
                /* Only C comments could be invalid I suppose. */
                .isC(token.getType() == Token.CCOMMENT || token.getType() == Token.INVALID)
                .invalid(invalid)
                .build();

        /*
         * Report invalid comment.
         */
        if (listener != null) {
            listener.comment(comment);
            if (invalid) {
                final String msg = (String) token.getValue();
                listener.error(fileName, line, column, Optional.<Integer>absent(), Optional.<Integer>absent(), msg);
            }
        }
    }

    private void lexOtherToken(Token token, Symbol.Builder builder) throws IOException, LexerException {
        final int code = getSymbolCode(token.getType());
        if (code == SymbolFactory.UNKNOWN_TOKEN) {
            throw new IllegalArgumentException("Unknown token " + code);
        }
        /*
         * Try to find: "<-" pattern.
         */
        if (code == LT) {
            final Token next = popToken();
            final int nextCode = getSymbolCode(next.getType());

            if (nextCode == MINUS) {
                builder.symbolCode(LEFT_ARROW)
                        .value("<-")
                        .endLine(token.getLine())
                        .endColumn(token.getColumn() + 2);
            } else {
                builder.symbolCode(LT)
                        .value("<")
                        .endLine(token.getLine())
                        .endColumn(token.getColumn() + 1);
                pushToken(next);
            }
        } else {
            builder.symbolCode(code)
                    .value(token.getText())
                    .endLine(token.getLine())
                    .endColumn(token.getColumn() + token.getText().length());
        }
    }

    private void pushToken(Token token) {
        this.tokenQueue.addFirst(token);
    }

    private Token popToken() throws IOException, LexerException {
        if (this.tokenQueue.isEmpty()) {
            return this.preprocessor.token();
        } else {
            return this.tokenQueue.removeFirst();
        }
    }

    /**
     * NesC lexer builder.
     *
     * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
     */
    public static final class Builder {

        private String mainFilePath;
        private List<String> userIncludePaths;
        private List<String> systemIncludePaths;
        private List<String> includeFilePaths;
        private Collection<PreprocessorMacro> macros;
        private Map<String, String> unparsedMacros;

        public Builder() {
        }

        public Builder mainFile(String mainFile) {
            this.mainFilePath = mainFile;
            return this;
        }

        public Builder userIncludePaths(List<String> userIncludePaths) {
            this.userIncludePaths = userIncludePaths;
            return this;
        }

        public Builder systemIncludePaths(List<String> systemIncludePaths) {
            this.systemIncludePaths = systemIncludePaths;
            return this;
        }

        public Builder includeFilePaths(List<String> includeFilePaths) {
            this.includeFilePaths = includeFilePaths;
            return this;
        }

        public Builder macros(Collection<PreprocessorMacro> macros) {
            this.macros = macros;
            return this;
        }

        public Builder unparsedMacros(Map<String, String> unparsedMacros) {
            this.unparsedMacros = unparsedMacros;
            return this;
        }

        public NescLexer build() throws IOException, pl.edu.mimuw.nesc.exception.LexerException {
            validate();
            return new NescLexer(this);
        }

        protected void validate() {
            checkNotNull(mainFilePath, "main file path cannot be null");
            if (userIncludePaths == null) {
                userIncludePaths = new LinkedList<>();
            }
            if (systemIncludePaths == null) {
                systemIncludePaths = new LinkedList<>();
            }
            if (includeFilePaths == null) {
                includeFilePaths = new LinkedList<>();
            }
            if (macros == null) {
                macros = new LinkedList<>();
            }
            if (unparsedMacros == null) {
                unparsedMacros = new HashMap<>();
            }
        }

    }

    /**
     * Preprocessor listener.
     *
     * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
     */
    private class NescPreprocessorListener implements PreprocessorListener {

        private static final String PUSH = "push";
        private static final String POP = "pop";

        /**
         * Helper for building directives.
         */
        private final PreprocessorDirectiveHelper preprocessorDirectiveHelper;
        private LastIncludeDirective lastIncludeDirective;

        private NescPreprocessorListener() {
            this.preprocessorDirectiveHelper = new PreprocessorDirectiveHelper();
        }

        @Override
        public void handleWarning(Source source, int line, int column, String msg) throws LexerException {
            LOG.info("Preprocessor warning in " + source + " at " + line + ", " + column + "; " + msg);
            if (listener != null) {
                final Optional<String> fileName = getSourcePath(source);
                listener.warning(fileName.isPresent() ? fileName.get() : getCurrentFile(), line, column + 1,
                        Optional.<Integer>absent(), Optional.<Integer>absent(), msg);
            }
        }

        @Override
        public void handleError(Source source, int line, int column, String msg) throws LexerException {
            LOG.info("Preprocessor error in " + source + " at " + line + ", " + column + "; " + msg);

            /*
             * Catch "file not found error" when file from include directive
             * cannot be found.
             */
            if (msg.startsWith("File not found: ")) {
                buildIncludeDirective(Optional.<String>absent());
            }

            if (listener != null) {
                final Optional<String> fileName = getSourcePath(source);
                listener.error(fileName.isPresent() ? fileName.get() : getCurrentFile(), line, column + 1,
                        Optional.<Integer>absent(), Optional.<Integer>absent(), msg);
            }
        }

        @Override
        public void handleSourceChange(Source source, String event) {
            // FIXME: check exactly lexer flow
            final Optional<String> path = getSourcePath(source);
            if (!path.isPresent()) {
                return;
            }

            if (PUSH.equals(event)) {
                final String previous = sourceStack.peek();
                sourceStack.push(normalizePath(path.get()));
                if (!previous.equals(path.get())) {
                    notifyFileChange(previous, path.get(), true);
                }
            } else if (POP.equals(event)) {
                final String previousFile = sourceStack.pop();
                final String current = sourceStack.peek();
                if (!previousFile.equals(current)) {
                    notifyFileChange(previousFile, current, false);
                }
            }
        }

        @Override
        public boolean beforeInclude(String filePath, int line) {
            filePath = normalizePath(filePath);
            if (listener != null) {
                buildIncludeDirective(Optional.of(filePath));
                return listener.beforeInclude(filePath, line);
            }
            return false;
        }

        @Override
        public void handlePreprocesorDirective(Source source, org.anarres.cpp.PreprocessorDirective directive) {
            final String sourceFile = getSourcePath(source).get();

            /*
             * Postpone reporting that include directive was recognized.
             *
             * First we must get the absolute path to the included file.
             * There are two possibilities:
             *  (1) file was found and then beforeInclude() will be called,
             *  (2) file was not found and handleError() will be called.
             * Both callbacks are called after this one.
             */
            if (directive.getCommand() == PreprocessorCommand.PP_INCLUDE) {
                this.lastIncludeDirective = new LastIncludeDirective(directive, source);
            } else {
                final pl.edu.mimuw.nesc.preprocessor.directive.PreprocessorDirective frontendDirective =
                        this.preprocessorDirectiveHelper.buildPreprocessorDirective(directive, sourceFile);

                if (listener != null) {
                    listener.preprocessorDirective(frontendDirective);
                }
            }
        }

        @Override
        public void handleMacroExpansion(Source source, int line, int column, String macro,
                    String definitionFileName, int definitionLine, int definitionColumn) {
            LOG.trace("MACRO expansion " + line + ", " + column + "; " + macro);

            if (listener != null) {
                final String sourcePath = getSourcePath(source).orNull();
                final String startLocFileName = sourcePath != null ? sourcePath : "";
                final Location startLoc = new Location(startLocFileName, line, column + 1),
                               endLoc = new Location(startLocFileName, line, column + macro.length());
                final Optional<Location> definitionLoc =
                            definitionFileName != null && definitionLine > 0 && definitionColumn >= 0
                        ?   Optional.of(new Location(definitionFileName, definitionLine, definitionColumn + 1))
                        :   Optional.<Location>absent();  // explicit type parameter to suppress a compilation error

                listener.macroInstantiation(new MacroToken(startLoc, endLoc, macro, definitionLoc));
            }
        }

        private void notifyFileChange(String from, String to, boolean push) {
            if (listener != null) {
                listener.fileChanged(Optional.fromNullable(from), to, push);
            }
        }

        private void buildIncludeDirective(Optional<String> filePath) {
            final IncludeDirective.Builder frontendDirective =
                    (IncludeDirective.Builder) preprocessorDirectiveHelper.getPreprocessorDirectiveBuilder(
                            lastIncludeDirective.getPreprocessorDirective(),
                            lastIncludeDirective.getSource().getPath());

            lastIncludeDirective = null;

            /* Setting included file path. */
            frontendDirective.filePath(filePath.orNull());

            if (listener != null) {
                listener.preprocessorDirective(frontendDirective.build());
            }
        }

        private final class LastIncludeDirective {

            private final org.anarres.cpp.PreprocessorDirective preprocessorDirective;
            private final Source source;

            private LastIncludeDirective(PreprocessorDirective preprocessorDirective, Source source) {
                this.preprocessorDirective = preprocessorDirective;
                this.source = source;
            }

            public PreprocessorDirective getPreprocessorDirective() {
                return preprocessorDirective;
            }

            public Source getSource() {
                return source;
            }
        }
    }

    private static Optional<String> getSourcePath(Source source) {
        final String path = (source == null) ? null : source.getPath();
        /* Skip paths like <internal-data>. */
        if (path == null || path.charAt(0) == '<') {
            return Optional.absent();
        }
        return Optional.of(normalizePath(path));
    }

}
