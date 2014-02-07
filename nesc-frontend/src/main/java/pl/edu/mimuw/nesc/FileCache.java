package pl.edu.mimuw.nesc;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.Node;
import pl.edu.mimuw.nesc.common.FileType;
import pl.edu.mimuw.nesc.lexer.Comment;
import pl.edu.mimuw.nesc.parser.Parser;
import pl.edu.mimuw.nesc.parser.SymbolTable;
import pl.edu.mimuw.nesc.preprocessor.PreprocessorMacro;
import pl.edu.mimuw.nesc.preprocessor.directive.PreprocessorDirective;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
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
    private final SymbolTable.Scope globalScope;
    private final Optional<Node> entityRoot;
    private final List<Declaration> extdefs;
    private final List<Comment> comments;
    private final List<PreprocessorDirective> preprocessorDirectives;
    private final Map<String, PreprocessorMacro> macros;

    private FileCache(Builder builder) {
        this.filePath = builder.filePath;
        this.fileType = builder.fileType;
        this.globalScope = builder.globalScope;
        this.entityRoot = Optional.fromNullable(builder.entityRoot);
        this.extdefs = builder.extdefs.build();
        this.comments = builder.comments.build();
        this.preprocessorDirectives = builder.preprocessorDirectives.build();
        this.macros = builder.macros.build();
    }

    public String getFilePath() {
        return filePath;
    }

    public FileType getFileType() {
        return fileType;
    }

    public SymbolTable.Scope getGlobalScope() {
        return globalScope;
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

    @Override
    public String toString() {
        return "FileCache{" +
                "filePath='" + filePath + '\'' +
                ", fileType=" + fileType +
                ", globalScope=" + globalScope +
                ", entityRoot=" + entityRoot +
                ", extdefs=" + extdefs +
                ", comments=" + comments +
                ", preprocessorDirectives=" + preprocessorDirectives +
                ", macros=" + macros +
                '}';
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
        private SymbolTable.Scope globalScope;
        private Node entityRoot;
        private ImmutableList.Builder<Declaration> extdefs;
        private ImmutableList.Builder<Comment> comments;
        private ImmutableList.Builder<PreprocessorDirective> preprocessorDirectives;
        private ImmutableMap.Builder<String, PreprocessorMacro> macros;

        public Builder() {
            this.globalScope = SymbolTable.Scope.ofGlobalScope();
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

        public Builder globalId(String name, int type) {
            checkArgument(ID_TOKEN_TYPES.contains(type),
                    "unknown type " + type + "; expected one of " + ID_TOKEN_TYPES);
            this.globalScope.add(name, type);
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

        public FileCache build() {
            verify();
            return new FileCache(this);
        }

        private void verify() {
            checkNotNull(filePath, "file path cannot be null");
            checkNotNull(fileType, "file type cannot be null");
        }

    }

}
