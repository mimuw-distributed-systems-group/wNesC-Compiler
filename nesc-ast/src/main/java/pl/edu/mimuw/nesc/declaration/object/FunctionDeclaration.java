package pl.edu.mimuw.nesc.declaration.object;

import pl.edu.mimuw.nesc.ast.Location;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class FunctionDeclaration extends ObjectDeclaration {

    public FunctionDeclaration(String name, Location location) {
        super(name, location);
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

}
