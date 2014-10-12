package pl.edu.mimuw.nesc.declaration.object;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.gen.ComponentRef;
import pl.edu.mimuw.nesc.ast.gen.Word;
import pl.edu.mimuw.nesc.ast.type.ComponentType;
import pl.edu.mimuw.nesc.ast.type.Type;
import pl.edu.mimuw.nesc.declaration.nesc.NescDeclaration;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Component reference.</p>
 * <p><code>name</code> is not the name of component but the name of the
 * reference (component can be aliased).</p>
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class ComponentRefDeclaration extends ObjectDeclaration {

    private final Word componentName;

    private final ComponentRef astComponentRef;
    /**
     * Component declaration (absent if reference is erroneous).
     */
    private final Optional<? extends NescDeclaration> componentDeclaration;

    public static Builder builder() {
        return new Builder();
    }

    protected ComponentRefDeclaration(Builder builder) {
        super(builder);
        this.componentName = builder.componentName;
        this.astComponentRef = builder.astComponentRef;
        this.componentDeclaration = builder.componentDeclaration;
    }

    public Word getComponentName() {
        return componentName;
    }

    public ComponentRef getAstComponentRef() {
        return astComponentRef;
    }

    public Optional<? extends NescDeclaration> getComponentDeclaration() {
        return componentDeclaration;
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    /**
     * Builder for the component reference declaration.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static class Builder extends ObjectDeclaration.Builder<ComponentRefDeclaration> {
        /**
         * Data needed to build the declaration object.
         */
        private Word componentName;
        private ComponentRef astComponentRef;
        private Optional<? extends NescDeclaration> componentDeclaration = Optional.absent();

        /**
         * Set the name of the referred component.
         *
         * @param componentName Name of the component that is referred.
         * @return <code>this</code>
         */
        public Builder componentName(Word componentName) {
            this.componentName = componentName;
            return this;
        }

        /**
         * Set the AST node that the declaration object will be associated with.
         *
         * @param astComponentRef AST node to set in the created declaration
         *                        object.
         * @return <code>this</code>
         */
        public Builder astNode(ComponentRef astComponentRef) {
            this.astComponentRef = astComponentRef;
            return this;
        }

        /**
         * Set the NesC declaration object that represents the referred
         * component.
         *
         * @param declaration Declaration object to be set.
         * @return <code>this</code>
         */
        public Builder nescDeclaration(Optional<? extends NescDeclaration> declaration) {
            this.componentDeclaration = declaration;
            return this;
        }

        @Override
        protected void beforeBuild() {
            super.beforeBuild();

            if (componentName != null) {
                setType(Optional.<Type>of(new ComponentType(componentName.getName())));
            }
            setLinkage(Optional.of(Linkage.NONE));
            setKind(ObjectKind.COMPONENT);
        }

        @Override
        protected void validate() {
            super.validate();
            checkNotNull(componentName, "the component name cannot be null");
            checkNotNull(astComponentRef, "the AST of component reference cannot be null");
            checkNotNull(componentDeclaration, "the component declaration cannot be null");
        }

        @Override
        protected ComponentRefDeclaration create() {
            return new ComponentRefDeclaration(this);
        }
    }
}
