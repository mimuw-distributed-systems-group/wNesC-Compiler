package pl.edu.mimuw.nesc.optimization;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import pl.edu.mimuw.nesc.ast.RID;
import pl.edu.mimuw.nesc.ast.StructSemantics;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.astutil.DeclaratorUtils;
import pl.edu.mimuw.nesc.astutil.TypeElementUtils;
import pl.edu.mimuw.nesc.declaration.object.ConstantDeclaration;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Cleaner that takes a list of top-level declarations and removes from it
 * the following elements:</p>
 * <ul>
 *     <li>unused global type definitions</li>
 *     <li>unused global structures and unions definitions</li>
 *     <li>enumerated types definitions if the types themselves are not used and
 *     all constants are unused</li>
 * </ul>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class TypeDeclarationsCleaner {
    /**
     * Queues with names of declarations to traverse.
     */
    private final Queue<String> objectNamesQueue;
    private final Queue<String> tagNamesQueue = new ArrayDeque<>();

    /**
     * Set with names of type definitions and enumeration constants that have
     * been visited.
     */
    private final Set<String> visitedObjects = new HashSet<>();

    /**
     * Set with names of tags that have been visited. A separate set is
     * necessary because the namespaces of tags and objects are different
     * in C.
     */
    private final Set<String> visitedTags = new HashSet<>();

    /**
     * Set with unnamed enums that have been visited.
     */
    private final Set<EnumRef> visitedUnnnamedEnums = new HashSet<>();

    /**
     * Map with definitions of tags.
     */
    private final ImmutableMap<String, TagRef> tags;

    /**
     * Map with definitions of functions.
     */
    private final ImmutableMap<String, FunctionDecl> functions;

    /**
     * Map with declarations of type definitions and variables and forward
     * declarations of functions.
     */
    private final ImmutableListMultimap<String, FullVariableDecl> variableDecls;

    /**
     * Map with names of enumeration constants as keys. Each name is mapped to
     * the AST node of the enumerated type it is defined in.
     */
    private final ImmutableMap<String, EnumRef> constants;

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
     * List with declarations after cleaning.
     */
    private Optional<ImmutableList<Declaration>> cleanedDeclarations = Optional.absent();

    /**
     * Get a builder that will create a type declarations cleaner.
     *
     * @return Newly created builder of a type declarations cleaner.
     */
    public static Builder builder() {
        return new Builder();
    }

    private TypeDeclarationsCleaner(PrivateBuilder builder) {
        this.objectNamesQueue = builder.buildObjectNamesQueue();
        this.tags = builder.buildTags();
        this.functions = builder.buildFunctions();
        this.variableDecls = builder.buildVariableDecls();
        this.objectsForRemoval = builder.buildObjectsForRemoval();
        this.tagsForRemoval = builder.buildTagsForRemoval();
        this.declarations = builder.buildDeclarations();
        this.constants = builder.buildConstants();
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

        initializeVisitedSets();
        traverseDeclarations();
        cleanedDeclarations = Optional.of(filterDeclarations());

        return cleanedDeclarations.get();
    }

    private void initializeVisitedSets() {
        visitedObjects.addAll(objectNamesQueue);
    }

    private void traverseDeclarations() {
        final LookingVisitor lookingVisitor = new LookingVisitor();

        while (!objectNamesQueue.isEmpty() || !tagNamesQueue.isEmpty()) {
            if (!objectNamesQueue.isEmpty()) {
                final String nextObject = objectNamesQueue.remove();

                if (functions.containsKey(nextObject)) {
                    functions.get(nextObject).traverse(lookingVisitor, null);
                } else if (variableDecls.containsKey(nextObject)) {
                    for (FullVariableDecl fullVariableDecl : variableDecls.get(nextObject)) {
                        for (TypeElement typeElement : fullVariableDecl.dataDecl.getModifiers()) {
                            typeElement.traverse(lookingVisitor, null);
                        }
                        fullVariableDecl.variableDecl.traverse(lookingVisitor, null);
                    }
                } else {
                    throw new RuntimeException("Cannot find object with name '" + nextObject + "'");
                }
            } else {
                tags.get(tagNamesQueue.remove()).traverse(lookingVisitor, null);
            }
        }
    }

    private ImmutableList<Declaration> filterDeclarations() {
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
     * Visitor that looks for references to type definitions and tags.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class LookingVisitor extends IdentityVisitor<Void> {
        @Override
        public Void visitTypename(Typename typename, Void arg) {
            lookAtTypename(typename);
            return null;
        }

        @Override
        public Void visitComponentTyperef(ComponentTyperef typename, Void arg) {
            lookAtTypename(typename);
            return null;
        }

        @Override
        public Void visitStructRef(StructRef node, Void arg) {
            lookAtTagRef(node);
            return null;
        }

        @Override
        public Void visitUnionRef(UnionRef node, Void arg) {
            lookAtTagRef(node);
            return null;
        }

        @Override
        public Void visitEnumRef(EnumRef node, Void arg) {
            lookAtTagRef(node);
            return null;
        }

        @Override
        public Void visitNxStructRef(NxStructRef node, Void arg) {
            lookAtTagRef(node);
            return null;
        }

        @Override
        public Void visitNxUnionRef(NxUnionRef node, Void arg) {
            lookAtTagRef(node);
            return null;
        }

        @Override
        public Void visitIdentifier(Identifier identifier, Void arg) {
            if (identifier.getUniqueName().isPresent()) {
                final String name = identifier.getUniqueName().get();

                if (constants.containsKey(name)) {
                    final EnumRef enumRef = constants.get(name);

                    if (enumRef.getUniqueName().isPresent()) {
                        lookAtTagRef(constants.get(name));
                    } else if (!visitedUnnnamedEnums.contains(enumRef)) {
                        visitedUnnnamedEnums.add(enumRef);
                        enumRef.traverse(this, null);
                    }

                    objectsForRemoval.remove(identifier.getUniqueName().get());
                }
            }

            return null;
        }

        private void lookAtTypename(Typename typename) {
            final String name = typename.getUniqueName();

            if (variableDecls.containsKey(name) && !visitedObjects.contains(name)) {
                objectNamesQueue.add(name);
                visitedObjects.add(name);
            }

            objectsForRemoval.remove(name);
        }

        private void lookAtTagRef(TagRef tagRef) {
            if (!tagRef.getUniqueName().isPresent()) {
                return;
            }

            final String name = tagRef.getUniqueName().get();

            if (tags.containsKey(name) && !visitedTags.contains(name)) {
                tagNamesQueue.add(name);
                visitedTags.add(name);
            }

            tagsForRemoval.remove(name);
        }
    }

    /**
     * Visitor that decides if declarations are to be preserved and cleans them.
     * <code>true</code> is returned if the declaration is preserved.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class FilteringVisitor extends ExceptionVisitor<Boolean, Void> {
        @Override
        public Boolean visitFunctionDecl(FunctionDecl declaration, Void arg) {
            return true;
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

                return preserve || !declaration.getDeclarations().isEmpty();
            }

            return !declaration.getDeclarations().isEmpty();
        }

        @Override
        public Boolean visitVariableDecl(VariableDecl declaration, Void arg) {
            return !objectsForRemoval.contains(DeclaratorUtils.getUniqueName(
                                declaration.getDeclarator().get()).get());
        }

        private boolean filterEnumeration(EnumRef enumRef) {
            // Check if all constants can be removed
            if (enumRef.getDeclaration().getConstants().isPresent()) {
                final List<String> constantsNames = new ArrayList<>();
                for (ConstantDeclaration cstDeclaration : enumRef.getDeclaration().getConstants().get()) {
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
        Queue<String> buildObjectNamesQueue();
        ImmutableMap<String, TagRef> buildTags();
        ImmutableMap<String, FunctionDecl> buildFunctions();
        ImmutableListMultimap<String, FullVariableDecl> buildVariableDecls();
        ImmutableMap<String, EnumRef> buildConstants();
        Set<String> buildObjectsForRemoval();
        Set<String> buildTagsForRemoval();
        ImmutableList<Declaration> buildDeclarations();
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
        private final ImmutableList.Builder<Declaration> declarationsBuilder = ImmutableList.builder();

        /**
         * Private constructor to limit its accessibility.
         */
        private Builder() {
        }

        /**
         * Add declarations from the given collection for cleaning. The order of
         * declarations after cleaning is the same as adding them and returning
         * by the iterator fo the given collection.
         *
         * @param declarations Declarations to add for cleaning.
         * @return <code>this</code>
         */
        public Builder addDeclarations(Collection<? extends Declaration> declarations) {
            this.declarationsBuilder.addAll(declarations);
            return this;
        }

        public TypeDeclarationsCleaner build() {
            return new TypeDeclarationsCleaner(new RealBuilder(declarationsBuilder.build()));
        }
    }

    /**
     * Object that builds particular elements of the type cleaner.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class RealBuilder extends ExceptionVisitor<Void, Void> implements PrivateBuilder {
        /**
         * List with declarations that will be cleaned.
         */
        private final ImmutableList<Declaration> declarations;

        /**
         * Objects for building the other elements.
         */
        private final ImmutableMap.Builder<String, FunctionDecl> functionsBuilder = ImmutableMap.builder();
        private final ImmutableMap.Builder<String, TagRef> tagsBuilder = ImmutableMap.builder();
        private final ImmutableListMultimap.Builder<String, FullVariableDecl> variableDeclsBuilder = ImmutableListMultimap.builder();
        private final ImmutableMap.Builder<String, EnumRef> constantsBuilder = ImmutableMap.builder();
        private final Set<String> objectsForRemoval = new HashSet<>();
        private final Set<String> tagsForRemoval = new HashSet<>();
        private final Queue<String> objectNamesQueue = new ArrayDeque<>();
        private boolean visited = false;

        private RealBuilder(ImmutableList<Declaration> declarations) {
            checkNotNull(declarations, "declaration cannot be null");
            this.declarations = declarations;
        }

        @Override
        public Queue<String> buildObjectNamesQueue() {
            visitDeclarations();
            return objectNamesQueue;
        }

        @Override
        public ImmutableMap<String, TagRef> buildTags() {
            visitDeclarations();
            return tagsBuilder.build();
        }

        @Override
        public ImmutableMap<String, FunctionDecl> buildFunctions() {
            visitDeclarations();
            return functionsBuilder.build();
        }

        @Override
        public ImmutableListMultimap<String, FullVariableDecl> buildVariableDecls() {
            visitDeclarations();
            return variableDeclsBuilder.build();
        }

        @Override
        public ImmutableMap<String, EnumRef> buildConstants() {
            visitDeclarations();
            return constantsBuilder.build();
        }

        @Override
        public Set<String> buildObjectsForRemoval() {
            visitDeclarations();
            return objectsForRemoval;
        }

        @Override
        public Set<String> buildTagsForRemoval() {
            visitDeclarations();
            return tagsForRemoval;
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
            final String funName = DeclaratorUtils.getUniqueName(declaration.getDeclarator()).get();
            functionsBuilder.put(funName, declaration);
            objectNamesQueue.add(funName);
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

            if (isTypedef || declaration.getDeclarations().isEmpty()) {
                // Visit potential tags references
                for (TypeElement typeElement : declaration.getModifiers()) {
                    if (typeElement instanceof TagRef) {
                        addTag((TagRef) typeElement);
                    }
                }
            }

            for (Declaration innerDecl : declaration.getDeclarations()) {
                final VariableDecl variableDecl = (VariableDecl) innerDecl;
                final String name = DeclaratorUtils.getUniqueName(variableDecl.getDeclarator()).get();

                variableDeclsBuilder.put(name, new FullVariableDecl(declaration, variableDecl));

                if (isTypedef) {
                    objectsForRemoval.add(name);
                } else {
                    objectNamesQueue.add(name);
                }
            }

            return null;
        }

        private void addTag(TagRef tagRef) {
            if (tagRef instanceof EnumRef && tagRef.getSemantics() != StructSemantics.OTHER) {
                for (Declaration declaration : tagRef.getFields()) {
                    final Enumerator enumerator = (Enumerator) declaration;
                    objectsForRemoval.add(enumerator.getUniqueName());
                    constantsBuilder.put(enumerator.getUniqueName(), (EnumRef) tagRef);
                }
            }

            if (!tagRef.getUniqueName().isPresent()) {
                return;
            }

            final String tagName = tagRef.getUniqueName().get();

            if (tagRef.getSemantics() != StructSemantics.OTHER) {
                tagsBuilder.put(tagName, tagRef);
            }
            tagsForRemoval.add(tagName);
        }
    }

    /**
     * Helper class that allows associating a variable declaration with data
     * declaration that contains it.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class FullVariableDecl {
        private final VariableDecl variableDecl;
        private final DataDecl dataDecl;

        private FullVariableDecl(DataDecl dataDecl, VariableDecl variableDecl) {
            checkNotNull(dataDecl, "data declaration cannot be null");
            checkNotNull(variableDecl, "variable declaration cannot be null");
            this.dataDecl = dataDecl;
            this.variableDecl = variableDecl;
        }
    }
}
