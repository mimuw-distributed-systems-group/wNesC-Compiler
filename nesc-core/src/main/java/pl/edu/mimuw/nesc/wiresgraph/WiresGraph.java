package pl.edu.mimuw.nesc.wiresgraph;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.ast.util.AstUtils;
import pl.edu.mimuw.nesc.ast.util.DeclaratorUtils;
import pl.edu.mimuw.nesc.ast.util.TypeElementUtils;
import pl.edu.mimuw.nesc.common.util.list.Lists;
import pl.edu.mimuw.nesc.facade.component.specification.InterfaceEntityElement;
import pl.edu.mimuw.nesc.facade.component.specification.ModuleTable;
import pl.edu.mimuw.nesc.facade.component.specification.TaskElement;
import pl.edu.mimuw.nesc.facade.iface.InterfaceEntity;
import pl.edu.mimuw.nesc.names.mangling.NameMangler;
import pl.edu.mimuw.nesc.substitution.GenericParametersSubstitution;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

/**
 * <p>Class that represents the graph of connections in a NesC application.
 * Nodes represent specification elements from components.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class WiresGraph {
    /**
     * Map with all nodes that constitute the graph. Keys are in the format
     * "{0}.{1}" where {0} is the name of a component and {1} is the name of
     * a specification element from component {0} for bare commands and events.
     * For interface references keys are of form "{0}.{1}.{2}" where {0} is the
     * name of the component, {1} - name of the interface reference and
     * {2} - name of the command or event from the referred interface.
     */
    private final ImmutableMap<String, SpecificationElementNode> nodes;

    /**
     * Map with keys of format "{0}.{1}" where {0} is the name of a component
     * and {1} is the name of an interface reference from the component and
     * values are names of referred interfaces.
     */
    private final ImmutableMap<String, String> interfaceRefs;

    /**
     * Map with names of interfaces as keys and names of commands and events
     * contained in them as values.
     */
    private final ImmutableMap<String, InterfaceContents> interfaces;

    /**
     * <p>Get a builder that will build an initial version of the graph with no
     * edges.</p>
     *
     * @param nameMangler Name mangler that will be used for mangling names of
     *                    parameters for intermediate functions.
     * @return Newly created builder of a graph with no edges.
     * @throws NullPointerException Name mangler is <code>null</code>.
     */
    public static Builder builder(NameMangler nameMangler) {
        checkNotNull(nameMangler, "name mangler cannot be null");
        return new Builder(nameMangler);
    }

    /**
     * <p>Initializes the graph with information provided by given builder.</p>
     *
     * @param builder Builder with information necessary to initialize the
     *                graph.
     */
    private WiresGraph(PrivateBuilder builder) {
        builder.buildAll();;
        this.nodes = builder.getNodesMap();
        this.interfaceRefs = builder.getInterfacesRefsMap();
        this.interfaces = builder.getInterfacesContents();
    }

    /**
     * <p>Add directed edges from <code>edgeTailName</code> to
     * <code>edgeHeadName</code> to the graph. All commands and events
     * associated with given specification names are properly connected. If the
     * given names correspond to a bare command or event, only one edge is
     * added, otherwise as many edges as the referred interface contains are
     * added.</p>
     *
     * @param edgesTailName Name of an interface reference or a bare
     *                      command or event the edges start in.
     * @param edgesHeadName Name of an interface reference or a bare command
     *                      or event the edges end in.
     * @param tailParameters List with expressions that constitute parameters
     *                       for the tail specification element.
     * @param headParameters List with expressions that constitute parameters
     *                       for the head specification element.
     * @return Count of edges that have been added.
     * @throws NullPointerException One of the arguments is <code>null</code>.
     * @throws IllegalArgumentException There is no a specification element with
     *                                  name <code>edgeTailName</code> or
     *                                  <code>edgeHeadName</code>.
     */
    public int connectElements(String edgesTailName, String edgesHeadName, Optional<LinkedList<Expression>> tailParameters,
            Optional<LinkedList<Expression>> headParameters) {

        checkNotNull(tailParameters, "parameters of the specification element at the tail of the edge cannot be null");
        checkNotNull(headParameters, "parameters of the specification element at the head of the edge cannot be null");

        /* Check whether interfaces references or bare commands or events are
           connected. */

        if (interfaceRefs.containsKey(edgesTailName)) {
            return connectInterfaceRefElements(edgesTailName, edgesHeadName, tailParameters, headParameters);
        } else {
            addEdge(edgesTailName, edgesHeadName, tailParameters, headParameters);
            return 1;
        }
    }

    private int connectInterfaceRefElements(String tailName, String headName, Optional<LinkedList<Expression>> tailParameters,
            Optional<LinkedList<Expression>> headParameters) {
        final InterfaceContents interfaceContents = interfaces.get(interfaceRefs.get(tailName));

        for (String commandName : interfaceContents.commandsNames) {
            addEdge(format("%s.%s", tailName, commandName), format("%s.%s", headName, commandName),
                    tailParameters, headParameters);
        }

        for (String eventName : interfaceContents.eventsNames) {
            addEdge(format("%s.%s", headName, eventName), format("%s.%s", tailName, eventName),
                    headParameters, tailParameters);
        }

        return interfaceContents.commandsNames.size() + interfaceContents.eventsNames.size();
    }

    private void addEdge(String tailName, String headName, Optional<LinkedList<Expression>> tailParameters,
               Optional<LinkedList<Expression>> headParameters) {
        final SpecificationElementNode tailNode = requireNode(tailName);
        final SpecificationElementNode headNode = requireNode(headName);
        tailNode.addSuccessor(headNode, tailParameters, headParameters);
    }

    /**
     * <p>Get the node with given name. If it does not exist, no exception is
     * thrown and the returned object is absent.</p>
     *
     * @param name Name of the node.
     * @return Node with given name. If it does not exist, the object is absent.
     * @throws NullPointerException Name is null.
     */
    public Optional<SpecificationElementNode> getNode(String name) {
        checkNotNull(name, "name of the node cannot be null");
        return Optional.fromNullable(nodes.get(name));
    }

    /**
     * <p>Get the node with given name. If it does not exist, an exception is
     * thrown.</p>
     *
     * @param name Name of the node.
     * @return Node with given name.
     * @throws NullPointerException Name is null.
     * @throws IllegalArgumentException There is no node with given name in this
     *                                  graph.
     */
    public SpecificationElementNode requireNode(String name) {
        final Optional<SpecificationElementNode> optNode = getNode(name);
        checkArgument(optNode.isPresent(), "node '%s' does not exist", name);
        return optNode.get();
    }

    /**
     * Private builder that creates particular elements of the graph.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private interface PrivateBuilder {
        void buildAll();
        ImmutableMap<String, SpecificationElementNode> getNodesMap();
        ImmutableMap<String, String> getInterfacesRefsMap();
        ImmutableMap<String, InterfaceContents> getInterfacesContents();
    }

    /**
     * Builder of a graph with no edges (they can be added after the building
     * process).
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static final class Builder {
        /**
         * Data needed to build the initial graph.
         */
        private final List<Component> components = new ArrayList<>();
        private final List<Interface> interfaces = new ArrayList<>();
        private final NameMangler nameMangler;

        /**
         * Private constructor to limit its accessibility.
         */
        private Builder(NameMangler nameMangler) {
            this.nameMangler = nameMangler;
        }

        /**
         * <p>Add components with specification elements that are nodes of the
         * graph. AST nodes of interfaces are also saved. Other nodes are
         * ignored.</p>
         *
         * @param nodes List with nodes (and possibly components and interfaces)
         *              to add.
         * @return <code>this</code>
         * @throws IllegalStateException An interface from the list with the
         *                               same name has been already added.
         */
        public Builder addNescDeclarations(List<? extends Node> nodes) {
            FluentIterable.from(nodes)
                    .filter(Component.class)
                    .copyInto(components);
            FluentIterable.from(nodes)
                    .filter(Interface.class)
                    .copyInto(interfaces);
            return this;
        }

        private void validate() {
            final Set<String> names = new HashSet<>();

            for (Component component : components) {
                if (!names.add(component.getName().getName())) {
                    throw new IllegalStateException("added components do not have unique names");
                }
                checkState(!component.getIsAbstract(), "a generic component has been added");
            }

            for (Interface interfaceAst : interfaces) {
                if (!names.add(interfaceAst.getName().getName())) {
                    throw new IllegalStateException("added interfaces and components do not have unique names");
                }
            }
        }

        public WiresGraph build() {
            validate();
            return new WiresGraph(new WiresGraphPrivateBuilder());
        }

        /**
         * Builder of particular elements of the graph. One instance is supposed
         * to build exactly one graph.
         *
         * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
         */
        private final class WiresGraphPrivateBuilder implements PrivateBuilder {
            private final ImmutableMap.Builder<String, SpecificationElementNode> nodesMapBuilder = ImmutableMap.builder();
            private final ImmutableMap.Builder<String, String> interfaceRefsMapBuilder = ImmutableMap.builder();
            private final Map<String, InterfaceContents> interfacesContentMap = new HashMap<>();
            private final ImmutableMap<String, Interface> interfacesMap;

            // Initialization block for interfaces map
            {
                final ImmutableMap.Builder<String, Interface> interfaceMapBuilder = ImmutableMap.builder();
                for (Interface interfaceAst : interfaces) {
                    interfaceMapBuilder.put(interfaceAst.getName().getName(), interfaceAst);
                }
                interfacesMap = interfaceMapBuilder.build();
            }

            @Override
            public ImmutableMap<String, SpecificationElementNode> getNodesMap() {
                return nodesMapBuilder.build();
            }

            @Override
            public ImmutableMap<String, String> getInterfacesRefsMap() {
                return interfaceRefsMapBuilder.build();
            }

            @Override
            public ImmutableMap<String, InterfaceContents> getInterfacesContents() {
                return ImmutableMap.copyOf(interfacesContentMap);
            }

            @Override
            public void buildAll() {
                // Iterate over all added components
                for (Component component : components) {
                    buildComponentNodes(component);
                }
            }

            private void buildComponentNodes(Component component) {
                final String componentName = component.getName().getName();
                final Optional<ModuleTable> moduleTable = component instanceof Module
                        ? Optional.of(((Module) component).getModuleTable())
                        : Optional.<ModuleTable>absent();

                // Iterate over all specification elements of the given component

                final FluentIterable<RpInterface> specificationElements =
                        FluentIterable.from(component.getDeclarations())
                                .filter(RpInterface.class);

                for (RpInterface usesProvides : specificationElements) {
                    for (Declaration declaration : usesProvides.getDeclarations()) {
                        if (declaration instanceof InterfaceRef) {
                            buildFromInterfaceRef((InterfaceRef) declaration, componentName, moduleTable);
                        } else if (declaration instanceof DataDecl) {
                            buildFromDataDecl((DataDecl) declaration, componentName, moduleTable);
                        }
                    }
                }
            }

            private void buildFromInterfaceRef(InterfaceRef interfaceRef, String componentName,
                    Optional<ModuleTable> moduleTable) {

                final String interfaceRefName = interfaceRef.getAlias().isPresent()
                        ? interfaceRef.getAlias().get().getName()
                        : interfaceRef.getName().getName();
                final String interfaceName = interfaceRef.getName().getName();
                interfaceRefsMapBuilder.put(format("%s.%s", componentName, interfaceRefName), interfaceName);

                // Collect all commands and events from the interface
                final Interface interfaceCopy = copyInterfaceAst(interfaceRef);
                final InterfaceEntitiesVisitor interfaceVisitor = new InterfaceEntitiesVisitor(
                        componentName,
                        interfaceRefName,
                        interfaceRef.getGenericParameters(),
                        moduleTable
                );
                interfaceCopy.accept(interfaceVisitor, null);

                if (!interfacesContentMap.containsKey(interfaceName)) {
                    final InterfaceContents contents = new InterfaceContents(interfaceVisitor.commandsNamesBuilder.build(),
                            interfaceVisitor.eventsNamesBuilder.build());
                    interfacesContentMap.put(interfaceName, contents);
                }
                nodesMapBuilder.putAll(interfaceVisitor.nodes);
            }

            private Interface copyInterfaceAst(InterfaceRef interfaceRef) {
                final Interface interfaceCopy = interfacesMap.get(interfaceRef.getName().getName()).deepCopy(true);

                // Substitute parameters if the interface is generic
                if (interfaceCopy.getParameters().isPresent()) {
                    final GenericParametersSubstitution substitution = GenericParametersSubstitution.forInterface()
                            .interfaceRef(interfaceRef)
                            .interfaceAstNode(interfaceCopy)
                            .build();
                    interfaceCopy.substitute(substitution);
                }

                return interfaceCopy;
            }

            private void buildFromDataDecl(DataDecl dataDecl, String componentName,
                    Optional<ModuleTable> moduleTable) {

                for (Declaration innerDeclaration : dataDecl.getDeclarations()) {
                    if (!(innerDeclaration instanceof VariableDecl)) {
                        continue;
                    }

                    final VariableDecl variableDecl = (VariableDecl) innerDeclaration;
                    checkState(variableDecl.getDeclarator().isPresent(), "declarator of a specification element is absent");
                    final Optional<String> optEntityName = DeclaratorUtils.getDeclaratorName(variableDecl.getDeclarator().get());
                    checkState(optEntityName.isPresent(), "declarator of a specification element does not contain name");

                    final String entityName = optEntityName.get();
                    final EntityData entityData = createBareEntityData(dataDecl.getModifiers(),
                            variableDecl, entityName, moduleTable);
                    final SpecificationElementNode newNode = new SpecificationElementNode(componentName,
                            Optional.<String>absent(), entityName, entityData);
                    nodesMapBuilder.put(newNode.getName(), newNode);
                }
            }

            private EntityData createBareEntityData(LinkedList<TypeElement> typeElements, VariableDecl variableDecl,
                        String entityName, Optional<ModuleTable> moduleTable) {
                // Handle the case of a sink
                if (moduleTable.isPresent()) {
                    final InterfaceEntityElement bareElement = moduleTable.get().get(entityName).get();
                    if (bareElement.isProvided()) {
                        return new SinkFunctionData(bareElement.getUniqueName().get());
                    }
                }

                final boolean voidOccurred = removeKeywords(typeElements);
                boolean containsNotVoidDeclarator = false;
                Declarator declarator = variableDecl.getDeclarator().get();

                while (!(declarator instanceof FunctionDeclarator)) {
                    containsNotVoidDeclarator = containsNotVoidDeclarator
                            || declarator instanceof PointerDeclarator;
                    declarator = ((NestedDeclarator) declarator).getDeclarator().get();
                }

                final boolean returnsVoid = voidOccurred && !containsNotVoidDeclarator;

                // Move the instance parameters to the function parameters
                final FunctionDeclarator funDeclarator = (FunctionDeclarator) declarator;
                if (funDeclarator.getGenericParameters().isPresent()) {
                    final LinkedList<Declaration> finalParams = funDeclarator.getGenericParameters().get();
                    finalParams.addAll(funDeclarator.getParameters());
                    funDeclarator.setGenericParameters(Optional.<LinkedList<Declaration>>absent());
                    funDeclarator.setParameters(finalParams);
                }

                final FunctionDecl funDecl = new FunctionDecl(
                        Location.getDummyLocation(),
                        variableDecl.getDeclarator().get(),
                        typeElements,
                        Lists.<Attribute>newList(),
                        AstUtils.newEmptyCompoundStmt(),
                        false
                );

                return new IntermediateFunctionData(prepareParametersNames(funDeclarator.getParameters()),
                        returnsVoid, funDecl);
            }

            /**
             * Mangle names of parameters and create list of their names in
             * proper order.
             *
             * @param paramsDecls List with declarations of parameters.
             * @return Newly created list with names of consecutive parameters.
             */
            private ImmutableList<String> prepareParametersNames(LinkedList<Declaration> paramsDecls) {
                final ImmutableList.Builder<String> paramsNamesListBuilder = ImmutableList.builder();
                int counter = 0;

                for (Declaration declaration : paramsDecls) {
                    ++counter;
                    final DataDecl dataDecl = (DataDecl) declaration;
                    final VariableDecl variableDecl = (VariableDecl) dataDecl.getDeclarations().getFirst();
                    final Optional<NestedDeclarator> deepestDeclarator =
                            DeclaratorUtils.getDeepestNestedDeclarator(variableDecl.getDeclarator());
                    final String paramName = format("arg%d", counter);
                    final IdentifierDeclarator identDeclarator = new IdentifierDeclarator(
                            Location.getDummyLocation(),
                            paramName
                    );
                    identDeclarator.setUniqueName(Optional.of(nameMangler.mangle(paramName)));
                    paramsNamesListBuilder.add(identDeclarator.getUniqueName().get());

                    if (deepestDeclarator.isPresent()) {
                        deepestDeclarator.get().setDeclarator(Optional.<Declarator>of(identDeclarator));
                    } else {
                        variableDecl.setDeclarator(Optional.<Declarator>of(identDeclarator));
                    }
                }

                return paramsNamesListBuilder.build();
            }

            /**
             * Remove 'command' and 'event' keywords from the given list.
             *
             * @param typeElements List with type elements to filter.
             * @return <code>true</code> if and only if the given list contains
             *         'void' keyword.
             */
            private boolean removeKeywords(LinkedList<TypeElement> typeElements) {
                final Iterator<TypeElement> typeElementsIt = typeElements.iterator();
                boolean voidOccurred = false;

                while (typeElementsIt.hasNext()) {
                    final TypeElement typeElement = typeElementsIt.next();
                    if (!(typeElement instanceof Rid)) {
                        continue;
                    }

                    final Rid rid = (Rid) typeElement;
                    switch (rid.getId()) {
                        case VOID:
                            voidOccurred = true;
                            break;
                        case COMMAND:
                        case EVENT:
                            typeElementsIt.remove();
                            break;
                    }
                }

                return voidOccurred;
            }

            /**
             * Visitor responsible for creating nodes for a single interface.
             *
             * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
             */
            private final class InterfaceEntitiesVisitor extends ExceptionVisitor<Void, Void> {
                private final String componentName;
                private final String interfaceRefName;
                private final Optional<LinkedList<Declaration>> instanceParameters;
                private final Optional<ModuleTable> moduleTable;
                private final Map<String, SpecificationElementNode> nodes = new HashMap<>();
                private final ImmutableSet.Builder<String> commandsNamesBuilder = ImmutableSet.builder();
                private final ImmutableSet.Builder<String> eventsNamesBuilder = ImmutableSet.builder();
                private InterfaceEntity.Kind interfaceEntityKind;

                /**
                 * Data needed for the creation of
                 * {@link IntermediateFunctionData} object collected
                 * incrementally.
                 */
                private LinkedList<TypeElement> typeElements;
                private Declarator intermediateDeclarator;
                private boolean returnsVoid;
                private ImmutableList<String> parametersNames;

                private InterfaceEntitiesVisitor(String componentName, String interfaceRefName,
                        Optional<LinkedList<Declaration>> instanceParameters,
                        Optional<ModuleTable> moduleTable) {
                    this.componentName = componentName;
                    this.interfaceRefName = interfaceRefName;
                    this.instanceParameters = instanceParameters;
                    this.moduleTable = moduleTable;
                }

                @Override
                public Void visitInterface(Interface interfaceAst, Void arg) {
                    for (Declaration declaration : interfaceAst.getDeclarations()) {
                        declaration.accept(this, null);
                    }
                    return null;
                }

                @Override
                public Void visitDataDecl(DataDecl dataDecl, Void arg) {
                    switch (TypeElementUtils.getFunctionType(dataDecl.getModifiers())) {
                        case COMMAND:
                            interfaceEntityKind = InterfaceEntity.Kind.COMMAND;
                            break;
                        case EVENT:
                            interfaceEntityKind = InterfaceEntity.Kind.EVENT;
                            break;
                        default:
                            throw new RuntimeException("declaration other than command or event inside an interface");
                    }

                    typeElements = dataDecl.getModifiers();
                    final boolean returnsVoid = removeKeywords(typeElements);

                    for (Declaration declaration : dataDecl.getDeclarations()) {
                        this.returnsVoid = returnsVoid;
                        declaration.accept(this, null);
                    }
                    return null;
                }

                @Override
                public Void visitVariableDecl(VariableDecl variableDecl, Void arg) {
                    intermediateDeclarator = variableDecl.getDeclarator().get();
                    variableDecl.getDeclarator().get().accept(this, null);
                    return null;
                }

                @Override
                public Void visitIdentifierDeclarator(IdentifierDeclarator declarator, Void arg) {
                    final String entityName = declarator.getName();
                    final String elementName = format("%s.%s.%s", componentName, interfaceRefName, entityName);
                    checkState(!nodes.containsKey(elementName), "duplicate command or event in an interface");

                    // Update data
                    nodes.put(elementName, new SpecificationElementNode(componentName,
                            Optional.of(interfaceRefName), declarator.getName(),
                            createInterfaceEntityData(entityName)));
                    switch (interfaceEntityKind) {
                        case COMMAND:
                            commandsNamesBuilder.add(declarator.getName());
                            break;
                        case EVENT:
                            eventsNamesBuilder.add(declarator.getName());
                            break;
                        default:
                            throw new RuntimeException("unexpected interface entity kind '" + interfaceEntityKind + "'");
                    }

                    return null;
                }

                private EntityData createInterfaceEntityData(String entityName) {
                    final Optional<String> sinkUniqueName;

                    // Check if the element is implemented in a module
                    if (moduleTable.isPresent()) {
                        final Optional<InterfaceEntityElement> optInterfaceEntityElement =
                                moduleTable.get().get(format("%s.%s", interfaceRefName, entityName));
                        final Optional<TaskElement> optTaskElement = Optional.fromNullable(
                                moduleTable.get().getTasksInterfaceRefs().get(interfaceRefName));
                        checkState(optTaskElement.isPresent() || optInterfaceEntityElement.isPresent(),
                                "lack of information for a specification element in the module table");
                        checkState(!optTaskElement.isPresent() || !optInterfaceEntityElement.isPresent(),
                                "task interface reference with the same name as normal element");

                        if (optInterfaceEntityElement.isPresent()) {
                            sinkUniqueName = optInterfaceEntityElement.get().isProvided()
                                    ? Optional.of(optInterfaceEntityElement.get().getUniqueName().get())
                                    : Optional.<String>absent();
                        } else {
                            sinkUniqueName = interfaceEntityKind == InterfaceEntity.Kind.EVENT
                                    ? Optional.of(optTaskElement.get().getUniqueName().get())
                                    : Optional.<String>absent();
                        }
                    } else {
                        sinkUniqueName = Optional.absent();
                    }

                    if (sinkUniqueName.isPresent()) {
                        return new SinkFunctionData(sinkUniqueName.get());
                    }

                    /* Make template of function definition for intermediate
                       function. */
                    final FunctionDecl functionDecl = new FunctionDecl(
                            Location.getDummyLocation(),
                            intermediateDeclarator,
                            typeElements,
                            Lists.<Attribute>newList(),
                            AstUtils.newEmptyCompoundStmt(),
                            false
                    );

                    final EntityData result = new IntermediateFunctionData(parametersNames,
                            returnsVoid, functionDecl);

                    intermediateDeclarator = null;
                    parametersNames = null;

                    return result;
                }

                @Override
                public Void visitQualifiedDeclarator(QualifiedDeclarator declarator, Void arg) {
                    declarator.getDeclarator().get().accept(this, null);
                    return null;
                }

                @Override
                public Void visitFunctionDeclarator(FunctionDeclarator declarator, Void arg) {
                    // Prepare parameters for the potential intermediate function
                    if (instanceParameters.isPresent()) {
                        final LinkedList<Declaration> params = AstUtils.deepCopyNodes(instanceParameters.get(), true);
                        params.addAll(declarator.getParameters());
                        declarator.setParameters(params);
                    }
                    parametersNames = prepareParametersNames(declarator.getParameters());

                    declarator.getDeclarator().get().accept(this, null);
                    return null;
                }

                @Override
                public Void visitPointerDeclarator(PointerDeclarator declarator, Void arg) {
                    returnsVoid = false;
                    declarator.getDeclarator().get().accept(this, null);
                    return null;
                }

                @Override
                public Void visitArrayDeclarator(ArrayDeclarator declarator, Void arg) {
                    declarator.getDeclarator().get().accept(this, null);
                    return null;
                }
            }
        }
    }

    /**
     * A small helper class that contains information about contents of an
     * interface.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class InterfaceContents {
        private final ImmutableSet<String> commandsNames;
        private final ImmutableSet<String> eventsNames;

        private InterfaceContents(ImmutableSet<String> commandsNames,
                ImmutableSet<String> eventsNames) {
            this.commandsNames = commandsNames;
            this.eventsNames = eventsNames;
        }
    }
}
