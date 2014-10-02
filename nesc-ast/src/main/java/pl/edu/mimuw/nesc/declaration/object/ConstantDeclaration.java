package pl.edu.mimuw.nesc.declaration.object;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.type.IntType;
import pl.edu.mimuw.nesc.ast.type.Type;

/**
 * <p>Enumeration constant declaration.</p>
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class ConstantDeclaration extends ObjectDeclaration {

    public static Builder builder() {
        return new Builder();
    }

    protected ConstantDeclaration(Builder builder) {
        super(builder);
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    /**
     * Builder for the constant declaration.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static class Builder extends ObjectDeclaration.Builder<ConstantDeclaration> {
        @Override
        protected void beforeBuild() {
            super.beforeBuild();

            setType(Optional.<Type>of(new IntType(true, false)));
            setLinkage(Optional.of(Linkage.NONE));
            setKind(ObjectKind.CONSTANT);
        }

        @Override
        protected ConstantDeclaration create() {
            return new ConstantDeclaration(this);
        }
    }
}
