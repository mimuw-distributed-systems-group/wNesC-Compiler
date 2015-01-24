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
import pl.edu.mimuw.nesc.type.Type;

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
    private Optional<ImmutableList<ConstantDeclaration>> enumerators;

    /**
     * Object that represents this enumeration from AST.
     */
    private EnumRef astEnumRef;

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

        this.enumerators = builder.buildEnumerators();
        this.astEnumRef = builder.astEnumRef;
    }

    /**
     * Get the enumerators for this enum declaration. If it corresponds to
     * a definition, the list is present.
     *
     * @return The list with enumerators for this declaration.
     */
    public Optional<ImmutableList<ConstantDeclaration>> getEnumerators() {
        return enumerators;
    }

    /**
     * Make this object represent a definition of an enumeration type.
     *
     * @param enumerators Enumerators that define the enumerated type.
     * @throws NullPointerException Given argument is null.
     * @throws IllegalStateException This enum declaration corresponds to
     *                               a defined enumerated type.
     */
    public void define(List<ConstantDeclaration> enumerators) {
        checkNotNull(enumerators, "enumerators cannot be null");
        checkState(!isDefined(), "the enum declaration contains information about definition");

        this.enumerators = Optional.of(ImmutableList.copyOf(enumerators));
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

    @Override
    public EnumRef getAstNode() {
        return astEnumRef;
    }

    @Override
    public Type getType(boolean constQualified, boolean volatileQualified) {
        return new EnumeratedType(constQualified, volatileQualified, this);
    }

    @Override
    public boolean isDefined() {
        return enumerators.isPresent();
    }

    @Override
    public <R, A> R visit(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    @Override
    public EnumDeclaration deepCopy(CopyController controller) {
        final EnumDeclaration.Builder builder;

        if (isDefined()) {
            final EnumDeclaration.DefinitionBuilder definitionBuilder =
                    EnumDeclaration.definitionBuilder();

            for (ConstantDeclaration constant : this.enumerators.get()) {
                definitionBuilder.addEnumerator(controller.copy(constant));
            }

            builder = definitionBuilder;
        } else {
            builder = EnumDeclaration.declarationBuilder();
        }

        return builder.astNode(controller.mapNode(this.astEnumRef))
                .name(this.getName().orNull(), controller.mapUniqueName(this.getUniqueName()).orNull())
                .startLocation(this.location)
                .build();
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

        abstract Optional<ImmutableList<ConstantDeclaration>> buildEnumerators();

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
        Optional<ImmutableList<ConstantDeclaration>> buildEnumerators() {
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
        private final ImmutableList.Builder<ConstantDeclaration> enumeratorsBuilder = ImmutableList.builder();

        /**
         * Add next enumerator of an enumerated type definition.
         *
         * @param enumerator Enumerator to be added.
         * @return <code>this</code>
         * @throws NullPointerException Given argument is null.
         */
        public DefinitionBuilder addEnumerator(ConstantDeclaration enumerator) {
            checkNotNull(enumerator, "the enumerator cannot be null");
            enumeratorsBuilder.add(enumerator);
            return this;
        }

        /**
         * Add enumerators from a list.
         *
         * @param enumerators List of enumerators to add.
         * @return <code>this</code>
         * @throws NullPointerException Given argument is null.
         */
        public DefinitionBuilder addAllEnumerators(List<ConstantDeclaration> enumerators) {
            checkNotNull(enumerators, "enumerators cannot be null");
            enumeratorsBuilder.addAll(enumerators);
            return this;
        }

        @Override
        Optional<ImmutableList<ConstantDeclaration>> buildEnumerators() {
            return Optional.of(enumeratorsBuilder.build());
        }
    }
}
