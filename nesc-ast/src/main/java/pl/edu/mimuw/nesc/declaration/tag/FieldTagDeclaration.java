package pl.edu.mimuw.nesc.declaration.tag;

import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.TagRef;

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
     * <code>true</code> if and only if this declaration corresponds to an
     * external tag declaration.
     */
    private final boolean isExternal;

    /**
     * AST node that corresponds to this declaration.
     */
    private final T astTagRef;

    /* TODO pointers to definitions of structures, unions and enumerations
       for type analysis */

    protected FieldTagDeclaration(Optional<String> maybeName, Location location,
                                  boolean isDefined, T astNode, boolean isExternal,
                                  Optional<List<FieldDeclaration>> maybeFields) {
        super(maybeName, location, isDefined);
        checkNotNull(astNode, "AST node for a tag declaration cannot be null");
        this.isExternal = isExternal;
        this.astTagRef = astNode;

        if (maybeFields.isPresent()) {
            final List<FieldDeclaration> fields = maybeFields.get();
            this.allFields = Optional.of(Collections.unmodifiableList(new ArrayList<>(fields)));
            this.namedFields = Optional.of(createFieldsMap(fields));
        } else {
            this.allFields = Optional.absent();
            this.namedFields = Optional.absent();
        }
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

    private static Map<String, FieldDeclaration> createFieldsMap(List<FieldDeclaration> fields) {
        final Map<String, FieldDeclaration> result = new HashMap<>();

        for (FieldDeclaration field : fields) {
            final Optional<String> maybeName = field.getName();
            if (maybeName.isPresent()) {
                result.put(maybeName.get(), field);
            }
        }

        return Collections.unmodifiableMap(result);
    }
}
