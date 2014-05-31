package pl.edu.mimuw.nesc;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.Node;
import pl.edu.mimuw.nesc.common.FileType;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.problem.NescIssue;
import pl.edu.mimuw.nesc.lexer.Comment;
import pl.edu.mimuw.nesc.parser.Parser;
import pl.edu.mimuw.nesc.preprocessor.PreprocessorMacro;
import pl.edu.mimuw.nesc.preprocessor.directive.PreprocessorDirective;
import pl.edu.mimuw.nesc.token.Token;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class FileCache {

    public static Builder builder() {
        return new Builder();
    }

    private final String filePath;
    private final FileType fileType;
    private final Optional<Node> entityRoot;
    private final List<Declaration> extdefs;
    private final List<Comment> comments;
    private final List<PreprocessorDirective> preprocessorDirectives;
    private final Map<String, PreprocessorMacro> macros;
    private final Multimap<Integer, Token> tokens;
    private final Multimap<Integer, NescIssue> issues;
    private final Environment environment;

    private FileCache(Builder builder) {
        this.filePath = builder.filePath;
        this.fileType = builder.fileType;
        this.entityRoot = Optional.fromNullable(builder.entityRoot);
        this.extdefs = builder.extdefs.build();
        this.comments = builder.comments.build();
        this.preprocessorDirectives = builder.preprocessorDirectives.build();
        this.macros = builder.macros.build();
        this.tokens = builder.tokens;
        this.issues = builder.issues;
        this.environment = builder.environment;
    }

    public String getFilePath() {
        return filePath;
    }

    public FileType getFileType() {
        return fileType;
    }

    public Optional<Node> getEntityRoot() {
        return entityRoot;
    }

    public List<Declaration> getExtdefs() {
        return extdefs;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public List<PreprocessorDirective> getPreprocessorDirectives() {
        return preprocessorDirectives;
    }

    public Map<String, PreprocessorMacro> getMacros() {
        return macros;
    }

    public Multimap<Integer, Token> getTokens() {
        return tokens;
    }

    public Multimap<Integer, NescIssue> getIssues() {
        return issues;
    }

    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("filePath", filePath)
                .add("fileType", fileType)
                .add("entityRoot", entityRoot)
                .add("extdefs", extdefs)
                .add("comments", comments)
                .add("preprocessorDirectives", preprocessorDirectives)
                .add("macros", macros)
                .add("tokens", tokens)
                .add("issues", issues)
                // environment
                .toString();
    }

    /**
     * Builder.
     */
    public static class Builder {

        private static final List<Integer> ID_TOKEN_TYPES = ImmutableList.<Integer>builder()
                .add(Parser.Lexer.IDENTIFIER)
                .add(Parser.Lexer.TYPEDEF_NAME)
                .add(Parser.Lexer.COMPONENTREF)
                .build();


        private String filePath;
        private FileType fileType;
        private Node entityRoot;
        private ImmutableList.Builder<Declaration> extdefs;
        private ImmutableList.Builder<Comment> comments;
        private ImmutableList.Builder<PreprocessorDirective> preprocessorDirectives;
        private ImmutableMap.Builder<String, PreprocessorMacro> macros;
        private Multimap<Integer, Token> tokens;
        private Multimap<Integer, NescIssue> issues;
        private Environment environment;

        public Builder() {
            this.extdefs = ImmutableList.builder();
            this.comments = ImmutableList.builder();
            this.preprocessorDirectives = ImmutableList.builder();
            this.macros = ImmutableMap.builder();
        }

        public Builder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public Builder fileType(FileType fileType) {
            this.fileType = fileType;
            return this;
        }

        public Builder entityRoot(Node node) {
            this.entityRoot = node;
            return this;
        }

        public Builder extdef(Declaration declaration) {
            checkNotNull(declaration, "declaration cannot be null");
            this.extdefs.add(declaration);
            return this;
        }

        public Builder comment(Comment comment) {
            checkNotNull(comment, "comment cannot be null");
            this.comments.add(comment);
            return this;
        }

        public Builder directive(PreprocessorDirective directive) {
            checkNotNull(directive, "directive cannot be null");
            this.preprocessorDirectives.add(directive);
            return this;
        }

        public Builder macro(String name, PreprocessorMacro macro) {
            checkNotNull(name, "name cannot be null");
            checkNotNull(macro, "macro cannot be null");
            this.macros.put(name, macro);
            return this;
        }

        /**
         * <p>Sets tokens multimap.</p>
         * <p>NOTICE: to improve performance the reference to multimap is set,
         * defensive copy is not created.</p>
         * TODO: test performance impact
         *
         * @param tokens tokens multimap
         * @return builder
         */
        public Builder tokens(Multimap<Integer, Token> tokens) {
            this.tokens = tokens;
            return this;
        }

        public Builder issues(Multimap<Integer, NescIssue> issues) {
            this.issues = issues;
            return this;
        }

        public Builder environment(Environment environment) {
            this.environment = environment;
            return this;
        }

        public FileCache build() {
            verify();
            return new FileCache(this);
        }

        private void verify() {
            checkState(filePath != null, "file path cannot be null");
            checkState(fileType!= null, "file type cannot be null");
            checkState(tokens != null, "tokens multimap cannot be null");
            checkState(issues != null, "issues multimap cannot be null");
            checkState(environment != null, "environment cannot be null");
        }

    }

}
