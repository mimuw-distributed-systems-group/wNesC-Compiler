package pl.edu.mimuw.nesc.declaration.nesc;

import com.google.common.base.Optional;
import java.util.LinkedList;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.Configuration;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.facade.component.specification.ConfigurationTable;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class ConfigurationDeclaration extends ComponentDeclaration {

    private Configuration astConfiguration;

    /**
     * Object with information about the elements from the specification of this
     * configuration.
     */
    private ConfigurationTable table;

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

    @Override
    public Configuration getAstComponent() {
        return astConfiguration;
    }

    public void setAstConfiguration(Configuration astConfiguration) {
        this.astConfiguration = astConfiguration;
    }

    /**
     * <p>Get the object with information about the elements from the
     * specification of this configuration. It is not null if and only if the
     * specification of the configuration has been already parsed and analyzed.
     * </p>
     *
     * @return The table for this configuration.
     */
    public ConfigurationTable getConfigurationTable() {
        return table;
    }

    public void setConfigurationTable(ConfigurationTable table) {
        checkNotNull(table, "the table cannot be null");
        checkState(this.table == null, "the table has been already set");

        this.table = table;
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
