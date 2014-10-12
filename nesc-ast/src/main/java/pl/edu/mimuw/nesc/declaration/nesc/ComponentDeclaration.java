package pl.edu.mimuw.nesc.declaration.nesc;

import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.environment.Environment;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public abstract class ComponentDeclaration extends NescDeclaration {

    public ComponentDeclaration(String name, Location location) {
        super(name, location);
    }

    /**
     * Return the specification environment of the NesC component.
     *
     * @return specification environment
     */
    public abstract Environment getSpecificationEnvironment();
}
