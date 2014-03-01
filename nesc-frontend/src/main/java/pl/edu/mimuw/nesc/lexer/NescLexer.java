package pl.edu.mimuw.nesc.lexer;

import com.google.common.base.Optional;
import org.anarres.cpp.*;
import pl.edu.mimuw.nesc.parser.Symbol;
import pl.edu.mimuw.nesc.preprocessor.PreprocessorMacro;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static pl.edu.mimuw.nesc.lexer.SymbolFactory.getSymbolCode;
import static pl.edu.mimuw.nesc.parser.Parser.Lexer.*;

/**
 * NesC language lexer.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class NescLexer extends AbstractLexer {

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
     * Helper for building directives.
     */
    private final PreprocessorDirectiveHelper preprocessorDirectiveHelper;
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

        this.preprocessorDirectiveHelper = new PreprocessorDirectiveHelper();

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
            Symbol s = popSymbol();
            System.out.println(s);
            return s;
            //return popSymbol();
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
            final Optional<String> path = Optional.fromNullable(getSourcePath(object.getSource()));
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

        final Token token = popToken();

        /*
         * Handle token which should not produce parser token.
         */
        if (ignoreToken(token)) {
            return lex();
        } else if (isComment(token)) {
            handleComment(token);
            return lex();
        } else if (isHash(token)) {
            // TODO handle hash
            return lex();
        }

        /*
         * Handle token which should produce parser token.
         */

        builder.file(getCurrentFile())
                .line(token.getLine())
                .column(token.getColumn() + 1);
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
                lexNumber(token, builder);
                break;
            case Token.STRING:
                lexString(token, builder);
                break;
            case Token.CHARACTER:
                lexCharacter(token, builder);
                break;
            case Token.INVALID:
                // TODO
                break;
            case Token.EOF:
                builder.symbolCode(EOF);
                break;
            default:
                lexOtherToken(token, builder);
                break;
        }

        return builder.build();
    }

    private boolean ignoreToken(Token token) {
        final int type = token.getType();
        return ((type == Token.WHITESPACE) || (type == Token.NL) || (type == Token.LITERAL) || (type == Token.PASTE) ||
                (type == Token.P_LINE));
    }

    private boolean isComment(Token token) {
        final int type = token.getType();
        return (type == Token.CCOMMENT || type == Token.CPPCOMMENT);
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

    private void lexNumber(Token token, Symbol.Builder builder) {
        final NumericValue numeric = (NumericValue) token.getValue();

        // FIXME distinguish between int and double or unify those two literals
        builder.symbolCode(INTEGER_LITERAL)
                .value(numeric.toString())
                .endLine(token.getLine())
                .endColumn(token.getColumn() + token.getText().length());
    }

    private void lexString(Token token, Symbol.Builder builder) throws IOException, LexerException {
        final String string = (String) token.getValue();

        builder.symbolCode(STRING_LITERAL)
                .value(string)
                .endLine(token.getLine())
                .endColumn(token.getColumn() + token.getText().length());
        // FIXME: end location, string literal may be broken by backslash!
        // TODO: get next token location and subtract one from column (line the same)
        // currently preprocessor does not handle backslashes properly
    }

    private void lexCharacter(Token token, Symbol.Builder builder) {
        final String character = (String) token.getValue();
        builder.symbolCode(CHARACTER_LITERAL)
                .value(character)
                .endLine(token.getLine())
                .endColumn(token.getColumn() + token.getText().length());
    }

    private void handleComment(Token token) {
        final Comment comment = Comment.builder()
                .file(getCurrentFile())
                .line(token.getLine())
                .column(token.getColumn())
                .body(token.getText())
                .isC(token.getType() == Token.CCOMMENT)
                .build();

        if (listener != null) {
            listener.comment(comment);
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
                        .endLine(token.getLine())
                        .endColumn(token.getColumn() + 2);
            } else {
                builder.symbolCode(LT)
                        .endLine(token.getLine())
                        .endColumn(token.getColumn() + 1);
                pushToken(next);
            }
        } else {
            builder.symbolCode(code)
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

        @Override
        public void handleWarning(Source source, int line, int column, String msg) throws LexerException {
            // FIXME: handle warning in upper level
            System.out.println("Preprocessor warning in " + source + " at " + line + ", " + column + "; " + msg);
        }

        @Override
        public void handleError(Source source, int line, int column, String msg) throws LexerException {
            // FIXME: handle error in upper level
            System.out.println("Preprocessor error in " + source + " at " + line + ", " + column + "; " + msg);
        }

        @Override
        public void handleSourceChange(Source source, String event) {
            // FIXME: check exactly lexer flow
            final String path = getSourcePath(source);
            if (path == null) {
                return;
            }

            if (PUSH.equals(event)) {
                final String previous = sourceStack.peek();
                sourceStack.push(path);
                if (!previous.equals(path)) {
                    notifyFileChange(previous, path, true);
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
        public boolean beforeInclude(String filePath) {
            if (listener != null) {
                return listener.beforeInclude(filePath);
            }
            return false;
        }

        @Override
        public void handlePreprocesorDirective(Source source, org.anarres.cpp.PreprocessorDirective directive) {
            final String sourceFile = source.getPath();
            assert (sourceFile != null);

            final pl.edu.mimuw.nesc.preprocessor.directive.PreprocessorDirective frontendDirective =
                    NescLexer.this.preprocessorDirectiveHelper.buildPreprocessorDirective(directive, sourceFile);

            if (listener != null) {
                listener.preprocessorDirective(frontendDirective);
            }
        }

        @Override
        public void handleMacroExpansion(Source source, int line, int column, String macro) {
            System.out.println("MACRO expansion " + line + ", " + column + "; " + macro);
        }

        private void notifyFileChange(String from, String to, boolean push) {
            if (listener != null) {
                listener.fileChanged(Optional.fromNullable(from), to, push);
            }
        }
    }

    private static String getSourcePath(Source source) {
        final String path = (source == null) ? null : source.getPath();
        /* Skip paths like <internal-data>. */
        if (path == null || path.charAt(0) == '<') {
            return null;
        }
        return path;
    }

}
