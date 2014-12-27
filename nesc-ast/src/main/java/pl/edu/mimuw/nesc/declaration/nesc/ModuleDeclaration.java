package pl.edu.mimuw.nesc.declaration.nesc;

import com.google.common.base.Optional;
import java.util.LinkedList;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.Module;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.facade.component.specification.ModuleTable;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class ModuleDeclaration extends ComponentDeclaration {

    private Module astModule;

    public ModuleDeclaration(String name, Location location) {
        super(name, location);
    }

    @Override
    public Environment getParameterEnvironment() {
        return astModule.getParameterEnvironment();
    }

    @Override
    public Environment getSpecificationEnvironment() {
        return astModule.getSpecificationEnvironment();
    }

    @Override
    public Optional<LinkedList<Declaration>> getGenericParameters() {
        return astModule.getParameters();
    }

    @Override
    public Module getAstComponent() {
        return astModule;
    }

    public void setAstModule(Module astModule) {
        this.astModule = astModule;
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
