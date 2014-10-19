package pl.edu.mimuw.nesc.declaration.object;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import pl.edu.mimuw.nesc.ast.gen.DataDecl;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.ErrorDecl;
import pl.edu.mimuw.nesc.ast.gen.InterfaceRef;
import pl.edu.mimuw.nesc.ast.gen.VariableDecl;
import pl.edu.mimuw.nesc.ast.type.InterfaceType;
import pl.edu.mimuw.nesc.ast.type.Type;
import pl.edu.mimuw.nesc.declaration.nesc.InterfaceDeclaration;

import java.util.LinkedList;
import java.util.List;

import static com.google.common.base.Preconditions.*;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class InterfaceRefDeclaration extends ObjectDeclaration {

    private final String ifaceName;

    private InterfaceRef astInterfaceRef;
    /**
     * Interface declaration (absent if reference is erroneous).
     */
    private Optional<InterfaceDeclaration> ifaceDeclaration;

    private boolean provides;

    /**
     * <p>Immutable list with types of instance parameters of this interface
     * reference. It is absent if this is not a parameterised interface.
     * A type is absent if it is incorrectly specified. Instance parameters
     * are considered the parameters in brackets, e.g.:</p>
     *
     * <pre>
     *  provides interface SendMsg[uint8_t id];
     *                             ▲        ▲
     *                             |        |
     *                             |        |
     *                             |        |
     * </pre>
     */
    private final Optional<ImmutableList<Optional<Type>>> instanceParameters;

    public static Builder builder() {
        return new Builder();
    }

    protected InterfaceRefDeclaration(Builder builder) {
        super(builder);
        this.ifaceName = builder.interfaceName;
        this.astInterfaceRef = builder.astInterfaceRef;
        this.instanceParameters = builder.buildInstanceParameters();
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
     * Get a list with instance parameters of this interface. More information
     * about the returned list are
     * {@link InterfaceRefDeclaration#instanceParameters here}.
     *
     * @return Immutable list with instance parameters.
     */
    public Optional<ImmutableList<Optional<Type>>> getInstanceParameters() {
        return instanceParameters;
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
            setKind(ObjectKind.INTERFACE);
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

        private Optional<ImmutableList<Optional<Type>>> buildInstanceParameters() {
            if (!astInterfaceRef.getGenericParameters().isPresent()) {
                return Optional.absent();
            }

            final LinkedList<Declaration> declarations = astInterfaceRef.getGenericParameters().get();
            final ImmutableList.Builder<Optional<Type>> typesBuilder = ImmutableList.builder();

            for (Declaration declaration : declarations) {
                if (declaration instanceof ErrorDecl) {
                    continue;
                }

                checkState(declaration instanceof DataDecl, "unexpected instance parameter declaration class '%s'",
                        declaration.getClass().getCanonicalName());

                final DataDecl dataDecl = (DataDecl) declaration;
                final LinkedList<Declaration> dataDeclDeclarations = dataDecl.getDeclarations();
                checkState(dataDeclDeclarations.size() == 1, "unexpected declarations count %d", dataDeclDeclarations.size());

                final Declaration innerDeclaration = dataDeclDeclarations.getFirst();
                checkState(innerDeclaration instanceof VariableDecl, "unexpected inner instance parameter declaration class '%s'",
                        innerDeclaration.getClass().getCanonicalName());

                final VariableDecl variableDecl = (VariableDecl) innerDeclaration;
                typesBuilder.add(variableDecl.getType());
            }

            return Optional.of(typesBuilder.build());
        }
    }
}
