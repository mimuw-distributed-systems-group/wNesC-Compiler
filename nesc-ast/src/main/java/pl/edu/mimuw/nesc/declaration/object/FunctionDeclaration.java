package pl.edu.mimuw.nesc.declaration.object;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.FunctionDeclarator;
import pl.edu.mimuw.nesc.ast.type.Type;
import pl.edu.mimuw.nesc.ast.util.AstUtils;

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

    public static Builder builder() {
        return new Builder();
    }

    protected FunctionDeclaration(Builder builder) {
        super(builder);
        this.ifaceName = builder.interfaceName;
        this.isDefined = builder.isDefined;
        this.functionType = builder.functionType.orNull();
        this.instanceParameters = builder.buildInstanceParameters();
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
     * Builder for a function declaration.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static class Builder extends ExtendedBuilder<FunctionDeclaration> {
        /**
         * Data needed to build a function declaration.
         */
        private Optional<String> interfaceName = Optional.absent();
        private Optional<FunctionType> functionType = Optional.absent();
        private Optional<LinkedList<Declaration>> instanceParameters = Optional.absent();
        private boolean isDefined = false;

        protected Builder() {
        }

        /**
         * Set the interface name that the function depicted by the declaration
         * object comes from.
         *
         * @param interfaceName Name of the interface to set.
         * @return <code>this</code>
         */
        public Builder interfaceName(String interfaceName) {
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
        public Builder functionType(FunctionType type) {
            this.functionType = Optional.fromNullable(type);
            return this;
        }

        /**
         * Set the instance parameters if this declaration represents a command
         * or event (either parameterised bare or a command or event from
         * a parameterised interface). <code>null</code> is a legal value and
         * means that no instance parameters are present. Setting instance
         * parameters for anything other than a command or event is not correct.
         *
         * @param instanceParams Instance parameters for the declaration object.
         * @return <code>this</code>
         */
        public Builder instanceParameters(LinkedList<Declaration> instanceParams) {
            this.instanceParameters = Optional.fromNullable(instanceParams);
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
        }

        @Override
        protected FunctionDeclaration create() {
            return new FunctionDeclaration(this);
        }

        private Optional<ImmutableList<Optional<Type>>> buildInstanceParameters() {
            return instanceParameters.isPresent()
                    ? Optional.of(AstUtils.getTypes(instanceParameters.get()))
                    : Optional.<ImmutableList<Optional<Type>>>absent();
        }
    }
}
