package pl.edu.mimuw.nesc.connect;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.apache.log4j.Logger;
import pl.edu.mimuw.nesc.abi.ABI;
import pl.edu.mimuw.nesc.ast.gen.Component;
import pl.edu.mimuw.nesc.ast.gen.Configuration;
import pl.edu.mimuw.nesc.ast.gen.ConfigurationImpl;
import pl.edu.mimuw.nesc.ast.gen.Connection;
import pl.edu.mimuw.nesc.ast.gen.EndPoint;
import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.gen.NescDecl;
import pl.edu.mimuw.nesc.ast.gen.Node;
import pl.edu.mimuw.nesc.ast.gen.ParameterisedIdentifier;
import pl.edu.mimuw.nesc.constexpr.ConstExprInterpreter;
import pl.edu.mimuw.nesc.constexpr.value.ConstantValue;
import pl.edu.mimuw.nesc.constexpr.value.IntegerConstantValue;
import pl.edu.mimuw.nesc.constexpr.value.type.ConstantType;
import pl.edu.mimuw.nesc.names.mangling.NameMangler;
import pl.edu.mimuw.nesc.wiresgraph.WiresGraph;

import static com.google.common.base.Preconditions.checkNotNull;
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
     * Interpreter used for evaluation of parameters given at endpoints.
     */
    private final ConstExprInterpreter interpreter;

    /**
     * <p>Get the builder that will build a connect executor.</p>
     *
     * @param nameMangler Name mangler that will be used for mangling names of
     *                    parameters for intermediate functions.
     * @param abi ABI that is necessary for evaluation endpoints expressions.
     * @return Newly created builder that will build a connect executor.
     * @throws NullPointerException Name mangler is <code>null</code>.
     */
    public static Builder builder(NameMangler nameMangler, ABI abi) {
        checkNotNull(nameMangler, "name mangler cannot be null");
        checkNotNull(abi, "ABI cannot be null");
        return new Builder(nameMangler, abi);
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
        this.interpreter = new ConstExprInterpreter(builder.abi);
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
        final Optional<ImmutableList<BigInteger>> tailIndices =
                evaluateEndpointParameters(getEndpointParameters(edgeTail));
        final Optional<ImmutableList<BigInteger>> headIndices =
                evaluateEndpointParameters(getEndpointParameters(edgeHead));

        // Add the edge

        final int newEdgesCount = graph.connectElements(edgeTailName, edgeHeadName,
                tailIndices, headIndices);

        if (LOG.isDebugEnabled()) {
            LOG.debug(format("Add connection %s -> %s (%d new edge(s))",
                    buildEndpointString(edgeTailName, tailIndices),
                    buildEndpointString(edgeHeadName, headIndices),
                    newEdgesCount));
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

    private Optional<ImmutableList<BigInteger>> evaluateEndpointParameters(Optional<LinkedList<Expression>> params) {
        if  (!params.isPresent()) {
            return Optional.absent();
        }

        final ImmutableList.Builder<BigInteger> indicesBuilder = ImmutableList.builder();

        for (Expression indexExpr : params.get()) {
            final ConstantValue value = interpreter.evaluate(indexExpr);
            if (value.getType().getType() != ConstantType.Type.SIGNED_INTEGER
                    && value.getType().getType() != ConstantType.Type.UNSIGNED_INTEGER) {
                throw new RuntimeException("endpoint parameter expression evaluated to a constant of type '"
                        + value.getType().getType() + "'");
            }
            final IntegerConstantValue<?> integerValue = (IntegerConstantValue<?>) value;
            indicesBuilder.add(integerValue.getValue());
        }

        return Optional.of(indicesBuilder.build());
    }

    private String buildEndpointString(String endpointName, Optional<ImmutableList<BigInteger>> indices) {
        final StringBuilder builder = new StringBuilder(endpointName);

        if (indices.isPresent()) {
            builder.append('[');

            final Iterator<BigInteger> indicesIt = indices.get().iterator();
            if (indicesIt.hasNext()) {
                builder.append(indicesIt.next());

                while (indicesIt.hasNext()) {
                    builder.append(", ");
                    builder.append(indicesIt.next());
                }
            }

            builder.append(']');
        }

        return builder.toString();
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
        private final NameMangler nameMangler;
        private final ABI abi;

        /**
         * Private constructor to limit its accessibility.
         */
        private Builder(NameMangler nameMangler, ABI abi) {
            this.nameMangler = nameMangler;
            this.abi = abi;
        }

        /**
         * Add a declaration that defines the graph that will be built.
         *
         * @param nescDecl NesC declaration to add.
         * @return <code>this</code>
         */
        public Builder addNescDeclaration(NescDecl nescDecl) {
            checkNotNull(nescDecl, "NesC declaration to add cannot be null");
            nescDeclarations.add(nescDecl);
            return this;
        }

        /**
         * <p>Add NesC declarations that define the graph that will be built.
         * Only components and interfaces from the given collection are used,
         * other nodes are ignored. If one of the components is generic, an
         * exception is thrown during building.</p>
         *
         * @param nodes Collection with nodes to add.
         * @return <code>this</code>
         */
        public Builder addNescDeclarations(Collection<? extends Node> nodes) {
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
            return WiresGraph.builder(nameMangler)
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
