package pl.edu.mimuw.nesc.analysis.type;

import pl.edu.mimuw.nesc.declaration.tag.MaybeExternalTagDeclaration;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class that represents tag types that can be external, i.e. structure types
 * and union types.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public abstract class MaybeExternalTagType<D extends MaybeExternalTagDeclaration> extends DerivedType {
    /**
     * Object that actually represents the type. It is contained in a symbol
     * table if and only if it is named.
     */
    private final D maybeExternalTagDeclaration;

    protected MaybeExternalTagType(boolean constQualified, boolean volatileQualified,
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
}
