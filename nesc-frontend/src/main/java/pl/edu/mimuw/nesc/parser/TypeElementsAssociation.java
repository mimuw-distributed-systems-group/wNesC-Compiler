package pl.edu.mimuw.nesc.parser;

import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.Declarator;
import pl.edu.mimuw.nesc.ast.gen.TypeElement;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.problem.ErrorHelper;
import pl.edu.mimuw.nesc.ast.type.Type;

import com.google.common.base.Optional;
import java.util.LinkedList;
import java.util.List;

import static pl.edu.mimuw.nesc.analysis.TypesAnalysis.resolveBaseType;
import static pl.edu.mimuw.nesc.analysis.TypesAnalysis.resolveDeclaratorType;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class TypeElementsAssociation {
    /**
     * List with the types element from this association. It is unmodifiable.
     */
    private final LinkedList<TypeElement> typeElements;

    /**
     * The type that corresponds to the given type elements. It may be absent
     * if it is invalid. It is null if and only if it has not been yet resolved.
     */
    private Optional<Type> type = null;

    public TypeElementsAssociation(List<TypeElement> typeElements) {
        checkNotNull(typeElements, "type elements cannot be null");
        this.typeElements = new LinkedList<>(typeElements);
    }

    public LinkedList<TypeElement> getTypeElements() {
        return typeElements;
    }

    public Optional<Type> getType(Environment environment, boolean isStandalone,
                                  ErrorHelper errorHelper, Location apxStartLoc,
                                  Location apxEndLoc) {
        if (type == null) {
            type = resolveBaseType(environment, typeElements, isStandalone, errorHelper,
                                   apxStartLoc, apxEndLoc);
        }

        return type;
    }

    /**
     * Shortcut for using <code>getType</code> method and
     * <code>TypesAnalysis.resolveDeclaratorType</code> at once created for
     * convenience.
     */
    public Optional<Type> resolveType(Optional<Declarator> maybeDeclarator,
            Environment environment, ErrorHelper errorHelper,
            Location apxStartLoc, Location apxEndLoc) {

        final Optional<Type> maybeType = getType(environment, false, errorHelper,
                                                 apxStartLoc, apxEndLoc);

        return   maybeType.isPresent() && maybeDeclarator.isPresent()
               ? resolveDeclaratorType(maybeDeclarator.get(), errorHelper, maybeType.get())
               : maybeType;
    }
}
