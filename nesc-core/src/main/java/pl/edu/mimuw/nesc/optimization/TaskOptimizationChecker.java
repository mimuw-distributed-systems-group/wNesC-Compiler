package pl.edu.mimuw.nesc.optimization;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.Configuration;
import pl.edu.mimuw.nesc.ast.gen.ConfigurationImpl;
import pl.edu.mimuw.nesc.ast.gen.Connection;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.EndPoint;
import pl.edu.mimuw.nesc.ast.gen.EqConnection;
import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.gen.IdentityVisitor;
import pl.edu.mimuw.nesc.ast.gen.Module;
import pl.edu.mimuw.nesc.ast.gen.ParameterisedIdentifier;
import pl.edu.mimuw.nesc.ast.gen.RpConnection;
import pl.edu.mimuw.nesc.ast.gen.UniqueCall;
import pl.edu.mimuw.nesc.ast.gen.UniqueCountCall;
import pl.edu.mimuw.nesc.ast.gen.UniqueNCall;
import pl.edu.mimuw.nesc.astutil.AliasesCollector;
import pl.edu.mimuw.nesc.common.SchedulerSpecification;
import pl.edu.mimuw.nesc.problem.NescWarning;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * <p>Class responsible for checking if the task optimization can be safely
 * performed.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class TaskOptimizationChecker {
    /**
     * Specification of the scheduler.
     */
    private final SchedulerSpecification schedulerSpec;

    /**
     * List with declarations to check.
     */
    private final ImmutableList<Declaration> declarations;

    /**
     * Warning for the given declarations.
     */
    private Optional<NescWarning> warning;

    /**
     * Get a new builder that will build a task optimization checker.
     *
     * @param schedulerSpec Scheduler specification to use by the built
     *                      task optimizations checkers.
     * @return Newly created builder that will build a task optimization checker
     *         that will use given scheduler specification.
     */
    public static Builder builder(SchedulerSpecification schedulerSpec) {
        checkNotNull(schedulerSpec, "scheduler specification cannot be null");
        return new Builder(schedulerSpec);
    }

    /**
     * Initialize member fields with data from the builder.
     *
     * @param builder Builder with necessary data.
     */
    private TaskOptimizationChecker(Builder builder) {
        this.schedulerSpec = builder.schedulerSpec;
        this.declarations = builder.declarationsBuilder.build();
        this.warning = Optional.absent();
    }

    /**
     * Check if the task optimization can be safely performed on declarations
     * given to the builder.
     *
     * @return Warning with explanation why the task optimization cannot be
     *         safely performed. If it can be performed, the object is absent.
     */
    public Optional<NescWarning> check() {
        if (warning.isPresent()) {
            return warning;
        }

        final CheckingVisitor checkingVisitor = new CheckingVisitor();

        for (Declaration declaration : declarations) {
            declaration.traverse(checkingVisitor, new Oracle());

            if (warning.isPresent()) {
                return warning;
            }
        }

        return warning;
    }

    /**
     * Visitor that checks if the optimization can be safely performed.
     * The value indicates if the current node is inside a parameterised
     * identifier AST node.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class CheckingVisitor extends IdentityVisitor<Oracle> {
        /**
         * Endpoints that can contain 'unique' function calls.
         */
        private final Set<EndPoint> allowedEndpoints = new HashSet<>();

        /**
         * Set of AST nodes that can use the scheduler identifier.
         */
        private final Set<UniqueCall> allowedUniqueCalls = new HashSet<>();

        @Override
        public Oracle visitUniqueCall(UniqueCall expr, Oracle oracle) {
            if (schedulerSpec.getUniqueIdentifier().equals(expr.getIdentifier())
                    && !allowedUniqueCalls.contains(expr) && !warning.isPresent()) {
                warning = Optional.of(makeWarning(format("scheduler identifier '%s' is used in 'unique' not to connect a task to the scheduler",
                        schedulerSpec.getUniqueIdentifier()), expr));
            } else if (!schedulerSpec.getUniqueIdentifier().equals(expr.getIdentifier())
                    && allowedUniqueCalls.contains(expr) && !warning.isPresent()) {
                warning = Optional.of(makeWarning(format("a task is connected to the scheduler using an identifier in 'unique' that differs from '%s'",
                        schedulerSpec.getUniqueIdentifier()), expr));
            }

            allowedUniqueCalls.remove(expr);
            return oracle;
        }

        @Override
        public Oracle visitUniqueNCall(UniqueNCall expr, Oracle oracle) {
            if (schedulerSpec.getUniqueIdentifier().equals(expr.getIdentifier())
                    && !warning.isPresent()) {
                warning = Optional.of(makeWarning(format("scheduler identifier '%s' for 'unique' is used in 'uniqueN'",
                                schedulerSpec.getUniqueIdentifier()), expr));
            }
            return oracle;
        }

        @Override
        public Oracle visitUniqueCountCall(UniqueCountCall expr, Oracle oracle) {
            if (oracle.insideParameterisedId && !warning.isPresent()) {
                warning = Optional.of(makeWarning(format("scheduler identifier '%s' is used in 'uniqueCount' inside a connection",
                        schedulerSpec.getUniqueIdentifier()), expr));
            }
            return oracle;
        }

        @Override
        public Oracle visitConfiguration(Configuration component, Oracle oracle) {
            return oracle.modifyComponentName(component.getName().getName());
        }

        @Override
        public Oracle visitModule(Module component, Oracle oracle) {
            return oracle.modifyComponentName(component.getName().getName());
        }

        @Override
        public Oracle visitConfigurationImpl(ConfigurationImpl impl, Oracle oracle) {
            return oracle.modifyNamesMap(new AliasesCollector(impl).collect());
        }

        @Override
        public Oracle visitEndPoint(EndPoint endpoint, Oracle oracle) {
            if (!allowedEndpoints.contains(endpoint)) {
                return oracle;
            }

            allowedEndpoints.remove(endpoint);
            Optional<Expression> invalidTaskIfaceArg = Optional.absent();

            if (endpoint.getIds().size() == 2 && endpoint.getIds().getLast().getArguments().size() == 1) {
                final Deque<Expression> args = endpoint.getIds().getLast().getArguments();
                final String firstId = endpoint.getIds().getFirst().getName().getName();
                final String secondId = endpoint.getIds().getLast().getName().getName();
                final String referredComponentName = oracle.componentRefsNamesMap.get().get(firstId);

                if (referredComponentName.equals(schedulerSpec.getComponentName())
                        && secondId.equals(schedulerSpec.getInterfaceNameInScheduler())) {
                    if (args.getFirst() instanceof UniqueCall) {
                        allowedUniqueCalls.add((UniqueCall) args.getFirst());
                    } else if (!warning.isPresent()) {
                        invalidTaskIfaceArg = Optional.of(args.getFirst());
                    }
                }
            } else if (endpoint.getIds().size() == 1 && endpoint.getIds().getLast().getArguments().size() == 1) {
                final Deque<Expression> args = endpoint.getIds().getLast().getArguments();
                final String id = endpoint.getIds().getFirst().getName().getName();

                if (!oracle.componentRefsNamesMap.get().containsKey(id)
                        && oracle.componentName.get().equals(schedulerSpec.getComponentName())
                        && id.equals(schedulerSpec.getInterfaceNameInScheduler())) {
                    if (args.getFirst() instanceof UniqueCall) {
                        allowedUniqueCalls.add((UniqueCall) args.getFirst());
                    } else if (!warning.isPresent()) {
                        invalidTaskIfaceArg = Optional.of(args.getFirst());
                    }
                }
            }

            if (invalidTaskIfaceArg.isPresent()) {
                warning = Optional.of(makeWarning(format("a task is connected to the scheduler component '%s' without using 'unique'",
                        schedulerSpec.getComponentName()), invalidTaskIfaceArg.get()));
            }

            return oracle;
        }

        @Override
        public Oracle visitParameterisedIdentifier(ParameterisedIdentifier node, Oracle oracle) {
            return oracle.modifyInsideParameterisedId(true);
        }

        @Override
        public Oracle visitRpConnection(RpConnection connection, Oracle oracle) {
            enterConnection(connection);
            return oracle;
        }

        @Override
        public Oracle visitEqConnection(EqConnection connection, Oracle oracle) {
            enterConnection(connection);
            return oracle;
        }

        private void enterConnection(Connection connection) {
            final EndPoint allowedEndoint;

            switch (connection.getCallDirection()) {
                case FROM_1_TO_2:
                    allowedEndoint = connection.getEndPoint2();
                    break;
                case FROM_2_TO_1:
                    allowedEndoint = connection.getEndPoint1();
                    break;
                default:
                    throw new RuntimeException("unexpected call direction "
                            + connection.getCallDirection());
            }

            allowedEndpoints.add(allowedEndoint);
        }

        private NescWarning makeWarning(String msg, Expression location) {
            return new NescWarning(Optional.fromNullable(location.getLocation()),
                    Optional.fromNullable(location.getEndLocation()), msg);
        }
    }

    /**
     * Class with necessary block information.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class Oracle {
        private final Optional<ImmutableMap<String, String>> componentRefsNamesMap;
        private final Optional<String> componentName;
        private final boolean insideParameterisedId;

        private Oracle() {
            this.componentRefsNamesMap = Optional.absent();
            this.componentName = Optional.absent();
            this.insideParameterisedId = false;
        }

        private Oracle(Optional<ImmutableMap<String, String>> componentRefsNamesMap,
                    Optional<String> componentName, boolean insideParameterisedId) {
            this.componentRefsNamesMap = componentRefsNamesMap;
            this.componentName = componentName;
            this.insideParameterisedId = insideParameterisedId;
        }

        private Oracle modifyNamesMap(ImmutableMap<String, String> componentRefsNamesMap) {
            return new Oracle(Optional.of(componentRefsNamesMap), this.componentName, this.insideParameterisedId);
        }

        private Oracle modifyInsideParameterisedId(boolean insideParameterisedId) {
            return new Oracle(this.componentRefsNamesMap, this.componentName, insideParameterisedId);
        }

        private Oracle modifyComponentName(String componentName) {
            return new Oracle(this.componentRefsNamesMap, Optional.of(componentName), this.insideParameterisedId);
        }
    }

    /**
     * Builder for a task optimization checker.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static final class Builder {
        /**
         * Data necessary to build a task optimization checker.
         */
        private final ImmutableList.Builder<Declaration> declarationsBuilder;
        private final SchedulerSpecification schedulerSpec;

        /**
         * Private constructor to limit its accessibility.
         */
        private Builder(SchedulerSpecification schedulerSpec) {
            this.schedulerSpec = schedulerSpec;
            this.declarationsBuilder = ImmutableList.builder();
        }

        /**
         * Add the given declarations for checking.
         *
         * @param declarations Declarations to add.
         * @return <code>this</code>
         */
        public Builder addDeclarations(Collection<? extends Declaration> declarations) {
            this.declarationsBuilder.addAll(declarations);
            return this;
        }

        /**
         * Add the given declaration for checking.
         *
         * @param declaration Declaration to add.
         * @return <code>this</code>
         */
        public Builder addDeclaration(Declaration declaration) {
            this.declarationsBuilder.add(declaration);
            return this;
        }

        public TaskOptimizationChecker build() {
            return new TaskOptimizationChecker(this);
        }
    }
}
