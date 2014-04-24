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
     * Get environment which current one is enclosed in.
     *
     * @return parent
     */
    Optional<Environment> getParent();

    /**
     * Get object table.
     *
     * @return object table
     */
    SymbolTable<ObjectDeclaration> getObjects();

    /**
     * Get tag table.
     *
     * @return tag table
     */
    SymbolTable<TagDeclaration> getTags();

    /**
     * Gets start location of environment.
     *
     * @return start location of <code>Optional.absent()</code> for
     * global scope
     */
    Optional<Location> getStartLocation();

    /**
     * Sets start location of environment.
     *
     * @param location start location
     */
    void setStartLocation(Location location);

    /**
     * Gets end location of environment.
     *
     * @return end location of <code>Optional.absent()</code> for global scope
     */
    Optional<Location> getEndLocation();

    /**
     * Sets end location of environment.
     *
     * @param location end location
     */
    void setEndLocation(Location location);

    /**
     * Get type of environment.
     *
     * @return type of environment
     */
    ScopeType getScopeType();

    /**
     * Sets scope type.
     *
     * @param type scope type
     */
    void setScopeType(ScopeType type);

    /**
     * Adds environment enclosed in current one.
     *
     * @param environment nested environment
     */
    void addEnclosedEnvironment(Environment environment);

    /**
     * Returns the list of environments that are enclosed in current one.
     *
     * @return list of nested environments
     */
    List<Environment> getEnclosedEnvironments();

}
