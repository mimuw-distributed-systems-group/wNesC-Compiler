package pl.edu.mimuw.nesc.refsgraph;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import pl.edu.mimuw.nesc.ast.StructSemantics;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.astutil.CountingDecoratorVisitor;
import pl.edu.mimuw.nesc.astutil.DeclaratorUtils;
import pl.edu.mimuw.nesc.astutil.TypeElementUtils;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static pl.edu.mimuw.nesc.astutil.CountingDecoratorVisitor.CountingOracle;

/**
 * <p>Class that represents a graph of references between global C entities.
 * Nodes in the graph are all global variables, functions, enumeration
 * constants and named tags that are not nested inside a tag definition.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class ReferencesGraph {
    /**
     * Map with nodes that represent tags.
     */
    private final Map<String, EntityNode> tags;

    /**
     * Map with nodes that represent global variables and functions.
     */
    private final Map<String, EntityNode> ordinaryIds;

    /**
     * Map with nodes that represent tags that is unmodifiable.
     */
    private final Map<String, EntityNode> unmodifiableTags;

    /**
     * Map with nodes that represent global variables and function that is
     * unmodifiable.
     */
    private final Map<String, EntityNode> unmodifiableOrdinaryIds;

    /**
     * Get a new builder that will create a references graph.
     *
     * @return Newly created builder of a references graph.
     */
    public static Builder builder() {
        return new Builder();
    }

    private ReferencesGraph(PrivateBuilder builder) {
        this.tags = builder.buildTags();
        this.ordinaryIds = builder.buildOrdinaryIds();
        this.unmodifiableTags = Collections.unmodifiableMap(this.tags);
        this.unmodifiableOrdinaryIds = Collections.unmodifiableMap(this.ordinaryIds);
    }

    /**
     * Get map with nodes that represent global variables, functions,
     * enumerations constants and type definitions.
     *
     * @return Unmodifiable map with nodes.
     */
    public Map<String, EntityNode> getOrdinaryIds() {
        return unmodifiableOrdinaryIds;
    }

    /**
     * Get map with nodes that represent global names tags that are not nested
     * inside other tags.
     *
     * @return Unmodifiable map with tags.
     */
    public Map<String, EntityNode> getTags() {
        return unmodifiableTags;
    }

    /**
     * Remove the node that represents the tag with given name from the graph.
     * The node and all edges it is a part of are removed.
     *
     * @param tagUniqueName Unique name of tag to remove.
     */
    public void removeTag(String tagUniqueName) {
        checkNotNull(tagUniqueName, "unique name of tag cannot be null");
        checkArgument(!tagUniqueName.isEmpty(), "unique name of tag cannot be empty");
        removeNode(Namespace.TAG, tagUniqueName);
    }

    /**
     * Remove the node that represents an entity with given name from the graph.
     * The node and all edges it is a part of are removed.
     *
     * @param uniqueName Unique name of the ordinary entity to remove.
     */
    public void removeOrdinaryId(String uniqueName) {
        checkNotNull(uniqueName, "unique name cannot be null");
        checkArgument(!uniqueName.isEmpty(), "unique name cannot be empty");
        removeNode(Namespace.ORDINARY, uniqueName);
    }

    private void removeNode(Namespace namespace, String key) {
        final Map<String, EntityNode> nodesMap;

        switch (namespace) {
            case ORDINARY:
                nodesMap = ordinaryIds;
                break;
            case TAG:
                nodesMap = tags;
                break;
            default:
                throw new RuntimeException("unexpected namespace '" + namespace
                        + "'");
        }

        if (nodesMap.containsKey(key)) {
            nodesMap.get(key).removeAllEdges();
            nodesMap.remove(key);
        }
    }

    /**
     * Merge the node that represents an entity with given name with its
     * predecessors. All references made by the entity become references of
     * its predecessors. The node is removed from the graph.
     *
     * @param uniqueName Unique name of the entity to merge.
     */
    public void mergeOrdinaryId(String uniqueName) {
        checkNotNull(uniqueName, "unique name cannot be null");
        checkArgument(!uniqueName.isEmpty(), "unique name cannot be an empty string");
        mergeNode(Namespace.ORDINARY, uniqueName);
    }

    /**
     * Merge the node that represents the tag with given name with its
     * predecessors. All references made by the tag become references of
     * its predecessors. The node is removed from the graph.
     *
     * @param tagUniqueName Unique name of the tag to merge.
     */
    public void mergeTag(String tagUniqueName) {
        checkNotNull(tagUniqueName, "unique name of the tag cannot be null");
        checkArgument(!tagUniqueName.isEmpty(), "unique name of the tag cannot be an empty string");
        mergeNode(Namespace.TAG, tagUniqueName);
    }

    private void mergeNode(Namespace namespace, String name) {
        final EntityNode nodeToMerge = requireNode(namespace, name);

        for (Reference referencePredecessor : nodeToMerge.getPredecessors()) {
            final EntityNode referencingNode = referencePredecessor.getReferencingNode();

            for (Reference referenceSuccessor : nodeToMerge.getSuccessors()) {
                referencingNode.addReference(
                        referenceSuccessor.getReferencedNode(),
                        referenceSuccessor.getType(),
                        referenceSuccessor.getASTNode(),
                        referenceSuccessor.isInsideNotEvaluatedExpr(),
                        referenceSuccessor.isInsideAtomic(),
                        referencePredecessor.getEnclosingLoopsCount()
                                + referenceSuccessor.getEnclosingLoopsCount(),
                        referencePredecessor.getEnclosingConditionalStmtsCount()
                                + referenceSuccessor.getEnclosingConditionalStmtsCount()
                );
            }
        }

        removeNode(namespace, name);
    }

    /**
     * A convenience method that adds a reference from an ordinary identifier to
     * an ordinary identifier. It calls {@link ReferencesGraph#addReference}.
     */
    public void addReferenceOrdinaryToOrdinary(String referencingEntity, String referencedEntity,
            Reference.Type referenceType, Node astNode, boolean insideNotEvaluatedExpr,
            boolean insideAtomic, int enclosingLoopsCount, int enclosingConditionalStmtsCount) {
        addReference(ReferenceDirection.FROM_ORDINARY_ID_TO_ORDINARY_ID, referencingEntity,
                referencedEntity, referenceType, astNode, insideNotEvaluatedExpr, insideAtomic,
                enclosingLoopsCount, enclosingConditionalStmtsCount);
    }

    /**
     * A convenience method that adds a reference from an ordinary identifier to
     * a tag. It calls {@link ReferencesGraph#addReference}.
     */
    public void addReferenceOrdinaryToTag(String referencingEntity, String referencedEntity,
            Reference.Type referenceType, Node astNode, boolean insideNotEvaluatedExpr,
            boolean insideAtomic, int enclosingLoopsCount, int enclosingConditionalStmtsCount) {
        addReference(ReferenceDirection.FROM_ORDINARY_ID_TO_TAG, referencingEntity,
                referencedEntity, referenceType, astNode, insideNotEvaluatedExpr,
                insideAtomic, enclosingLoopsCount, enclosingConditionalStmtsCount);
    }

    /**
     * A convenience method that adds a reference from a tag to an ordinary
     * identifier. It calls {@link ReferencesGraph#addReference}.
     */
    public void addReferenceTagToOrdinary(String referencingEntity, String referencedEntity,
            Reference.Type referenceType, Node astNode, boolean insideNotEvaluatedExpr,
            boolean insideAtomic, int enclosingLoopsCount, int enclosingConditionalStmtsCount) {
        addReference(ReferenceDirection.FROM_TAG_TO_ORDINARY_ID, referencingEntity,
                referencedEntity, referenceType, astNode, insideNotEvaluatedExpr,
                insideAtomic, enclosingLoopsCount, enclosingConditionalStmtsCount);
    }

    /**
     * A convenience method that adds a reference from a tag to a tag. It calls
     * {@link ReferencesGraph#addReference}.
     */
    public void addReferenceTagToTag(String referencingEntity, String referencedEntity,
            Reference.Type referenceType, Node astNode, boolean insideNotEvaluatedExpr,
            boolean insideAtomic, int enclosingLoopsCount, int enclosingConditionalStmtsCount) {
        addReference(ReferenceDirection.FROM_TAG_TO_TAG, referencingEntity,
                referencedEntity, referenceType, astNode, insideNotEvaluatedExpr,
                insideAtomic, enclosingLoopsCount, enclosingConditionalStmtsCount);
    }

    /**
     * Add a reference defined by given parameters. It is determined from the
     * reference direction whether the referencing entity is an ordinary
     * identifier or a tag. The same applies to the referenced entity.
     *
     * @throws IllegalStateException There are no entities with names
     *                               <code>referencingEntity</code> or
     *                               <code>referencedEntity</code> of
     *                               kinds specified by the reference direction.
     */
    public void addReference(ReferenceDirection direction, String referencingEntity,
            String referencedEntity, Reference.Type referenceType, Node astNode,
            boolean insideNotEvaluatedExpr, boolean insideAtomic, int enclosingLoopsCount,
            int enclosingConditionalStmtsCount) {
        checkNotNull(direction, "direction cannot be null");
        checkNotNull(referencingEntity, "referencing entity cannot be null");
        checkNotNull(referencedEntity, "referenced entity cannot be null");
        checkNotNull(referenceType, "reference type cannot be null");
        checkNotNull(astNode, "AST node cannot be null");
        checkArgument(!referencingEntity.isEmpty(), "referencing entity cannot be an empty string");
        checkArgument(!referencedEntity.isEmpty(), "referenced entity cannot be an empty string");
        checkArgument(enclosingLoopsCount >= 0, "count of enclosing loops cannot be negative");
        checkArgument(enclosingConditionalStmtsCount >= 0, "count of enclosing conditional statements cannot be negative");

        final Namespace namespaceReferencing, namespaceReferenced;

        // Determine the source and target namespaces
        switch (direction) {
            case FROM_ORDINARY_ID_TO_ORDINARY_ID:
                namespaceReferencing = namespaceReferenced = Namespace.ORDINARY;
                break;
            case FROM_ORDINARY_ID_TO_TAG:
                namespaceReferencing = Namespace.ORDINARY;
                namespaceReferenced = Namespace.TAG;
                break;
            case FROM_TAG_TO_TAG:
                namespaceReferencing = namespaceReferenced = Namespace.TAG;
                break;
            case FROM_TAG_TO_ORDINARY_ID:
                namespaceReferencing = Namespace.TAG;
                namespaceReferenced = Namespace.ORDINARY;
                break;
            default:
                throw new RuntimeException("unexpected reference direction '"
                        + direction + "'");
        }

        // Get the nodes in the graph
        final EntityNode referencingNode = requireNode(namespaceReferencing, referencingEntity);
        final EntityNode referencedNode = requireNode(namespaceReferenced, referencedEntity);

        // Add the reference
        referencingNode.addReference(referencedNode, referenceType, astNode,
                insideNotEvaluatedExpr, insideAtomic, enclosingLoopsCount,
                enclosingConditionalStmtsCount);
    }

    private EntityNode requireNode(Namespace namespace, String key) {
        final Optional<EntityNode> result;

        switch (namespace) {
            case ORDINARY:
                result = Optional.fromNullable(ordinaryIds.get(key));
                break;
            case TAG:
                result = Optional.fromNullable(tags.get(key));
                break;
            default:
                throw new RuntimeException("unexpected namespace '"
                        + namespace + "'");
        }

        checkState(result.isPresent(), "no entity of the requested kind with name '%s'", key);

        return result.get();
    }

    /**
     * Write the call graph implied by this references graph to the file with
     * given name. If it already exists, it is overwritten.
     *
     * @param fileName Name of the file to write the call graph.
     */
    public void writeCallGraph(String fileName) throws IOException {
        checkNotNull(fileName, "file name cannot be null");
        checkArgument(!fileName.isEmpty(), "file name cannot be an empty string");

        try (final FileOutputStream output = new FileOutputStream(fileName)) {
            // Truncate the file
            output.getChannel().truncate(0L);

            // Write the call graph
            writeCallGraph(output);
        }
    }

    /**
     * <p>Save the call graph that is implied by this references graph to the
     * given stream. It is saved in textual form in the following format. The
     * first line contains the number of vertices and number of edges (in this
     * order) separated by a space. The following lines contain directed edges:
     * <code>u v</code> means that <code>v</code> is called from <code>u</code>.
     * </p>
     *
     * <p>The given output stream is not closed after the call.</p>
     *
     * @param output Output stream to save the graph to.
     */
    public void writeCallGraph(OutputStream output) throws IOException {
        checkNotNull(output, "output cannot be null");

        final ImmutableMap.Builder<String, Integer> funsMapBuilder = ImmutableMap.builder();
        int nextNumber = 1;
        int edgesCount = 0;

        // Map functions to numbers and count edges
        for (Map.Entry<String, EntityNode> nodeEntry : getOrdinaryIds().entrySet()) {
            if (nodeEntry.getValue().getKind() == EntityNode.Kind.FUNCTION) {
                funsMapBuilder.put(nodeEntry.getKey(), nextNumber++);

                for (Reference successor : nodeEntry.getValue().getSuccessors()) {
                    if (successor.getType() == Reference.Type.CALL) {
                        ++edgesCount;
                    }
                }
            }
        }

        final ImmutableMap<String, Integer> funsMap = funsMapBuilder.build();
        final PrintWriter writer;

        try {
            writer = new PrintWriter(new OutputStreamWriter(output, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            /* The exception cannot be thrown because the support for UTF-8 is
               obligatory for a Java platform. */
            throw new RuntimeException("UTF-8 is unsupported", e);
        }

        // Write the graph
        writer.print(funsMap.size());
        writer.print(' ');
        writer.println(edgesCount);
        for (Map.Entry<String, Integer> funEntry : funsMap.entrySet()) {
            for (Reference reference : getOrdinaryIds().get(funEntry.getKey()).getSuccessors()) {
                if (reference.getType() == Reference.Type.CALL) {
                    writer.print(funEntry.getValue());
                    writer.print(' ');
                    writer.println((int) funsMap.get(reference.getReferencedNode().getUniqueName()));
                    --edgesCount;
                }
            }
        }

        if (writer.checkError()) {
            throw new IOException("error while writing data");
        } else if (edgesCount != 0) {
            throw new RuntimeException("invalid count of edges written");
        }
    }

    /**
     * Builder that collects elements necessary for building a references
     * graph.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static final class Builder {
        /**
         * Data necessary for building a references graph.
         */
        private final ImmutableList.Builder<Declaration> declarationsBuilder = ImmutableList.builder();

        /**
         * Private constructor to limit its accessibility.
         */
        private Builder() {
        }

        /**
         * Add top-level declarations that indicate nodes of the graph.
         *
         * @param declarations Collection of declarations to add.
         * @return <code>this</code>
         */
        public Builder addDeclarations(Collection<Declaration> declarations) {
            this.declarationsBuilder.addAll(declarations);
            return this;
        }

        public ReferencesGraph build() {
            return new ReferencesGraph(new RealBuilder(declarationsBuilder.build()));
        }
    }

    /**
     * Interface for building particular elements of the graph.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private interface PrivateBuilder {
        Map<String, EntityNode> buildTags();
        Map<String, EntityNode> buildOrdinaryIds();
    }

    /**
     * Class responsible for building particular elements of the graph - nodes
     * and all edges.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class RealBuilder implements PrivateBuilder {
        /**
         * List with declarations that indicate the nodes of the graph.
         */
        private final ImmutableList<Declaration> declarations;

        /**
         * Map with created tag nodes.
         */
        private final Map<String, EntityNode> tags;

        /**
         * Map with created ordinary identifiers.
         */
        private final Map<String, EntityNode> ordinaryIds;

        /**
         * Value indicating if all elements have been built.
         */
        private boolean built;

        private RealBuilder(ImmutableList<Declaration> declarations) {
            this.declarations = declarations;
            this.tags = new HashMap<>();
            this.ordinaryIds = new HashMap<>();
            this.built = false;
        }

        @Override
        public Map<String, EntityNode> buildTags() {
            buildAllElements();
            return tags;
        }

        @Override
        public Map<String, EntityNode> buildOrdinaryIds() {
            buildAllElements();
            return ordinaryIds;
        }

        private void buildAllElements() {
            if (built) {
                return;
            }

            built = true;

            // Discover all global entities

            final EntitiesDiscovererVisitor discoverer =
                    new EntitiesDiscovererVisitor(ordinaryIds, tags);

            for (Declaration declaration : declarations) {
                declaration.accept(discoverer, null);
            }

            // Add edges in the graph

            final EdgeAdditionControllerVisitor edgeController =
                    new EdgeAdditionControllerVisitor(Collections.unmodifiableMap(ordinaryIds),
                            Collections.unmodifiableMap(tags));

            for (Declaration declaration : declarations) {
                declaration.accept(edgeController, null);
            }
        }
    }

    /**
     * Visitor that creates nodes for entities without any edges.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class EntitiesDiscovererVisitor extends ExceptionVisitor<Void, Void> {
        /**
         * Map with tags to fill.
         */
        private final Map<String, EntityNode> tags;

        /**
         * Map with ordinary identifiers to fill.
         */
        private final Map<String, EntityNode> ordinaryIds;

        private EntitiesDiscovererVisitor(Map<String, EntityNode> ordinaryIds, Map<String, EntityNode> tags) {
            this.tags = tags;
            this.ordinaryIds = ordinaryIds;
        }

        @Override
        public Void visitFunctionDecl(FunctionDecl declaration, Void arg) {
            final String uniqueName = DeclaratorUtils.getUniqueName(declaration.getDeclarator()).get();
            final Optional<EntityNode> optFunNode = Optional.fromNullable(ordinaryIds.get(uniqueName));

            if (optFunNode.isPresent()) {
                /* This case can happen if there is a forward declaration of
                   a function. */
                checkState(optFunNode.get().getKind() == EntityNode.Kind.FUNCTION,
                        "entity node already present with different kind, expected FUNCTION, actual "
                                + optFunNode.get().getKind());
            } else {
                ordinaryIds.put(uniqueName, new EntityNode(uniqueName, EntityNode.Kind.FUNCTION));
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
            final boolean isTypedef = TypeElementUtils.isTypedef(declaration.getModifiers());

            for (TypeElement typeElement : declaration.getModifiers()) {
                if (typeElement instanceof TagRef) {
                    addTag((TagRef) typeElement);
                }
            }

            for (Declaration innerDecl : declaration.getDeclarations()) {
                final VariableDecl variableDecl = (VariableDecl) innerDecl;
                final Optional<NestedDeclarator> deepestDeclarator =
                        DeclaratorUtils.getDeepestNestedDeclarator(variableDecl.getDeclarator());

                final EntityNode.Kind kind;
                if (isTypedef) {
                    kind = EntityNode.Kind.TYPE_DEFINITION;
                } else if (!deepestDeclarator.isPresent()) {
                    kind = EntityNode.Kind.VARIABLE;
                } else if (deepestDeclarator.get() instanceof FunctionDeclarator) {
                    kind = EntityNode.Kind.FUNCTION;
                } else if (deepestDeclarator.get() instanceof InterfaceRefDeclarator) {
                    throw new RuntimeException("unexpected interface reference declarator");
                } else {
                    kind = EntityNode.Kind.VARIABLE;
                }

                final String uniqueName = DeclaratorUtils.getUniqueName(
                        variableDecl.getDeclarator().get()).get();
                final Optional<EntityNode> optNode = Optional.fromNullable(ordinaryIds.get(uniqueName));

                if (optNode.isPresent()) {
                    checkState(optNode.get().getKind() == kind, "previous kind differs");
                } else {
                    ordinaryIds.put(uniqueName, new EntityNode(uniqueName, kind));
                }
            }

            return null;
        }

        private void addTag(TagRef tagRef) {
            if (tagRef instanceof EnumRef && tagRef.getSemantics() != StructSemantics.OTHER) {
                for (Declaration declaration : tagRef.getFields()) {
                    declaration.accept(this, null);
                }
            }

            final String uniqueName;
            if (tagRef.getUniqueName().isPresent()) {
                uniqueName = tagRef.getUniqueName().get();
            } else {
                return;
            }

            final Optional<EntityNode> optNode = Optional.fromNullable(tags.get(uniqueName));

            if (optNode.isPresent()) {
                checkState(optNode.get().getKind() == EntityNode.Kind.TAG,
                        "kind of the entity node differs");
            } else {
                tags.put(uniqueName, new EntityNode(uniqueName, EntityNode.Kind.TAG));
            }
        }

        @Override
        public Void visitEnumerator(Enumerator enumerator, Void arg) {
            checkState(!ordinaryIds.containsKey(enumerator.getUniqueName()),
                    "node with the name of the constant already present");
            ordinaryIds.put(enumerator.getUniqueName(), new EntityNode(enumerator.getUniqueName(),
                    EntityNode.Kind.CONSTANT));
            return null;
        }
    }

    /**
     * Visitor responsible for detecting AST nodes that correspond to nodes of
     * the graph and adding edges to referenced entities in correct manner.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class EdgeAdditionControllerVisitor extends ExceptionVisitor<Void, Void> {
        /**
         * Map with tags.
         */
        private final Map<String, EntityNode> tags;

        /**
         * Map with variables and functions.
         */
        private final Map<String, EntityNode> ordinaryIds;

        private EdgeAdditionControllerVisitor(Map<String, EntityNode> ordinaryIds,
                    Map<String, EntityNode> tags) {
            this.tags = tags;
            this.ordinaryIds = ordinaryIds;
        }

        @Override
        public Void visitExtensionDecl(ExtensionDecl declaration, Void arg) {
            declaration.getDeclaration().accept(this, null);
            return null;
        }

        @Override
        public Void visitFunctionDecl(FunctionDecl declaration, Void arg) {
            final String uniqueName = DeclaratorUtils.getUniqueName(declaration.getDeclarator()).get();
            declaration.traverse(new CountingDecoratorVisitor<>(new ReferencesCreatorVisitor(
                    ordinaryIds.get(uniqueName), ordinaryIds, tags)), new CountingOracle<>(new Oracle()));
            return null;
        }

        @Override
        public Void visitDataDecl(DataDecl declaration, Void arg) {
            // Add references from tag definitions

            for (TypeElement typeElement : declaration.getModifiers()) {
                if (typeElement instanceof TagRef) {
                    addEntitiesReferencedByTag((TagRef) typeElement);
                }
            }

            /* Add references from declared type definitions, functions or
            variables. */

            for (Declaration innerDecl : declaration.getDeclarations()) {
                final VariableDecl variableDecl = (VariableDecl) innerDecl;
                final EntityNode referencingNode = ordinaryIds.get(DeclaratorUtils.getUniqueName(
                        variableDecl.getDeclarator()).get());
                variableDecl.traverse(new CountingDecoratorVisitor<>(new ReferencesCreatorVisitor(
                        referencingNode, ordinaryIds, tags)), new CountingOracle<>(new Oracle()));

                for (TypeElement typeElement : declaration.getModifiers()) {
                    if (typeElement instanceof TagRef) {
                        final TagRef tagRef = (TagRef) typeElement;
                        if (tagRef.getUniqueName().isPresent()) {
                            referencingNode.addReference(tags.get(tagRef.getUniqueName().get()),
                                    Reference.Type.NORMAL, tagRef, false, false, 0, 0);
                        } else {
                            tagRef.traverse(new CountingDecoratorVisitor<>(new ReferencesCreatorVisitor(
                                    referencingNode, ordinaryIds, tags)), new CountingOracle<>(new Oracle()));
                        }
                    } else if (typeElement instanceof Typename) {
                        referencingNode.addReference(ordinaryIds.get(((Typename) typeElement).getUniqueName()),
                                Reference.Type.NORMAL, typeElement, false, false, 0, 0);
                    }
                }
            }

            return null;
        }

        private void addEntitiesReferencedByTag(TagRef tagRef) {
            if (tagRef.getSemantics() == StructSemantics.OTHER) {
                return;
            }

            if (tagRef instanceof EnumRef) {
                addEntitiesReferencedByTag((EnumRef) tagRef);
            } else if (tagRef.getUniqueName().isPresent()) {
                tagRef.traverse(new CountingDecoratorVisitor<>(new ReferencesCreatorVisitor(
                        tags.get(tagRef.getUniqueName().get()), ordinaryIds, tags)),
                        new CountingOracle<>(new Oracle()));
            }
        }

        private void addEntitiesReferencedByTag(EnumRef enumRef) {
            // Add entities referenced by each enumerator

            for (Declaration declaration : enumRef.getFields()) {
                declaration.accept(this, null);
            }

            // Add references to the enum or other enumerators if unnamed enum

            if (enumRef.getUniqueName().isPresent()) {
                final EntityNode enumNode = tags.get(enumRef.getUniqueName().get());

                for (Declaration declaration : enumRef.getFields()) {
                    final Enumerator enumerator = (Enumerator) declaration;
                    ordinaryIds.get(enumerator.getUniqueName()).addReference(enumNode,
                            Reference.Type.NORMAL, enumerator, false, false, 0, 0);
                }
            } else {
                for (Declaration declReferencing : enumRef.getFields()) {
                    for (Declaration declReferenced : enumRef.getFields()) {
                        if (declReferencing != declReferenced) {
                            final EntityNode referencing = ordinaryIds.get(((Enumerator) declReferencing).getUniqueName());
                            final EntityNode referenced = ordinaryIds.get(((Enumerator) declReferenced).getUniqueName());
                            referencing.addReference(referenced, Reference.Type.NORMAL, declReferencing, false, false, 0, 0);
                        }
                    }
                }
            }
        }

        @Override
        public Void visitEnumerator(Enumerator enumerator, Void arg) {
            if (enumerator.getValue().isPresent()) {
                enumerator.getValue().get().traverse(new CountingDecoratorVisitor<>(
                        new ReferencesCreatorVisitor(ordinaryIds.get(enumerator.getUniqueName()),
                        ordinaryIds, tags)), new CountingOracle<>(new Oracle()));
            }
            return null;
        }
    }

    /**
     * Visitor that adds references to entities it encounters.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class ReferencesCreatorVisitor extends IdentityVisitor<CountingOracle<Oracle>> {
        /**
         * Node whose references this visitor encounters.
         */
        private final EntityNode referencingNode;

        /**
         * Map with all tags whose references that are tracked.
         */
        private final Map<String, EntityNode> tags;

        /**
         * Map with identifiers of variables, type definitions and functions
         * whose references are tracked.
         */
        private final Map<String, EntityNode> ordinaryIds;

        /**
         * Set with identifiers to ignore in
         * {@link ReferencesCreatorVisitor#visitIdentifier}.
         */
        private final Set<Identifier> ignoredIdentifiers;

        private ReferencesCreatorVisitor(EntityNode referencingNode, Map<String, EntityNode> ordinaryIds,
                    Map<String, EntityNode> tags) {
            checkNotNull(referencingNode, "the referencing node cannot be null");
            checkNotNull(ordinaryIds, "ordinary identifier cannot be null");
            checkNotNull(tags, "tags cannot be null");

            this.referencingNode = referencingNode;
            this.tags = tags;
            this.ordinaryIds = ordinaryIds;
            this.ignoredIdentifiers = new HashSet<>();
        }

        @Override
        public CountingOracle<Oracle> visitAtomicStmt(AtomicStmt stmt, CountingOracle<Oracle> oracle) {
            return oracle.modifyDecoratedOracle(oracle.getDecoratedOracle().modifyInsideAtomic(true));
        }

        @Override
        public CountingOracle<Oracle> visitCompoundStmt(CompoundStmt stmt, CountingOracle<Oracle> oracle) {
            final Oracle nestedOracle = oracle.getDecoratedOracle();
            return oracle.modifyDecoratedOracle(nestedOracle.modifyInsideAtomic(nestedOracle.insideAtomic
                    || stmt.getAtomicVariableUniqueName() != null && stmt.getAtomicVariableUniqueName().isPresent()));
        }

        @Override
        public CountingOracle<Oracle> visitSizeofExpr(SizeofExpr expr, CountingOracle<Oracle> oracle) {
            return oracle.modifyDecoratedOracle(oracle.getDecoratedOracle()
                    .modifyInsideNotEvaluatedExpr(true));
        }

        @Override
        public CountingOracle<Oracle> visitAlignofExpr(AlignofExpr expr, CountingOracle<Oracle> oracle) {
            return oracle.modifyDecoratedOracle(oracle.getDecoratedOracle()
                    .modifyInsideNotEvaluatedExpr(true));
        }

        @Override
        public CountingOracle<Oracle> visitSizeofType(SizeofType expr, CountingOracle<Oracle> oracle) {
            return oracle.modifyDecoratedOracle(oracle.getDecoratedOracle()
                    .modifyInsideNotEvaluatedExpr(true));
        }

        @Override
        public CountingOracle<Oracle> visitAlignofType(AlignofType expr, CountingOracle<Oracle> oracle) {
            return oracle.modifyDecoratedOracle(oracle.getDecoratedOracle()
                    .modifyInsideNotEvaluatedExpr(true));
        }

        @Override
        public CountingOracle<Oracle> visitIdentifier(Identifier identifier, CountingOracle<Oracle> oracle) {
            if (ignoredIdentifiers.contains(identifier)) {
                return oracle;
            }

            if (identifier.getUniqueName().isPresent()) {
                addOrdinaryIdReference(identifier.getUniqueName().get(), identifier, oracle);
            }

            return oracle;
        }

        @Override
        public CountingOracle<Oracle> visitFunctionCall(FunctionCall expr, CountingOracle<Oracle> oracle) {
            if (expr.getFunction() instanceof Identifier) {
                final Identifier identifier = (Identifier) expr.getFunction();

                if (identifier.getUniqueName().isPresent()
                        && ordinaryIds.containsKey(identifier.getUniqueName().get())) {
                    this.referencingNode.addReference(ordinaryIds.get(identifier.getUniqueName().get()),
                            Reference.Type.CALL, expr, oracle.getDecoratedOracle().insideNotEvaluatedExpr,
                            oracle.getDecoratedOracle().insideAtomic, oracle.getEnclosingLoopsCount(),
                            oracle.getEnclosingConditionalStmtsCount());
                }

                ignoredIdentifiers.add(identifier);
            }

            return oracle;
        }

        @Override
        public CountingOracle<Oracle> visitTypename(Typename typename, CountingOracle<Oracle> oracle) {
            addOrdinaryIdReference(typename.getUniqueName(), typename, oracle);
            return oracle;
        }

        @Override
        public CountingOracle<Oracle> visitComponentTyperef(ComponentTyperef typename, CountingOracle<Oracle> oracle) {
            addOrdinaryIdReference(typename.getUniqueName(), typename, oracle);
            return oracle;
        }

        @Override
        public CountingOracle<Oracle> visitStructRef(StructRef tagRef, CountingOracle<Oracle> oracle) {
            tagReference(tagRef, oracle);
            return oracle;
        }

        @Override
        public CountingOracle<Oracle> visitUnionRef(UnionRef tagRef, CountingOracle<Oracle> oracle) {
            tagReference(tagRef, oracle);
            return oracle;
        }

        @Override
        public CountingOracle<Oracle> visitEnumRef(EnumRef tagRef, CountingOracle<Oracle> oracle) {
            tagReference(tagRef, oracle);
            return oracle;
        }

        @Override
        public CountingOracle<Oracle> visitNxStructRef(NxStructRef tagRef, CountingOracle<Oracle> oracle) {
            tagReference(tagRef, oracle);
            return oracle;
        }

        @Override
        public CountingOracle<Oracle> visitNxUnionRef(NxUnionRef tagRef, CountingOracle<Oracle> oracle) {
            tagReference(tagRef, oracle);
            return oracle;
        }

        private void tagReference(TagRef tagRef, CountingOracle<Oracle> oracle) {
            if (tagRef.getUniqueName().isPresent()) {
                addTagReference(tagRef.getUniqueName().get(), tagRef, oracle);
            }
        }

        private void addOrdinaryIdReference(String uniqueName, Node astNode, CountingOracle<Oracle> oracle) {
            if (ordinaryIds.containsKey(uniqueName)) {
                referencingNode.addReference(
                        ordinaryIds.get(uniqueName),
                        Reference.Type.NORMAL,
                        astNode,
                        oracle.getDecoratedOracle().insideNotEvaluatedExpr,
                        oracle.getDecoratedOracle().insideAtomic,
                        oracle.getEnclosingLoopsCount(),
                        oracle.getEnclosingConditionalStmtsCount()
                );
            }
        }

        private void addTagReference(String uniqueName, TagRef astNode, CountingOracle<Oracle> oracle) {
            if (tags.containsKey(uniqueName)) {
                referencingNode.addReference(
                        tags.get(uniqueName),
                        Reference.Type.NORMAL,
                        astNode,
                        oracle.getDecoratedOracle().insideNotEvaluatedExpr,
                        oracle.getDecoratedOracle().insideAtomic,
                        oracle.getEnclosingLoopsCount(),
                        oracle.getEnclosingConditionalStmtsCount()
                );
            }
        }
    }

    /**
     * Class with block information about the current node.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class Oracle {
        private final boolean insideNotEvaluatedExpr;
        private final boolean insideAtomic;

        private Oracle() {
            this.insideNotEvaluatedExpr = false;
            this.insideAtomic = false;
        }

        private Oracle(boolean insideNotEvaluatedExpr, boolean insideAtomic) {
            this.insideNotEvaluatedExpr = insideNotEvaluatedExpr;
            this.insideAtomic = insideAtomic;
        }

        private Oracle modifyInsideNotEvaluatedExpr(boolean insideNotEvaluateExpr) {
            return new Oracle(insideNotEvaluateExpr, this.insideAtomic);
        }

        private Oracle modifyInsideAtomic(boolean insideAtomic) {
            return new Oracle(this.insideNotEvaluatedExpr, insideAtomic);
        }
    }

    /**
     * Helper enum type that represents a namespace in C.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private enum Namespace {
        ORDINARY,
        TAG,
    }
}
