package pl.edu.mimuw.nesc.environment;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.declaration.label.LabelDeclaration;
import pl.edu.mimuw.nesc.declaration.object.ObjectDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.TagDeclaration;
import pl.edu.mimuw.nesc.symboltable.LabelSymbolTable;
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
     * Get the labels table. There is a separate labels table for each compound
     * statement to support local labels from GCC:
     *
     * https://gcc.gnu.org/onlinedocs/gcc/Local-Labels.html
     *
     * Objects in the symbol table that represent local labels are located in
     * the symbol table of the block they are declared in. Objects that
     * represent normal labels are added to the symbol table that corresponds to
     * the body compound statement of the least nested function (if there are
     * nested functions) - the function top-level compound statement.
     *
     * @return The labels table. The object is absent if the current scope
     *         does not allow declarations of labels.
     */
    Optional<? extends LabelSymbolTable<LabelDeclaration>> getLabels();

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

    /**
     * Check if this environment is located inside a NesC entity, i.e.
     * a component or interface.
     *
     * @return <code>true</code> if and only if this environment is located
     *         inside a NesC entity.
     */
    boolean isEnclosedInNescEntity();

    /**
     * Set the name of the NesC entity this environment is located inside.
     *
     * @param nescEntityName Name of the NesC entity to set.
     * @throws IllegalStateException The NesC entity name is already set.
     */
    void setNescEntityName(String nescEntityName);

    /**
     * If this environment is located inside a NesC entity, the name is present
     * and it is the name of the entity. Otherwise, it is absent.
     *
     * @return Name of the NesC entity this environment is located inside.
     */
    Optional<String> getNescEntityName();

    /**
     * Check if this environment is an environment of a NesC generic component
     * or a generic interface.
     *
     * @return <code>true</code> if and only if this environment is an
     *         environment located inside a NesC generic component or a generic
     *         interface.
     */
    boolean isEnclosedInGenericNescEntity();

    /**
     * Set if this environment is an environment of a NesC generic component
     * or a generic interface.
     *
     * @param isEnclosed Value to set, indicating if this environment is located
     *                   inside a generic component or a generic interface.
     */
    void setEnclosedInGenericNescEntity(boolean isEnclosed);

    /**
     * Check if there is an object declared in this environment or one of the
     * enclosing environments and if the declaration is located inside a NesC
     * entity.
     *
     * @param name Name of the object to look for.
     * @return <code>true</code> if and only if an object with given name is
     *         declared in this or one of parent environments and the first
     *         declaration found is located inside a NesC entity.
     * @throws NullPointerException Name is <code>null</code>.
     * @throws IllegalArgumentException Name is an empty string.
     */
    boolean isObjectDeclaredInsideNescEntity(String name);

    /**
     * Check if there is a tag declared in this environment or one of parent
     * environments and if its declaration is located inside a NesC entity.
     *
     * @param name Name of the tag to look for.
     * @return <code>true</code> if and only if an object with given name is
     *         declared in this or one of parent environments and the first
     *         declaration found is located inside a NesC entity.
     */
    boolean isTagDeclaredInsideNescEntity(String name);

    /**
     * Check if this environment is nested inside an environment with the given
     * scope type.
     *
     * @param scopeType A type of a scope.
     * @return <code>true</code> if and only if this environment is nested in an
     *         environment with given scope type or has the given scope type.
     * @throws NullPointerException Scope type is <code>null</code>.
     */
    boolean isEnclosedInScope(ScopeType scopeType);
}
