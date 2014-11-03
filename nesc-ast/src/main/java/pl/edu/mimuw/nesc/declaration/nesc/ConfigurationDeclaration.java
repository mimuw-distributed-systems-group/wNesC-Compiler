package pl.edu.mimuw.nesc.declaration.nesc;

import com.google.common.base.Optional;
import java.util.LinkedList;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.Configuration;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.environment.Environment;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class ConfigurationDeclaration extends ComponentDeclaration {

    private Configuration astConfiguration;

    public ConfigurationDeclaration(String name, Location location) {
        super(name, location);
    }

    @Override
    public Environment getParameterEnvironment() {
        return astConfiguration.getParameterEnvironment();
    }

    @Override
    public Environment getSpecificationEnvironment() {
        return astConfiguration.getSpecificationEnvironment();
    }

    @Override
    public Optional<LinkedList<Declaration>> getGenericParameters() {
        return astConfiguration.getParameters();
    }

    public Configuration getAstConfiguration() {
        return astConfiguration;
    }

    public void setAstConfiguration(Configuration astConfiguration) {
        this.astConfiguration = astConfiguration;
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
