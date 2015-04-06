package pl.edu.mimuw.nesc.analysis.entityconnection;

import pl.edu.mimuw.nesc.declaration.tag.TagDeclaration;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.symboltable.SymbolTable;

/**
 * Entity connector that looks for tag declarations.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 * @see EntityLinker
 */
public final class TagLinker extends EntityLinker<TagDeclaration> {
    @Override
    protected SymbolTable<TagDeclaration> getSymbolTable(Environment environment) {
        return environment.getTags();
    }
}
