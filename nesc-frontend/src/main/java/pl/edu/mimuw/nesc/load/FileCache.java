package pl.edu.mimuw.nesc.load;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.Node;
import pl.edu.mimuw.nesc.common.FileType;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.lexer.Comment;
import pl.edu.mimuw.nesc.preprocessor.PreprocessorMacro;
import pl.edu.mimuw.nesc.preprocessor.directive.PreprocessorDirective;
import pl.edu.mimuw.nesc.problem.NescIssue;
import pl.edu.mimuw.nesc.token.Token;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class FileCache {

    public static Builder builder() {
        return new Builder();
    }

    private final String filePath;
    private final FileType fileType;
    /**
     * Indicates whether the object represents structures for file that
     * is a separate unit or was parsed as a part of another unit
     * (e.g. a file included in another included file).
     */
    private final boolean isRoot;
    private final Optional<Node> entityRoot;
    private final List<Declaration> extdefs;
    private final List<Comment> comments;
    private final List<PreprocessorDirective> preprocessorDirectives;
    private final Map<String, PreprocessorMacro> macros;
    private final Map<String, String> globalNames;
    /**
     * Macros that are visible for subsequently parsed files.
     */
    private final Map<String, PreprocessorMacro> endFileMacros;
    private final Multimap<Integer, Token> tokens;
    private final Multimap<Integer, NescIssue> issues;
    private final Environment environment;

    private FileCache(Builder builder) {
        this.filePath = builder.filePath;
        this.fileType = builder.fileType;
        this.isRoot = builder.isRoot;
        this.entityRoot = Optional.fromNullable(builder.entityRoot);
        this.extdefs = builder.extdefs.build();
        this.comments = builder.comments.build();
        this.preprocessorDirectives = builder.preprocessorDirectives.build();
        this.macros = builder.macros.build();
        this.globalNames = builder.globalNames.build();
        this.endFileMacros = builder.endFileMacros.build();
        this.tokens = builder.tokens.build();
        this.issues = builder.issues.build();
        this.environment = builder.environment;
    }

    public String getFilePath() {
        return filePath;
    }

    public FileType getFileType() {
        return fileType;
    }

    public boolean isRoot() {
        return isRoot;
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

    public Map<String, String> getGlobalNames() {
        return globalNames;
    }

    public Map<String, PreprocessorMacro> getEndFileMacros() {
        return endFileMacros;
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
                .toString();
    }

    /**
     * Builder.
     */
    public static class Builder {

        private String filePath;
        private FileType fileType;
        private boolean isRoot;
        private Node entityRoot;
        private ImmutableList.Builder<Declaration> extdefs;
        private ImmutableList.Builder<Comment> comments;
        private ImmutableList.Builder<PreprocessorDirective> preprocessorDirectives;
        private ImmutableMap.Builder<String, PreprocessorMacro> macros;
        private ImmutableMap.Builder<String, String> globalNames;
        private ImmutableMap.Builder<String, PreprocessorMacro> endFileMacros;
        private ImmutableMultimap.Builder<Integer, Token> tokens;
        private ImmutableMultimap.Builder<Integer, NescIssue> issues;
        private Environment environment;

        public Builder() {
            this.extdefs = ImmutableList.builder();
            this.comments = ImmutableList.builder();
            this.preprocessorDirectives = ImmutableList.builder();
            this.macros = ImmutableMap.builder();
            this.globalNames = ImmutableMap.builder();
            this.endFileMacros = ImmutableMap.builder();
            this.tokens = ImmutableMultimap.builder();
            this.issues = ImmutableMultimap.builder();
            this.isRoot = true;
        }

        public Builder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public Builder fileType(FileType fileType) {
            this.fileType = fileType;
            return this;
        }

        public Builder isRoot(boolean isRoot) {
            this.isRoot = isRoot;
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

        public Builder endFileMacros(Map<String, PreprocessorMacro> macros) {
            checkNotNull(macros, "macros cannot be null");
            this.endFileMacros.putAll(macros);
            return this;
        }

        public Builder globalNames(Map<String, String> globalNames) {
            checkNotNull(globalNames, "global names cannot be null");
            this.globalNames.putAll(globalNames);
            return this;
        }

        public Builder tokens(Multimap<Integer, Token> tokens) {
            this.tokens.putAll(tokens);
            return this;
        }

        public Builder issues(Multimap<Integer, NescIssue> issues) {
            this.issues.putAll(issues);
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
            checkNotNull(filePath, "file path cannot be null");
            checkNotNull(fileType, "file type cannot be null");
            checkNotNull(environment, "environment cannot be null");
        }
    }
}
