package pl.edu.mimuw.nesc;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.Node;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.problem.NescIssue;
import pl.edu.mimuw.nesc.lexer.Comment;
import pl.edu.mimuw.nesc.preprocessor.directive.PreprocessorDirective;
import pl.edu.mimuw.nesc.token.Token;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Contains the results of parsing of single nesC source file.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class FileData {

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates {@link FileData} instance from {@link FileCache} object.
     *
     * @param fileCache {@link FileCache} object
     * @return {@link FileData} instance
     */
    public static FileData convertFrom(FileCache fileCache) {
        checkNotNull(fileCache, "file cache cannot be null");

        return builder()
                .fileCache(fileCache)
                .build();
    }

    private final String filePath;
    private final Optional<Node> entityRoot;
    private final List<Declaration> extdefs;
    private final List<Comment> comments;
    private final List<PreprocessorDirective> preprocessorDirectives;
    private final Multimap<Integer, Token> tokens;
    private final Multimap<Integer, NescIssue> issues;
    private final Environment environment;

    // TODO: extends attributes list (e.g. macros...).

    private FileData(Builder builder) {
        this.filePath = builder.filePath;
        this.entityRoot = builder.entityRoot;
        this.extdefs = builder.extdefsBuilder.build();
        this.comments = builder.commentsBuilder.build();
        this.preprocessorDirectives = builder.directivesBuilder.build();
        this.tokens = builder.tokens;
        this.issues = builder.issues;
        this.environment = builder.environment;
    }

    public String getFilePath() {
        return filePath;
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
                .add("entityRoot", entityRoot)
                .add("extdefs", extdefs)
                .add("comments", comments)
                .add("preprocessorDirectives", preprocessorDirectives)
                .add("tokens", tokens)
                .add("issues", issues)
                // environment
                .toString();
    }

    /**
     * FileData builder.
     *
     * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
     */
    public static final class Builder {

        private String filePath;
        private Optional<Node> entityRoot;
        private final ImmutableList.Builder<Declaration> extdefsBuilder;
        private final ImmutableList.Builder<Comment> commentsBuilder;
        private final ImmutableList.Builder<PreprocessorDirective> directivesBuilder;
        private Multimap<Integer, Token> tokens;
        private Multimap<Integer, NescIssue> issues;
        private Environment environment;

        public Builder() {
            this.extdefsBuilder = new ImmutableList.Builder<>();
            this.commentsBuilder = new ImmutableList.Builder<>();
            this.directivesBuilder = new ImmutableList.Builder<>();
        }

        public Builder fileCache(FileCache cache) {
            this.filePath = cache.getFilePath();
            this.entityRoot = cache.getEntityRoot();
            this.extdefsBuilder.addAll(cache.getExtdefs());
            this.commentsBuilder.addAll(cache.getComments());
            this.directivesBuilder.addAll(cache.getPreprocessorDirectives());
            this.tokens = cache.getTokens();
            this.issues = cache.getIssues();
            this.environment = cache.getEnvironment();
            return this;
        }

        public Builder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public Builder entityRoot(Node entityRoot) {
            this.entityRoot = Optional.fromNullable(entityRoot);
            return this;
        }

        public Builder extdef(Declaration extdef) {
            this.extdefsBuilder.add(extdef);
            return this;
        }

        public Builder extdefs(List<Declaration> extdefs) {
            this.extdefsBuilder.addAll(extdefs);
            return this;
        }

        public Builder comment(Comment comment) {
            this.commentsBuilder.add(comment);
            return this;
        }

        public Builder comments(List<Comment> comments) {
            this.commentsBuilder.addAll(comments);
            return this;
        }

        public Builder preprocessorDirective(PreprocessorDirective directive) {
            this.directivesBuilder.add(directive);
            return this;
        }

        public Builder preprocessorDirectives(List<PreprocessorDirective> directives) {
            this.directivesBuilder.addAll(directives);
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

        public FileData build() {
            if (entityRoot == null) {
                entityRoot = Optional.absent();
            }
            validate();
            return new FileData(this);
        }

        private void validate() {
            checkState(filePath != null, "file path cannot be null");
            checkState(tokens != null, "tokens multimap cannot be null");
            checkState(issues != null, "issues multimap cannot be null");
            checkState(environment != null, "environment cannot be null");
        }
    }

}
