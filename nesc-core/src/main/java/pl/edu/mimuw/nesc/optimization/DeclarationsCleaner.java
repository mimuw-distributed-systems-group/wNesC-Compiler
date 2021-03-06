package pl.edu.mimuw.nesc.optimization;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import pl.edu.mimuw.nesc.ast.RID;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.astutil.DeclaratorUtils;
import pl.edu.mimuw.nesc.astutil.TypeElementUtils;
import pl.edu.mimuw.nesc.declaration.object.ConstantDeclaration;
import pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration;
import pl.edu.mimuw.nesc.declaration.object.ObjectKind;
import pl.edu.mimuw.nesc.declaration.object.VariableDeclaration;
import pl.edu.mimuw.nesc.refsgraph.EntityNode;
import pl.edu.mimuw.nesc.refsgraph.Reference;
import pl.edu.mimuw.nesc.refsgraph.ReferencesGraph;

/**
 * <p>Cleaner that takes a list of top-level declarations and removes from it
 * the following elements:</p>
 * <ul>
 *     <li>variables with names different from all external names given at
 *     construction of the cleaner and that not referred (either directly or
 *     indirectly) from global variables with these names or from spontaneous
 *     functions</li>
 *     <li>functions that are not spontaneous and are not referred, directly or
 *     indirectly, from spontaneous functions</li>
 *     <li>unused global type definitions</li>
 *     <li>unused global structures and unions definitions</li>
 *     <li>enumerated types definitions if the types themselves are not used and
 *     all constants are unused</li>
 * </ul>
 *
 * <p>The order of declarations in the returned list is the same as in the
 * input list.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class DeclarationsCleaner {
    /**
     * Queue with entities to visit.
     */
    private final Queue<EntityNode> entitiesQueue;

    /**
     * Sets with names of type definitions, structures, unions, enumerated
     * types and enumeration constants that will be removed. Initially, they
     * contain names of all global entities of these kinds. Elements from them
     * are removed while traversing declarations.
     */
    private final Set<String> objectsForRemoval;
    private final Set<String> tagsForRemoval;

    /**
     * List with declarations that are to be cleaned.
     */
    private final ImmutableList<Declaration> declarations;

    /**
     * Graph with references between entities.
     */
    private final ReferencesGraph refsGraph;

    /**
     * List with declarations after cleaning.
     */
    private Optional<ImmutableList<Declaration>> cleanedDeclarations;

    /**
     * Get a builder that will create a type declarations cleaner.
     *
     * @param refsGraph Graph with references between entities that will be
     *                  added later to the builder.
     * @return Newly created builder of a type declarations cleaner.
     */
    public static Builder builder(ReferencesGraph refsGraph) {
        return new Builder(refsGraph);
    }

    private DeclarationsCleaner(PrivateBuilder builder) {
        // Objects built by the builder
        this.entitiesQueue = builder.buildEntitiesQueue();
        this.objectsForRemoval = builder.buildObjectsForRemoval();
        this.tagsForRemoval = builder.buildTagsForRemoval();
        this.declarations = builder.buildDeclarations();
        this.refsGraph = builder.buildRefsGraph();

        // Other member fields
        this.cleanedDeclarations = Optional.absent();
    }

    /**
     * Performs the process of removing unused top-level type and tag
     * definitions.
     *
     * @return Declarations after cleaning.
     */
    public ImmutableList<Declaration> clean() {
        if (cleanedDeclarations.isPresent()) {
            return cleanedDeclarations.get();
        }

        traverse();
        cleanedDeclarations = Optional.of(filter());

        return cleanedDeclarations.get();
    }

    private void traverse() {
        final Set<EntityNode> visitedEntities = new HashSet<>(entitiesQueue);

        while (!entitiesQueue.isEmpty()) {
            final EntityNode node = entitiesQueue.remove();

            switch (node.getKind()) {
                case TAG:
                    tagsForRemoval.remove(node.getUniqueName());
                    break;
                default:
                    objectsForRemoval.remove(node.getUniqueName());
                    break;
            }

            for (Reference ref : node.getSuccessors()) {
                if (!visitedEntities.contains(ref.getReferencedNode())) {
                    visitedEntities.add(ref.getReferencedNode());
                    entitiesQueue.add(ref.getReferencedNode());
                }
            }
        }
    }

    private ImmutableList<Declaration> filter() {
        final FilteringVisitor filteringVisitor = new FilteringVisitor();
        final ImmutableList.Builder<Declaration> declarationsBuilder = ImmutableList.builder();

        for (Declaration declaration : declarations) {
            if (declaration.accept(filteringVisitor, null)) {
                declarationsBuilder.add(declaration);
            }
        }

        return declarationsBuilder.build();
    }

    /**
     * Visitor that decides if declarations are to be preserved and cleans them.
     * It also removes nodes from the references graph. <code>true</code> is
     * returned if the declaration is preserved.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class FilteringVisitor extends ExceptionVisitor<Boolean, Void> {
        @Override
        public Boolean visitFunctionDecl(FunctionDecl declaration, Void arg) {
            final String funUniqueName = DeclaratorUtils.getUniqueName(
                    declaration.getDeclarator()).get();
            final boolean preserve = !objectsForRemoval.contains(funUniqueName);

            if (!preserve) {
                refsGraph.removeOrdinaryId(funUniqueName);
            }

            return preserve;
        }

        @Override
        public Boolean visitExtensionDecl(ExtensionDecl declaration, Void arg) {
            return declaration.getDeclaration().accept(this, null);
        }

        @Override
        public Boolean visitDataDecl(DataDecl declaration, Void arg) {
            // Clean inner declarations

            final Iterator<Declaration> innerDeclsIt = declaration.getDeclarations().iterator();
            while (innerDeclsIt.hasNext()) {
                if (!innerDeclsIt.next().accept(this, null)) {
                    innerDeclsIt.remove();
                }
            }

            // Clean the whole data declaration

            if (declaration.getDeclarations().isEmpty()) {
                TypeElementUtils.removeRID(declaration.getModifiers(), RID.TYPEDEF);
            }

            for (TypeElement typeElement : declaration.getModifiers()) {
                if (!(typeElement instanceof TagRef)) {
                    continue;
                }

                final boolean preserve = typeElement instanceof EnumRef
                        ? filterEnumeration((EnumRef) typeElement)
                        : filterStructOrUnion((TagRef) typeElement);

                final boolean finalPreserve = preserve || !declaration.getDeclarations().isEmpty();

                if (!finalPreserve) {
                    removeFromRefsGraph((TagRef) typeElement);
                }

                return finalPreserve;
            }

            return !declaration.getDeclarations().isEmpty();
        }

        private void removeFromRefsGraph(TagRef tagRef) {
            // Remove enumeration constants

            if (tagRef instanceof EnumRef) {
                final EnumRef enumRef = (EnumRef) tagRef;

                if (enumRef.getDeclaration().getConstants().isPresent()) {
                    for (ConstantDeclaration cstDeclaration : enumRef.getDeclaration().getConstants().get()) {
                        refsGraph.removeOrdinaryId(cstDeclaration.getEnumerator().getUniqueName());
                    }
                }
            }

            // Remove the tag

            if (tagRef.getUniqueName().isPresent()) {
                refsGraph.removeTag(tagRef.getUniqueName().get());
            }
        }

        @Override
        public Boolean visitVariableDecl(VariableDecl declaration, Void arg) {
            final String uniqueName = DeclaratorUtils.getUniqueName(
                    declaration.getDeclarator().get()).get();
            final boolean preserve = !objectsForRemoval.contains(uniqueName);

            if (!preserve) {
                refsGraph.removeOrdinaryId(uniqueName);
            }

            return preserve;
        }

        private boolean filterEnumeration(EnumRef enumRef) {
            // Check if all constants can be removed
            if (enumRef.getDeclaration().getConstants().isPresent()) {
                final List<String> constantsNames = new ArrayList<>();
                for (ConstantDeclaration cstDeclaration : enumRef.getDeclaration().getConstants().get()) {
                    /* We take the unique name of the constant because currently
                       unique names in declaration objects are not updated after
                       remangling reversing. */
                    constantsNames.add(cstDeclaration.getEnumerator().getUniqueName());
                }

                if (!objectsForRemoval.containsAll(constantsNames)) {
                    return true;
                }
            }

            return enumRef.getUniqueName().isPresent()
                    && !tagsForRemoval.contains(enumRef.getUniqueName().get());
        }

        private boolean filterStructOrUnion(TagRef tagRef) {
            return tagRef.getUniqueName().isPresent()
                    && !tagsForRemoval.contains(tagRef.getUniqueName().get());
        }
    }

    /**
     * Interface for building particular elements of the cleaner.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private interface PrivateBuilder {
        Queue<EntityNode> buildEntitiesQueue();
        Set<String> buildObjectsForRemoval();
        Set<String> buildTagsForRemoval();
        ImmutableList<Declaration> buildDeclarations();
        ReferencesGraph buildRefsGraph();
    }

    /**
     * Builder that collects information necessary for building a type
     * declarations cleaner.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static final class Builder {
        /**
         * Data necessary to build a type declarations cleaner.
         */
        private final ReferencesGraph refsGraph;
        private final ImmutableList.Builder<Declaration> declarationsBuilder = ImmutableList.builder();
        private final ImmutableSet.Builder<String> preservedObjectsBuilder = ImmutableSet.builder();
        private final ImmutableSet.Builder<String> preservedTagsBuilder = ImmutableSet.builder();

        /**
         * Private constructor to limit its accessibility.
         */
        private Builder(ReferencesGraph refsGraph) {
            this.refsGraph = refsGraph;
        }

        /**
         * Add declarations from the given collection for cleaning. The order of
         * declarations after cleaning is the same as adding them and returning
         * by the iterator for the given iterable.
         *
         * @param declarations Declarations to add for cleaning.
         * @return <code>this</code>
         */
        public Builder addDeclarations(Iterable<? extends Declaration> declarations) {
            this.declarationsBuilder.addAll(declarations);
            return this;
        }

        /**
         * Add a preserved object. If an object (or typedef) with such name
         * exists in the declarations, it will not be removed.
         *
         * @param name Name of an object or typedef to preserve.
         * @return <code>this</code>
         */
        public Builder addPreservedObject(String name) {
            this.preservedObjectsBuilder.add(name);
            return this;
        }

        /**
         * Add a preserved tag. If such tag exists in the declarations, it will
         * not be removed.
         *
         * @param name Name of the tag to preserve.
         * @return <code>this</code>
         */
        public Builder addPreservedTag(String name) {
            this.preservedTagsBuilder.add(name);
            return this;
        }

        public DeclarationsCleaner build() {
            return new DeclarationsCleaner(new RealBuilder(refsGraph, declarationsBuilder.build(),
                    preservedObjectsBuilder.build(), preservedTagsBuilder.build()));
        }
    }

    /**
     * Object that builds particular elements of the type cleaner.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class RealBuilder extends ExceptionVisitor<Void, Void> implements PrivateBuilder {
        /**
         * Necessary input data for the builder.
         */
        private final ReferencesGraph refsGraph;
        private final ImmutableList<Declaration> declarations;
        private final ImmutableSet<String> preservedObjects;

        /**
         * Objects for building the other elements.
         */
        private final Queue<EntityNode> entitiesQueue = new ArrayDeque<>();
        private final TagPreservingVisitor tagPreservingVisitor;
        private boolean visited = false;

        private RealBuilder(ReferencesGraph refsGraph, ImmutableList<Declaration> declarations,
                    ImmutableSet<String> preservedObjects, ImmutableSet<String> preservedTags) {
            this.refsGraph = refsGraph;
            this.declarations = declarations;
            this.preservedObjects = preservedObjects;
            this.tagPreservingVisitor = new TagPreservingVisitor(refsGraph, entitiesQueue,
                    preservedTags);
        }

        @Override
        public ReferencesGraph buildRefsGraph() {
            return refsGraph;
        }

        @Override
        public Queue<EntityNode> buildEntitiesQueue() {
            visitDeclarations();
            return entitiesQueue;
        }

        @Override
        public Set<String> buildObjectsForRemoval() {
            return new HashSet<>(refsGraph.getOrdinaryIds().keySet());
        }

        @Override
        public Set<String> buildTagsForRemoval() {
            return new HashSet<>(refsGraph.getTags().keySet());
        }

        @Override
        public ImmutableList<Declaration> buildDeclarations() {
            return declarations;
        }

        private void visitDeclarations() {
            if (visited) {
                return;
            }

            visited = true;

            for (Declaration declaration : declarations) {
                declaration.accept(this, null);
            }
        }

        @Override
        public Void visitFunctionDecl(FunctionDecl declaration, Void arg) {
            for (TypeElement typeElement : declaration.getModifiers()) {
                if (typeElement instanceof TagRef) {
                    typeElement.traverse(tagPreservingVisitor, null);
                }
            }

            final String funName = DeclaratorUtils.getUniqueName(declaration.getDeclarator()).get();

            if (preserveObject(funName)) {
                return null;
            }

            final FunctionDeclaration functionDeclaration = declaration.getDeclaration();
            enqueueSpontaneousFunction(funName, functionDeclaration);
            return null;
        }

        @Override
        public Void visitExtensionDecl(ExtensionDecl declaration, Void arg) {
            declaration.getDeclaration().accept(this, null);
            return null;
        }

        @Override
        public Void visitDataDecl(DataDecl declaration, Void arg) {
            for (TypeElement typeElement : declaration.getModifiers()) {
                if (typeElement instanceof TagRef) {
                   typeElement.traverse(tagPreservingVisitor, null);
                }
            }

            for (Declaration innerDecl : declaration.getDeclarations()) {
                final VariableDecl variableDecl = (VariableDecl) innerDecl;
                final String name = DeclaratorUtils.getUniqueName(variableDecl.getDeclarator()).get();

                // Enqueue a preserved object

                if (preserveObject(name)) {
                    continue;
                }

                /* Check if it is a declaration of external variable and if so
                   add it to the queue. */

                if (variableDecl.getDeclaration() != null
                        && variableDecl.getDeclaration().getKind() == ObjectKind.VARIABLE) {

                    final VariableDeclaration variableDeclaration =
                            (VariableDeclaration) variableDecl.getDeclaration();

                    if (variableDeclaration.isExternalVariable()) {
                        entitiesQueue.add(refsGraph.getOrdinaryIds().get(name));
                    }
                } else if (variableDecl.getDeclaration() != null
                        && variableDecl.getDeclaration().getKind() == ObjectKind.FUNCTION) {
                    enqueueSpontaneousFunction(name, (FunctionDeclaration) variableDecl.getDeclaration());
                }
            }

            return null;
        }

        private void enqueueSpontaneousFunction(String funName, FunctionDeclaration functionDeclaration) {
            if (functionDeclaration != null) {
                switch (functionDeclaration.getCallAssumptions()) {
                    case SPONTANEOUS:
                    case HWEVENT:
                    case ATOMIC_HWEVENT:
                        entitiesQueue.add(refsGraph.getOrdinaryIds().get(funName));
                        break;
                    case NONE:
                        break;
                    default:
                        throw new RuntimeException("unexpected call assumptions '"
                                + functionDeclaration.getCallAssumptions() + "'");
                }
            }
        }

        private boolean preserveObject(String name) {
            if (preservedObjects.contains(name)) {
                entitiesQueue.add(refsGraph.getOrdinaryIds().get(name));
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Visitor that is responsible for enqueuing preserved tags.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class TagPreservingVisitor extends IdentityVisitor<Void> {
        private final ReferencesGraph refsGraph;
        private final Queue<EntityNode> entitiesQueue;
        private final ImmutableSet<String> preservedTags;

        private TagPreservingVisitor(ReferencesGraph refsGraph, Queue<EntityNode> entitiesQueue,
                    ImmutableSet<String> preservedTags) {
            this.refsGraph = refsGraph;
            this.entitiesQueue = entitiesQueue;
            this.preservedTags = preservedTags;
        }

        @Override
        public Void visitStructRef(StructRef node, Void arg) {
            preserveTag(node);
            return null;
        }

        @Override
        public Void visitUnionRef(UnionRef node, Void arg) {
            preserveTag(node);
            return null;
        }

        @Override
        public Void visitEnumRef(EnumRef node, Void arg) {
            preserveTag(node);
            return null;
        }

        @Override
        public Void visitNxStructRef(NxStructRef node, Void arg) {
            preserveTag(node);
            return null;
        }

        @Override
        public Void visitNxUnionRef(NxUnionRef node, Void arg) {
            preserveTag(node);
            return null;
        }

        @Override
        public Void visitAttributeRef(AttributeRef node, Void arg) {
            preserveTag(node);
            return null;
        }

        private void preserveTag(TagRef tagRef) {
            if (tagRef.getUniqueName().isPresent()
                    && preservedTags.contains(tagRef.getUniqueName().get())) {
                entitiesQueue.add(refsGraph.getTags().get(tagRef.getUniqueName().get()));
            }
        }
    }
}
