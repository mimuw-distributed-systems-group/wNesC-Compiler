package pl.edu.mimuw.nesc.compilation;

import java.util.List;
import pl.edu.mimuw.nesc.FileData;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.NescDecl;
import pl.edu.mimuw.nesc.preprocessor.directive.PreprocessorDirective;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Data of a NesC interface or component that has a direct representation
 * as a file, i.e. it is not an interface or component that has been
 * instantiated.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class PreservedData extends ProcessedNescData {
    /**
     * The file data object carried by this data object.
     */
    private final FileData fileData;

    PreservedData(FileData fileData) {
        checkNotNull(fileData, "file data cannot be null");
        checkArgument(fileData.getEntityRoot().isPresent(),
                "the root node in the file data cannot be absent");

        this.fileData = fileData;
    }

    /**
     * Get the file data object carried by this data object.
     *
     * @return The file data object carried by this object.
     */
    FileData getFileData() {
        return fileData;
    }

    /**
     * Get the root NesC node contained in this data object.
     *
     * @return The root NesC node contained in the carried file data object.
     */
    NescDecl getEntityRoot() {
        return (NescDecl) fileData.getEntityRoot().get();
    }

    /**
     * Get the external declarations contained in this data object.
     *
     * @return External declarations contained in the carried file data object.
     */
    List<Declaration> getExtdefs() {
        return fileData.getExtdefs();
    }

    /**
     * Get the preprocessor directives contained in this data object.
     *
     * @return Preprocessor directives contained in the carried file data
     *         object.
     */
    List<PreprocessorDirective> getPreprocessorDirectives() {
        return fileData.getPreprocessorDirectives();
    }

    @Override
    <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
