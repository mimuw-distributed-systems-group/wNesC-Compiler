package pl.edu.mimuw.nesc.declaration.object;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import java.util.List;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.FunctionDeclarator;
import pl.edu.mimuw.nesc.declaration.CopyController;
import pl.edu.mimuw.nesc.type.Type;
import pl.edu.mimuw.nesc.astutil.AstUtils;

import java.util.LinkedList;

import static com.google.common.base.Preconditions.*;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class FunctionDeclaration extends ObjectDeclaration {

    public static enum FunctionType {
        IMPLICIT, NORMAL, STATIC, NESTED, COMMAND, EVENT,
        /**
         * FIXME: in nesc compiler function type does not contain task value.
         * But it is convenient to use in proposal completion.
         */
        TASK,
    }

    private final Optional<String> ifaceName;

    /**
     * Globally unique name of this function.
     */
    private final String uniqueName;

    private FunctionDeclarator astFunctionDeclarator;
    private FunctionType functionType;

    /**
     * Indicates whether function definition (do not be confused with
     * declaration!) have been already parsed.
     */
    private boolean isDefined;

    /**
     * <p>Immutable list with instance parameters if this declaration represents
     * a parameterised bare command or event or an implementation of a command
     * or event from a parameterised interface. The list shall be present if the
     * declaration represents a function declared in one of the following ways
     * (instance parameters are indicated by arrows):</p>
     *
     * <pre>
     *     command result_t Send.send[uint8_t id](uint8_t length, TOS_Msg* data) { &hellip; }
     *                                ▲        ▲
     *                                |        |
     *                                |        |
     * </pre>
     * <pre>
     *     provides command void sent[uint8_t id](int x);
     *                                ▲        ▲
     *                                |        |
     *                                |        |
     * </pre>
     */
    private final Optional<ImmutableList<Optional<Type>>> instanceParameters;

    /**
     * <p>Value is present if this declaration represents a bare command or
     * event that is declared in a component specification, e.g.:</p>
     *
     * <pre>
     *     provides command uint8_t getCounterHigh();
     * </pre>
     */
    private Optional<Boolean> isProvided = Optional.absent();

    /**
     * Assumptions that can be made about calls to the function.
     */
    private CallAssumptions callAssumptions = CallAssumptions.NONE;

    /**
     * Normal call assumptions cannot be set to weaker than these.
     */
    private CallAssumptions minimalCallAssumptions = CallAssumptions.NONE;

    public static Builder builder() {
        return new Builder();
    }

    protected FunctionDeclaration(FunctionDeclarationBuilder<?> builder) {
        super(builder);
        this.ifaceName = builder.interfaceName;
        this.isDefined = builder.isDefined;
        this.functionType = builder.functionType.orNull();
        this.instanceParameters = builder.buildInstanceParameters();
        this.uniqueName = builder.uniqueName;
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    @Override
    public String getName() {
        if (ifaceName.isPresent()) {
            return String.format("%s.%s", ifaceName.get(), name);
        }
        return name;
    }

    public Optional<String> getIfaceName() {
        return ifaceName;
    }

    public String getFunctionName() {
        return name;
    }

    public FunctionDeclarator getAstFunctionDeclarator() {
        return astFunctionDeclarator;
    }

    public void setAstFunctionDeclarator(FunctionDeclarator functionDeclarator) {
        this.astFunctionDeclarator = functionDeclarator;
    }

    public FunctionType getFunctionType() {
        return functionType;
    }

    public void setFunctionType(FunctionType functionType) {
        this.functionType = functionType;
    }

    public boolean isDefined() {
        return isDefined;
    }

    public void setDefined(boolean isDefined) {
        this.isDefined = isDefined;
    }

    /**
     * Get the instance parameters that can be present if this object represents
     * a parameterised bare command or event or an implementation of a command
     * or event from a parameterised interface.
     *
     * @return Immutable list with instance parameters if they are present.
     * @see FunctionDeclaration#instanceParameters
     */
    public Optional<ImmutableList<Optional<Type>>> getInstanceParameters() {
        return instanceParameters;
    }

    /**
     * <p>Check if this declaration represents a bare command or event declared
     * in the specification of a component and if it is to be implemented in the
     * component.</p>
     *
     * @return The value is present if this declaration represents a bare
     *         command or event declared in the specification of a component and
     *         is <code>true</code> if and only if the component is to implement
     *         the command or event. The value is not meaningful and is never
     *         present before the specification of the component is fully parsed
     *         and analyzed.
     */
    public Optional<Boolean> isProvided() {
        return isProvided;
    }

    /**
     * <p>Store the information that this declaration object represents a bare
     * command or event declared in the specification of a component and the
     * given value indicating if the command or event has to be implemented.</p>
     *
     * @param isProvided Value indicating if the bare command or event
     *                   represented by this declaration is to be implemented
     *                   (if so the argument shall be <code>true</code>).
     * @throws IllegalStateException The value has been already set.
     */
    public void setProvided(boolean isProvided) {
        checkState(!this.isProvided.isPresent(), "the value indicating if the command or event is to be implemented has been already set");
        this.isProvided = Optional.of(isProvided);
    }

    /**
     * Get the globally unique name of this function.
     *
     * @return The globally unique name for this function.
     */
    public String getUniqueName() {
        return uniqueName;
    }

    /**
     * Get the assumptions that can be made about calls to the function.
     *
     * @return The call assumptions.
     * @see CallAssumptions
     */
    public CallAssumptions getCallAssumptions() {
        return callAssumptions;
    }

    /**
     * Get the call assumptions that are certain for this function.
     *
     * @return Certain call assumptions for this function.
     */
    public CallAssumptions getMinimalCallAssumptions() {
        return minimalCallAssumptions;
    }

    /**
     * Set the call assumptions for the function. The assumptions can only be
     * set to the greater or equal to the current minimal assumptions in its
     * natural ordering.
     *
     * @param newCallAssumptions New call assumptions to set.
     * @throws IllegalArgumentException The given assumptions are less than the
     *                                  current minimal assumptions in their
     *                                  natural ordering.
     */
    public void setCallAssumptions(CallAssumptions newCallAssumptions) {
        checkNotNull(newCallAssumptions, "call assumptions cannot be null");
        checkArgument(newCallAssumptions.compareTo(minimalCallAssumptions) >= 0,
                "call assumptions cannot be less than the current minimal assumptions");

        this.callAssumptions = newCallAssumptions;
    }

    /**
     * Set the minimal call assumptions to the given ones. Normal assumptions,
     * if weaker, are also set to the given value.
     *
     * @param newMinimalCallAssumptions New call assumptions to set.
     * @throws IllegalArgumentException The given assumptions are less than the
     *                                  current minimal assumptions in their
     *                                  natural ordering.
     */
    public void setMinimalCallAssumptions(CallAssumptions newMinimalCallAssumptions) {
        checkNotNull(newMinimalCallAssumptions, "new minimal call assumptions cannot be null");
        checkArgument(newMinimalCallAssumptions.compareTo(minimalCallAssumptions) >= 0,
                "minimal call assumptions cannot be less than the current minimal assumptions");

        this.minimalCallAssumptions = newMinimalCallAssumptions;
        if (newMinimalCallAssumptions.compareTo(this.callAssumptions) > 0) {
            this.callAssumptions = newMinimalCallAssumptions;
        }
    }

    @Override
    public FunctionDeclaration deepCopy(CopyController controller) {
        final FunctionDeclaration newDeclaration = FunctionDeclaration.builder()
                .functionType(this.functionType)
                .interfaceName(this.ifaceName.orNull())
                .instanceParameters(controller.mapTypes(this.instanceParameters).orNull())
                .uniqueName(controller.mapUniqueName(this.uniqueName))
                .linkage(this.linkage.orNull())
                .type(controller.mapType(this.type).orNull())
                .name(this.name)
                .startLocation(this.location)
                .build();

        if (this.astFunctionDeclarator != null) {
            newDeclaration.astFunctionDeclarator = controller.mapNode(this.astFunctionDeclarator);
        }
        newDeclaration.isDefined = this.isDefined;
        newDeclaration.isProvided = this.isProvided;
        newDeclaration.callAssumptions = this.callAssumptions;

        return newDeclaration;
    }

    /**
     * <p>Enum type that represents assumptions that can be made about calls to
     * a function. They correspond to predefined NesC attributes:
     * <code>spontaneous</code>, <code>hwevent</code> and
     * <code>atomic_hwevent</code>.</p>
     *
     * <p>The order in which the constants are declared is the order of increasing
     * count of assumptions that are implied by each constant. It can be used to
     * compare constants using {@link pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration.CallAssumptions#compareTo compareTo}.</p>
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static enum CallAssumptions {
        /**
         * No assumptions about calls to the function.
         */
        NONE,

        /**
         * There are calls to the function that are not visible in the source code.
         * Corresponds to the predefined NesC attribute with the same name.
         */
        SPONTANEOUS,

        /**
         * The function is an interrupt handler and there are calls to the function
         * invisible in the source code. Corresponds to the predefined NesC
         * attribute with the same name.
         */
        HWEVENT,

        /**
         * The function is an interrupt handler executed atomically and there are
         * calls to the function invisible in the source code. Corresponds to the
         * predefined NesC attribute with the same name.
         */
        ATOMIC_HWEVENT,
    }

    /**
     * Abstract builder for a function declaration.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static abstract class FunctionDeclarationBuilder<T extends FunctionDeclaration>
            extends ExtendedBuilder<T> {
        /**
         * Data needed to build a function declaration.
         */
        private Optional<String> interfaceName = Optional.absent();
        private Optional<FunctionType> functionType = Optional.absent();
        private Optional<LinkedList<Declaration>> instanceParametersFromDecls = Optional.absent();
        private Optional<List<Optional<Type>>> instanceParametersFromTypes = Optional.absent();
        private boolean isDefined = false;
        private String uniqueName;

        protected FunctionDeclarationBuilder() {
        }

        /**
         * Set the interface name that the function depicted by the declaration
         * object comes from.
         *
         * @param interfaceName Name of the interface to set.
         * @return <code>this</code>
         */
        public FunctionDeclarationBuilder<T> interfaceName(String interfaceName) {
            this.interfaceName = Optional.fromNullable(interfaceName);
            return this;
        }

        /**
         * Set the type of the function that the declaration object will
         * represent. <code>null</code> is a legal argument value and can be
         * used for a currently unknown function type.
         *
         * @param type Type of the function to set.
         * @return <code>this</code>
         */
        public FunctionDeclarationBuilder<T> functionType(FunctionType type) {
            this.functionType = Optional.fromNullable(type);
            return this;
        }

        /**
         * <p>Set the instance parameters if this declaration represents
         * a command or event (either parameterised bare or a command or event
         * from a parameterised interface). <code>null</code> is a legal value
         * and means that no instance parameters are present. Setting instance
         * parameters for anything other than a command or event is not
         * correct.</p>
         * <p>Call to this method discards all previously set instance
         * parameters.</p>
         *
         * @param instanceParams Instance parameters for the declaration object.
         * @return <code>this</code>
         */
        public FunctionDeclarationBuilder<T> instanceParameters(LinkedList<Declaration> instanceParams) {
            this.instanceParametersFromDecls = Optional.fromNullable(instanceParams);
            this.instanceParametersFromTypes = Optional.absent();
            return this;
        }

        /**
         * <p>Acts as {@link FunctionDeclaration.Builder#instanceParameters(LinkedList)}.</p>
         *
         * @param instanceParams Instance parameters for the declaration object.
         * @return <code>this</code>
         */
        public FunctionDeclarationBuilder<T> instanceParameters(List<Optional<Type>> instanceParams) {
            this.instanceParametersFromTypes = Optional.fromNullable(instanceParams);
            this.instanceParametersFromDecls = Optional.absent();
            return this;
        }

        /**
         * Set the unique name for the function.
         *
         * @param uniqueName Unique name to set.
         * @return <code>this</code>
         */
        public FunctionDeclarationBuilder<T> uniqueName(String uniqueName) {
            this.uniqueName = uniqueName;
            return this;
        }

        @Override
        protected void beforeBuild() {
            super.beforeBuild();
            setKind(ObjectKind.FUNCTION);
        }

        @Override
        protected void validate() {
            super.validate();

            checkNotNull(interfaceName, "the interface name cannot be null");
            checkNotNull(functionType, "function type cannot be null");
            checkNotNull(uniqueName, "unique name cannot be null");
            checkState(!uniqueName.isEmpty(), "unique name cannot be an empty string");
        }

        private Optional<ImmutableList<Optional<Type>>> buildInstanceParameters() {
            if (instanceParametersFromDecls.isPresent()) {
                return Optional.of(AstUtils.getTypes(instanceParametersFromDecls.get()));
            } else if (instanceParametersFromTypes.isPresent()) {
                return Optional.of(ImmutableList.copyOf(instanceParametersFromTypes.get()));
            } else {
                return Optional.absent();
            }
        }
    }

    /**
     * Builder for only a function declaration object.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static class Builder extends FunctionDeclarationBuilder<FunctionDeclaration> {
        @Override
        protected FunctionDeclaration create() {
            return new FunctionDeclaration(this);
        }
    }
}
