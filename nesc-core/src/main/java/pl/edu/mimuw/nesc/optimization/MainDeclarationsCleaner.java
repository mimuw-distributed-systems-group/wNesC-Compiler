package pl.edu.mimuw.nesc.optimization;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import pl.edu.mimuw.nesc.ast.StructSemantics;
import pl.edu.mimuw.nesc.ast.gen.DataDecl;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.ExceptionVisitor;
import pl.edu.mimuw.nesc.ast.gen.ExtensionDecl;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;
import pl.edu.mimuw.nesc.ast.gen.Identifier;
import pl.edu.mimuw.nesc.ast.gen.IdentityVisitor;
import pl.edu.mimuw.nesc.ast.gen.TagRef;
import pl.edu.mimuw.nesc.ast.gen.TypeElement;
import pl.edu.mimuw.nesc.ast.gen.VariableDecl;
import pl.edu.mimuw.nesc.astutil.DeclaratorUtils;
import pl.edu.mimuw.nesc.astutil.TypeElementUtils;
import pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration;

/**
 * <p>Cleaner that takes a list of declarations and returns the same list with
 * some declarations removed. Declarations that are not present in the resulting
 * list:</p>
 * <ul>
 *     <li>variables with names different from all external names given at
 *     construction of the cleaner and that not referred from initializers of
 *     global variables or from spontaneous functions or functions that can be
 *     called within them</li>
 *     <li>functions that are not spontaneous and are not referred from
 *     spontaneous functions</li>
 * </ul>
 *
 * <p>The order of declarations in the returned list is the same as in the
 * input list.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class MainDeclarationsCleaner {
    /**
     * The queue for the traversal of declarations.
     */
    private final Queue<String> namesQueue;

    /**
     * Set that contains names of external variables that will not be removed.
     */
    private final ImmutableSet<String> externalVariables;

    /**
     * Map that allows easy lookup of specific nodes. Variable declarations are
     * contained in multimap since declarations of global variables can be
     * repeated.
     */
    private final ImmutableMap<String, FunctionDecl> functions;
    private final ImmutableListMultimap<String, VariableDecl> variableDecls;

    /**
     * Set with names of functions and variables that have been visited - it
     * allows ensuring that the clean process will end.
     */
    private final Set<String> visitedEntities = new HashSet<>();

    /**
     * Names of functions and variables that will be removed. After construction
     * it contains names of all global variables and functions. Elements that
     * are not to be removed are removed from this set during the clean process.
     */
    private final Set<String> entitiesForRemoval;

    /**
     * List with declarations that is to be filtered.
     */
    private final ImmutableList<Declaration> declarations;

    /**
     * Visitor that detects references to global variables and functions.
     */
    private final ReferencesCollectorVisitor collectorVisitor = new ReferencesCollectorVisitor();

    /**
     * Visitor that removes declarations and decides which declarations are to
     * be retained.
     */
    private final FilteringVisitor filteringVisitor = new FilteringVisitor();

    /**
     * List the the result of cleaning.
     */
    private Optional<ImmutableList<Declaration>> cleanedDeclarations = Optional.absent();

    /**
     * Get the builder that will build a main declarations cleaner object.
     *
     * @return Newly created instance of the builder object.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Initializes all member fields using the builder.
     *
     * @param builder Builder that is able to build the object.
     */
    private MainDeclarationsCleaner(PrivateBuilder builder) {
        this.externalVariables = builder.buildExternalVariables();
        this.functions = builder.buildFunctions();
        this.variableDecls = builder.buildVariableDecls();
        this.entitiesForRemoval = builder.buildEntitiesForRemoval();
        this.declarations = builder.buildDeclarations();
        this.namesQueue = builder.buildNamesQueue();
    }

    /**
     * Performs the cleaning operation.
     *
     * @return List of declarations after cleaning. They are in the order
     *         of adding them to the builder.
     */
    public ImmutableList<Declaration> clean() {
        if (cleanedDeclarations.isPresent()) {
            return cleanedDeclarations.get();
        }

        initializeVariables();
        traverseDeclarations();
        cleanedDeclarations = Optional.of(filterDeclarations());

        return cleanedDeclarations.get();
    }

    private void initializeVariables() {
        this.entitiesForRemoval.removeAll(externalVariables);
        this.entitiesForRemoval.removeAll(namesQueue);
        this.visitedEntities.addAll(namesQueue);
    }

    private void traverseDeclarations() {
        while (!namesQueue.isEmpty()) {
            final String nextEntity = namesQueue.remove();

            if (functions.containsKey(nextEntity)) {
                functions.get(nextEntity).traverse(collectorVisitor, null);
            } else if (variableDecls.containsKey(nextEntity)) {
                for (VariableDecl variableDecl : variableDecls.get(nextEntity)) {
                    variableDecl.traverse(collectorVisitor, null);
                }
            } else {
                throw new IllegalStateException("cannot find declaration of '"
                        + nextEntity + "'");
            }
        }
    }

    private ImmutableList<Declaration> filterDeclarations() {
        final ImmutableList.Builder<Declaration> filteredDeclarationsBuilder = ImmutableList.builder();

        for (Declaration declaration : declarations) {
            if (declaration.accept(filteringVisitor, null)) {
                filteredDeclarationsBuilder.add(declaration);
            }
        }

        return filteredDeclarationsBuilder.build();
    }

    /**
     * Visitor that looks for references of functions and variables in
     * visited nodes and updates state of the cleaner.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class ReferencesCollectorVisitor extends IdentityVisitor<Void> {
        @Override
        public Void visitIdentifier(Identifier identifier, Void arg) {
            if (identifier.getUniqueName().isPresent()) {
                final String uniqueName = identifier.getUniqueName().get();

                if ((functions.containsKey(uniqueName) || variableDecls.containsKey(uniqueName))
                        && !visitedEntities.contains(uniqueName)) {
                    visitedEntities.add(uniqueName);
                    namesQueue.add(uniqueName);
                    entitiesForRemoval.remove(uniqueName);
                }
            }

            return null;
        }
    }

    /**
     * Visitor that returns if a declaration is to be retained and removes
     * declarations from nodes.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class FilteringVisitor extends ExceptionVisitor<Boolean, Void> {
        @Override
        public Boolean visitFunctionDecl(FunctionDecl funDecl, Void arg) {
            final String funName = DeclaratorUtils.getUniqueName(
                    funDecl.getDeclarator()).get();
            return !entitiesForRemoval.contains(funName);
        }

        @Override
        public Boolean visitExtensionDecl(ExtensionDecl declaration, Void arg) {
            return declaration.getDeclaration().accept(this, null);
        }

        @Override
        public Boolean visitDataDecl(DataDecl declaration, Void arg) {
            final Iterator<Declaration> innerDeclsIt = declaration.getDeclarations().iterator();

            while (innerDeclsIt.hasNext()) {
                if (!innerDeclsIt.next().accept(this, null)) {
                    innerDeclsIt.remove();
                }
            }

            // Check if the whole declaration can be removed

            if (declaration.getDeclarations().isEmpty()) {
                for (TypeElement typeElement : declaration.getModifiers()) {
                    if (typeElement instanceof TagRef) {
                        final TagRef tagRef = (TagRef) typeElement;
                        if (tagRef.getSemantics() != StructSemantics.OTHER) {
                            return true;
                        }
                    }
                }

                return false;
            } else {
                return true;
            }
        }

        @Override
        public Boolean visitVariableDecl(VariableDecl declaration, Void arg) {
            final String uniqueName = DeclaratorUtils.getUniqueName(declaration.getDeclarator()).get();
            return !entitiesForRemoval.contains(uniqueName);
        }
    }

    /**
     * Interface for building particular elements of the cleaner.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private interface PrivateBuilder {
        ImmutableList<Declaration> buildDeclarations();
        ImmutableSet<String> buildExternalVariables();
        Set<String> buildEntitiesForRemoval();
        ImmutableMap<String, FunctionDecl> buildFunctions();
        ImmutableListMultimap<String, VariableDecl> buildVariableDecls();
        Queue<String> buildNamesQueue();
    }

    /**
     * Builder for collecting elements necessary to build a main declarations
     * cleaner.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static final class Builder {
        /**
         * Data needed to build a main declarations cleaner.
         */
        private final ImmutableList.Builder<Declaration> declarationsBuilder = ImmutableList.builder();
        private final ImmutableSet.Builder<String> externalVariablesBuilder = ImmutableSet.builder();

        /**
         * Private constructor to limit its accessibility.
         */
        private Builder() {
        }

        /**
         * Add declarations for cleaning. The order of adding declarations and
         * the order of returning declarations by the iterator of the given
         * collection is the same as the order of the returned declarations.
         *
         * @param declarations Declarations to add.
         * @return <code>this</code>
         */
        public Builder addDeclarations(Collection<? extends Declaration> declarations) {
            this.declarationsBuilder.addAll(declarations);
            return this;
        }

        /**
         * Add names from the given collection as names of external variables.
         *
         * @param names Names to add.
         * @return <code>this</code>
         */
        public Builder addExternalVariables(Collection<String> names) {
            this.externalVariablesBuilder.addAll(names);
            return this;
        }

        public MainDeclarationsCleaner build() {
            return new MainDeclarationsCleaner(new RealBuilder(declarationsBuilder.build(),
                    externalVariablesBuilder.build()));
        }
    }

    /**
     * Builder of particular elements of the cleaner.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class RealBuilder extends ExceptionVisitor<Void, Void> implements PrivateBuilder {
        /**
         * Necessary input data for the builder.
         */
        private final ImmutableList<Declaration> declarations;
        private final ImmutableSet<String> externalVariables;

        /**
         * Data that are collected during declarations traversal.
         */
        private final ImmutableMap.Builder<String, FunctionDecl> functionsBuilder = ImmutableMap.builder();
        private final ImmutableListMultimap.Builder<String, VariableDecl> variableDeclsBuilder = ImmutableListMultimap.builder();
        private final Set<String> entitiesForRemoval = new HashSet<>();
        private final Queue<String> namesQueue = new ArrayDeque<>();
        private boolean declarationsTraversed = false;

        private RealBuilder(ImmutableList<Declaration> declarations, ImmutableSet<String> externalVariables) {
            this.declarations = declarations;
            this.externalVariables = externalVariables;
        }

        @Override
        public ImmutableList<Declaration> buildDeclarations() {
            return declarations;
        }

        @Override
        public ImmutableSet<String> buildExternalVariables() {
            return externalVariables;
        }

        @Override
        public Set<String> buildEntitiesForRemoval() {
            traverseDeclarations();
            return entitiesForRemoval;
        }

        @Override
        public ImmutableMap<String, FunctionDecl> buildFunctions() {
            traverseDeclarations();
            return functionsBuilder.build();
        }

        @Override
        public ImmutableListMultimap<String, VariableDecl> buildVariableDecls() {
            traverseDeclarations();
            return variableDeclsBuilder.build();
        }

        @Override
        public Queue<String> buildNamesQueue() {
            traverseDeclarations();
            return namesQueue;
        }

        private void traverseDeclarations() {
            if (declarationsTraversed) {
                return;
            }

            declarationsTraversed = true;

            for (Declaration declaration : declarations) {
                declaration.accept(this, null);
            }
        }

        @Override
        public Void visitFunctionDecl(FunctionDecl declaration, Void arg) {
            final String funName = DeclaratorUtils.getUniqueName(declaration.getDeclarator()).get();
            final FunctionDeclaration functionDeclaration = declaration.getDeclaration();

            functionsBuilder.put(funName, declaration);
            entitiesForRemoval.add(funName);

            if (functionDeclaration != null) {
                switch (functionDeclaration.getCallAssumptions()) {
                    case SPONTANEOUS:
                    case HWEVENT:
                    case ATOMIC_HWEVENT:
                        namesQueue.add(funName);
                        break;
                    case NONE:
                        break;
                    default:
                        throw new RuntimeException("unexpected call assumptions '"
                                + functionDeclaration.getCallAssumptions() + "'");
                }
            }

            return null;
        }

        @Override
        public Void visitExtensionDecl(ExtensionDecl declaration, Void arg) {
            declaration.getDeclaration().accept(this, null);
            return null;
        }

        @Override
        public Void visitDataDecl(DataDecl declaration, Void arg) {
            if (TypeElementUtils.isTypedef(declaration.getModifiers())) {
                return null;
            }

            for (Declaration innerDeclaration : declaration.getDeclarations()) {
                innerDeclaration.accept(this, null);
            }

            return null;
        }

        @Override
        public Void visitVariableDecl(VariableDecl declaration, Void arg) {
            final String name = DeclaratorUtils.getUniqueName(declaration.getDeclarator().get()).get();

            this.variableDeclsBuilder.put(name, declaration);
            this.entitiesForRemoval.add(name);

            return null;
        }
    }
}
