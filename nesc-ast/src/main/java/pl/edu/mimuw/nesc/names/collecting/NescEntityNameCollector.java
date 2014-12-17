package pl.edu.mimuw.nesc.names.collecting;

import com.google.common.collect.FluentIterable;
import java.util.Collection;
import pl.edu.mimuw.nesc.ast.gen.NescDecl;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A collector of names of components and interfaces.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class NescEntityNameCollector extends AbstractNameCollector<NescDecl> {
    @Override
    public void collect(NescDecl nescDecl) {
        checkNotNull(nescDecl, "NesC declaration AST node cannot be null");
        names.add(nescDecl.getName().getName());
    }

    @Override
    public void collect(Collection<?> objects) {
        checkCollection(objects);

        final Iterable<NescDecl> nescDeclIt = FluentIterable.from(objects)
                .filter(NescDecl.class);

        for (NescDecl nescDeclaration : nescDeclIt) {
            names.add(nescDeclaration.getName().getName());
        }
    }
}
