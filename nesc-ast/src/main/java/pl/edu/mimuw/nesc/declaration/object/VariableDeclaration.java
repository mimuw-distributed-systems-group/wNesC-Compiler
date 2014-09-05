package pl.edu.mimuw.nesc.declaration.object;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.type.Type;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class VariableDeclaration extends ObjectDeclaration {

    public VariableDeclaration(String name, Location location, Optional<Type> type) {
        super(name, location, type);
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

}
