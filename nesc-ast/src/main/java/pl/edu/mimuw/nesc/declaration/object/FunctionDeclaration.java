package pl.edu.mimuw.nesc.declaration.object;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.gen.FunctionDeclarator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
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

    public static Builder builder() {
        return new Builder();
    }

    protected FunctionDeclaration(Builder builder) {
        super(builder);
        this.ifaceName = builder.interfaceName;
        this.isDefined = builder.isDefined;
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
     * Builder for a function declaration.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static class Builder extends ExtendedBuilder<FunctionDeclaration> {
        /**
         * Data needed to build a function declaration.
         */
        private Optional<String> interfaceName = Optional.absent();
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

        @Override
        protected void validate() {
            super.validate();
            checkNotNull(interfaceName, "the interface name cannot be null");
        }

        @Override
        protected FunctionDeclaration create() {
            return new FunctionDeclaration(this);
        }
    }
}
