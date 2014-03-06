package pl.edu.mimuw.nesc;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.Node;
import pl.edu.mimuw.nesc.exception.LexerException;
import pl.edu.mimuw.nesc.lexer.Comment;
import pl.edu.mimuw.nesc.lexer.LexerListener;
import pl.edu.mimuw.nesc.lexer.NescLexer;
import pl.edu.mimuw.nesc.parser.Parser;
import pl.edu.mimuw.nesc.parser.SymbolTable;
import pl.edu.mimuw.nesc.preprocessor.directive.PreprocessorDirective;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static pl.edu.mimuw.nesc.common.util.file.FileUtils.*;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class ParseExecutor {

    // TODO: handle errors
    private final ParseContext context;

    /**
     * Creates parser executor.
     *
     * @param parseContext context
     */
    public ParseExecutor(ParseContext parseContext) {
        checkNotNull(parseContext, "parse context cannot be null");

        this.context = parseContext;
    }

    /**
     * Starts parsing.
     */
    public void start() {
        final String filePath = context.getOrderResolver().getStartFile();
        final List<String> filesToInclude = context.getPathsResolver().getIncludeFilePaths();
        try {
            new ParseFileExecutor().parseFile(filePath, filesToInclude);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Parses entity.
     *
     * @param currentEntityName current entity name
     * @param entityName        entity name
     * @return <code>true</code> when entity was parsed successfully
     */
    public boolean parseEntity(String currentEntityName, String entityName) {
        checkNotNull(currentEntityName, "current entity name cannot be null");
        checkNotNull(entityName, "entity name cannot be null");

        if (context.getOrderResolver().wasParsed(entityName)) {
            return true;
        }
        final String filePath = context.getOrderResolver().findEntity(entityName);
        try {
            new ParseFileExecutor().parseFile(filePath, new LinkedList<String>());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Executor that parses single source file and all header files included
     * into this file.
     *
     * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
     */
    private final class ParseFileExecutor implements LexerListener {

        private final Map<String, FileResult.Builder> fileResultBuilders;

        public ParseFileExecutor() {
            this.fileResultBuilders = new HashMap<>();
        }

        /**
         * Parses file.
         *
         * @param filePath       file path
         * @param filesToInclude list of path of header files that should be
         *                       included parsed before the main file
         * @throws IOException
         * @throws LexerException
         */
        public void parseFile(final String filePath, final List<String> filesToInclude)
                throws IOException, LexerException {
            System.out.println("Start parsing " + filePath + " ...");

            /* Build symbol table with given global scope. */
            final SymbolTable symbolTable = new SymbolTable(context.getGlobalScope());
            /* Prepare file result builder. */
            final FileResult.Builder builder = FileResult.builder();
            this.fileResultBuilders.put(filePath, builder);

		    /* Setup lexer */
            final NescLexer lexer = NescLexer.builder().mainFile(filePath)
                    .systemIncludePaths(context.getPathsResolver().getSearchOrder())
                    .userIncludePaths(context.getPathsResolver().getSearchOrder())
                    .includeFilePaths(filesToInclude)
                    .macros(context.getMacrosManager().getMacros().values())
                    .unparsedMacros(context.getMacrosManager().removeUnparsedMacros())
                    .build();

            lexer.addListener(this);
            lexer.start();

            /* Setup parser */
            final Parser parser = new Parser(filePath, lexer, symbolTable, new ParseExecutor(context),
                    context.getMacrosManager());

		    /* Parsing */
            final boolean parseSuccess = parser.parse();
            final boolean errors = parser.errors();

		    /* Cleanup parser */
            // nothing to do

		    /* Cleanup lexer. */
            lexer.removeListener(this);
            lexer.close();

		    /* Errors occurred. */
            if (!parseSuccess || errors) {
                // TODO
            }
            /* Parsed successfully. */
            else {
                // TODO
            }

            final Node entity = parser.getEntityRoot();
            final List<Declaration> extdefs = parser.getExtdefs();

            builder.fileName(filePath)
                    .entityRoot(entity);

            /*
             * NOTE: The extdefs list of the currently parsed file containing
             * entity definition may contain declarations that does not come
             * from the main file. The actual source of declaration may be
             * header file which was included into the current file. Therefore
             * the declarations should be associated with the files of their
             * actual origin (and put into the proper structures).
             */
            for (Declaration extdef : extdefs) {
                /*
                 * FIXME: temporarily all declarations are associated with
                 * current file to be able to determine the actual source of
                 * extdef, the location filed must be set.
                 */
                builder.extdef(extdef);
            }

            final FileResult fileResult = builder.build();
            context.getParseResult().addFile(fileResult);

            System.out.println(filePath + " parsed.");
        }

        @Override
        public void fileChanged(Optional<String> from, String to, boolean push) {
            // FIXME: remove
            //System.out.println("CHANGE: " + from + " -> " + to);
            if (push) {
                // TODO: update components/files graph
                /*
                 * Check if a new C or header file will be processed now. If
                 * the file is seen for the first time, create proper
                 * structure for the file.
                 */
                if ((isHeaderFile(to) || isCFile(to)) && !context.getParseResult().getFilesMap().containsKey(to)) {
                    final FileResult.Builder builder = FileResult.builder()
                            .fileName(to)
                            .entityRoot(null);
                    this.fileResultBuilders.put(to, builder);
                }
            }
        }

        @Override
        public void preprocessorDirective(PreprocessorDirective directive) {
            final FileResult.Builder builder = this.fileResultBuilders.get(directive.getSourceFile());
            builder.preprocessorDirective(directive);
        }

        @Override
        public void comment(Comment comment) {
            final String source = comment.getLocation().getFilename();
            final FileResult.Builder builder = this.fileResultBuilders.get(source);
            builder.comment(comment);
        }
    }
}
