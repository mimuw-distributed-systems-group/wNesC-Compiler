package pl.edu.mimuw.nesc.ast.type;

import pl.edu.mimuw.nesc.declaration.tag.FieldTagDeclaration;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class that represents tag types that contain fields. One of their features is
 * that they can be external.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public abstract class FieldTagType<D extends FieldTagDeclaration> extends DerivedType {
    /**
     * Object that actually represents the type. It is contained in a symbol
     * table if and only if it is named.
     */
    private final D maybeExternalTagDeclaration;

    protected FieldTagType(boolean constQualified, boolean volatileQualified,
                           D tagDeclaration) {
        super(constQualified, volatileQualified);
        checkNotNull(tagDeclaration, "the maybe external tag declaration object cannot be null");
        this.maybeExternalTagDeclaration = tagDeclaration;
    }

    public final D getDeclaration() {
        return maybeExternalTagDeclaration;
    }

    @Override
    public final boolean isScalarType() {
        return false;
    }

    @Override
    public final boolean isPointerType() {
        return false;
    }

    @Override
    public final boolean isFieldTagType() {
        return true;
    }
}
