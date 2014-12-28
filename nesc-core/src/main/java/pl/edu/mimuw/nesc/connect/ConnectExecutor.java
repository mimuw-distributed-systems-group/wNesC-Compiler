package pl.edu.mimuw.nesc.connect;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.apache.log4j.Logger;
import pl.edu.mimuw.nesc.ast.gen.Component;
import pl.edu.mimuw.nesc.ast.gen.Configuration;
import pl.edu.mimuw.nesc.ast.gen.ConfigurationImpl;
import pl.edu.mimuw.nesc.ast.gen.Connection;
import pl.edu.mimuw.nesc.ast.gen.EndPoint;
import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.gen.NescDecl;
import pl.edu.mimuw.nesc.ast.gen.Node;
import pl.edu.mimuw.nesc.ast.gen.ParameterisedIdentifier;
import pl.edu.mimuw.nesc.wiresgraph.WiresGraph;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

/**
 * <p>Class responsible for building the graph of components specification
 * elements. It represents the wiring between components.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class ConnectExecutor {
    /**
     * Logger for connect executors.
     */
    private static final Logger LOG = Logger.getLogger(ConnectExecutor.class);

    /**
     * The graph of components specification elements that is built.
     */
    private final WiresGraph graph;

    /**
     * List with all configurations that indicate edges in the graph.
     */
    private final ImmutableList<Configuration> configurations;

    /**
     * <p>Get the builder that will build a connect executor.</p>
     *
     * @return Newly created builder that will build a connect executor.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * <p>Initialize this connect executor with information provided by given
     * builder.</p>
     *
     * @param builder Builder with all necessary information to build this
     *                executor.
     */
    private ConnectExecutor(Builder builder) {
        this.graph = builder.buildInitialGraph();
        this.configurations = builder.buildConfigurationsList();
    }

    /**
     * <p>Builds the full graph defined by the components that has been
     * previously added to the builder.</p>
     * <p>This method shall be called exactly once on each instance of this
     * class. Calling it multiple times corrupts the graph which possibly has
     * been previously returned.</p>
     *
     * @return Graph of the specification elements from components given to the
     *         builder.
     */
    public WiresGraph connect() {
        // Iterate over all configurations and add edges they define

        for (Configuration configuration : configurations) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Start adding connections from '" + configuration.getName().getName() + "'");
            }

            final ConfigurationImpl impl = (ConfigurationImpl) configuration.getImplementation();

            final AliasesResolver aliasesResolver = AliasesResolver.builder()
                    .addImplementationDeclarations(impl.getDeclarations())
                    .build();

            addConnections(impl, configuration.getName().getName(), aliasesResolver);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Connections from '" + configuration.getName().getName() + "' successfully added");
            }
        }

        return graph;
    }

    private void addConnections(ConfigurationImpl confImpl, String confName, AliasesResolver aliasesResolver) {
        // Iterate over all connections

        final FluentIterable<Connection> connectionsIterable =
                FluentIterable.from(confImpl.getDeclarations())
                        .filter(Connection.class);

        for (Connection connection : connectionsIterable) {
            addConnection(connection, confName, aliasesResolver);
        }
    }

    private void addConnection(Connection connection, String confName, AliasesResolver aliasesResolver) {
        final EndPoint edgeTail, edgeHead;

        switch (connection.getCallDirection()) {
            case FROM_1_TO_2:
                edgeTail = connection.getEndPoint1();
                edgeHead = connection.getEndPoint2();
                break;
            case FROM_2_TO_1:
                edgeTail = connection.getEndPoint2();
                edgeHead = connection.getEndPoint1();
                break;
            default:
                throw new RuntimeException("unexpected call direction '" + connection.getCallDirection() + "'");
        }

        final String edgeTailName = resolveEndpoint(edgeTail, confName, aliasesResolver);
        final String edgeHeadName = resolveEndpoint(edgeHead, confName, aliasesResolver);
        final Optional<LinkedList<Expression>> tailParameters = getEndpointParameters(edgeTail);
        final Optional<LinkedList<Expression>> headParameters = getEndpointParameters(edgeHead);

        // Add the edge

        final int newEdgesCount = graph.connectElements(edgeTailName, edgeHeadName,
                tailParameters, headParameters);

        if (LOG.isDebugEnabled()) {
            LOG.debug(format("Add connection %s -> %s (%d new edge(s))", edgeTailName,
                    edgeHeadName, newEdgesCount));
        }

    }

    private String resolveEndpoint(EndPoint endpoint, String confName, AliasesResolver aliasesResolver) {
        final LinkedList<ParameterisedIdentifier> identifiers = endpoint.getIds();
        final Optional<String> implicitIdentifier = endpoint.getImplicitIdentifier();

        if (identifiers.size() == 2) {
            // An element from the specification of another component - explicit
            return format("%s.%s", aliasesResolver.resolve(identifiers.getFirst().getName().getName()),
                    identifiers.getLast().getName().getName());
        } else if (identifiers.size() == 1 && implicitIdentifier.isPresent()) {
            // An element from the specification of another component - implicit
            return format("%s.%s", aliasesResolver.resolve(identifiers.getFirst().getName().getName()),
                    implicitIdentifier.get());
        } else if (identifiers.size() == 1) {
            // An element from the specification of processed configuration
            return format("%s.%s", confName, identifiers.getFirst().getName().getName());
        } else {
            throw new RuntimeException("unexpected number of identifiers in an endpoint: " + identifiers.size());
        }
    }

    private Optional<LinkedList<Expression>> getEndpointParameters(EndPoint endpoint) {
        final LinkedList<Expression> paramsList = endpoint.getIds().getLast().getArguments();
        return paramsList != null && !paramsList.isEmpty()
                ? Optional.of(paramsList)
                : Optional.<LinkedList<Expression>>absent();
    }

    /**
     * Builder for a connect executor.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static final class Builder {
        /**
         * Data needed to build a connect executor.
         */
        private final List<NescDecl> nescDeclarations = new ArrayList<>();

        /**
         * Private constructor to limit its accessibility.
         */
        private Builder() {
        }

        /**
         * <p>Add components that define the graph that will be built. Only
         * components from the given list are used, other nodes are ignored.
         * If one of the components is generic, an exception is thrown during
         * building.</p>
         *
         * @param nodes List with nodes to add.
         * @return <code>this</code>
         */
        public Builder addComponents(List<? extends Node> nodes) {
            FluentIterable.from(nodes)
                    .filter(NescDecl.class)
                    .copyInto(nescDeclarations);
            return this;
        }

        private void validate() {
            // Check if no added components are generic

            for (NescDecl nescDecl : nescDeclarations) {
                if (nescDecl instanceof Component) {
                    final Component component = (Component) nescDecl;
                    checkState(!component.getIsAbstract(), "a generic component has been added");
                }
            }
        }

        public ConnectExecutor build() {
            validate();
            return new ConnectExecutor(this);
        }

        private WiresGraph buildInitialGraph() {
            return WiresGraph.builder()
                    .addNescDeclarations(nescDeclarations)
                    .build();
        }

        private ImmutableList<Configuration> buildConfigurationsList() {
            return FluentIterable.from(nescDeclarations)
                    .filter(Configuration.class)
                    .toList();
        }
    }
}
