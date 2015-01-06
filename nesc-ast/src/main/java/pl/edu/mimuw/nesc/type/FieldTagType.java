package pl.edu.mimuw.nesc.type;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import pl.edu.mimuw.nesc.declaration.tag.FieldDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.FieldTagDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.fieldtree.BlockElement.BlockType;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class that represents tag types that contain fields. One of their features is
 * that they can be external.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public abstract class FieldTagType<D extends FieldTagDeclaration<?>> extends DerivedType {
    /**
     * Object that actually represents the type. It is contained in a symbol
     * table if and only if it is named.
     */
    private final D fieldTagDeclaration;

    /**
     * Type of the block that this field tag type is associated.
     */
    private final BlockType blockType;

    protected FieldTagType(boolean constQualified, boolean volatileQualified,
                           D tagDeclaration, BlockType blockType) {
        super(constQualified, volatileQualified);
        checkNotNull(tagDeclaration, "the maybe external tag declaration object cannot be null");
        checkNotNull(blockType, "the block type cannot be null");
        this.fieldTagDeclaration = tagDeclaration;
        this.blockType = blockType;
    }

    public final D getDeclaration() {
        return fieldTagDeclaration;
    }

    /**
     * @return The type of the block associated with this field tag type. Never
     *         null.
     */
    public final BlockType getBlockType() {
        return blockType;
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
    public final boolean isArrayType() {
        return false;
    }

    @Override
    public final boolean isFieldTagType() {
        return true;
    }

    @Override
    public final boolean isObjectType() {
        return true;
    }

    @Override
    public final boolean isFunctionType() {
        return false;
    }

    @Override
    public final boolean isModifiable() {
        // If this type is incomplete, immediately return
        if (!fieldTagDeclaration.getAllFields().isPresent()) {
            return false;
        }

        // Handle the case if this type is const-qualified
        if (isConstQualified()) {
            return false;
        }

        final ImmutableList<FieldDeclaration> allFields =
                fieldTagDeclaration.getAllFields().get();

        for (FieldDeclaration fieldDecl : allFields) {
            final Optional<Type> fieldType = fieldDecl.getType();

            if (fieldType.isPresent() && !fieldType.get().isModifiable()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean isCompatibleWith(Type type) {
        if (!super.isCompatibleWith(type)) {
            return false;
        }

        final FieldTagType<? extends FieldTagDeclaration<?>> fieldTagType =
                (FieldTagType<? extends FieldTagDeclaration<?>>) type;
        return getDeclaration() == fieldTagType.getDeclaration();
    }

    @Override
    public final boolean isComplete() {
        return fieldTagDeclaration.isDefined();
    }

    @Override
    public final Type decay() {
        return this;
    }
}
