package pl.edu.mimuw.nesc.analysis.entityconnection;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import pl.edu.mimuw.nesc.ast.gen.Attribute;
import pl.edu.mimuw.nesc.ast.gen.ErrorExpr;
import pl.edu.mimuw.nesc.ast.gen.InitList;
import pl.edu.mimuw.nesc.ast.gen.NescAttribute;
import pl.edu.mimuw.nesc.astutil.predicates.AttributePredicates;
import pl.edu.mimuw.nesc.declaration.Declaration;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.environment.ScopeType;
import pl.edu.mimuw.nesc.symboltable.SymbolTable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Class of objects responsible for finding entities in the global scope that
 * are also declared in implementation scopes of modules. It is possible if
 * a declaration in a module is annotated with @C() attribute.</p>
 *
 * @param <T> Class of the declaration object from the global scope to find.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
abstract class EntityLinker<T extends Declaration> {
    /**
     * Get the symbol table to be checked for the declaration object from the
     * given environment.
     *
     * @param environment Environment to get the symbol table form.
     * @return The symbol table to check the object.
     */
    protected abstract SymbolTable<T> getSymbolTable(Environment environment);

    /**
     * Look for the declaration object of the entity with the given name in the
     * global scope if it is annotated with @C() attribute.
     *
     * @param entityName Name of the entity.
     * @param environment Environment of the declaration of the entity.
     * @param attributes Attributes applied to the entity.
     * @return Declaration object from the global scope that represents the
     *         entity with the given name.
     */
    public Optional<? extends T> link(String entityName, Iterable<? extends Attribute> attributes,
            Environment environment) {
        checkNotNull(entityName, "entity name cannot be null");
        checkNotNull(attributes, "attributes cannot be null");
        checkNotNull(environment, "environment cannot be null");
        checkArgument(!entityName.isEmpty(), "entity name cannot be an empty string");

        if (!checkCAttributeUsage(attributes, environment)) {
            return Optional.absent();
        }

        // Look for the global environment
        while (environment.getScopeType() != ScopeType.GLOBAL) {
            environment = environment.getParent().get();
        }

        // Check if a declaration of the entity exists
        return getSymbolTable(environment).get(entityName, true);
    }

    private boolean checkCAttributeUsage(Iterable<? extends Attribute> attributes,
                Environment environment) {
        // Check the environment
        if (environment.getScopeType() != ScopeType.MODULE_IMPLEMENTATION
                || environment.isEnclosedInGenericNescEntity()) {
            return false;
        }

        boolean attributePresent = false;
        boolean correctUsage = true;
        final Predicate<Attribute> predicateCAttribute = AttributePredicates.getCPredicate();

        // Look for C attribute
        for (Attribute attribute : attributes) {
            if (predicateCAttribute.apply(attribute)) {
                attributePresent = true;

                final NescAttribute cAttribute = (NescAttribute) attribute;
                if (cAttribute.getValue() instanceof ErrorExpr) {
                    correctUsage = false;
                } else {
                    final InitList initList = (InitList) cAttribute.getValue();
                    correctUsage = correctUsage && initList.getArguments().isEmpty();
                }
            }
        }

        return attributePresent && correctUsage;
    }
}
