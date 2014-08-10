package pl.edu.mimuw.nesc.environment;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.declaration.object.ObjectDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.TagDeclaration;
import pl.edu.mimuw.nesc.symboltable.SymbolTable;

import java.util.List;

/**
 * <p>Environment represents a stack of scopes of objects and tags.</p>
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public interface Environment {

    /**
     * Get the environment which the current one is enclosed in.
     *
     * @return parent
     */
    Optional<Environment> getParent();

    /**
     * Get the object table.
     *
     * @return object table
     */
    SymbolTable<ObjectDeclaration> getObjects();

    /**
     * Get the tag table.
     *
     * @return tag table
     */
    SymbolTable<TagDeclaration> getTags();

    /**
     * Gets the start location of the environment.
     *
     * @return start location of <code>Optional.absent()</code> for
     * global scope
     */
    Optional<Location> getStartLocation();

    /**
     * Sets the start location of the environment. The location cannot be null.
     *
     * @param location start location
     */
    void setStartLocation(Location location);

    /**
     * Gets the end location of the environment.
     *
     * @return end location of <code>Optional.absent()</code> for global scope
     */
    Optional<Location> getEndLocation();

    /**
     * Sets the end location of the environment. The location can be null.
     *
     * @param location end location
     */
    void setEndLocation(Location location);

    /**
     * Get the type of the environment.
     *
     * @return the type of the environment
     */
    ScopeType getScopeType();

    /**
     * Sets the scope type.
     *
     * @param type scope type
     */
    void setScopeType(ScopeType type);

    /**
     * Adds an environment enclosed in the current one.
     *
     * @param environment nested environment
     */
    void addEnclosedEnvironment(Environment environment);

    /**
     * Returns the list of environments that are enclosed in the current one.
     *
     * @return list of nested environments
     */
    List<Environment> getEnclosedEnvironments();
}
