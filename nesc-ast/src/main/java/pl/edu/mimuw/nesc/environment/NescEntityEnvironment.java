package pl.edu.mimuw.nesc.environment;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.declaration.nesc.NescDeclaration;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class NescEntityEnvironment {

    private final Map<String, NescDeclaration> entities;

    public NescEntityEnvironment() {
        this.entities = new HashMap<>();
    }

    public boolean add(String name, NescDeclaration declaration) {
        if (this.entities.containsKey(name)) {
            return false;
        }

        this.entities.put(name, declaration);
        return true;
    }

    public Optional<? extends NescDeclaration> get(String name) {
        return Optional.fromNullable(this.entities.get(name));
    }

    public void remove(String name) {
        this.entities.remove(name);
    }

}
