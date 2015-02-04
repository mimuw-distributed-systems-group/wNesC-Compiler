package pl.edu.mimuw.nesc.declaration.tag;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import pl.edu.mimuw.nesc.ast.StructSemantics;
import pl.edu.mimuw.nesc.ast.gen.TagRef;
import pl.edu.mimuw.nesc.declaration.CopyController;
import pl.edu.mimuw.nesc.declaration.tag.fieldtree.TreeElement;
import pl.edu.mimuw.nesc.type.FieldTagType;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

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
    private Optional<ImmutableMap<String, FieldDeclaration>> namedFields = Optional.absent();

    /**
     * All fields of this tag. It shall be present if and only if this object
     * corresponds to a definition of a tag type.
     */
    private Optional<ImmutableList<FieldDeclaration>> allFields = Optional.absent();

    /**
     * Object with subsequent elements that depict the structure of this tag. It
     * shall be present if and only if this object corresponds to a definition
     * of a tag type.
     */
    private Optional<ImmutableList<TreeElement>> structure = Optional.absent();

    /**
     * AST node that corresponds to this declaration.
     */
    private T astTagRef;

    /**
     * Initialize this object using the given builder.
     *
     * @param builder Builder with information necessary for initialization.
     */
    protected FieldTagDeclaration(Builder<T, ? extends FieldTagDeclaration<T>> builder) {
        super(builder);

        this.astTagRef = builder.astTagRef;
        if (builder.structure.isPresent()) {
            define(builder.structure.get());
        }
    }

    /**
     * Transforms this object to represent the definition of an field tag type.
     *
     * @param structure The structure of fields of the tag.
     * @throws NullPointerException Given argument is null.
     * @throws IllegalStateException This object corresponds to a definition.
     */
    public final void define(List<TreeElement> structure) {
        checkNotNull(structure, "the structure cannot be null");
        checkState(!isDefined(), "this field tag contains information about a definition");

        final DefinitionElementsBuilder elementsBuilder = new DefinitionElementsBuilder(structure);
        this.namedFields = Optional.of(elementsBuilder.buildNamedFields());
        this.allFields = Optional.of(elementsBuilder.buildAllFields());
        this.structure = Optional.of(elementsBuilder.buildStructure());
    }

    /**
     * Update the AST node referred by this declaration. The purpose is to make
     * it point to the definition of a tag if it currently points to a
     * declaration node.
     *
     * @param node Node to be set as the AST node for this declaration.
     * @throws NullPointerException Given argument is null.
     * @throws IllegalArgumentException Given node represents a declaration that
     *                                  is not definition.
     * @throws IllegalStateException This declaration is currently associated
     *                               with a definition AST node.
     */
    public final void setPredefinitionNode(T node) {
        checkNotNull(node, "the AST node cannot be null");
        checkArgument(node.getSemantics() != StructSemantics.OTHER,
                "given nodes is not a pre-definition or definition node");
        checkState(astTagRef.getSemantics() == StructSemantics.OTHER,
                "the pre-definition node has been already set");

        this.astTagRef = node;
    }

    public final Optional<ImmutableList<TreeElement>> getStructure() {
        return structure;
    }

    public final Optional<ImmutableMap<String, FieldDeclaration>> getNamedFields() {
        return namedFields;
    }

    /**
     * <p>Get an immutable list with information about all fields of this tag
     * type. Unnamed members are contained in the list. Fields that are members
     * of this tag type because of presence of an unnamed member of an anonymous
     * tag type are also included.</p>
     * <p>The object is present if and only if this tag type is defined.</p>
     *
     * @return Immutable list with all fields of this tag type if it is defined.
     */
    public final Optional<ImmutableList<FieldDeclaration>> getAllFields() {
        return allFields;
    }

    /**
     * Find a field with given name of the type this declaration represents.
     *
     * @param fieldName Name of the field to find.
     * @return Field declaration object that is associated with field with given
     *         name. If this type is not defined, then the object is absent. If
     *         it is defined, but a field with given name does not exist, the
     *         object is absent, too.
     * @throws NullPointerException Given argument is null.
     * @throws IllegalArgumentException Given field name is empty.
     */
    public final Optional<FieldDeclaration> findField(String fieldName) {
        checkNotNull(fieldName, "name of the field to find cannot be null");
        checkArgument(!fieldName.isEmpty(), "name of the field to find is empty");

        if (!namedFields.isPresent()) {
            return Optional.absent();
        }

        return Optional.fromNullable(namedFields.get().get(fieldName));
    }

    @Override
    public abstract FieldTagType<?> getType(boolean constQualified, boolean volatileQualified);

    @Override
    public final T getAstNode() {
        return astTagRef;
    }

    @Override
    public final boolean isDefined() {
        return structure.isPresent();
    }

    @Override
    public abstract FieldTagDeclaration<T> deepCopy(CopyController controller);

    protected <D extends FieldTagDeclaration<T>> D copyHelp(Builder<T, D> builder,
            CopyController controller) {

        if (this.structure.isPresent()) {
            final List<TreeElement> newStructure = new ArrayList<>();
            for (TreeElement element : this.structure.get()) {
                newStructure.add(element.deepCopy(controller));
            }
            builder.structure(newStructure);
        }

        final D result = builder.astNode(this.astTagRef)
                .name(getName().orNull(), controller.mapUniqueName(getUniqueName()).orNull())
                .startLocation(this.location)
                .build();
        if (hasLayout()) {
            result.setLayout(getSize(), getAlignment());
        }

        return result;
    }

    @Override
    public void ownContents() {
        if (allFields.isPresent()) {
            for (FieldDeclaration field : allFields.get()) {
                field.ownedBy(this);
            }
        }
    }

    /**
     * Builder for a field tag type declaration.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static abstract class Builder<T extends TagRef, D extends FieldTagDeclaration<T>> extends TagDeclaration.Builder<D> {
        /**
         * Data needed to control the building process.
         */
        private final boolean definitionBuilder;

        /**
         * Data needed to build a field tag type declaration.
         */
        private T astTagRef;
        private Optional<List<TreeElement>> structure = Optional.absent();

        protected Builder(boolean definitionBuilder) {
            this.definitionBuilder = definitionBuilder;
        }

        /**
         * Set the AST node that the tag declaration represents.
         *
         * @param tagRef Tag reference to set.
         * @return <code>this</code>
         */
        public Builder<T, D> astNode(T tagRef) {
            this.astTagRef = tagRef;
            return this;
        }

        /**
         * Set the structure of the tag. It may be null and then no structure
         * will be used.
         *
         * @param structure Structure to set.
         * @return <code>this</code>
         */
        public Builder<T, D> structure(List<TreeElement> structure) {
            this.structure = Optional.fromNullable(structure);
            return this;
        }

        @Override
        protected void validate() {
            super.validate();

            checkNotNull(astTagRef, "AST node cannot be null");
            checkNotNull(structure, "structure cannot be null");

            if (definitionBuilder) {
                checkState(structure.isPresent(), "the structure must be present in a definition builder");
            } else {
                checkState(!structure.isPresent(), "the structure must be absent in a declaration builder");
                checkState(getName().isPresent(), "the name must be present in a declaration builder");
            }
        }
    }

    /**
     * Builder that allows setting if the tag is external.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static abstract class ExtendedBuilder<T extends TagRef, D extends FieldTagDeclaration<T>> extends Builder<T, D> {
        protected boolean isExternal;

        protected ExtendedBuilder(boolean definitionBuilder) {
            super(definitionBuilder);
        }

        /**
         * Set if the field tag is external.
         *
         * @param isExternal Value to set.
         * @return <code>this</code>
         */
        public ExtendedBuilder<T, D> isExternal(boolean isExternal) {
            this.isExternal = isExternal;
            return this;
        }
    }

    /**
     * Builder for the objects that constitute a definition of a field tag.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class DefinitionElementsBuilder {
        private final List<TreeElement> elements;

        private DefinitionElementsBuilder(List<TreeElement> elements) {
            this.elements = elements;
        }

        private ImmutableList<TreeElement> buildStructure() {
            return ImmutableList.copyOf(elements);
        }

        private ImmutableList<FieldDeclaration> buildAllFields() {
            final ImmutableList.Builder<FieldDeclaration> builder = ImmutableList.builder();

            for (TreeElement treeElement : elements) {
                for (FieldDeclaration fieldDeclaration : treeElement) {
                    builder.add(fieldDeclaration);
                }
            }

            return builder.build();
        }

        private ImmutableMap<String, FieldDeclaration> buildNamedFields() {
            final ImmutableMap.Builder<String, FieldDeclaration> builder = ImmutableMap.builder();
            final Set<String> processedFields = new HashSet<>();

            for (TreeElement treeElement : elements) {
                for (FieldDeclaration fieldDeclaration : treeElement) {
                    final Optional<String> name = fieldDeclaration.getName();
                    if (name.isPresent() && processedFields.add(name.get())) {
                        builder.put(name.get(), fieldDeclaration);
                    }
                }
            }

            return builder.build();
        }
    }
}
