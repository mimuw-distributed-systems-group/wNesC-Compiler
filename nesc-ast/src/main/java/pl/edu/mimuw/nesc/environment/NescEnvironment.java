package pl.edu.mimuw.nesc.environment;

import pl.edu.mimuw.nesc.declaration.nesc.NescDeclaration;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents global environment. Besides environment for objects and tags,
 * it contains nesc entities table (there exists only global scope).
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class NescEnvironment {

    private final NescEntityEnvironment entities;
    private final PartitionedEnvironment global;

    public NescEnvironment() {
        this.entities = new NescEntityEnvironment();
        this.global = new PartitionedEnvironment();
    }

    public NescEntityEnvironment getEntities() {
        return entities;
    }

    public PartitionedEnvironment getGlobal() {
        return global;
    }
}
