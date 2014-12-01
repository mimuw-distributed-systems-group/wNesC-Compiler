package pl.edu.mimuw.nesc.environment;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.type.*;
import pl.edu.mimuw.nesc.ast.util.NameMangler;
import pl.edu.mimuw.nesc.declaration.object.*;
import pl.edu.mimuw.nesc.symboltable.SymbolTable;

import java.util.List;

import static com.google.common.base.Preconditions.*;

/**
 * <p>Represents a global environment.</p>
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class TranslationUnitEnvironment extends DefaultEnvironment {

    /**
     * Declaration object that represents the predefined function
     * <code>unique</code>.
     */
    private static final FunctionDeclaration DECLARATION_UNIQUE;
    static {
        final Type[] argsTypes = { new PointerType(new CharType()) };
        final Type uniqueType = new FunctionType(new UnsignedIntType(), argsTypes, false);
        final FunctionDeclaration.Builder declBuilder = FunctionDeclaration.builder();

        declBuilder.uniqueName(NameMangler.getInstance().mangle("unique"))
                .type(uniqueType)
                .linkage(Linkage.EXTERNAL)
                .name("unique")
                .startLocation(Location.getDummyLocation());

        DECLARATION_UNIQUE = declBuilder.build();
        DECLARATION_UNIQUE.setDefined(true);
    }

    /**
     * Declaration object that represents the predefined function
     * <code>uniqueN</code>.
     */
    private static final FunctionDeclaration DECLARATION_UNIQUEN;
    static {
        final Type[] argsTypes = { new PointerType(new CharType()), new UnsignedIntType() };
        final Type uniqueNType = new FunctionType(new UnsignedIntType(), argsTypes, false);
        final FunctionDeclaration.Builder declBuilder = FunctionDeclaration.builder();

        declBuilder.uniqueName(NameMangler.getInstance().mangle("uniqueN"))
                .type(uniqueNType)
                .linkage(Linkage.EXTERNAL)
                .name("uniqueN")
                .startLocation(Location.getDummyLocation());

        DECLARATION_UNIQUEN = declBuilder.build();
        DECLARATION_UNIQUEN.setDefined(true);
    }

    /**
     * Declaration object that represents the predefined function
     * <code>uniqueCount</code>.
     */
    private static final FunctionDeclaration DECLARATION_UNIQUECOUNT;
    static {
        final Type[] argsTypes = { new PointerType(new CharType()) };
        final Type uniqueCountType = new FunctionType(new UnsignedIntType(), argsTypes, false);
        final FunctionDeclaration.Builder declBuilder = FunctionDeclaration.builder();

        declBuilder.uniqueName(NameMangler.getInstance().mangle("uniqueCount"))
                .type(uniqueCountType)
                .linkage(Linkage.EXTERNAL)
                .name("uniqueCount")
                .startLocation(Location.getDummyLocation());

        DECLARATION_UNIQUECOUNT = declBuilder.build();
        DECLARATION_UNIQUECOUNT.setDefined(true);
    }

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

        ok = objectsTable.add(DECLARATION_UNIQUE.getName(), DECLARATION_UNIQUE);
        ok = objectsTable.add(DECLARATION_UNIQUEN.getName(), DECLARATION_UNIQUEN) && ok;
        ok = objectsTable.add(DECLARATION_UNIQUECOUNT.getName(), DECLARATION_UNIQUECOUNT) && ok;

        checkState(ok, "the symbol table contains an object with the name of a NesC constant function");
    }
}
