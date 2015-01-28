package pl.edu.mimuw.nesc.declaration.tag;

import pl.edu.mimuw.nesc.ast.StructKind;
import pl.edu.mimuw.nesc.ast.gen.AttributeRef;
import pl.edu.mimuw.nesc.declaration.CopyController;
import pl.edu.mimuw.nesc.type.FieldTagType;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class AttributeDeclaration extends FieldTagDeclaration<AttributeRef> {
    /**
     * Get a builder for a pre-definition of an attribute.
     *
     * @return Newly created builder.
     */
    public static Builder preDefinitionBuilder() {
        return new Builder(false);
    }

    /**
     * Get a builder for a definition of an attribute.
     *
     * @return Newly created builder that will build an object that represents
     *         a definition of an attribute.
     */
    public static Builder definitionBuilder() {
        return new Builder(true);
    }

    /**
     * Initialize this object.
     *
     * @param builder Builder with information necessary to initialize.
     */
    private AttributeDeclaration(Builder builder) {
        super(builder);
    }

    @Override
    public FieldTagType<?> getType(boolean constQualified, boolean volatileQualified) {
        throw new UnsupportedOperationException("an attribute declaration " +
                 "does not support the operation of getting the type it represents");
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    @Override
    public AttributeDeclaration deepCopy(CopyController controller) {
        return copyHelp(definitionBuilder(), controller);
    }

    /**
     * Builder for an attribute declaration.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static final class Builder extends FieldTagDeclaration.Builder<AttributeRef, AttributeDeclaration> {

        private Builder(boolean definitionBuilder) {
            super(definitionBuilder);
        }

        @Override
        protected void beforeBuild() {
            super.beforeBuild();
            setKind(StructKind.ATTRIBUTE);
        }

        @Override
        protected AttributeDeclaration create() {
            return new AttributeDeclaration(this);
        }
    }
}
