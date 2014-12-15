package pl.edu.mimuw.nesc.environment;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.declaration.object.*;
import pl.edu.mimuw.nesc.declaration.object.unique.UniqueCountDeclaration;
import pl.edu.mimuw.nesc.declaration.object.unique.UniqueDeclaration;
import pl.edu.mimuw.nesc.declaration.object.unique.UniqueNDeclaration;
import pl.edu.mimuw.nesc.symboltable.SymbolTable;

import java.util.List;

import static com.google.common.base.Preconditions.*;

/**
 * <p>Represents a global environment.</p>
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class TranslationUnitEnvironment extends DefaultEnvironment {

    @Override
    public Optional<Environment> getParent() {
        return Optional.absent();
    }

    @Override
    public Optional<Location> getStartLocation() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setStartLocation(Location location) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Location> getEndLocation() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setEndLocation(Location location) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ScopeType getScopeType() {
        return ScopeType.GLOBAL;
    }

    @Override
    public void setScopeType(ScopeType type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addEnclosedEnvironment(Environment environment) {
        this.enclosedEnvironments.add(environment);
    }

    @Override
    public List<Environment> getEnclosedEnvironments() {
        return this.enclosedEnvironments;
    }

    /**
     * <p>Add the declarations of NesC constant functions to this environment.
     * The NesC constant functions are:</p>
     * <ul>
     *     <li><code>unique</code></li>
     *     <li><code>uniqueN</code></li>
     *     <li><code>uniqueCount</code></li>
     * </ul>
     * <p>The declaration objects that represent these functions are shared
     * among all instances of environments.</p>
     *
     * @throws IllegalStateException The symbol table contains an object with
     *                               the same name as one of the constant
     *                               functions.
     */
    public void addConstantFunctions() {
        final SymbolTable<ObjectDeclaration> objectsTable = getObjects();
        boolean ok;

        final UniqueDeclaration uniqueDeclaration = UniqueDeclaration.getInstance();
        final UniqueNDeclaration uniqueNDeclaration = UniqueNDeclaration.getInstance();
        final UniqueCountDeclaration uniqueCountDeclaration = UniqueCountDeclaration.getInstance();

        ok = objectsTable.add(uniqueDeclaration.getName(), uniqueDeclaration);
        ok = objectsTable.add(uniqueNDeclaration.getName(), uniqueNDeclaration) && ok;
        ok = objectsTable.add(uniqueCountDeclaration.getName(), uniqueCountDeclaration) && ok;

        checkState(ok, "the symbol table contains an object with the name of a NesC constant function");
    }
}
