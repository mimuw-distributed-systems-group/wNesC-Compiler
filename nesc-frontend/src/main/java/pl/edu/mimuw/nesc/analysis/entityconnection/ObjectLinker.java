package pl.edu.mimuw.nesc.analysis.entityconnection;

import pl.edu.mimuw.nesc.declaration.object.ObjectDeclaration;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.symboltable.SymbolTable;

/**
 * Entity connector that connects objects.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 * @see EntityLinker
 */
public final class ObjectLinker extends EntityLinker<ObjectDeclaration> {
    @Override
    protected SymbolTable<ObjectDeclaration> getSymbolTable(Environment environment) {
        return environment.getObjects();
    }
}
