package pl.edu.mimuw.nesc.refsgraph;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import pl.edu.mimuw.nesc.ast.StructSemantics;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.astutil.DeclaratorUtils;
import pl.edu.mimuw.nesc.astutil.TypeElementUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

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
            final EntityNode.Kind kind = TypeElementUtils.isTypedef(declaration.getModifiers())
                    ? EntityNode.Kind.TYPE_DEFINITION
                    : EntityNode.Kind.VARIABLE;

            for (TypeElement typeElement : declaration.getModifiers()) {
                if (typeElement instanceof TagRef) {
                    addTag((TagRef) typeElement);
                }
            }

            for (Declaration innerDecl : declaration.getDeclarations()) {
                if (innerDecl instanceof VariableDecl) {
                    final VariableDecl variableDecl = (VariableDecl) innerDecl;
                    final String uniqueName = DeclaratorUtils.getUniqueName(
                            variableDecl.getDeclarator().get()).get();
                    final Optional<EntityNode> optNode = Optional.fromNullable(ordinaryIds.get(uniqueName));

                    if (optNode.isPresent()) {
                        checkState(optNode.get().getKind() == kind, "previous kind differs");
                    } else {
                        ordinaryIds.put(uniqueName, new EntityNode(uniqueName, kind));
                    }
                } else {
                    throw new RuntimeException("unexpected inner class of data declaration '"
                            + innerDecl.getClass().getCanonicalName() + "'");
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
            declaration.traverse(new ReferencesCreatorVisitor(ordinaryIds.get(uniqueName), ordinaryIds, tags),
                    new Oracle());
            return null;
        }

        @Override
        public Void visitDataDecl(DataDecl declaration, Void arg) {
            // Add references from tag definitions

            Optional<EntityNode> typeNode = Optional.absent();

            for (TypeElement typeElement : declaration.getModifiers()) {
                if (typeElement instanceof TagRef) {
                    checkState(!typeNode.isPresent(), "multiple conflicting type specifiers in a declaration");
                    final TagRef tagRef = (TagRef) typeElement;
                    addEntitiesReferencedByTag(tagRef);

                    if (tagRef.getUniqueName().isPresent()) {
                        typeNode = Optional.of(tags.get(tagRef.getUniqueName().get()));
                    }
                } else if (typeElement instanceof Typename) {
                    checkState(!typeNode.isPresent(), "multiple conflicting type specifiers in a declaration");
                    final Typename typename = (Typename) typeElement;
                    typeNode = Optional.of(ordinaryIds.get(typename.getUniqueName()));
                }
            }

            // Add references

            for (Declaration innerDecl : declaration.getDeclarations()) {
                final VariableDecl variableDecl = (VariableDecl) innerDecl;
                final EntityNode referencingNode = ordinaryIds.get(DeclaratorUtils.getUniqueName(
                        variableDecl.getDeclarator()).get());
                variableDecl.traverse(new ReferencesCreatorVisitor(referencingNode, ordinaryIds, tags),
                        new Oracle());

                if (typeNode.isPresent()) {
                    referencingNode.addReference(typeNode.get(), Reference.Type.NORMAL, false, false);
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
                tagRef.traverse(new ReferencesCreatorVisitor(tags.get(tagRef.getUniqueName().get()), ordinaryIds, tags),
                        new Oracle());
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
                            Reference.Type.NORMAL, false, false);
                }
            } else {
                final ArrayList<EntityNode> constantsNodes = new ArrayList<>();
                for (Declaration declaration : enumRef.getFields()) {
                    constantsNodes.add(ordinaryIds.get(((Enumerator) declaration).getUniqueName()));
                }

                for (EntityNode referencing : constantsNodes) {
                    for (EntityNode referred : constantsNodes) {
                        if (referencing != referred) {
                            referencing.addReference(referred, Reference.Type.NORMAL, false, false);
                        }
                    }
                }
            }
        }

        @Override
        public Void visitEnumerator(Enumerator enumerator, Void arg) {
            if (enumerator.getValue().isPresent()) {
                enumerator.getValue().get().traverse(new ReferencesCreatorVisitor(
                        ordinaryIds.get(enumerator.getUniqueName()), ordinaryIds, tags),
                        new Oracle());
            }
            return null;
        }
    }

    /**
     * Visitor that adds references to entities it encounters.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class ReferencesCreatorVisitor extends IdentityVisitor<Oracle> {
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
        public Oracle visitAtomicStmt(AtomicStmt stmt, Oracle oracle) {
            return oracle.modifyInsideAtomic(true);
        }

        @Override
        public Oracle visitCompoundStmt(CompoundStmt stmt, Oracle oracle) {
            return oracle.modifyInsideAtomic(oracle.insideAtomic
                    || stmt.getAtomicVariableUniqueName() != null && stmt.getAtomicVariableUniqueName().isPresent());
        }

        @Override
        public Oracle visitSizeofExpr(SizeofExpr expr, Oracle oracle) {
            return oracle.modifyInsideNotEvaluatedExpr(true);
        }

        @Override
        public Oracle visitAlignofExpr(AlignofExpr expr, Oracle oracle) {
            return oracle.modifyInsideNotEvaluatedExpr(true);
        }

        @Override
        public Oracle visitSizeofType(SizeofType expr, Oracle oracle) {
            return oracle.modifyInsideNotEvaluatedExpr(true);
        }

        @Override
        public Oracle visitAlignofType(AlignofType expr, Oracle oracle) {
            return oracle.modifyInsideNotEvaluatedExpr(true);
        }

        @Override
        public Oracle visitIdentifier(Identifier identifier, Oracle oracle) {
            if (ignoredIdentifiers.contains(identifier)) {
                return oracle;
            }

            if (identifier.getUniqueName().isPresent()) {
                addOrdinaryIdReference(identifier.getUniqueName().get(), oracle);
            }

            return oracle;
        }

        @Override
        public Oracle visitFunctionCall(FunctionCall expr, Oracle oracle) {
            if (expr.getFunction() instanceof Identifier) {
                final Identifier identifier = (Identifier) expr.getFunction();

                if (identifier.getUniqueName().isPresent()
                        && ordinaryIds.containsKey(identifier.getUniqueName().get())) {
                    this.referencingNode.addReference(ordinaryIds.get(identifier.getUniqueName().get()),
                            Reference.Type.CALL, oracle.insideNotEvaluatedExpr, oracle.insideAtomic);
                }

                ignoredIdentifiers.add(identifier);
            }

            return oracle;
        }

        @Override
        public Oracle visitTypename(Typename typename, Oracle oracle) {
            addOrdinaryIdReference(typename.getUniqueName(), oracle);
            return oracle;
        }

        @Override
        public Oracle visitComponentTyperef(ComponentTyperef typename, Oracle oracle) {
            addOrdinaryIdReference(typename.getUniqueName(), oracle);
            return oracle;
        }

        @Override
        public Oracle visitStructRef(StructRef tagRef, Oracle oracle) {
            tagReference(tagRef, oracle);
            return oracle;
        }

        @Override
        public Oracle visitUnionRef(UnionRef tagRef, Oracle oracle) {
            tagReference(tagRef, oracle);
            return oracle;
        }

        @Override
        public Oracle visitEnumRef(EnumRef tagRef, Oracle oracle) {
            tagReference(tagRef, oracle);
            return oracle;
        }

        @Override
        public Oracle visitNxStructRef(NxStructRef tagRef, Oracle oracle) {
            tagReference(tagRef, oracle);
            return oracle;
        }

        @Override
        public Oracle visitNxUnionRef(NxUnionRef tagRef, Oracle oracle) {
            tagReference(tagRef, oracle);
            return oracle;
        }

        private void tagReference(TagRef tagRef, Oracle oracle) {
            if (tagRef.getUniqueName().isPresent()) {
                addTagReference(tagRef.getUniqueName().get(), oracle);
            }
        }

        private void addOrdinaryIdReference(String uniqueName, Oracle oracle) {
            if (ordinaryIds.containsKey(uniqueName)) {
                referencingNode.addReference(
                        ordinaryIds.get(uniqueName),
                        Reference.Type.NORMAL,
                        oracle.insideNotEvaluatedExpr,
                        oracle.insideAtomic
                );
            }
        }

        private void addTagReference(String uniqueName, Oracle oracle) {
            if (tags.containsKey(uniqueName)) {
                referencingNode.addReference(
                        tags.get(uniqueName),
                        Reference.Type.NORMAL,
                        oracle.insideNotEvaluatedExpr,
                        oracle.insideAtomic
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
}
