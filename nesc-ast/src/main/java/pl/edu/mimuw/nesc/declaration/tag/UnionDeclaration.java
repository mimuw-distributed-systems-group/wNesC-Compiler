package pl.edu.mimuw.nesc.declaration.tag;

import pl.edu.mimuw.nesc.ast.StructKind;
import pl.edu.mimuw.nesc.ast.gen.UnionRef;
import pl.edu.mimuw.nesc.type.ExternalUnionType;
import pl.edu.mimuw.nesc.type.FieldTagType;
import pl.edu.mimuw.nesc.type.UnionType;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class UnionDeclaration extends FieldTagDeclaration<UnionRef> {
    /**
     * Get a builder for a union declaration that corresponds to an union
     * declaration that is not simultaneously a definition.
     *
     * @return Newly created builder that will build an object that reflects a
     *         declaration that is not simultaneously a definition.
     */
    public static Builder declarationBuilder() {
        return new Builder(FieldTagDeclaration.Builder.Kind.DECLARATION);
    }

    /**
     * Get a builder for an union declaration that corresponds to an union
     * definition but without the requirement of specifying its fields that
     * is present in the definition builder.
     *
     * @return Newly created pre-definition builder.
     */
    public static Builder preDefinitionBuilder() {
        return new Builder(FieldTagDeclaration.Builder.Kind.PREDEFINITION);
    }

    /**
     * Get a builder for a union declaration that corresponds to an union
     * definition.
     *
     * @return Newly created builder that will build an object that reflects an
     *         union definition.
     */
    public static Builder definitionBuilder() {
        return new Builder(FieldTagDeclaration.Builder.Kind.DEFINITION);
    }

    /**
     * Initialize this union declaration.
     *
     * @param builder Builder with necessary information.
     */
    private UnionDeclaration(Builder builder) {
        super(builder);
    }

    @Override
    public FieldTagType<UnionDeclaration> getType(boolean constQualified, boolean volatileQualified) {
        return   isExternal()
               ? new ExternalUnionType(constQualified, volatileQualified, this)
               : new UnionType(constQualified, volatileQualified, this);
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    /**
     * Builder for an union declaration.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static final class Builder extends FieldTagDeclaration.ExtendedBuilder<UnionRef, UnionDeclaration> {

        private Builder(Kind builderKind) {
            super(builderKind);
        }

        @Override
        protected void beforeBuild() {
            super.beforeBuild();
            setKind(isExternal ? StructKind.NX_UNION : StructKind.UNION);
        }

        @Override
        protected UnionDeclaration create() {
            return new UnionDeclaration(this);
        }
    }
}
