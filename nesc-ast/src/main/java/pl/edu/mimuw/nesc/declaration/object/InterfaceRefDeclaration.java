package pl.edu.mimuw.nesc.declaration.object;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.gen.InterfaceRef;
import pl.edu.mimuw.nesc.ast.type.InterfaceType;
import pl.edu.mimuw.nesc.ast.type.Type;
import pl.edu.mimuw.nesc.declaration.nesc.InterfaceDeclaration;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class InterfaceRefDeclaration extends ObjectDeclaration {

    private final String ifaceName;

    private InterfaceRef astInterfaceRef;
    /**
     * Interface declaration (absent if reference is erroneous).
     */
    private Optional<InterfaceDeclaration> ifaceDeclaration;

    private boolean provides;

    public static Builder builder() {
        return new Builder();
    }

    protected InterfaceRefDeclaration(Builder builder) {
        super(builder);
        this.ifaceName = builder.interfaceName;
        this.astInterfaceRef = builder.astInterfaceRef;
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    public String getIfaceName() {
        return ifaceName;
    }

    public InterfaceRef getAstInterfaceRef() {
        return astInterfaceRef;
    }

    public void setAstInterfaceRef(InterfaceRef astInterfaceRef) {
        this.astInterfaceRef = astInterfaceRef;
    }

    public Optional<InterfaceDeclaration> getIfaceDeclaration() {
        return ifaceDeclaration;
    }

    public void setIfaceDeclaration(Optional<InterfaceDeclaration> ifaceDeclaration) {
        this.ifaceDeclaration = ifaceDeclaration;
    }

    public boolean isProvides() {
        return provides;
    }

    public void setProvides(boolean provides) {
        this.provides = provides;
    }

    /**
     * Builder for the interface reference declaration.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static class Builder extends ObjectDeclaration.Builder<InterfaceRefDeclaration> {
        /**
         * Data needed to build an interface reference declaration.
         */
        private String interfaceName;
        private Optional<List<Optional<Type>>> typeArguments = Optional.absent();
        private InterfaceRef astInterfaceRef;

        protected Builder() {
        }

        /**
         * Set the name of the referred interface.
         *
         * @param interfaceName Name of the referred interface to set.
         * @return <code>this</code>
         */
        public Builder interfaceName(String interfaceName) {
            this.interfaceName = interfaceName;
            return this;
        }

        /**
         * Set the list of type arguments given in the interface reference the
         * declaration object will represent. Value should be absent if no
         * arguments have been given.
         *
         * @param typeArguments List with type arguments to set if it has been
         *                      specified or absent value otherwise.
         * @return <code>this</code>
         */
        public Builder typeArguments(Optional<List<Optional<Type>>> typeArguments) {
            this.typeArguments = typeArguments;
            return this;
        }

        /**
         * Set the AST node that corresponds to the interface reference.
         *
         * @param interfaceRef AST node to be set.
         * @return <code>this</code>
         */
        public Builder astNode(InterfaceRef interfaceRef) {
            this.astInterfaceRef = interfaceRef;
            return this;
        }

        @Override
        protected void beforeBuild() {
            super.beforeBuild();

            if (interfaceName != null) {
                setType(Optional.<Type>of(new InterfaceType(interfaceName, typeArguments)));
            }
            setLinkage(Optional.of(Linkage.NONE));
        }

        @Override
        protected void validate() {
            super.validate();
            checkNotNull(interfaceName, "the interface name cannot be null");
            checkNotNull(typeArguments, "type arguments cannot be null");
            checkNotNull(astInterfaceRef, "the AST node that represents the interface reference cannot be null");
        }

        @Override
        protected InterfaceRefDeclaration create() {
            return new InterfaceRefDeclaration(this);
        }
    }
}
