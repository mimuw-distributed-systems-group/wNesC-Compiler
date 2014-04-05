package pl.edu.mimuw.nesc.environment;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents global environment. Besides environment for objects and tags,
 * it contains nesc entities table (there exists only global scope).
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class NescEnvironment {

    private final Map<String, Object> entities;
    private final PartitionedEnvironment global;

    public NescEnvironment() {
        this.entities = new HashMap<>();
        this.global = new PartitionedEnvironment();
    }

    public Map<String, Object> getEntities() {
        return entities;
    }

    public PartitionedEnvironment getGlobal() {
        return global;
    }
}
