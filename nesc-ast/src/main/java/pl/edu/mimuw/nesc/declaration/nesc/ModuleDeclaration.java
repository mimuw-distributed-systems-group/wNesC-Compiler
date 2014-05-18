package pl.edu.mimuw.nesc.declaration.nesc;

import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.Module;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class ModuleDeclaration extends NescDeclaration {

    private Module astModule;

    public ModuleDeclaration(String name, Location location) {
        super(name, location);
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    public Module getAstModule() {
        return astModule;
    }

    public void setAstModule(Module astModule) {
        this.astModule = astModule;
    }
}
