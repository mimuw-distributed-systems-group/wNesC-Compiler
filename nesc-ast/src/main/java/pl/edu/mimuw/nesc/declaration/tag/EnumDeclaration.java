package pl.edu.mimuw.nesc.declaration.tag;

import com.google.common.base.Optional;
import java.util.List;

import com.google.common.collect.ImmutableList;
import pl.edu.mimuw.nesc.ast.StructKind;
import pl.edu.mimuw.nesc.ast.StructSemantics;
import pl.edu.mimuw.nesc.ast.gen.EnumRef;
import pl.edu.mimuw.nesc.declaration.CopyController;
import pl.edu.mimuw.nesc.declaration.object.ConstantDeclaration;
import pl.edu.mimuw.nesc.type.EnumeratedType;
import pl.edu.mimuw.nesc.type.IntegerType;

import static com.google.common.base.Preconditions.*;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class EnumDeclaration extends TagDeclaration {
    /**
     * List with all enumerators from this enumeration type. It should be
     * unmodifiable. It may be empty (however, program that contains such
     * enumeration is ill-formed). This object is absent if and only if the
     * tag is not defined.
     */
    private Optional<ImmutableList<ConstantDeclaration>> constants;

    /**
     * Object that represents this enumeration from AST.
     */
    private EnumRef astEnumRef;

    /**
     * Integer type that is compatible with this enumerated type and capable of
     * representing all enumeration constants.
     */
    private Optional<IntegerType> compatibleType;

    /**
     * Get a builder for a definition of an enumeration type.
     *
     * @return Newly created builder that will build an enum declaration that
     *         corresponds to an enumerated type definition.
     */
    public static DefinitionBuilder definitionBuilder() {
        return new DefinitionBuilder();
    }

    /**
     * Get a builder for a declaration that is not a definition of an
     * enumeration type. However, it is forbidden in the ISO standard.
     *
     * @return Newly created builder that will build an enum declaration that
     *         corresponds to an enumerated type declaration that is not its
     *         definition.
     */
    public static DeclarationBuilder declarationBuilder() {
        return new DeclarationBuilder();
    }

    /**
     * Initialize this enum declaration.
     *
     * @param builder Builder with data for initialization.
     */
    private EnumDeclaration(Builder builder) {
        super(builder);

        this.constants = builder.buildConstants();
        this.astEnumRef = builder.astEnumRef;
        this.compatibleType = Optional.absent();
    }

    /**
     * Get the enumerators for this enum declaration. If it corresponds to
     * a definition, the list is present.
     *
     * @return The list with enumerators for this declaration.
     */
    public Optional<ImmutableList<ConstantDeclaration>> getConstants() {
        return constants;
    }

    /**
     * Make this object represent a definition of an enumeration type.
     *
     * @param constants Enumerators that define the enumerated type.
     * @throws NullPointerException Given argument is null.
     * @throws IllegalStateException This enum declaration corresponds to
     *                               a defined enumerated type.
     */
    public void define(List<ConstantDeclaration> constants) {
        checkNotNull(constants, "constants cannot be null");
        checkState(!isDefined(), "the enum declaration contains information about definition");

        this.constants = Optional.of(ImmutableList.copyOf(constants));
    }

    /**
     * Update the AST node associated with this enumerated type declaration. The
     * purpose is to allow pointing to a definition of the enumerated type if it
     * currently refers a declaration but not definition.
     *
     * @param predefinitionNode AST node to set.
     * @throws NullPointerException Given argument is null.
     * @throws IllegalArgumentException Given node does not represent
     *                                  a definition.
     * @throws IllegalStateException This declaration has been already
     *                               associated with a definition AST node.
     */
    public void setPredefinitionNode(EnumRef predefinitionNode) {
        checkNotNull(predefinitionNode, "the AST node cannot be null");
        checkArgument(predefinitionNode.getSemantics() != StructSemantics.OTHER,
                "the given node does not represent an enumerated type definition");
        checkState(astEnumRef.getSemantics() == StructSemantics.OTHER,
                "this declaration has been already associated with a definition AST node");

        this.astEnumRef = predefinitionNode;
    }

    /**
     * Get the type compatible with this enumerated type. The type is capable of
     * representing all constants of this enumeration.
     *
     * @return Compatible type of this enumerated type.
     * @throws IllegalStateException The compatible type has not been set.
     */
    public IntegerType getCompatibleType() {
        checkState(compatibleType.isPresent(), "compatible type has not been set yet");
        return compatibleType.get();
    }

    /**
     * Set the compatible type of this enumerated type. It can be done exactly
     * once.
     *
     * @param type Type to set as the compatible type of the enumerated type.
     * @throws NullPointerException <code>type</code> is <code>null</code>.
     * @throws IllegalStateException The compatible type has been already set.
     */
    public void setCompatibleType(IntegerType type) {
        checkNotNull(type, "the compatible type cannot be null");
        checkState(!compatibleType.isPresent(), "the compatible type has been already set");

        this.compatibleType = Optional.of(type);
    }

    @Override
    public EnumRef getAstNode() {
        return astEnumRef;
    }

    @Override
    public EnumeratedType getType(boolean constQualified, boolean volatileQualified) {
        return new EnumeratedType(constQualified, volatileQualified, this);
    }

    @Override
    public boolean isDefined() {
        return constants.isPresent();
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    @Override
    public EnumDeclaration deepCopy(CopyController controller) {
        final EnumDeclaration.Builder builder;

        if (isDefined()) {
            final EnumDeclaration.DefinitionBuilder definitionBuilder =
                    EnumDeclaration.definitionBuilder();

            for (ConstantDeclaration constant : this.constants.get()) {
                definitionBuilder.addConstant(controller.copy(constant));
            }

            builder = definitionBuilder;
        } else {
            builder = EnumDeclaration.declarationBuilder();
        }

        final EnumDeclaration result = builder.astNode(controller.mapNode(this.astEnumRef))
                .name(this.getName().orNull(), controller.mapUniqueName(this.getUniqueName()).orNull())
                .startLocation(this.location)
                .build();

        if (hasLayout()) {
            result.setLayout(getSize(), getAlignment());
        }

        if (this.compatibleType.isPresent()) {
            result.compatibleType = this.compatibleType;
        }

        if (isCorrect().isPresent()) {
            result.setIsCorrect(isCorrect().get());
        }

        if (isTransformed()) {
            result.transformed();
        }

        return result;
    }

    @Override
    public void ownContents() {
        if (constants.isPresent()) {
            for (ConstantDeclaration constant : constants.get()) {
                constant.ownedBy(this);
            }
        }
    }

    /**
     * Builder for an enum declaration.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static abstract class Builder extends TagDeclaration.Builder<EnumDeclaration> {
        /**
         * Data needed to build an enum declaration.
         */
        private EnumRef astEnumRef;

        private Builder() {
        }

        /**
         * Set the AST node that corresponds to the enum declaration.
         *
         * @param astEnumRef AST node to set.
         * @return <code>this</code>
         */
        public Builder astNode(EnumRef astEnumRef) {
            this.astEnumRef = astEnumRef;
            return this;
        }

        abstract Optional<ImmutableList<ConstantDeclaration>> buildConstants();

        @Override
        protected void beforeBuild() {
            super.beforeBuild();
            setKind(StructKind.ENUM);
        }

        @Override
        protected final EnumDeclaration create() {
            return new EnumDeclaration(this);
        }
    }

    /**
     * A declaration but not definition builder.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static final class DeclarationBuilder extends Builder {
        @Override
        Optional<ImmutableList<ConstantDeclaration>> buildConstants() {
            return Optional.absent();
        }

        @Override
        protected void validate() {
            super.validate();
            checkNotNull(getName(), "the name for a declaration cannot be null");
        }
    }

    /**
     * Builder for an enumeration definition.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static final class DefinitionBuilder extends Builder {
        /**
         * Data needed to build the object.
         */
        private final ImmutableList.Builder<ConstantDeclaration> constantsBuilder = ImmutableList.builder();

        /**
         * Add next constant of an enumerated type definition.
         *
         * @param constant Enumerator to be added.
         * @return <code>this</code>
         * @throws NullPointerException Given argument is null.
         */
        public DefinitionBuilder addConstant(ConstantDeclaration constant) {
            checkNotNull(constant, "the constant cannot be null");
            constantsBuilder.add(constant);
            return this;
        }

        /**
         * Add constants from a list.
         *
         * @param constants List of constants to add.
         * @return <code>this</code>
         * @throws NullPointerException Given argument is null.
         */
        public DefinitionBuilder addAllConstants(List<ConstantDeclaration> constants) {
            checkNotNull(constants, "constants cannot be null");
            constantsBuilder.addAll(constants);
            return this;
        }

        @Override
        Optional<ImmutableList<ConstantDeclaration>> buildConstants() {
            return Optional.of(constantsBuilder.build());
        }
    }
}
