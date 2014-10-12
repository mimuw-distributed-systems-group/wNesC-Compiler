package pl.edu.mimuw.nesc.declaration.nesc;

import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.Interface;
import pl.edu.mimuw.nesc.environment.Environment;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class InterfaceDeclaration extends NescDeclaration {

    private Interface astInterface;

    public InterfaceDeclaration(String name, Location location) {
        super(name, location);
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    public Interface getAstInterface() {
        return astInterface;
    }

    public void setAstInterface(Interface astInterface) {
        this.astInterface = astInterface;
    }

    @Override
    public Environment getParameterEnvironment() {
        return astInterface.getParameterEnvironment();
    }


    /**
     * Returns the environment containing NesC interface's declarations.
     *
     * @return declaration environment
     */
    public Environment getDeclarationEnvironment() {
        return astInterface.getDeclarationEnvironment();
    }
}
