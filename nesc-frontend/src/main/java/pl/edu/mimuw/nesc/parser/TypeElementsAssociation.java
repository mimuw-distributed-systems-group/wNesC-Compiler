package pl.edu.mimuw.nesc.parser;

import pl.edu.mimuw.nesc.analysis.attributes.AttributeAnalyzer;
import pl.edu.mimuw.nesc.analysis.SemanticListener;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.Declarator;
import pl.edu.mimuw.nesc.ast.gen.TypeElement;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.problem.ErrorHelper;
import pl.edu.mimuw.nesc.type.Type;

import com.google.common.base.Optional;
import java.util.LinkedList;
import java.util.List;
import pl.edu.mimuw.nesc.problem.issue.InvalidSpecifiersCombinationError;

import static pl.edu.mimuw.nesc.analysis.SpecifiersAnalysis.NonTypeSpecifier;
import static pl.edu.mimuw.nesc.analysis.SpecifiersAnalysis.SpecifiersSet;
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

    /**
     * Data about main specifiers.
     */
    private Optional<Boolean> isMainSpecifierValid = Optional.absent();
    private Optional<NonTypeSpecifier> mainSpecifier = Optional.absent();

    public TypeElementsAssociation(List<TypeElement> typeElements) {
        checkNotNull(typeElements, "type elements cannot be null");
        this.typeElements = new LinkedList<>(typeElements);
    }

    public LinkedList<TypeElement> getTypeElements() {
        return typeElements;
    }

    public Optional<Type> getType(Environment environment, boolean isStandalone,
                                  ErrorHelper errorHelper, Location apxStartLoc,
                                  Location apxEndLoc, SemanticListener semanticListener,
                                  AttributeAnalyzer attributeAnalyzer) {
        if (type == null) {
            type = resolveBaseType(environment, typeElements, isStandalone, errorHelper,
                                   apxStartLoc, apxEndLoc, semanticListener, attributeAnalyzer);
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
            Location apxStartLoc, Location apxEndLoc,
            SemanticListener semanticListener, AttributeAnalyzer attributeAnalyzer) {

        final Optional<Type> maybeType = getType(environment, false, errorHelper,
                                                 apxStartLoc, apxEndLoc, semanticListener,
                                                 attributeAnalyzer);

        return   maybeType.isPresent() && maybeDeclarator.isPresent()
               ? resolveDeclaratorType(maybeDeclarator.get(), environment,
                        errorHelper, maybeType.get())
               : maybeType;
    }

    public boolean containsMainSpecifier(ErrorHelper errorHelper) {
        analyzeSpecifiers(errorHelper);
        return isMainSpecifierValid.get();
    }

    /**
     * Analyzes the specifiers and determines the main one if it has not been
     * done yet.
     *
     * @param errorHelper Object that will be notified about detected errors.
     * @return The main specifier is if is present and correctly specified.
     *         Otherwise, the value is absent.
     */
    public Optional<NonTypeSpecifier> getMainSpecifier(ErrorHelper errorHelper) {
        analyzeSpecifiers(errorHelper);
        return mainSpecifier;
    }

    private void analyzeSpecifiers(ErrorHelper errorHelper) {
        if (isMainSpecifierValid.isPresent()) {
            return;
        }

        final SpecifiersSet specifiers = new SpecifiersSet(typeElements, errorHelper);
        isMainSpecifierValid = Optional.of(specifiers.goodMainSpecifier());

        if (isMainSpecifierValid.get()) {
            mainSpecifier = specifiers.firstMainSpecifier();
        } else {
            specifiers.emitError(new InvalidSpecifiersCombinationError(specifiers.getMainSpecifiers()));
        }
    }
}
