package pl.edu.mimuw.nesc.analysis;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.ast.type.FunctionType;
import pl.edu.mimuw.nesc.ast.type.InterfaceType;
import pl.edu.mimuw.nesc.ast.type.Type;
import pl.edu.mimuw.nesc.ast.type.UnknownType;
import pl.edu.mimuw.nesc.declaration.object.ComponentRefDeclaration;
import pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration;
import pl.edu.mimuw.nesc.declaration.object.InterfaceRefDeclaration;
import pl.edu.mimuw.nesc.declaration.object.ObjectDeclaration;
import pl.edu.mimuw.nesc.declaration.object.ObjectKind;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.facade.component.BareEntity;
import pl.edu.mimuw.nesc.facade.component.ComponentRefFacade;
import pl.edu.mimuw.nesc.facade.component.InterfaceRefEntity;
import pl.edu.mimuw.nesc.facade.component.SpecificationEntity;
import pl.edu.mimuw.nesc.facade.iface.InterfaceEntity;
import pl.edu.mimuw.nesc.problem.ErrorHelper;
import pl.edu.mimuw.nesc.problem.issue.ErroneousIssue;
import pl.edu.mimuw.nesc.problem.issue.InvalidConnectionError;
import pl.edu.mimuw.nesc.problem.issue.InvalidEndpointError;
import pl.edu.mimuw.nesc.problem.issue.InvalidEqConnectionError;
import pl.edu.mimuw.nesc.problem.issue.InvalidInterfaceInstantiationError;
import pl.edu.mimuw.nesc.problem.issue.InvalidInterfaceParameterError;
import pl.edu.mimuw.nesc.problem.issue.InvalidRpConnectionError;
import pl.edu.mimuw.nesc.problem.issue.UndeclaredIdentifierError;
import pl.edu.mimuw.nesc.symboltable.SymbolTable;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static pl.edu.mimuw.nesc.problem.issue.InvalidInterfaceInstantiationError.InterfaceRefProblemKind;
import static pl.edu.mimuw.nesc.problem.issue.InvalidInterfaceParameterError.IfaceParamProblemKind;
import static java.lang.String.format;

