package pl.edu.mimuw.nesc.declaration.nesc;

import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.Configuration;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class ConfigurationDeclaration extends NescDeclaration {

    private Configuration astConfiguration;

    public ConfigurationDeclaration(String name, Location location) {
        super(name, location);
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    public Configuration getAstConfiguration() {
        return astConfiguration;
    }

    public void setAstConfiguration(Configuration astConfiguration) {
        this.astConfiguration = astConfiguration;
    }
}
