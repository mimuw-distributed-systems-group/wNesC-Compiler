package pl.edu.mimuw.nesc.compilation;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import pl.edu.mimuw.nesc.FileData;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.astutil.AstUtils;
import pl.edu.mimuw.nesc.preprocessor.directive.IncludeDirective;
import pl.edu.mimuw.nesc.preprocessor.directive.PreprocessorDirective;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * <p>Class responsible for generating code for GCC which is the default
 * backend.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class DefaultCodeGenerator {
    /**
     * Stack that is used for emitting the declarations in a proper order.
     */
    private final Deque<DeclarationsNode> declarationsStack = new ArrayDeque<>();

    /**
     * Set with paths of header files whose declarations has been (or are
     * currently) emitted.
     */
    private final Set<String> outputtedHeaderFiles = new HashSet<>();

    /**
     * Builder of a list with declarations that constitute the while compiled
     * program.
     */
    private final ImmutableList.Builder<Declaration> outputBuilder = ImmutableList.builder();

    /**
     * Map with names of NesC interfaces and components as keys and their data
     * objects as values.
     */
    private final ImmutableMap<String, ProcessedNescData> nescDeclarations;

    /**
     * Name of the main configuration of the project.
     */
    private final String mainConfigurationName;

    /**
     * Map with paths of header files as keys and objects that represent their
     * contents as values.
     */
    private final ImmutableMap<String, FileData> headerFiles;

    /**
     * List with paths of files that have been included by default (in the same
     * order).
     */
    private final ImmutableList<String> defaultIncludeFiles;

    /**
     * Set with intermediate functions created for the application.
     */
    private final ImmutableSet<FunctionDecl> intermediateFunctions;

    /**
     * Visitor responsible for pushing visited NesC data objects. It pushes
     * unconditionally.
     */
    private final ProcessedNescData.Visitor<Void, Void> nescEntityPushVisitor = new ProcessedNescData.Visitor<Void, Void>() {
        @Override
        public Void visit(PreservedData preservedData, Void arg) {
            final FileData fileData = preservedData.getFileData();
            final NescDecl nescDecl = (NescDecl) fileData.getEntityRoot().get();

            if (nescDecl instanceof Interface) {
                pushInterface((Interface) nescDecl, fileData);
            } else if (nescDecl instanceof Component) {
                pushNongenericComponent((Component) nescDecl, fileData);
            } else {
                throw new RuntimeException("unexpected NesC declaration class '" + nescDecl.getClass() + "'");
            }

            return null;
        }

        @Override
        public Void visit(InstantiatedData instantiatedData, Void arg) {
            final Component component = instantiatedData.getComponent();
            final PeekingIterator<IncludeDirective> emptyInclIt =
                    Iterators.peekingIterator(Collections.<IncludeDirective>emptyIterator());

            // Push the implementation of the component
            final Optional<PeekingIterator<Declaration>> implIt =
                    getImplementationIterator(component.getImplementation());
            if (implIt.isPresent()) {
                declarationsStack.push(new DeclarationsNode(emptyInclIt, implIt.get(),
                        DeclarationsNode.NodeKind.NESC_ENTITY));
            }

            // Push the specification of the component
            declarationsStack.push(new DeclarationsNode(emptyInclIt,
                    Iterators.peekingIterator(component.getDeclarations().iterator()),
                    DeclarationsNode.NodeKind.NESC_ENTITY));

            // Get data of the parent component
            final Optional<ProcessedNescData> optGenericComponentData =
                    Optional.fromNullable(nescDeclarations.get(instantiatedData.getInstantiatedComponentName()));
            checkState(optGenericComponentData.isPresent(), "absent generic component '%s'",
                    instantiatedData.getInstantiatedComponentName());
            final PreservedData genericData = (PreservedData) optGenericComponentData.get();

            // Push the parent component (only external declarations)
            if (!genericData.isOutputted()) {
                genericData.outputted();
                declarationsStack.push(new DeclarationsNode(
                        getIncludesIterator(genericData.getPreprocessorDirectives()),
                        Iterators.peekingIterator(genericData.getExtdefs().iterator()),
                        DeclarationsNode.NodeKind.EXTERNAL
                ));
            }

            return null;
        }

        private void pushInterface(Interface interfaceAst, FileData fileData) {
            /* For interface only external declarations and included files are
               added. */
            declarationsStack.push(new DeclarationsNode(
                    getIncludesIterator(fileData.getPreprocessorDirectives()),
                    Iterators.peekingIterator(fileData.getExtdefs().iterator()),
                    DeclarationsNode.NodeKind.EXTERNAL
            ));
        }

        private void pushNongenericComponent(Component component, FileData fileData) {
            // Prepare the include directives iterator
            final PeekingIterator<IncludeDirective> includeIt =
                    getIncludesIterator(fileData.getPreprocessorDirectives());

            // Push the implementation
            final Optional<PeekingIterator<Declaration>> implIt =
                    getImplementationIterator(component.getImplementation());
            if (implIt.isPresent()) {
                declarationsStack.push(new DeclarationsNode(includeIt, implIt.get(),
                        DeclarationsNode.NodeKind.NESC_ENTITY));
            }

            // Push the specification
            declarationsStack.push(new DeclarationsNode(
                    includeIt,
                    Iterators.peekingIterator(component.getDeclarations().iterator()),
                    DeclarationsNode.NodeKind.NESC_ENTITY
            ));

            // Push external definitions
            declarationsStack.push(new DeclarationsNode(
                    includeIt,
                    Iterators.peekingIterator(fileData.getExtdefs().iterator()),
                    DeclarationsNode.NodeKind.EXTERNAL
            ));
        }

        private Optional<PeekingIterator<Declaration>> getImplementationIterator(Implementation impl) {
            final Optional<LinkedList<Declaration>> implDecls = getImplDeclarations(impl);
            return implDecls.isPresent()
                    ? Optional.of(Iterators.peekingIterator(implDecls.get().iterator()))
                    : Optional.<PeekingIterator<Declaration>>absent();
        }
    };

    /**
     * Visitor responsible for outputting declarations. The argument specifies
     * if a forward declaration of a function shall be output instead of
     * a function definition. Definitions of NesC attributes are not emitted.
     */
    private final ExceptionVisitor<Void, Boolean> declarationsProcessor = new ExceptionVisitor<Void, Boolean>() {
        @Override
        public Void visitDataDecl(DataDecl dataDecl, Boolean shortenFunDefs) {
            if (!AstUtils.isNescAttributeDefinition(dataDecl)) {
                outputBuilder.add(dataDecl);
            }
            return null;
        }

        @Override
        public Void visitFunctionDecl(FunctionDecl funDecl, Boolean shortenFunDefs) {
            final Declaration outputDeclaration = shortenFunDefs
                    ? AstUtils.createForwardDeclaration(funDecl)
                    : funDecl;
            outputBuilder.add(outputDeclaration);
            return null;
        }

        @Override
        public Void visitComponentsUses(ComponentsUses components, Boolean shortenFunDefs) {
            final Iterator<ComponentRef> componentsRefs = components.getComponents().descendingIterator();

            // Add the components in the reverse order
            while (componentsRefs.hasNext()) {
                final ComponentRef componentRef = componentsRefs.next();
                DefaultCodeGenerator.this.pushNescEntity(componentRef.getName().getName());
            }

            return null;
        }

        @Override
        public Void visitProvidesInterface(ProvidesInterface providesInterface, Boolean shortenFunDefs) {
            pushInterfaces(providesInterface.getDeclarations());
            return null;
        }

        @Override
        public Void visitRequiresInterface(RequiresInterface requiresInterface, Boolean shortenFunDef) {
            pushInterfaces(requiresInterface.getDeclarations());
            return null;
        }

        private void pushInterfaces(LinkedList<Declaration> declarations) {
            final Iterator<Declaration> declIt = declarations.descendingIterator();

            // Add the interfaces in the reverse order
            while (declIt.hasNext()) {
                final Declaration declaration = declIt.next();
                if (!(declaration instanceof InterfaceRef)) {
                    continue;
                }

                final InterfaceRef interfaceRef = (InterfaceRef) declaration;
                DefaultCodeGenerator.this.pushNescEntity(interfaceRef.getName().getName());
            }
        }

        @Override
        public Void visitExtensionDecl(ExtensionDecl extensionDecl, Boolean shortenFunDef) {
            outputBuilder.add(extensionDecl);
            return null;
        }

        @Override
        public Void visitEmptyDecl(EmptyDecl emptyDecl, Boolean shortenFunDef) {
            // empty declarations are ignored
            return null;
        }

        @Override
        public Void visitRpConnection(RpConnection connection, Boolean shortenFunDef) {
            // connections are ignored
            return null;
        }

        @Override
        public Void visitEqConnection(EqConnection connection, Boolean shortenFunDef) {
            // connections are ignored
            return null;
        }
    };

    /**
     * Visitor that is responsible for adding definitions of all function from
     * NesC declarations whose data objects it visits (except from generic
     * components).
     */
    private final ProcessedNescData.Visitor<Void, Void> nescFunDefsAddVisitor = new ProcessedNescData.Visitor<Void, Void>() {
        @Override
        public Void visit(InstantiatedData instantiatedData, Void arg) {
            addFunctionDefinitions(instantiatedData.getComponent().getImplementation());
            return null;
        }

        @Override
        public Void visit(PreservedData preservedData, Void arg) {
            final NescDecl entityRoot = preservedData.getEntityRoot();

            if (entityRoot instanceof Component) {
                final Component component = (Component) entityRoot;
                if (!component.getIsAbstract()) {
                    addFunctionDefinitions(component.getImplementation());
                }
            } else if (entityRoot instanceof Interface) {
                // do nothing
            } else {
                throw new RuntimeException("unexpected NesC declaration class '" + entityRoot.getClass() + "'");
            }

            return null;
        }

        private void addFunctionDefinitions(Implementation impl) {
            final Optional<LinkedList<Declaration>> declarations = getImplDeclarations(impl);

            if (declarations.isPresent()) {
                for (Declaration declaration : declarations.get()) {
                    if (declaration instanceof FunctionDecl) {
                        outputBuilder.add(declaration);
                    }
                }
            }
        }
    };

    /**
     * Create a new builder that has the name of the main configuration of the
     * application set to the value of the parameter.
     *
     * @param mainConfigurationName Name of the main configuration used in the
     *                              created code generator.
     * @return Newly created builder object that creates generators starting
     *         their runs in the configuration with given name.
     */
    public static Builder builder(String mainConfigurationName) {
        checkNotNull(mainConfigurationName, "name of the main configuration cannot be null");
        checkArgument(!mainConfigurationName.isEmpty(), "name of the main configuration cannot be an empty string");
        return new Builder(mainConfigurationName);
    }

    private DefaultCodeGenerator(Builder builder) {
        this.headerFiles = builder.buildHeaderFiles();
        this.nescDeclarations = builder.buildNescEntities();
        this.defaultIncludeFiles = ImmutableList.copyOf(builder.defaultIncludeFiles);
        this.mainConfigurationName = builder.mainConfigurationName;
        this.intermediateFunctions = builder.intermediateFunsBuilder.build();
    }

    /**
     * Generates and returns the C declarations for the application in proper
     * order. This method shall be called exactly once.
     *
     * @return List with C declarations that constitute the program in proper
     *         order.
     */
    public ImmutableList<Declaration> generate() {
        outputFilesIncludedByDefault();
        outputMainConfiguration();
        outputIntermediateFunsForwardDecls();
        outputFunctionsDefinitions(); // except external definitions
        return outputBuilder.build();
    }

    private void outputFilesIncludedByDefault() {
        for (String filePath : defaultIncludeFiles) {
            pushHeaderFile(filePath);
            output();
        }
    }

    private void outputMainConfiguration() {
        pushNescEntity(mainConfigurationName);
        output();
    }

    private void pushHeaderFile(String filePath) {
        if (outputtedHeaderFiles.contains(filePath)) {
            return;
        }

        outputtedHeaderFiles.add(filePath);
        final Optional<FileData> optHeaderFileData = Optional.fromNullable(headerFiles.get(filePath));
        checkState(optHeaderFileData.isPresent(), "absent file '%s'", filePath);
        final FileData headerFileData = optHeaderFileData.get();

        // Create the node and push it
        declarationsStack.push(new DeclarationsNode(
                getIncludesIterator(headerFileData.getPreprocessorDirectives()),
                Iterators.peekingIterator(headerFileData.getExtdefs().iterator()),
                DeclarationsNode.NodeKind.EXTERNAL
        ));
    }

    private void pushNescEntity(String name) {
        // Find the NesC entity
        final Optional<ProcessedNescData> optNescData = Optional.fromNullable(nescDeclarations.get(name));
        checkState(optNescData.isPresent(), "absent NesC entity '%s'", name);
        final ProcessedNescData nescData = optNescData.get();

        // Check and set 'outputted' flag
        if (nescData.isOutputted()) {
            return;
        }
        nescData.outputted();

        // Create nodes and push them
        nescData.accept(nescEntityPushVisitor, null);
    }

    private PeekingIterator<IncludeDirective> getIncludesIterator(List<PreprocessorDirective> directives) {
        final Iterator<IncludeDirective> includeIt = FluentIterable.from(directives)
                .filter(IncludeDirective.class)
                .iterator();
        return Iterators.peekingIterator(includeIt);
    }

    private void output() {
        while (!declarationsStack.isEmpty()) {
            final DeclarationsNode node = declarationsStack.peek();
            final Optional<DeclarationsNode.NextEntity> nextEntity
                    = node.hasNextEntity();

            if (!nextEntity.isPresent()) {
                declarationsStack.pop();
            } else {
                switch (nextEntity.get()) {
                    case INCLUDE_DIRECTIVE:
                        final IncludeDirective include = node.nextIncludeDirective();
                        pushHeaderFile(include.getFilePath().get());
                        break;
                    case DECLARATION:
                        node.nextDeclaration().accept(declarationsProcessor,
                                node.getKind() == DeclarationsNode.NodeKind.NESC_ENTITY);
                        break;
                    default:
                        throw new RuntimeException("unexpected next entity kind '"
                                + nextEntity.get() + "'");
                }
            }
        }
    }

    private void outputIntermediateFunsForwardDecls() {
        for (FunctionDecl funDecl : intermediateFunctions) {
            outputBuilder.add(AstUtils.createForwardDeclaration(funDecl));
        }
    }

    private void outputFunctionsDefinitions() {
        // Intermediate functions
        for (FunctionDecl funDecl : intermediateFunctions) {
            outputBuilder.add(funDecl);
        }

        // Functions from modules
        for (ProcessedNescData nescData : nescDeclarations.values()) {
            nescData.accept(nescFunDefsAddVisitor, null);
        }
    }

    private Optional<LinkedList<Declaration>> getImplDeclarations(Implementation impl) {
        if (impl instanceof ModuleImpl) {
            return Optional.of(((ModuleImpl) impl).getDeclarations());
        } else if (impl instanceof ConfigurationImpl) {
            return Optional.of(((ConfigurationImpl) impl).getDeclarations());
        } else if (impl instanceof BinaryComponentImpl) {
            return Optional.absent();
        } else {
            throw new RuntimeException("unexpected implementation subclass '"
                    + impl.getClass() + "'");
        }
    }

    /**
     * <p>Class that provides a source of declarations from the same scope, i.e.
     * global scope or the implementation scope of a module.</p>
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class DeclarationsNode {
        /**
         * Iterator of all 'include' directives.
         */
        private final PeekingIterator<IncludeDirective> includesIterator;

        /**
         * Iterator of declarations from a single scope.
         */
        private final PeekingIterator<Declaration> declarationsIterator;

        /**
         * Kind of this node.
         */
        private final NodeKind kind;

        /**
         * Initializes this object by storing given arguments in member fields.
         */
        private DeclarationsNode(PeekingIterator<IncludeDirective> includesIterator,
                    PeekingIterator<Declaration> declarationsIterator,
                    NodeKind kind) {
            checkNotNull(includesIterator, "the iterator of include directives cannot be null");
            checkNotNull(declarationsIterator, "the iterator of declarations cannot be null");
            checkNotNull(kind, "kind of the node cannot be null");

            this.includesIterator = includesIterator;
            this.declarationsIterator = declarationsIterator;
            this.kind = kind;
        }

        /**
         * Check if there is an entity in this node.
         *
         * @return Kind of the next entity in this node. The object is absent
         *         if there is no next entity in this node.
         */
        private Optional<NextEntity> hasNextEntity() {
            if (!includesIterator.hasNext() && !declarationsIterator.hasNext()) {
                return Optional.absent();
            } else if (includesIterator.hasNext() && !declarationsIterator.hasNext()) {
                return Optional.of(NextEntity.INCLUDE_DIRECTIVE);
            } else if (!includesIterator.hasNext() && declarationsIterator.hasNext()) {
                return Optional.of(NextEntity.DECLARATION);
            } else {
                /* There is simultaneously an include directive and
                   a declaration. */
                final PreprocessorDirective.TokenLocation includeLoc =
                        includesIterator.peek().getHashLocation();
                final Location declarationLoc = declarationsIterator.peek().getLocation();

                int cmpRes = Integer.compare(includeLoc.getLine(), declarationLoc.getLine());
                if (cmpRes == 0) {
                    cmpRes = Integer.compare(includeLoc.getColumn(), declarationLoc.getColumn());
                }
                return cmpRes > 0
                        ? Optional.of(NextEntity.DECLARATION)
                        : Optional.of(NextEntity.INCLUDE_DIRECTIVE);
            }
        }

        /**
         * Get the kind of this node.
         *
         * @return Kind of this node.
         */
        private NodeKind getKind() {
            return kind;
        }

        /**
         * Get the next declaration in this node.
         *
         * @return Next declaration from this node.
         * @throws NoSuchElementException There is no next declaration.
         */
        private Declaration nextDeclaration() {
            return declarationsIterator.next();
        }

        /**
         * Get the next include directive in this node.
         *
         * @return Next include directive from this node.
         * @throws NoSuchElementException There is no more include directives.
         */
        private IncludeDirective nextIncludeDirective() {
            return includesIterator.next();
        }

        /**
         * Enumeration type that represents next object in this node.
         *
         * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
         */
        private enum NextEntity {
            INCLUDE_DIRECTIVE,
            DECLARATION,
        }

        /**
         * Enumeration type that represents the type of this node.
         *
         * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
         */
        private enum NodeKind {
            /**
             * The declarations are directly from a NesC entity (e.g. from the
             * specification of a module or the implementation of
             * a configuration).
             */
            NESC_ENTITY,

            /**
             * Declarations come from a header file or are external declarations
             * before a NesC entity.
             */
            EXTERNAL,
        }
    }

    /**
     * Builder for a default code generator.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static final class Builder {
        /**
         * Data needed to construct a default code generator.
         */
        private final List<FileData> originalData = new ArrayList<>();
        private final List<Component> instantiatedComponents = new ArrayList<>();
        private final List<String> defaultIncludeFiles = new ArrayList<>();
        private final ImmutableSet.Builder<FunctionDecl> intermediateFunsBuilder = ImmutableSet.builder();
        private final String mainConfigurationName;

        private Builder(String mainConfigurationName) {
            this.mainConfigurationName = mainConfigurationName;
        }

        /**
         * Add all file data objects from the given list.
         *
         * @param originalData List with objects to add.
         * @return <code>this</code>
         */
        public Builder addOriginalData(Collection<FileData> originalData) {
            this.originalData.addAll(originalData);
            return this;
        }

        /**
         * Add all components from the given collection as instantiated
         * components.
         *
         * @param components Collection with instantiated components.
         * @return <code>this</code>
         */
        public Builder addInstantiatedComponents(Collection<Component> components) {
            this.instantiatedComponents.addAll(components);
            return this;
        }

        /**
         * Add files that are included by default (declarations from them will
         * be output as first in the order of adding and in the order they
         * appear on the given list).
         *
         * @param fileNames List of names of files included by default.
         * @return <code>this</code>
         */
        public Builder addDefaultIncludeFiles(List<String> fileNames) {
            this.defaultIncludeFiles.addAll(fileNames);
            return this;
        }

        /**
         * Add definitions of intermediate functions.
         *
         * @param functions Collection with definitions of intermediate
         *                  functions to add.
         * @return <code>this</code>
         */
        public Builder addIntermediateFunctions(Collection<FunctionDecl> functions) {
            this.intermediateFunsBuilder.addAll(functions);
            return this;
        }

        private void validate() {
            final Set<String> usedNames = new HashSet<>();
            final Set<String> filesNames = new HashSet<>();
            boolean mainConfigurationAdded = false;

            for (FileData fileData : originalData) {
                if (fileData.getEntityRoot().isPresent()) {
                    final NescDecl nescDecl = (NescDecl) fileData.getEntityRoot().get();
                    final String nescName = nescDecl.getName().getName();
                    if (!usedNames.add(nescName)) {
                        throw new IllegalStateException("multiple NesC declarations with name '" + nescName
                                + "' have been added");
                    }

                    if (nescDecl instanceof Configuration && !mainConfigurationAdded) {
                        final Configuration configuration = (Configuration) nescDecl;
                        mainConfigurationAdded = mainConfigurationName.equals(nescName) && !configuration.getIsAbstract();
                    }
                } else {
                    filesNames.add(fileData.getFilePath());
                }
            }

            for (Component component : instantiatedComponents) {
                if (!usedNames.add(component.getName().getName())) {
                    throw new IllegalStateException("multiple NesC declarations with name '" + component.getName().getName()
                        + "' have been added");
                }
            }

            checkState(mainConfigurationAdded, "main configuration '%s' has not been added",
                    mainConfigurationName);
            checkState(filesNames.containsAll(defaultIncludeFiles),
                    "there are files included by default whose file datas have not been added");
        }

        public DefaultCodeGenerator build() {
            validate();
            return new DefaultCodeGenerator(this);
        }

        private ImmutableMap<String, FileData> buildHeaderFiles() {
            final ImmutableMap.Builder<String, FileData> headerFilesBuilder = ImmutableMap.builder();
            for (FileData fileData : originalData) {
                if (!fileData.getEntityRoot().isPresent()) {
                    headerFilesBuilder.put(fileData.getFilePath(), fileData);
                }
            }
            return headerFilesBuilder.build();
        }

        private ImmutableMap<String, ProcessedNescData> buildNescEntities() {
            final ImmutableMap.Builder<String, ProcessedNescData> nescEntitiesBuilder = ImmutableMap.builder();

            for (FileData fileData : originalData) {
                if (fileData.getEntityRoot().isPresent()) {
                    final String nescName = ((NescDecl) fileData.getEntityRoot().get()).getName().getName();
                    nescEntitiesBuilder.put(nescName, new PreservedData(fileData));
                }
            }

            for (Component instantiatedComponent : instantiatedComponents) {
                nescEntitiesBuilder.put(instantiatedComponent.getName().getName(),
                        new InstantiatedData(instantiatedComponent));
            }

            return nescEntitiesBuilder.build();
        }
    }
}
