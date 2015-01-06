package pl.edu.mimuw.nesc.declaration.tag;

import pl.edu.mimuw.nesc.ast.StructKind;
import pl.edu.mimuw.nesc.ast.gen.StructRef;
import pl.edu.mimuw.nesc.type.ExternalStructureType;
import pl.edu.mimuw.nesc.type.StructureType;
import pl.edu.mimuw.nesc.type.Type;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class StructDeclaration extends FieldTagDeclaration<StructRef> {
    /**
     * Get the builder for declarations of structure tags that are not
     * definitions.
     *
     * @return Newly created builder that will build an object that corresponds
     *         to a declaration of a structure type that is not definition.
     */
    public static Builder declarationBuilder() {
        return new Builder(false);
    }

    /**
     * Get the builder for a definition of a structure tag.
     *
     * @return Newly created builder that will build an object that corresponds
     *         to a definition of a structure type.
     */
    public static Builder definitionBuilder() {
        return new Builder(true);
    }

    /**
     * Initialize this structure declaration.
     *
     * @param builder Builder with necessary information.
     */
    private StructDeclaration(Builder builder) {
        super(builder);
    }

    @Override
    public Type getType(boolean constQualified, boolean volatileQualified) {
        return   isExternal()
               ? new ExternalStructureType(constQualified, volatileQualified, this)
               : new StructureType(constQualified, volatileQualified, this);
    }

    @Override
    public <R, A> R visit(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    /**
     * Builder for a struct declaration.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static final class Builder extends FieldTagDeclaration.ExtendedBuilder<StructRef, StructDeclaration> {

        private Builder(boolean definitionBuilder) {
            super(definitionBuilder);
        }

        @Override
        protected void beforeBuild() {
            super.beforeBuild();
            setKind(isExternal ? StructKind.NX_STRUCT : StructKind.STRUCT);
        }

        @Override
        protected StructDeclaration create() {
            return new StructDeclaration(this);
        }
    }
}
