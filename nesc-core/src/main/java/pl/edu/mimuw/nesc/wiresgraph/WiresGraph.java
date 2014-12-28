package pl.edu.mimuw.nesc.wiresgraph;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import pl.edu.mimuw.nesc.ast.gen.Component;
import pl.edu.mimuw.nesc.ast.gen.DataDecl;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.ExceptionVisitor;
import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.gen.FunctionDeclarator;
import pl.edu.mimuw.nesc.ast.gen.IdentifierDeclarator;
import pl.edu.mimuw.nesc.ast.gen.Interface;
import pl.edu.mimuw.nesc.ast.gen.InterfaceRef;
import pl.edu.mimuw.nesc.ast.gen.Node;
import pl.edu.mimuw.nesc.ast.gen.PointerDeclarator;
import pl.edu.mimuw.nesc.ast.gen.QualifiedDeclarator;
import pl.edu.mimuw.nesc.ast.gen.RpInterface;
import pl.edu.mimuw.nesc.ast.gen.VariableDecl;
import pl.edu.mimuw.nesc.ast.util.DeclaratorUtils;
import pl.edu.mimuw.nesc.ast.util.TypeElementUtils;
import pl.edu.mimuw.nesc.facade.iface.InterfaceEntity;
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
     * @return Newly created builder of a graph with no edges.
     */
    public static Builder builder() {
        return new Builder();
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

        /**
         * Private constructor to limit its accessibility.
         */
        private Builder() {
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

                // Iterate over all specification elements of the given component

                final FluentIterable<RpInterface> specificationElements =
                        FluentIterable.from(component.getDeclarations())
                                .filter(RpInterface.class);

                for (RpInterface usesProvides : specificationElements) {
                    for (Declaration declaration : usesProvides.getDeclarations()) {
                        if (declaration instanceof InterfaceRef) {
                            buildFromInterfaceRef((InterfaceRef) declaration, componentName);
                        } else if (declaration instanceof DataDecl) {
                            buildFromDataDecl((DataDecl) declaration, componentName);
                        }
                    }
                }
            }

            private void buildFromInterfaceRef(InterfaceRef interfaceRef, String componentName) {

                final String interfaceRefName = interfaceRef.getAlias().isPresent()
                        ? interfaceRef.getAlias().get().getName()
                        : interfaceRef.getName().getName();
                final String interfaceName = interfaceRef.getName().getName();
                interfaceRefsMapBuilder.put(format("%s.%s", componentName, interfaceRefName), interfaceName);

                // Collect all commands and events from the interface
                final Interface interfaceCopy = copyInterfaceAst(interfaceRef);
                final InterfaceEntitiesVisitor interfaceVisitor =
                        new InterfaceEntitiesVisitor(componentName, interfaceRefName);
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

            private void buildFromDataDecl(DataDecl dataDecl, String componentName) {

                for (Declaration innerDeclaration : dataDecl.getDeclarations()) {
                    if (!(innerDeclaration instanceof VariableDecl)) {
                        continue;
                    }

                    final VariableDecl variableDecl = (VariableDecl) innerDeclaration;
                    checkState(variableDecl.getDeclarator().isPresent(), "declarator of a specification element is absent");
                    final Optional<String> optEntityName = DeclaratorUtils.getDeclaratorName(variableDecl.getDeclarator().get());
                    checkState(optEntityName.isPresent(), "declarator of a specification element does not contain name");

                    final String entityName = optEntityName.get();
                    final SpecificationElementNode newNode = new SpecificationElementNode(componentName,
                            Optional.<String>absent(), entityName);
                    nodesMapBuilder.put(newNode.getName(), newNode);
                }
            }

            /**
             * Visitor responsible for creating nodes for a single interface.
             *
             * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
             */
            private final class InterfaceEntitiesVisitor extends ExceptionVisitor<Void, Void> {
                private final String componentName;
                private final String interfaceRefName;
                private final Map<String, SpecificationElementNode> nodes = new HashMap<>();
                private final ImmutableSet.Builder<String> commandsNamesBuilder = ImmutableSet.builder();
                private final ImmutableSet.Builder<String> eventsNamesBuilder = ImmutableSet.builder();
                private InterfaceEntity.Kind interfaceEntityKind;

                private InterfaceEntitiesVisitor(String componentName, String interfaceRefName) {
                    this.componentName = componentName;
                    this.interfaceRefName = interfaceRefName;
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

                    for (Declaration declaration : dataDecl.getDeclarations()) {
                        declaration.accept(this, null);
                    }
                    return null;
                }

                @Override
                public Void visitVariableDecl(VariableDecl variableDecl, Void arg) {
                    variableDecl.getDeclarator().get().accept(this, null);
                    return null;
                }

                @Override
                public Void visitIdentifierDeclarator(IdentifierDeclarator declarator, Void arg) {
                    final String elementName = format("%s.%s.%s", componentName, interfaceRefName, declarator.getName());
                    checkState(!nodes.containsKey(elementName), "duplicate command or event in an interface");

                    // Update data
                    nodes.put(elementName, new SpecificationElementNode(componentName,
                            Optional.of(interfaceRefName), declarator.getName()));
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

                @Override
                public Void visitQualifiedDeclarator(QualifiedDeclarator declarator, Void arg) {
                    declarator.getDeclarator().get().accept(this, null);
                    return null;
                }

                @Override
                public Void visitFunctionDeclarator(FunctionDeclarator declarator, Void arg) {
                    declarator.getDeclarator().get().accept(this, null);
                    return null;
                }

                @Override
                public Void visitPointerDeclarator(PointerDeclarator declarator, Void arg) {
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
