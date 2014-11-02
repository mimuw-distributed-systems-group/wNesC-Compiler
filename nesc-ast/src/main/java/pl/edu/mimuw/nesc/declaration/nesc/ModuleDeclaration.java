package pl.edu.mimuw.nesc.declaration.nesc;

import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.Module;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.facade.component.ModuleTable;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class ModuleDeclaration extends ComponentDeclaration {

    private Module astModule;

    /**
     * Table that contains information about commands and events that this
     * module must or can implement.
     */
    private ModuleTable moduleTable;

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

    public Module getAstModule() {
        return astModule;
    }

    public void setAstModule(Module astModule) {
        this.astModule = astModule;
    }

    /**
     * <p>Get the table of this module with information about commands and
     * events that this module must or can implement. It contains full
     * information after parsing and analyzing the whole module.</p>
     *
     * <p>The object is present after parsing and analyzing the specification of
     * this module.</p>
     *
     * @return Module table object for this module.
     */
    public ModuleTable getModuleTable() {
        return moduleTable;
    }

    public void setModuleTable(ModuleTable moduleTable) {
        this.moduleTable = moduleTable;
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
