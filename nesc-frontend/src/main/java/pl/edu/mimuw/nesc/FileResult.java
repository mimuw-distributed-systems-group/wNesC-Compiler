package pl.edu.mimuw.nesc;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.Node;
import pl.edu.mimuw.nesc.lexer.Comment;
import pl.edu.mimuw.nesc.preprocessor.directive.PreprocessorDirective;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Contains the results of parsing of single nesC source file.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class FileResult {

    public static Builder builder() {
        return new Builder();
    }

    private final String fileName;
    private final Optional<Node> entityRoot;
    private final List<Declaration> extdefs;
    private final List<Comment> comments;
    private final List<PreprocessorDirective> preprocessorDirectives;

    // TODO: extends attributes list (e.g. macros...).

    private FileResult(Builder builder) {
        this.fileName = builder.fileName;
        this.entityRoot = builder.entityRoot;
        this.extdefs = builder.extdefsBuilder.build();
        this.comments = builder.commentsBuilder.build();
        this.preprocessorDirectives = builder.directivesBuilder.build();
    }

    public String getFileName() {
        return fileName;
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

    @Override
    public String toString() {
        return "{ FileResult; {fileName=" + fileName + ", entityRoot=" + entityRoot + ", extdefs=" + extdefs
                + ", comments=" + comments + "}}";
    }

    /**
     * FileResult builder.
     *
     * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
     */
    public static final class Builder {

        private String fileName;
        private Optional<Node> entityRoot;
        private final ImmutableList.Builder<Declaration> extdefsBuilder;
        private final ImmutableList.Builder<Comment> commentsBuilder;
        private final ImmutableList.Builder<PreprocessorDirective> directivesBuilder;

        public Builder() {
            this.extdefsBuilder = new ImmutableList.Builder<>();
            this.commentsBuilder = new ImmutableList.Builder<>();
            this.directivesBuilder = new ImmutableList.Builder<>();
        }

        public Builder fileName(String fileName) {
            this.fileName = fileName;
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

        public Builder comment(Comment comment) {
            this.commentsBuilder.add(comment);
            return this;
        }

        public Builder preprocessorDirective(PreprocessorDirective directive) {
            this.directivesBuilder.add(directive);
            return this;
        }

        public FileResult build() {
            if (entityRoot == null) {
                entityRoot = Optional.absent();
            }
            validate();
            return new FileResult(this);
        }

        private void validate() {
            checkNotNull(fileName, "file name cannot be null");
        }
    }

}
