package pl.edu.mimuw.nesc.declaration.object;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class VariableDeclaration extends ObjectDeclaration {

    public static Builder builder() {
        return new Builder();
    }

    protected VariableDeclaration(Builder builder) {
        super(builder);
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    /**
     * Builder for the variable declaration.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static class Builder extends ExtendedBuilder<VariableDeclaration> {
        @Override
        protected void beforeBuild() {
            super.beforeBuild();
            setKind(ObjectKind.VARIABLE);
        }

        @Override
        protected VariableDeclaration create() {
            return new VariableDeclaration(this);
        }
    }
}
