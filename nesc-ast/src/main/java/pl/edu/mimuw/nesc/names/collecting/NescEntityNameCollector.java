package pl.edu.mimuw.nesc.names.collecting;

import com.google.common.collect.FluentIterable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import pl.edu.mimuw.nesc.ast.gen.NescDecl;
import pl.edu.mimuw.nesc.ast.gen.Node;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A collector of names of components and interfaces.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class NescEntityNameCollector implements NameCollector<NescDecl> {
    /**
     * Set of currently collected names.
     */
    private final Set<String> names = new HashSet<>();

    @Override
    public void collect(NescDecl nescDecl) {
        checkNotNull(nescDecl, "NesC declaration AST node cannot be null");
        names.add(nescDecl.getName().getName());
    }

    @Override
    public void collect(Collection<? extends Node> declarations) {
        checkNotNull(declarations, "the collection cannot be null");
        for (Node object : declarations) {
            checkArgument(object != null, "an object in the collection is null");
        }

        final Iterable<NescDecl> nescDeclIt = FluentIterable.from(declarations)
                .filter(NescDecl.class);

        for (NescDecl nescDeclaration : nescDeclIt) {
            names.add(nescDeclaration.getName().getName());
        }
    }

    @Override
    public Set<String> get() {
        return names;
    }
}