/**
 * A class that is responsible for analysis of constructs that are tightly
 * connected with the NesC language, e.g. <code>provides</code> or
 * <code>uses</code> specification elements.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class NescAnalysis {
    /**
     * Check the correctness of an interface instantiation. The given error
     * helper is notified about detected errors.
     *
     * @param ifaceRefDecl Declaration object for interface reference that is
     *                     checked.
     * @param errorHelper Object that will be notified about detected errors.
     */
    public static void checkInterfaceInstantiation(InterfaceRefDeclaration ifaceRefDecl,
            ErrorHelper errorHelper) {

        if (!ifaceRefDecl.getIfaceDeclaration().isPresent()) {
            return;
        }

        final Optional<LinkedList<Declaration>> paramsDecls = ifaceRefDecl.getIfaceDeclaration()
                .get().getAstInterface().getParameters();
        final Optional<LinkedList<Expression>> instantiationTypes = ifaceRefDecl
                .getAstInterfaceRef().getArguments();

        final Optional<InterfaceRefProblemKind> problemKind;
        int definitionParamsCount = 0, providedParamsCount = 0;

        if (paramsDecls.isPresent() && !instantiationTypes.isPresent()) {
            problemKind = Optional.of(InterfaceRefProblemKind.MISSING_PARAMETERS);
        } else if (!paramsDecls.isPresent() && instantiationTypes.isPresent()) {
            problemKind = Optional.of(InterfaceRefProblemKind.UNEXPECTED_PARAMETERS);
        } else if (paramsDecls.isPresent()) {

            // Interface is generic and parameters are provided

            final LinkedList<Declaration> decls = paramsDecls.get();
            final LinkedList<Expression> types = instantiationTypes.get();

            definitionParamsCount = decls.size();
            providedParamsCount = types.size();

            if (definitionParamsCount != providedParamsCount) {
                problemKind = Optional.of(InterfaceRefProblemKind.PARAMETERS_COUNTS_DIFFER);
            } else {
                checkInterfaceParameters(decls.iterator(), types.iterator(),
                        ifaceRefDecl.getIfaceName(), errorHelper);
                problemKind = Optional.absent();
            }
        } else {
            // Interface is not generic and parameters are not provided
            problemKind = Optional.absent();
        }

        if (problemKind.isPresent()) {
            final InvalidInterfaceInstantiationError error = new InvalidInterfaceInstantiationError(
                    problemKind.get(),
                    ifaceRefDecl.getIfaceName(),
                    definitionParamsCount,
                    providedParamsCount
            );

            errorHelper.error(ifaceRefDecl.getAstInterfaceRef().getLocation(),
                    ifaceRefDecl.getAstInterfaceRef().getEndLocation(), error);
        }
    }

    /**
     * Check the constraints for generic parameters in an interface reference
     * and report detected errors.
     */
    private static void checkInterfaceParameters(Iterator<Declaration> declIt,
            Iterator<Expression> typeIt, String interfaceName, ErrorHelper errorHelper) {

        int paramNum = 0;

        while (declIt.hasNext()) {

            ++paramNum;
            final Declaration decl = declIt.next();
            final Expression expr = typeIt.next();

            // Currently the condition is never true
            if (decl instanceof ErrorDecl || expr instanceof ErrorExpr) {
                continue;
            }

            checkState(decl instanceof TypeParmDecl, "unexpected class of a generic parameter declaration: "
                    + decl.getClass().getCanonicalName());
            checkState(expr instanceof TypeArgument, "unexpected class of a type for generic parameter: "
                    + expr.getClass().getCanonicalName());

            final TypeParmDecl typeParmDecl = (TypeParmDecl) decl;
            final TypeArgument typeArgument = (TypeArgument) expr;

            if (!typeParmDecl.getDeclaration().getDenotedType().isPresent()
                    || !typeArgument.getAsttype().getType().isPresent()) {
                continue;
            }

            checkInterfaceParameter(
                    (UnknownType) typeParmDecl.getDeclaration().getDenotedType().get(),
                    typeArgument.getAsttype().getType().get(),
                    interfaceName,
                    typeArgument,
                    paramNum,
                    errorHelper
            );
        }
    }

    private static void checkInterfaceParameter(UnknownType definitionType, Type providedType,
            String interfaceName, TypeArgument expr, int paramNum, ErrorHelper errorHelper) {

        final Optional<IfaceParamProblemKind> problem;

        if (providedType.isFunctionType()) {
            problem = Optional.of(IfaceParamProblemKind.FUNCTION_TYPE);
        } else if (providedType.isArrayType()) {
            problem = Optional.of(IfaceParamProblemKind.ARRAY_TYPE);
        } else if (!providedType.isComplete()) {
            problem = Optional.of(IfaceParamProblemKind.INCOMPLETE_TYPE);
        } else if (definitionType.isUnknownIntegerType() && !providedType.isGeneralizedIntegerType()) {
            problem = Optional.of(IfaceParamProblemKind.INTEGER_TYPE_EXPECTED);
        } else if (definitionType.isUnknownArithmeticType() && !providedType.isGeneralizedArithmeticType()) {
            problem = Optional.of(IfaceParamProblemKind.ARITHMETIC_TYPE_EXPECTED);
        } else {
            problem = Optional.absent();
        }

        if (problem.isPresent()) {
            final ErroneousIssue error = new InvalidInterfaceParameterError(problem.get(),
                    interfaceName, definitionType, providedType, paramNum);
            errorHelper.error(expr.getLocation(), expr.getEndLocation(), error);
        }
    }

    /**
     * Check the correctness of given connection in the given environment. All
     * detected errors are reported to the given error helper.
     *
     * @param connection Connection to check.
     * @param environment Environment of the given connection.
     * @param errorHelper Object that will be notified about detected errors.
     * @throws NullPointerException One of the argument is null.
     */
    public static void checkRpConnection(RpConnection connection, Environment environment,
            ErrorHelper errorHelper) {
        checkNotNull(connection, "connection cannot be null");
        checkNotNull(environment, "environment cannot be null");
        checkNotNull(errorHelper, "error helper cannot be null");

        final RpConnectionAnalyzer analyzer = new RpConnectionAnalyzer(connection,
                environment, errorHelper);
        analyzer.analyze();
    }

    /**
     * Check the correctness of the given equate wires in the given environment.
     * All detected errors are reported to the given error helper.
     *
     * @param connection Equate wires to check.
     * @param environment Environment to check the connection in.
     * @param errorHelper Object that will be notified about detected errors.
     * @throws NullPointerException One of the arguments is null.
     */
    public static void checkEqConnection(EqConnection connection, Environment environment,
            ErrorHelper errorHelper) {
        checkNotNull(connection, "connection cannot be null");
        checkNotNull(environment, "environment cannot be null");
        checkNotNull(errorHelper, "error helper cannot be null");

        final EqConnectionAnalyzer analyzer = new EqConnectionAnalyzer(connection,
                environment, errorHelper);
        analyzer.analyze();
    }

    /**
     * Abstract class for analysis of a connection.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static abstract class ConnectionAnalyzer<T extends Connection> {
        private static final LinkedList<Expression> EMPTY_EXPR_LIST = new LinkedList<>();

        protected final ErrorHelper errorHelper;
        protected final Environment environment;
        protected final T connection;
        protected boolean errorDetected = false;

        protected ConnectionAnalyzer(T connection, Environment environment,
                ErrorHelper errorHelper) {
            this.connection = connection;
            this.environment = environment;
            this.errorHelper = errorHelper;
        }

        protected void firstEndpointCheck(EndPoint endpoint, boolean requireTwoIdsForArgumentsList) {
            final LinkedList<ParameterisedIdentifier> identifiers = endpoint.getIds();
            int listsCount = 0;
            boolean listNotLast = false;

            for (ParameterisedIdentifier identifer : identifiers) {
                if (listsCount > 0) {
                    listNotLast = true;
                }
                if (!identifer.getArguments().isEmpty()) {
                    ++listsCount;
                }
            }

            final Optional<? extends ErroneousIssue> error;

            if (listsCount > 1) {
                error = Optional.of(InvalidEndpointError.tooManyArgumentsLists(listsCount));
            } else if (listNotLast) {
                error = Optional.of(InvalidEndpointError.nestedArgumentsList());
            } else if (identifiers.size() != 1 && identifiers.size() != 2) {
                error = Optional.of(InvalidEndpointError.invalidIdentifiersCount(identifiers.size()));
            } else if (requireTwoIdsForArgumentsList && identifiers.size() == 1
                    && !identifiers.getFirst().getArguments().isEmpty()) {
                error = Optional.of(InvalidEndpointError.parameterisedComponentRef());
            } else {
                error = Optional.absent();
            }

            if (error.isPresent()) {
                errorHelper.error(endpoint.getLocation(), endpoint.getEndLocation(), error.get());
                errorDetected = true;
            }
        }

        protected boolean secondEndpointCheck(EndPoint endpoint, SpecificationEntity entity) {

            final LinkedList<Expression> providedParams = endpoint.getIds().getLast().getArguments();
            final String endpointName = endpoint.getIds().size() == 2
                    ? format("%s.%s", endpoint.getIds().getFirst().getName().getName(),
                            endpoint.getIds().getLast().getName().getName())
                    : endpoint.getIds().getFirst().getName().getName();
            boolean result = true;

            if (!providedParams.isEmpty() && !entity.isParameterised()) {
                errorDetected = true;
                errorHelper.error(endpoint.getLocation(), endpoint.getEndLocation(),
                        InvalidEndpointError.unexpectedArguments(endpointName, providedParams.size()));
                return false;
            } else if (!providedParams.isEmpty()) {
                if (providedParams.size() != entity.getInstanceParameters().get().size()) {
                    errorDetected = true;
                    final ErroneousIssue error = InvalidEndpointError.invalidArgumentsCount(endpointName,
                            entity.getInstanceParameters().get().size(), providedParams.size());
                    errorHelper.error(endpoint.getLocation(), endpoint.getEndLocation(), error);
                    return false;
                }

                int j = 0;

                // TODO check remaining conditions for endpoint arguments

                for (Expression expr : providedParams) {
                    ++j;

                    if (!expr.getType().isPresent()) {
                        continue;
                    }

                    if (!expr.getType().get().isGeneralizedIntegerType()) {
                        errorDetected = true;
                        final ErroneousIssue error = InvalidEndpointError.invalidEndpointArgumentType(endpointName,
                                j, expr, expr.getType().get());
                        errorHelper.error(endpoint.getLocation(), endpoint.getEndLocation(), error);
                        result = false;
                    }
                }
            }

            return result;
        }

        protected Optional<SpecificationEntity> requireSpecificationEntity(ComponentRefFacade facade,
                String name, Location errorStartLoc, Location errorEndLoc) {

            final Optional<SpecificationEntity> result = facade.get(name);

            if (!result.isPresent()) {
                final ErroneousIssue error = InvalidEndpointError.nonexistentSpecificationEntity(facade.getInstanceName(), name);
                errorHelper.error(errorStartLoc, errorEndLoc, error);
                errorDetected = true;
            }

            return result;
        }

        protected boolean matchSpecificationElements(SpecificationEntity entity1,
                EntityPack pack1, SpecificationEntity entity2, EntityPack pack2,
                boolean mustMatch, boolean checkDirection) {

            final Optional<MatcherUsesProvidesMode> towardsEntity2 = checkDirection
                    ? Optional.of(MatcherUsesProvidesMode.TOWARDS_STORED_ENTITY)
                    : Optional.<MatcherUsesProvidesMode>absent();

            final MatcherVisitor matcherVisitor = new MatcherVisitor(entity2,
                    pack2, mustMatch, towardsEntity2);
            return entity1.accept(matcherVisitor, pack1);
        }

        protected Optional<SpecificationEntity> implicitConnectionLookup(ComponentRefFacade componentFacade,
                SpecificationEntity entity, EntityPack pack, Optional<MatcherUsesProvidesMode> usesProvidedMode) {

            final Set<String> matchedElementsNames = new HashSet<>();
            Optional<SpecificationEntity> result = Optional.absent();

            final MatcherVisitor matcherVisitor = new MatcherVisitor(entity, pack, false,
                    usesProvidedMode);

            // Iterate over all specification elements and check which match
            for (Map.Entry<String, SpecificationEntity> specEntry : componentFacade.getAll()) {
                final String entityName = specEntry.getKey();
                final SpecificationEntity entity2 = specEntry.getValue();
                final String testedEntityStr = format("%s.%s", componentFacade.getInstanceName(), entityName);
                final EntityPack pack2 = new EntityPack(EMPTY_EXPR_LIST, testedEntityStr);

                if (entity2.accept(matcherVisitor, pack2)) {
                    result = Optional.of(entity2);
                    matchedElementsNames.add(testedEntityStr);
                }
            }

            // Report error if no elements have matched
            if (matchedElementsNames.isEmpty()) {
                reportError(InvalidConnectionError.implicitConnectionNoMatch(pack.endpointStr,
                        componentFacade.getInstanceName()));
            } else if (matchedElementsNames.size() != 1) {
                reportError(InvalidConnectionError.implicitConnectionManyMatches(pack.endpointStr,
                        matchedElementsNames));
            }

            return matchedElementsNames.size() == 1
                    ? result
                    : Optional.<SpecificationEntity>absent();
        }

        protected boolean canContinue() {
            return !errorDetected;
        }

        protected void reportError(ErroneousIssue error) {
            errorDetected = true;
            errorHelper.error(connection.getLocation(), connection.getEndLocation(), error);
        }

        /**
         * <p>Class that is responsible for checking if two specification
         * elements are compatible. The match occurs if and only if all of the
         * following conditions are fulfilled:</p>
         *
         * <ul>
         *     <li>Both specification elements are interface references or both
         *     are bare entities.</li>
         *     <li>If both specification elements are bare entities, then either
         *     both are commands or both are events and both have compatible
         *     function types.</li>
         *     <li>If both specification elements are interface references, then
         *     both have compatible interface types.</li>
         *     <li>Usage of parameterised entities is correct.</li>
         *     <li>The uses/provides relationship is to be checked and it is
         *     correct.</li>
         * </ul>
         *
         * <p>Errors are reported to the error helper and flag
         * <code>errorDetected</code> is set if <code>mustMatch</code>
         * is <code>true</code>.</p>
         *
         * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
         */
        private final class MatcherVisitor implements SpecificationEntity.Visitor<Boolean, EntityPack> {

            private final SpecificationEntity entity2;
            private final EntityPack pack2;
            private final boolean mustMatch;
            private final Optional<MatcherUsesProvidesMode> usesProvidesMode;

            private MatcherVisitor(SpecificationEntity entity2, EntityPack pack2, boolean mustMatch,
                    Optional<MatcherUsesProvidesMode> usesProvidesMode) {
                this.entity2 = entity2;
                this.pack2 = pack2;
                this.mustMatch = mustMatch;
                this.usesProvidesMode = usesProvidesMode;
            }

            @Override
            public Boolean visit(BareEntity bareEntity, EntityPack pack) {
                // Instance parameters

                if (!performInstanceParamsCheck(bareEntity, pack)) {
                    return false;
                }

                // Kind of specification elements

                if (entity2.getKind() != SpecificationEntity.Kind.BARE) {
                    if (mustMatch) {
                        reportError(InvalidConnectionError.entitiesKindMismatch(bareEntity.getInterfaceEntityKind(),
                                pack.endpointStr, pack2.endpointStr));
                    }
                    return false;
                }

                final BareEntity bareEntity2 = (BareEntity) entity2;

                // Interface entity kind

                if (bareEntity.getInterfaceEntityKind() != bareEntity2.getInterfaceEntityKind()) {
                    if (mustMatch) {
                        reportError(InvalidConnectionError.bareEntitiesKindMismatch(pack.endpointStr,
                                bareEntity.getInterfaceEntityKind(), pack2.endpointStr,
                                bareEntity2.getInterfaceEntityKind()));
                    }
                    return false;
                }

                // Types

                if (bareEntity.getType().isPresent() && bareEntity2.getType().isPresent()) {
                    final FunctionType type = bareEntity.getType().get();
                    final FunctionType type2 = bareEntity2.getType().get();

                    if (!type.isCompatibleWith(type2)) {
                        if (mustMatch) {
                            reportError(InvalidConnectionError.invalidBareFunctionTypes(bareEntity.getInterfaceEntityKind(),
                                    pack.endpointStr, type, pack2.endpointStr, type2));
                        }
                        return false;
                    }
                }

                // Direction of the connection (for link wires)

                return performUsesProvidesCheck(bareEntity, pack);
            }

            @Override
            public Boolean visit(InterfaceRefEntity ifaceEntity, EntityPack pack) {
                // Instance parameters

                if (!performInstanceParamsCheck(ifaceEntity, pack)) {
                    return false;
                }

                // Kind of specification elements

                if (entity2.getKind() != SpecificationEntity.Kind.INTERFACE) {
                    if (mustMatch) {
                        final BareEntity bareEntity = (BareEntity) entity2;
                        reportError(InvalidConnectionError.entitiesKindMismatch(bareEntity.getInterfaceEntityKind(),
                                pack2.endpointStr, pack.endpointStr));
                    }
                    return false;
                }

                final InterfaceRefEntity ifaceEntity2 = (InterfaceRefEntity) entity2;

                // Interface types

                if (ifaceEntity.getType().isPresent() && ifaceEntity2.getType().isPresent()) {
                    final InterfaceType type = ifaceEntity.getType().get();
                    final InterfaceType type2 = ifaceEntity2.getType().get();

                    if (!type.isCompatibleWith(type2)) {
                        if (mustMatch) {
                            reportError(InvalidConnectionError.invalidInterfaceTypes(pack.endpointStr,
                                    type, pack2.endpointStr, type2));
                        }
                        return false;
                    }
                }

                // Direction of the connection (for link wires)

                return performUsesProvidesCheck(ifaceEntity, pack);
            }

            private boolean performUsesProvidesCheck(SpecificationEntity entity1, EntityPack pack1) {
                if (!usesProvidesMode.isPresent()) {
                    return true;
                }

                switch (usesProvidesMode.get()) {
                    case TOWARDS_STORED_ENTITY:
                        return performDirectionCheck(entity1, pack1, true);
                    case TOWARDS_TESTED_ENTITY:
                        return performDirectionCheck(entity1, pack1, false);
                    case SAME_AS_STORED:
                        return bothUsedOrProvidedCheck(entity1, pack1);
                    default:
                        throw new RuntimeException("unexpected uses/provides mode '" + usesProvidesMode.get() + "'");
                }
            }

            private boolean performDirectionCheck(SpecificationEntity entity1, EntityPack pack1,
                    boolean towardsStoredEntity) {
                final SpecificationEntity fromEntity, toEntity;
                final EntityPack fromEntityPack, toEntityPack;

                if (towardsStoredEntity) {
                    toEntity = entity2;
                    toEntityPack = pack2;
                    fromEntity = entity1;
                    fromEntityPack = pack1;
                } else {
                    toEntity = entity1;
                    toEntityPack = pack1;
                    fromEntity = entity2;
                    fromEntityPack = pack2;
                }

                if (fromEntity.isProvided() || !toEntity.isProvided()) {
                    if (mustMatch) {
                        reportError(InvalidConnectionError.invalidDirection(fromEntityPack.endpointStr,
                                fromEntity.isProvided(), toEntityPack.endpointStr,
                                toEntity.isProvided()));
                    }
                    return false;
                }

                return true;
            }

            private boolean bothUsedOrProvidedCheck(SpecificationEntity entity1, EntityPack pack1) {
                if (entity2.isProvided() != entity1.isProvided()) {
                    if (mustMatch) {
                        reportError(InvalidConnectionError.expectedBothProvidedOrUsed(pack1.endpointStr,
                                entity1.isProvided(), pack2.endpointStr, entity2.isProvided()));
                    }
                    return false;
                }

                return true;
            }

            private boolean performInstanceParamsCheck(SpecificationEntity entity1, EntityPack pack1) {
                // If both endpoints have no arguments

                if (pack1.providedParams.isEmpty() && pack2.providedParams.isEmpty()) {
                    return checkEndpointsWithoutArguments(entity1, pack1);
                }

                // An endpoint has arguments

                if (pack1.providedParams.isEmpty()) {
                    return checkNoArgumentsEndpoint(entity1, pack1, entity2, pack2);
                } else if (pack2.providedParams.isEmpty()) {
                    return checkNoArgumentsEndpoint(entity2, pack2, entity1, pack1);
                } else {
                    return true;
                }
            }

            private boolean checkNoArgumentsEndpoint(SpecificationEntity entityNoArgs, EntityPack packNoArgs,
                    SpecificationEntity entityWithArgs, EntityPack packWithArgs) {

                if (entityNoArgs.isParameterised()) {
                    if (mustMatch) {
                        reportError(InvalidConnectionError.parameterisedEndpointMismatch(packNoArgs.endpointStr,
                                packWithArgs.endpointStr, true));
                    }
                    return false;
                }

                return true;
            }

            private boolean checkEndpointsWithoutArguments(SpecificationEntity entity1, EntityPack pack1) {

                if (entity1.isParameterised() != entity2.isParameterised()) {
                    if (mustMatch) {
                        reportError(InvalidConnectionError.parameterisedEndpointMismatch(pack1.endpointStr,
                                pack2.endpointStr, entity1.isParameterised()));
                    }
                    return false;
                } else if (entity1.isParameterised()) {
                    final ImmutableList<Optional<Type>> types1 = entity1.getInstanceParameters().get();
                    final ImmutableList<Optional<Type>> types2 = entity2.getInstanceParameters().get();

                    if (types1.size() != types2.size()) {
                        if (mustMatch) {
                            reportError(InvalidConnectionError.parameterisedEndpointCountMismatch(pack1.endpointStr,
                                    types1.size(), pack2.endpointStr, types2.size()));
                        }
                        return false;
                    }

                    for (int i = 0; i < types1.size(); ++i) {
                        final Optional<Type> type1 = types1.get(i);
                        final Optional<Type> type2 = types2.get(i);

                        if (type1.isPresent() && type2.isPresent()
                                && !type1.get().isCompatibleWith(type2.get())) {
                            if (mustMatch) {
                                reportError(InvalidConnectionError.parameterisedEndpointTypeMismatch(pack1.endpointStr,
                                        type1.get(), pack2.endpointStr, type2.get(), i + 1));
                            }
                            return false;
                        }
                    }
                }

                return true;
            }

            /**
             * Unconditionally reports the given error and sets
             * <code>errorDetected</code> flag.
             *
             * @param error Error to report.
             */
            private void reportError(ErroneousIssue error) {
                ConnectionAnalyzer.this.reportError(error);
            }
        }

        /**
         * Small class with data associated with a specification entity.
         *
         * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
         */
        protected static final class EntityPack {
            private final LinkedList<Expression> providedParams;
            private final String endpointStr;

            protected EntityPack(LinkedList<Expression> providedParams, String endpointStr) {
                this.providedParams = providedParams;
                this.endpointStr = endpointStr;
            }
        }

        /**
         * Enumeration type that represents how uses/provides relationship of
         * entities will be checked by the matcher visitor.
         *
         * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
         */
        enum MatcherUsesProvidesMode {
            /**
             * Additional requirement for a match to occur: the stored entity
             * {@link ConnectionAnalyzer.MatcherVisitor#entity2} is provided
             * and the tested one is used.
             */
            TOWARDS_STORED_ENTITY,
            /**
             * Additional requirement for a match to occur: the stored entity is
             * used and the tested one is provided.
             */
            TOWARDS_TESTED_ENTITY,
            /**
             * Additional requirement for a match to occur: the tested entity
             * is provided if and only if the stored one is.
             */
            SAME_AS_STORED,
        }
    }

    /**
     * Class responsible for analysis of link wires.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class RpConnectionAnalyzer extends ConnectionAnalyzer<RpConnection> {

        private final EndPoint from;
        private final EndPoint to;

        private ComponentRefDeclaration fromDeclaration;
        private ComponentRefDeclaration toDeclaration;

        private RpConnectionAnalyzer(RpConnection connection, Environment environment,
                ErrorHelper errorHelper) {
            super(connection, environment, errorHelper);

            this.from = connection.getEndPoint1();
            this.to = connection.getEndPoint2();
        }

        private void analyze() {
            analyzeEndpoints();
            analyzeComponents();
            analyzeWiring();
        }

        private void analyzeEndpoints() {
            firstEndpointCheck(from, true);
            firstEndpointCheck(to, true);
        }

        private void analyzeComponents() {
            if (!canContinue()) {
                return;
            }

            final SymbolTable<ObjectDeclaration> objectsTab = environment.getObjects();

            final Word[] names = {
                    from.getIds().getFirst().getName(),
                    to.getIds().getFirst().getName(),
            };
            final ComponentRefDeclaration[] declarations = new ComponentRefDeclaration[2];

            // Check if component references are correct

            for (int i = 0; i < names.length; ++i) {
                final Optional<? extends ObjectDeclaration> optDeclaration =
                        objectsTab.get(names[i].getName());

                if (!optDeclaration.isPresent()) {
                    errorHelper.error(names[i].getLocation(), names[i].getEndLocation(),
                            new UndeclaredIdentifierError(names[i].getName()));
                    errorDetected = true;
                } else if (optDeclaration.get().getKind() != ObjectKind.COMPONENT) {
                    errorHelper.error(names[i].getLocation(), names[i].getEndLocation(),
                            InvalidRpConnectionError.componentRefExpected(names[i].getName()));
                    errorDetected = true;
                } else {
                    declarations[i] = (ComponentRefDeclaration) optDeclaration.get();
                }
            }

            if (!canContinue()) {
                return;
            }

            fromDeclaration = declarations[0];
            toDeclaration = declarations[1];
        }

        private void analyzeWiring() {
            if (!canContinue()) {
                return;
            }

            if (from.getIds().size() == 1 && to.getIds().size() == 1) {
                reportError(InvalidRpConnectionError.missingSpecificationElement(fromDeclaration.getName(),
                        toDeclaration.getName()));
                return;
            }

            // End analysis if one of the components is erroneous

            if (!fromDeclaration.getFacade().goodComponentRef()
                    || !toDeclaration.getFacade().goodComponentRef()) {
                errorDetected = true;
                return;
            }

            // Different kind of wiring

            if (from.getIds().size() == 1 || to.getIds().size() == 1) {
                analyzeImplicitWiring();
            } else {
                analyzeExplicitWiring();
            }
        }

        private void analyzeImplicitWiring() {
            final MatcherUsesProvidesMode usesProvidesMode;
            final ComponentRefDeclaration explicitDeclaration;
            final ComponentRefDeclaration implicitDeclaration;
            final EndPoint explicitEndpoint;
            final SpecificationEntity explicitEntity;
            final EntityPack expliticEntityPack;

            if (from.getIds().size() == 1) {
                usesProvidesMode = MatcherUsesProvidesMode.TOWARDS_STORED_ENTITY;
                explicitDeclaration = toDeclaration;
                implicitDeclaration = fromDeclaration;
                explicitEndpoint = to;
            } else {
                usesProvidesMode = MatcherUsesProvidesMode.TOWARDS_TESTED_ENTITY;
                explicitDeclaration = fromDeclaration;
                implicitDeclaration = toDeclaration;
                explicitEndpoint = from;
            }

            final Word explicitName = explicitEndpoint.getIds().getLast().getName();
            final Optional<SpecificationEntity> optExplicitEntity = requireSpecificationEntity(explicitDeclaration.getFacade(),
                    explicitName.getName(), explicitName.getLocation(), explicitName.getEndLocation());

            if (!optExplicitEntity.isPresent()) {
                return;
            }

            explicitEntity = optExplicitEntity.get();
            expliticEntityPack = new EntityPack(explicitEndpoint.getIds().getLast().getArguments(),
                    format("%s.%s", explicitEndpoint.getIds().getFirst().getName().getName(),
                            explicitEndpoint.getIds().getLast().getName().getName()));

            // Check the explicit endpoint

            if (!secondEndpointCheck(explicitEndpoint, explicitEntity)) {
                return;
            }

            // Check the connection

            implicitConnectionLookup(implicitDeclaration.getFacade(), explicitEntity,
                    expliticEntityPack, Optional.of(usesProvidesMode));
        }

        private void analyzeExplicitWiring() {
            // Look for the entities from both components

            final Optional<SpecificationEntity> optFromEntity = requireSpecificationEntity(
                fromDeclaration.getFacade(),
                from.getIds().getLast().getName().getName(),
                from.getIds().getLast().getLocation(),
                from.getIds().getLast().getEndLocation()
            );

            final Optional<SpecificationEntity> optToEntity = requireSpecificationEntity(
                toDeclaration.getFacade(),
                to.getIds().getLast().getName().getName(),
                to.getIds().getLast().getLocation(),
                to.getIds().getLast().getEndLocation()
            );

            if (!optFromEntity.isPresent() || !optToEntity.isPresent()) {
                return;
            }

            final SpecificationEntity fromEntity = optFromEntity.get();
            final SpecificationEntity toEntity = optToEntity.get();

            // Further endpoint check (instance parameters if any)

            boolean invalidArgs = !secondEndpointCheck(from, fromEntity);
            invalidArgs = !secondEndpointCheck(to, toEntity) || invalidArgs;

            if (invalidArgs) {
                return;
            }

            // Check if endpoints are compatible

            final EntityPack packFrom = new EntityPack(from.getIds().getLast().getArguments(),
                    format("%s.%s", fromDeclaration.getName(), fromEntity.getName()));
            final EntityPack packTo = new EntityPack(to.getIds().getLast().getArguments(),
                    format("%s.%s", toDeclaration.getName(), toEntity.getName()));

            matchSpecificationElements(fromEntity, packFrom, toEntity, packTo, true, true);
        }
    }

    /**
     * Class responsible for analysis of equate wires.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class EqConnectionAnalyzer extends ConnectionAnalyzer<EqConnection> {
        /**
         * Simple function to safely cast a type to a function type.
         */
        private static final Function<Type, FunctionType> FUNCTION_TRANSFORM = new Function<Type, FunctionType>() {
            @Override
            public FunctionType apply(Type type) {
                checkNotNull(type, "type cannot be null");
                checkState(type instanceof FunctionType, "unexpected type '%s'", type);
                return (FunctionType) type;
            }
        };

        private final ImmutableList<EndPoint> endpoints;
        private final ObjectDeclaration[] declarations = new ObjectDeclaration[2];
        private final SpecificationEntity[] entities = new SpecificationEntity[2];
        private final EntityPack[] packs = new EntityPack[2];

        private EqConnectionAnalyzer(EqConnection connection, Environment environment,
                ErrorHelper errorHelper) {
            super(connection, environment, errorHelper);

            this.endpoints = ImmutableList.of(connection.getEndPoint1(),
                    connection.getEndPoint2());
        }

        private void analyze() {
            initialEndpointsCheck();
            resolveReferredObjects();
            intermediateEndpointsCheck();
            resolveSpecificationEntities();
            finalEndpointsCheck();
            analyzeWiring();
        }

        private void initialEndpointsCheck() {
            firstEndpointCheck(endpoints.get(0), false);
            firstEndpointCheck(endpoints.get(1), false);
        }

        private void resolveReferredObjects() {
            if (!canContinue()) {
                return;
            }

            final SymbolTable<ObjectDeclaration> objectsTab = environment.getObjects();
            final Word[] names = {
                endpoints.get(0).getIds().getFirst().getName(),
                endpoints.get(1).getIds().getFirst().getName(),
            };

            // Find referred objects

            for (int i = 0; i < names.length; ++i) {
                final Optional<? extends ObjectDeclaration> optDeclaration =
                        objectsTab.get(names[i].getName());

                if (!optDeclaration.isPresent()) {
                    errorDetected = true;
                    errorHelper.error(names[i].getLocation(), names[i].getEndLocation(),
                            new UndeclaredIdentifierError(names[i].getName()));
                } else if (optDeclaration.get().getKind() != ObjectKind.COMPONENT
                        && optDeclaration.get().getKind() != ObjectKind.INTERFACE
                        && optDeclaration.get().getKind() != ObjectKind.FUNCTION) {
                    errorDetected = true;
                    errorHelper.error(names[i].getLocation(), names[i].getEndLocation(),
                            InvalidEqConnectionError.invalidObjectReferenced(names[i].getName()));
                } else if (optDeclaration.get().getKind() == ObjectKind.FUNCTION) {
                    final FunctionDeclaration declaration = (FunctionDeclaration) optDeclaration.get();
                    if (!declaration.isProvided().isPresent()) {
                        errorDetected = true;
                        errorHelper.error(names[i].getLocation(), names[i].getEndLocation(),
                                InvalidEqConnectionError.invalidObjectReferenced(names[i].getName()));
                    } else {
                        declarations[i] = declaration;
                    }
                } else {
                    declarations[i] = optDeclaration.get();
                }
            }

            if (!canContinue()) {
                return;
            }

            // Check if an external specification element is present

            if (declarations[0].getKind() == ObjectKind.COMPONENT
                    && declarations[1].getKind() == ObjectKind.COMPONENT) {
                reportError(InvalidEqConnectionError.expectedExternalSpecElement());
            }
        }

        private void intermediateEndpointsCheck() {
            if (!canContinue()) {
                return;
            }

            for (int i = 0; i < declarations.length; ++i) {
                final EndPoint endpoint = endpoints.get(i);

                if (declarations[i].getKind() != ObjectKind.COMPONENT
                        && endpoint.getIds().size() != 1) {
                    errorDetected = true;
                    errorHelper.error(endpoint.getLocation(), endpoint.getEndLocation(),
                            InvalidEqConnectionError.externalSpecElementDereference(endpoint.getIds().size()));
                } else if (declarations[i].getKind() == ObjectKind.COMPONENT
                        && !endpoint.getIds().getLast().getArguments().isEmpty()
                        && endpoint.getIds().size() == 1) {
                    errorDetected = true;
                    errorHelper.error(endpoint.getLocation(), endpoint.getEndLocation(),
                            InvalidEqConnectionError.argumentsForComponent(endpoint.getIds().getLast().getName().getName()));
                }
            }
        }

        private void resolveSpecificationEntities() {
            if (!canContinue()) {
                return;
            }

            for (int i = 0; i < declarations.length; ++i) {
                final EndPoint endpoint = endpoints.get(i);

                if (declarations[i].getKind() == ObjectKind.COMPONENT) {
                    final ComponentRefDeclaration declaration = (ComponentRefDeclaration) declarations[i];
                    final ComponentRefFacade facade = declaration.getFacade();

                    if (!facade.goodComponentRef()) {
                        errorDetected = true;
                        continue;
                    }

                    if (endpoint.getIds().size() == 2) {
                        final Optional<SpecificationEntity> entity = requireSpecificationEntity(facade,
                                endpoint.getIds().getLast().getName().getName(), endpoint.getLocation(),
                                endpoint.getEndLocation());
                        if (!entity.isPresent()) {
                            continue;
                        }

                        entities[i] = entity.get();
                    }
                } else if (declarations[i].getKind() == ObjectKind.INTERFACE) {
                    final InterfaceRefDeclaration declaration = (InterfaceRefDeclaration) declarations[i];
                    entities[i] = new InterfaceRefEntity(declaration.isProvides(), declaration.getName(),
                            (InterfaceType) declaration.getType().get(), declaration.getInstanceParameters());
                } else if (declarations[i].getKind() == ObjectKind.FUNCTION) {
                    final FunctionDeclaration declaration = (FunctionDeclaration) declarations[i];
                    final InterfaceEntity.Kind kind;

                    switch (declaration.getFunctionType()) {
                        case COMMAND: kind = InterfaceEntity.Kind.COMMAND; break;
                        case EVENT: kind = InterfaceEntity.Kind.EVENT; break;
                        default: throw new RuntimeException("unexpected type of a bare command and event '"
                                    + declaration.getFunctionType() + "'");
                    }

                    entities[i] = new BareEntity(declaration.isProvided().get(), declaration.getName(),
                            declaration.getType().transform(FUNCTION_TRANSFORM), kind,
                            declaration.getInstanceParameters());
                } else {
                    throw new IllegalStateException("unexpected kind of an object in equate wires '"
                            + declarations[i].getKind() + "'");
                }
            }
        }

        private void finalEndpointsCheck() {
            if (!canContinue()) {
                return;
            }

            for (int i = 0; i < entities.length; ++i) {
                if (entities[i] == null) {
                    continue;
                }

                secondEndpointCheck(endpoints.get(i), entities[i]);
            }
        }

        private void analyzeWiring() {
            if (!canContinue()) {
                return;
            }

            // Prepare entity packs

            for (int i = 0; i < entities.length; ++i) {
                if (entities[i] == null) {
                    continue;
                }

                final EndPoint endpoint = endpoints.get(i);
                final String endpointStr = endpoint.getIds().size() == 2
                        ? format("%s.%s", endpoint.getIds().getFirst().getName().getName(),
                            endpoint.getIds().getLast().getName().getName())
                        : endpoint.getIds().getFirst().getName().getName();

                packs[i] = new EntityPack(endpoint.getIds().getLast().getArguments(), endpointStr);
            }

            // Check wiring

            if (entities[0] == null || entities[1] == null) {
                analyzeImplicitWiring();
            } else {
                analyzeExplicitWiring();
            }
        }

        private void analyzeImplicitWiring() {
            final int componentIndex = entities[0] == null ? 0 : 1;
            final int otherIndex = componentIndex ^ 1;
            final ComponentRefDeclaration ctDeclaration = (ComponentRefDeclaration) declarations[componentIndex];

            implicitConnectionLookup(ctDeclaration.getFacade(), entities[otherIndex],
                    packs[otherIndex], Optional.of(MatcherUsesProvidesMode.SAME_AS_STORED));
        }

        private void analyzeExplicitWiring() {
            if (!matchSpecificationElements(entities[0], packs[0], entities[1], packs[1], true, false)) {
                return;
            }

            // Check uses/provides relationship

            if (declarations[0].getKind() == ObjectKind.COMPONENT
                    || declarations[1].getKind() == ObjectKind.COMPONENT) {
                // One external and one internal element
                if (entities[0].isProvided() && !entities[1].isProvided()
                        || !entities[0].isProvided() && entities[1].isProvided()) {
                    reportError(InvalidEqConnectionError.expectedBothUsedOrProvided(packs[0].endpointStr,
                            entities[0].isProvided(), packs[1].endpointStr, entities[1].isProvided()));
                }
            } else {
                // Both elements external
                if (entities[0].isProvided() && entities[1].isProvided()
                        || !entities[0].isProvided() && !entities[1].isProvided()) {
                    reportError(InvalidEqConnectionError.expectedOneUsedAndOneProvided(packs[0].endpointStr,
                            packs[1].endpointStr, entities[0].isProvided()));
                }
            }
        }
    }

    /**
     * Private constructor to prevent from instantiating this class.
     */
    private NescAnalysis() {
    }
}
