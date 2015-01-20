package pl.edu.mimuw.nesc.type;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.declaration.tag.FieldDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.FieldTagDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.fieldtree.BlockElement;
import pl.edu.mimuw.nesc.declaration.tag.fieldtree.BlockElement.BlockType;
import pl.edu.mimuw.nesc.declaration.tag.fieldtree.FieldElement;
import pl.edu.mimuw.nesc.declaration.tag.fieldtree.TreeElement;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * <p>Class that represents tag types that contain fields. One of their features
 * is that they can be external.</p>
 *
 * <p>If a field tag type is created using the constructor with tag declaration
 * object, its variant is ONLY_DECLARATION. If it is created with the
 * constructor that takes the list of fields, its variant is ONLY_FIELDS.
 * The only possible transition of the variant is when the type is of
 * ONLY_DECLARATION variant and method {@link FieldTagType#fullyComplete}
 * is called - then the variant is changed to FULL.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public abstract class FieldTagType<D extends FieldTagDeclaration<?>> extends DerivedType {
    /**
     * Variant of the type object. Never null.
     */
    private Variant variant;

    /**
     * Object that actually represents the type. It is contained in a symbol
     * table if and only if it is named.
     */
    private final Optional<D> fieldTagDeclaration;

    /**
     * Type of the block that this field tag type is associated.
     */
    private final BlockType blockType;

    /**
     * Fields of this type. Unnamed anonymous structure or union members are
     * represented in the list as a single field.
     */
    private Optional<ImmutableList<Field>> fields = Optional.absent();

    protected FieldTagType(boolean constQualified, boolean volatileQualified,
                           D tagDeclaration, BlockType blockType) {
        super(constQualified, volatileQualified);
        checkNotNull(tagDeclaration, "the maybe external tag declaration object cannot be null");
        checkNotNull(blockType, "the block type cannot be null");
        this.fieldTagDeclaration = Optional.of(tagDeclaration);
        this.blockType = blockType;
        this.variant = Variant.ONLY_DECLARATION;
    }

    protected FieldTagType(boolean constQualified, boolean volatileQualified,
            ImmutableList<Field> fields, BlockType blockType) {
        super(constQualified, volatileQualified);
        checkNotNull(fields, "fields cannot be null");
        checkNotNull(blockType, "the block type cannot be null");
        this.fields = Optional.of(fields);
        this.blockType = blockType;
        this.fieldTagDeclaration = Optional.absent();
        this.variant = Variant.ONLY_FIELDS;
    }

    /**
     * Get the variant of a field tag type object.
     *
     * @return Variant of this type object.
     * @see Variant
     */
    public final Variant getVariant() {
        return variant;
    }

    /**
     * Get the declaration object that specifies this type.
     *
     * @return The declaration object.
     * @throws IllegalStateException Variant of this type is not
     *                               ONLY_DECLARATION or FULL.
     */
    public final D getDeclaration() {
        checkState(variant == Variant.ONLY_DECLARATION || variant == Variant.FULL,
                "invalid variant to get the declaration object");
        return fieldTagDeclaration.get();
    }

    /**
     * Get the list with fields of this type.
     *
     * @return List with fields of this type.
     * @throws IllegalStateException Variant of this type is ONLY_DECLARATION.
     */
    public final ImmutableList<Field> getFields() {
        checkState(variant == Variant.ONLY_FIELDS || variant == Variant.FULL,
                "invalid variant of the tag type to get fields list");
        return fields.get();
    }

    /**
     * This method works for every variant.
     *
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
        checkState(variant == Variant.ONLY_DECLARATION || variant == Variant.FULL,
                "invalid variant of this tag type");

        // If this type is incomplete, immediately return
        if (!fieldTagDeclaration.get().getAllFields().isPresent()) {
            return false;
        }

        // Handle the case if this type is const-qualified
        if (isConstQualified()) {
            return false;
        }

        final ImmutableList<FieldDeclaration> allFields =
                fieldTagDeclaration.get().getAllFields().get();

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
        return variant == Variant.ONLY_FIELDS || variant == Variant.FULL
                || fieldTagDeclaration.get().isDefined();
    }

    @Override
    public final Type decay() {
        return this;
    }

    /**
     * Create and store internally the list of fields of this type and ensure
     * that all fields are fully complete. After calling this method the variant
     * is either FULL or ONLY_FIELDS.
     */
    @Override
    public final void fullyComplete() {
        checkState(isComplete(), "cannot fully complete an incomplete type");

        // Build fields list if it is absent

        if (!fields.isPresent()) {
            final ImmutableList.Builder<Field> fieldsBuilder = ImmutableList.builder();
            for (TreeElement field : fieldTagDeclaration.get().getStructure().get()) {
                fieldsBuilder.add(new FieldBuilder(field).build());
            }

            this.fields = Optional.of(fieldsBuilder.build());
            this.variant = Variant.FULL;
        }

        // Ensure that all fields are fully complete

        for (Field field : fields.get()) {
            field.getType().fullyComplete();
        }
    }

    /**
     * Variant that specifies the content of this type.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public enum Variant {
        /**
         * This field tag type contains only tag declaration object (and
         * not fields).
         */
        ONLY_DECLARATION,
        /**
         * This field tag type contains only fields (and not the tag type
         * declaration object).
         */
        ONLY_FIELDS,
        /**
         * This field tag type contains reference to the tag declaration
         * object and list with fields.
         */
        FULL,
    }

    /**
     * A field of a structure, union, external structure or external union.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static final class Field {
        private final Type type;
        private final Optional<Expression> bitFieldWidth;

        public Field(Type type, Optional<Expression> bitFieldWidth) {
            checkNotNull(type, "type cannot be null");
            checkNotNull(bitFieldWidth, "width of the bit-field cannot be null");

            this.type = type;
            this.bitFieldWidth = bitFieldWidth;
        }

        /**
         * Get type of this field.
         *
         * @return Type of this field.
         */
        public Type getType() {
            return type;
        }

        /**
         * Get width of this field if it is a bit-field.
         *
         * @return Expression that evaluates to the width of this field if it
         *         is a bit-field.
         */
        public Optional<Expression> getWidth() {
            return bitFieldWidth;
        }

        /**
         * Check if this field is a bit-field.
         *
         * @return <code>true</code> if and only if this field is a bit-field.
         */
        public boolean isBitField() {
            return bitFieldWidth.isPresent();
        }
    }

    /**
     * Class responsible for creating Field objects from TreeElement objects.
     * Types in created objects are necessarily fully complete.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class FieldBuilder implements BlockElement.Visitor<Field, Void> {
        private final TreeElement element;

        private FieldBuilder(TreeElement treeElement) {
            this.element = treeElement;
        }

        private Field build() {
            return element.accept(this, null);
        }

        @Override
        public Field visit(FieldElement element, Void arg) {
            final FieldDeclaration fieldDecl = element.getFieldDeclaration();
            return new Field(fieldDecl.getType().get(), fieldDecl.getAstField().getBitfield());
        }

        @Override
        public Field visit(BlockElement element, Void arg) {
            final ImmutableList.Builder<Field> fieldsBuilder = ImmutableList.builder();
            for (TreeElement innerElement : element.getChildren()) {
                fieldsBuilder.add(new FieldBuilder(innerElement).build());
            }

            final Type fieldType;

            switch (element.getType()) {
                case STRUCTURE:
                    fieldType = new StructureType(fieldsBuilder.build());
                    break;
                case EXTERNAL_STRUCTURE:
                    fieldType = new ExternalStructureType(fieldsBuilder.build());
                    break;
                case UNION:
                    fieldType = new UnionType(fieldsBuilder.build());
                    break;
                case EXTERNAL_UNION:
                    fieldType = new ExternalUnionType(fieldsBuilder.build());
                    break;
                default:
                    throw new RuntimeException("unexpected type of field tag type element");
            }

            return new Field(fieldType, Optional.<Expression>absent());
        }
    }
}
