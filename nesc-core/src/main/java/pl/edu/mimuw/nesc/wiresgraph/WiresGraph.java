package pl.edu.mimuw.nesc.wiresgraph;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import pl.edu.mimuw.nesc.ast.gen.Component;
import pl.edu.mimuw.nesc.ast.gen.DataDecl;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.gen.InterfaceRef;
import pl.edu.mimuw.nesc.ast.gen.Node;
import pl.edu.mimuw.nesc.ast.gen.RpInterface;
import pl.edu.mimuw.nesc.ast.gen.VariableDecl;
import pl.edu.mimuw.nesc.ast.util.DeclaratorUtils;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

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
     * a specification element from component {0}.
     */
    private final ImmutableMap<String, SpecificationElementNode> nodes;

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
    private WiresGraph(Builder builder) {
        this.nodes = builder.buildNodesMap();
    }

    /**
     * <p>Add a directed edge from <code>edgeTailName</code> to
     * <code>edgeHeadName</code> to the graph. The head node is added to the
     * list of successors of the tail node and the tail node is added to the
     * list of predecessors of the head node.</p>
     *
     * @param edgeTailName Name of the tail of the directed edge to add (the
     *                     node that the edge starts in).
     * @param edgeHeadName Name of the head of the directed edge to add (the
     *                     node the edge goes to).
     * @param tailParameters List with expressions that constitute parameters
     *                       for the tail specification element.
     * @param headParameters List with expressions that constitute parameters
     *                       for the head specification element.
     * @throws NullPointerException One of the arguments is <code>null</code>.
     * @throws IllegalArgumentException This graph does not contain a node with
     *                                  name <code>edgeTailName</code> or
     *                                  <code>edgeHeadName</code>.
     */
    public void addEdge(String edgeTailName, String edgeHeadName, Optional<LinkedList<Expression>> tailParameters,
            Optional<LinkedList<Expression>> headParameters) {

        checkNotNull(tailParameters, "parameters of the specification element at the tail of the edge cannot be null");
        checkNotNull(headParameters, "parameters of the specification element at the head of the edge cannot be null");

        // Retrieve nodes with given names

        final SpecificationElementNode tailNode = requireNode(edgeTailName);
        final SpecificationElementNode headNode = requireNode(edgeHeadName);

        // Add the edge

        tailNode.addSuccessor(headNode, tailParameters, headParameters);
        headNode.addPredecessor(tailNode, headParameters, tailParameters);
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

        /**
         * Private constructor to limit its accessibility.
         */
        private Builder() {
        }

        /**
         * <p>Add components with specification elements that are nodes of the
         * graph. Nodes other than components are ignored.</p>
         *
         * @param nodes List with nodes (and possibly components) to add.
         * @return <code>this</code>
         */
        public Builder addComponents(List<? extends Node> nodes) {
            FluentIterable.from(nodes)
                    .filter(Component.class)
                    .copyInto(components);
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
        }

        public WiresGraph build() {
            validate();
            return new WiresGraph(this);
        }

        private ImmutableMap<String, SpecificationElementNode> buildNodesMap() {
            final ImmutableMap.Builder<String, SpecificationElementNode> nodesMapBuilder = ImmutableMap.builder();

            // Iterate over all added components

            for (Component component : components) {
                buildComponentNodes(nodesMapBuilder, component);
            }

            return nodesMapBuilder.build();
        }

        private void buildComponentNodes(ImmutableMap.Builder<String, SpecificationElementNode> nodesMapBuilder,
                Component component) {

            final String componentName = component.getName().getName();

            // Iterate over all specification elements of the given component

            final FluentIterable<RpInterface> specificationElements =
                    FluentIterable.from(component.getDeclarations())
                        .filter(RpInterface.class);

            for (RpInterface usesProvides : specificationElements) {
                for (Declaration declaration : usesProvides.getDeclarations()) {
                    if (declaration instanceof InterfaceRef) {
                        buildFromInterfaceRef((InterfaceRef) declaration, nodesMapBuilder, componentName);
                    } else if (declaration instanceof DataDecl) {
                        buildFromDataDecl((DataDecl) declaration, nodesMapBuilder, componentName);
                    }
                }
            }
        }

        private void buildFromInterfaceRef(InterfaceRef interfaceRef,
                ImmutableMap.Builder<String, SpecificationElementNode> nodesMapBuilder,
                String componentName) {

            final String entityName = interfaceRef.getAlias().isPresent()
                    ? interfaceRef.getAlias().get().getName()
                    : interfaceRef.getName().getName();
            final SpecificationElementNode newNode = new SpecificationElementNode(componentName, entityName);
            nodesMapBuilder.put(newNode.getName(), newNode);
        }

        private void buildFromDataDecl(DataDecl dataDecl,
                ImmutableMap.Builder<String, SpecificationElementNode> nodesMapBuilder,
                String componentName) {

            for (Declaration innerDeclaration : dataDecl.getDeclarations()) {
                if (!(innerDeclaration instanceof VariableDecl)) {
                    continue;
                }

                final VariableDecl variableDecl = (VariableDecl) innerDeclaration;
                checkState(variableDecl.getDeclarator().isPresent(), "declarator of a specification element is absent");
                final Optional<String> optEntityName = DeclaratorUtils.getDeclaratorName(variableDecl.getDeclarator().get());
                checkState(optEntityName.isPresent(), "declarator of a specification element does not contain name");

                final String entityName = optEntityName.get();
                final SpecificationElementNode newNode = new SpecificationElementNode(componentName, entityName);
                nodesMapBuilder.put(newNode.getName(), newNode);
            }
        }
    }
}
