package pl.edu.mimuw.nesc.declaration.object;

import pl.edu.mimuw.nesc.ast.Location;

/**
 * <p>Component reference.</p>
 * <p><code>name</code> is not the name of component but the name of the
 * reference (component can be aliased).</p>
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class ComponentRefDeclaration extends ObjectDeclaration {

    public ComponentRefDeclaration(String name, Location location) {
        super(name, location);
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

}
