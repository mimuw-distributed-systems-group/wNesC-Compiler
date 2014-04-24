package pl.edu.mimuw.nesc.declaration.object;

import pl.edu.mimuw.nesc.ast.Location;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class InterfaceRefDeclaration extends ObjectDeclaration {

    private final String interfaceRefName;

    public InterfaceRefDeclaration(String interfaceRefName, String name, Location location) {
        super(name, location);
        this.interfaceRefName = interfaceRefName;
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    public String getInterfaceRefName() {
        return interfaceRefName;
    }
}
