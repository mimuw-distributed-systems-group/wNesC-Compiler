package pl.edu.mimuw.nesc.declaration.nesc;

import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.Interface;
import pl.edu.mimuw.nesc.environment.Environment;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class InterfaceDeclaration extends NescDeclaration {

    private Interface astInterface;

    private Environment parameterEnvironment;
    private Environment declarationEnvironment;

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

    public Environment getParameterEnvironment() {
        return parameterEnvironment;
    }

    public void setParameterEnvironment(Environment parameterEnvironment) {
        this.parameterEnvironment = parameterEnvironment;
    }

    public Environment getDeclarationEnvironment() {
        return declarationEnvironment;
    }

    public void setDeclarationEnvironment(Environment declarationEnvironment) {
        this.declarationEnvironment = declarationEnvironment;
    }
}
