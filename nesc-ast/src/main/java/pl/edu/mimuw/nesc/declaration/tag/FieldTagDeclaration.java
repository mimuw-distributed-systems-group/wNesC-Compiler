package pl.edu.mimuw.nesc.declaration.tag;

import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.TagRef;
import pl.edu.mimuw.nesc.declaration.tag.fieldtree.TreeElement;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This class represents tags that contain fields. The only such tags are
 * structures, unions and attributes. One of their features is that they can be
 * external (except for attributes).
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public abstract class FieldTagDeclaration<T extends TagRef> extends TagDeclaration {
    /**
     * Map that allows easy retrieval of information about a named field. It
     * shall be present if and only if this object represents a definition of
     * a tag type.
     */
    private final Optional<Map<String, FieldDeclaration>> namedFields;

    /**
     * All fields of this tag. It shall be present if and only if this object
     * corresponds to a definition of a tag type.
     */
    private final Optional<List<FieldDeclaration>> allFields;

    /**
     * Object with subsequent elements that depict the structure of this tag. It
     * shall be present if and only if this object corresponds to a definition
     * of a tag type.
     */
    private final Optional<List<TreeElement>> structure;

    /**
     * <code>true</code> if and only if this declaration corresponds to an
     * external tag declaration.
     */
    private final boolean isExternal;

    /**
     * AST node that corresponds to this declaration.
     */
    private final T astTagRef;

    protected FieldTagDeclaration(Optional<String> maybeName, Location location,
                                  T astNode, boolean isExternal,
                                  Optional<List<TreeElement>> maybeStructure) {
        super(maybeName, location, maybeStructure.isPresent());
        checkNotNull(astNode, "AST node for a tag declaration cannot be null");
        this.isExternal = isExternal;
        this.astTagRef = astNode;

        if (maybeStructure.isPresent()) {
            final List<TreeElement> structure = maybeStructure.get();
            final FieldsInformation information = processFields(structure);

            this.structure = Optional.of(Collections.unmodifiableList(structure));
            this.allFields = Optional.of(information.allFields);
            this.namedFields = Optional.of(information.namedFields);
        } else {
            this.structure = Optional.absent();
            this.allFields = Optional.absent();
            this.namedFields = Optional.absent();
        }
    }

    public final Optional<List<TreeElement>> getStructure() {
        return structure;
    }

    public final Optional<Map<String, FieldDeclaration>> getNamedFields() {
        return namedFields;
    }

    public final Optional<List<FieldDeclaration>> getAllFields() {
        return allFields;
    }

    public final T getAstNode() {
        return astTagRef;
    }

    public final boolean isExternal() {
        return isExternal;
    }

    private static FieldsInformation processFields(List<TreeElement> fields) {
        final Map<String, FieldDeclaration> namedFields = new HashMap<>();
        final List<FieldDeclaration> allFields = new ArrayList<>();

        for (TreeElement element : fields) {
            for (FieldDeclaration fieldDeclaration : element) {
                final Optional<String> maybeName = fieldDeclaration.getName();

                if (maybeName.isPresent()) {
                    namedFields.put(maybeName.get(), fieldDeclaration);
                }
                allFields.add(fieldDeclaration);
            }
        }

        return new FieldsInformation(Collections.unmodifiableMap(namedFields),
                                     Collections.unmodifiableList(allFields));
    }

    /**
     * A simple helper class.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static class FieldsInformation {
        private final Map<String, FieldDeclaration> namedFields;
        private final List<FieldDeclaration> allFields;

        private FieldsInformation(Map<String, FieldDeclaration> namedFields,
                                  List<FieldDeclaration> allFields) {
            this.namedFields = namedFields;
            this.allFields = allFields;
        }
    }
}
