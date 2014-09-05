package pl.edu.mimuw.nesc.declaration.object;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.type.IntType;
import pl.edu.mimuw.nesc.ast.type.Type;

/**
 * <p>Enumeration constant declaration.</p>
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class ConstantDeclaration extends ObjectDeclaration {

    public ConstantDeclaration(String name, Location location) {
        super(name, location, Optional.of((Type) new IntType(true, false)));
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

}
