package pl.edu.mimuw.nesc.analysis.expressions;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import java.util.LinkedList;
import org.apache.log4j.Logger;
import pl.edu.mimuw.nesc.ast.gen.ComponentDeref;
import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.gen.FunctionCall;
import pl.edu.mimuw.nesc.ast.gen.GenericCall;
import pl.edu.mimuw.nesc.ast.gen.Identifier;
import pl.edu.mimuw.nesc.ast.gen.InterfaceDeref;
import pl.edu.mimuw.nesc.astbuilding.Declarations;
import pl.edu.mimuw.nesc.astwriting.ASTWriter;
import pl.edu.mimuw.nesc.declaration.object.ComponentRefDeclaration;
import pl.edu.mimuw.nesc.declaration.object.ConstantDeclaration;
import pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration;
import pl.edu.mimuw.nesc.declaration.object.InterfaceRefDeclaration;
import pl.edu.mimuw.nesc.declaration.object.ObjectDeclaration;
import pl.edu.mimuw.nesc.declaration.object.ObjectKind;
import pl.edu.mimuw.nesc.declaration.object.TypenameDeclaration;
import pl.edu.mimuw.nesc.declaration.object.VariableDeclaration;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.facade.component.reference.ComponentRefFacade;
import pl.edu.mimuw.nesc.facade.component.reference.EnumerationConstant;
import pl.edu.mimuw.nesc.facade.iface.InterfaceEntity;
import pl.edu.mimuw.nesc.facade.iface.InterfaceRefFacade;
import pl.edu.mimuw.nesc.problem.ErrorHelper;
import pl.edu.mimuw.nesc.problem.issue.ErroneousIssue;
import pl.edu.mimuw.nesc.problem.issue.InvalidComponentDerefExprError;
import pl.edu.mimuw.nesc.problem.issue.InvalidIdentifierUsageError;
import pl.edu.mimuw.nesc.problem.issue.InvalidNescCallError;
import pl.edu.mimuw.nesc.problem.issue.InvalidParameterTypeError;
import pl.edu.mimuw.nesc.problem.issue.InvalidPostTaskExprError;
import pl.edu.mimuw.nesc.problem.issue.UndeclaredIdentifierError;
import pl.edu.mimuw.nesc.type.FunctionType;
import pl.edu.mimuw.nesc.type.IntType;
import pl.edu.mimuw.nesc.type.Type;
import pl.edu.mimuw.nesc.type.UnsignedCharType;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class FullExpressionsAnalysis extends ExpressionsAnalysis {
    /**
     * Logger for this class.
     */
    private static final Logger LOG = Logger.getLogger(FullExpressionsAnalysis.class);

    /**
     * Environment of the expression that is analyzed.
     */
    private final Environment environment;

    /**
     * Analyze the given expression and report all detected errors to the given
     * error helper. This method shall not be called if
     * {@link pl.edu.mimuw.nesc.astutil.AstUtils#IS_INITIALIZER IS_INITIALIZER}
     * predicate is fulfilled for the expression.
     *
     * @param expr Expression to be analyzed.
     * @param environment Environment of the expression.
     * @param errorHelper Object that will be notified about detected problems.
     * @return Data about the given expression. The object is present if and
     *         only if the expression is valid, otherwise it is absent.
     */
    public static Optional<ExprData> analyze(Expression expr, Environment environment,
            ErrorHelper errorHelper) {
        checkNotNull(expr, "expression cannot be null");
        checkNotNull(environment, "environment cannot be null");
        checkNotNull(errorHelper, "error helper cannot be null");

        final ExpressionsAnalysis analysisVisitor = new FullExpressionsAnalysis(environment, errorHelper);
        return expr.accept(analysisVisitor, null);
    }

    /**
     * Private constructor to prevent this class from being instantiated.
     *
     * @param environment Environment of the analyzed expression.
     * @param errorHelper Object that will be notified about detected problems.
     */
    private FullExpressionsAnalysis(Environment environment, ErrorHelper errorHelper) {
        super(errorHelper);
        this.environment = environment;
    }

    @Override
    public Optional<ExprData> visitComponentDeref(ComponentDeref expr, Void arg) {
        touch(expr);

        final Identifier componentRefName = (Identifier) expr.getArgument();
        final Optional<? extends ObjectDeclaration> optDeclaration =
                environment.getObjects().get(componentRefName.getName());
        final Optional<? extends ErroneousIssue> error;

        componentRefName.setRefsDeclInThisNescEntity(false);
        componentRefName.setIsGenericReference(false);

        if (!optDeclaration.isPresent()) {
            error = Optional.of(new UndeclaredIdentifierError(componentRefName.getName()));
        } else if (optDeclaration.get().getKind() != ObjectKind.COMPONENT) {
            error = Optional.of(InvalidComponentDerefExprError.invalidObjectKind(componentRefName.getName()));
        } else {
            final ComponentRefDeclaration refDeclaration = (ComponentRefDeclaration) optDeclaration.get();
            final ComponentRefFacade facade = refDeclaration.getFacade();

            if (!facade.goodComponentRef()) {
                // error with the component reference or the component definition itself
                error = Optional.absent();
            } else {
                final Optional<EnumerationConstant> constant =
                        facade.getEnumerationConstant(expr.getFieldName().getName());

                if (!constant.isPresent()) {
                    error = Optional.of(InvalidComponentDerefExprError.nonexistentConstant(componentRefName.getName(),
                            expr.getFieldName().getName()));
                } else {
                    componentRefName.setUniqueName(Optional.of(constant.get().getUniqueName()));
                    componentRefName.setType(Optional.<Type>absent());
                    error = Optional.absent();
                }
            }
        }

        if (error.isPresent()) {
            errorHelper.error(expr.getLocation(), expr.getEndLocation(), error.get());
            return Optional.absent();
        }

        final ExprData result = ExprData.builder()
                .type(new IntType())
                .isLvalue(false)
                .isBitField(false)
                .isNullPointerConstant(false)
                .build()
                .spread(expr);

        return Optional.of(result);
    }

    @Override
    public Optional<ExprData> visitIdentifier(Identifier expr, Void arg) {
        touch(expr);

        final Optional<? extends ObjectDeclaration> objDecl =
                environment.getObjects().get(expr.getName());

        // Emit error if the identifier is undeclared
        if (!objDecl.isPresent()) {
            errorHelper.error(expr.getLocation(), expr.getEndLocation(),
                    new UndeclaredIdentifierError(expr.getName()));
            return Optional.absent();
        }

        // Get necessary information from the declaration object
        final ObjectDeclaration pureObjDecl = objDecl.get();
        final ObjectReferenceVisitor dataVisitor = new ObjectReferenceVisitor();
        pureObjDecl.accept(dataVisitor, null);

        expr.setDeclaration(pureObjDecl);
        expr.setIsGenericReference(dataVisitor.isGenericReference);
        expr.setUniqueName(dataVisitor.uniqueName);
        expr.setRefsDeclInThisNescEntity(environment.isObjectDeclaredInsideNescEntity(expr.getName()));

        // Emit error if an invalid type of object is referred
        if (!dataVisitor.entityCorrect) {
            errorHelper.error(expr.getLocation(), expr.getEndLocation(),
                    new InvalidIdentifierUsageError(expr.getName()));
            return Optional.absent();
        }

        if (dataVisitor.type.isPresent()) {
            final ExprData result = ExprData.builder()
                    .type(dataVisitor.type.get())
                    .isLvalue(dataVisitor.isLvalue)
                    .isBitField(false)
                    .isNullPointerConstant(false)
                    .build()
                    .spread(expr);
            return Optional.of(result);
        } else {
            return Optional.absent();
        }
    }

    @Override
    protected Optional<ExprData> analyzePostTask(FunctionCall expr) {
        final Optional<InvalidPostTaskExprError.PostProblemKind> problemKind;

        if (!expr.getArguments().isEmpty()) {
            problemKind = Optional.of(InvalidPostTaskExprError.PostProblemKind.PARAMETERS_GIVEN);
        } else if (!(expr.getFunction() instanceof Identifier)) {
            problemKind = Optional.of(InvalidPostTaskExprError.PostProblemKind.IDENTIFER_NOT_PROVIDED);
        } else {
            final Identifier identExpr = (Identifier) expr.getFunction();
            final String taskName = identExpr.getName();
            final Optional<? extends ObjectDeclaration> oDeclData =
                    environment.getObjects().get(taskName);

            // Check if the task has been declared
            if (!oDeclData.isPresent()) {
                errorHelper.error(expr.getFunction().getLocation(), expr.getFunction().getEndLocation(),
                        new UndeclaredIdentifierError(taskName));
                return Optional.absent();
            }

            final ObjectDeclaration declData = oDeclData.get();
            identExpr.setDeclaration(declData);
            identExpr.setIsGenericReference(false);
            identExpr.setRefsDeclInThisNescEntity(true);
            identExpr.setType(Optional.<Type>absent());

            // Check if a task is being posted
            if (declData.getKind() != ObjectKind.FUNCTION) {
                problemKind = Optional.of(InvalidPostTaskExprError.PostProblemKind.INVALID_OBJECT_REFERENCED);
            } else {
                final FunctionDeclaration funData = (FunctionDeclaration) declData;

                if (funData.getFunctionType() != FunctionDeclaration.FunctionType.TASK) {
                    problemKind = Optional.of(InvalidPostTaskExprError.PostProblemKind.INVALID_OBJECT_REFERENCED);
                } else if (funData.getType().isPresent()
                        && !funData.getType().get().isCompatibleWith(Declarations.TYPE_TASK)) {
                    problemKind = Optional.of(InvalidPostTaskExprError.PostProblemKind.INVALID_TASK_TYPE);
                } else {
                    identExpr.setUniqueName(Optional.of(funData.getUniqueName()));
                    problemKind = Optional.absent();
                }
            }
        }

        if (problemKind.isPresent()) {
            final ErroneousIssue error = new InvalidPostTaskExprError(problemKind.get(),
                    expr.getFunction(), expr.getArguments().size());
            errorHelper.error(expr.getLocation(), expr.getEndLocation(), error);
            return Optional.absent();
        }

        final ExprData result = ExprData.builder()
                .type(new UnsignedCharType())
                .isLvalue(false)
                .isBitField(false)
                .isNullPointerConstant(false)
                .build()
                .spread(expr);

        return Optional.of(result);
    }

    @Override
    protected Optional<ExprData> analyzeNescCall(FunctionCall expr, boolean isSignal) {
        final NescCallAnalyzer analyzer = new NescCallAnalyzer(expr, isSignal);
        return analyzer.analyze();
    }

    /**
     * An object that is responsible for analyzing NesC call and signal
     * expressions.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class NescCallAnalyzer {

        private final FunctionCall callExpr;
        private final boolean isSignal;
        private boolean anotherProblem = false;
        private Optional<InvalidNescCallError> problem = Optional.absent();

        /**
         * These variables are set after successful completion of method
         * {@link NescCallAnalyzer#decompose}.
         */
        private Optional<Identifier> identifier;
        private Optional<String> methodName;
        private Optional<LinkedList<Expression>> instanceParams;
        private LinkedList<Expression> normalParams;

        /**
         * Variables with results of analysis of parameters expressions. They
         * are set after successful completion of method
         * {@link NescCallAnalyzer#analyzeSubexpressions}.
         */
        private Optional<ImmutableList<Optional<ExprData>>> instanceParamsData;
        private ImmutableList<Optional<ExprData>> normalParamsData;

        /**
         * Variables with data about the called command or signaled event
         * retrieved from the symbol table. They are set after successful
         * completion of method {@link NescCallAnalyzer#analyzeCallee}.
         */
        private boolean isEvent;
        private Type returnType;
        private ImmutableList<Optional<Type>> normalParamsExpectedTypes;
        private Optional<ImmutableList<Optional<Type>>> instanceParamsExpectedTypes;

        private NescCallAnalyzer(FunctionCall callExpr, boolean isSignal) {
            this.callExpr = callExpr;
            this.isSignal = isSignal;
        }

        /**
         * Performs all operations related to the analysis of a NesC call
         * expression. It should be called instead of calling individual methods
         * of this class.
         *
         * @return Result of the analysis.
         */
        private Optional<ExprData> analyze() {
            decompose();
            updateIdentifierState();
            analyzeSubexpressions();
            analyzeCallee();
            analyzeParametersPresence();
            analyzeParametersTypes();
            analyzeCallKind();

            return finish();
        }

        /**
         * Extract all important elements of the call expression.
         */
        private void decompose() {
            Expression nextExpr = callExpr.getFunction();

            // 1. Function call
            normalParams = callExpr.getArguments();

            // 2. Generic call
            if (nextExpr instanceof GenericCall) {
                final GenericCall genericCall = (GenericCall) nextExpr;
                genericCall.setType(Optional.<Type>absent());
                instanceParams = Optional.of(genericCall.getArguments());
                nextExpr = genericCall.getName();
            } else {
                instanceParams = Optional.absent();
            }

            // 3. Interface dereference
            if (nextExpr instanceof InterfaceDeref) {
                final InterfaceDeref ifaceDeref = (InterfaceDeref) nextExpr;
                ifaceDeref.setType(Optional.<Type>absent());
                methodName = Optional.of(ifaceDeref.getMethodName().getName());
                nextExpr = ifaceDeref.getArgument();
            } else {
                methodName = Optional.absent();
            }

            // 4. Identifier
            identifier = nextExpr instanceof Identifier
                    ? Optional.of((Identifier) nextExpr)
                    : Optional.<Identifier>absent();
        }

        /**
         * Updates necessary data in the decomposed identifier object.
         */
        private void updateIdentifierState() {
            if (identifier.isPresent()) {
                final Identifier ident = identifier.get();
                ident.setIsGenericReference(false);
                ident.setUniqueName(Optional.<String>absent());
                ident.setRefsDeclInThisNescEntity(false);
                ident.setType(Optional.<Type>absent());
            }
        }

        /**
         * Performs analysis for expressions for both kinds of parameters:
         * instance and normal.
         */
        private void analyzeSubexpressions() {
            instanceParamsData = instanceParams.isPresent()
                    ? Optional.of(FullExpressionsAnalysis.this.analyzeSubexpressions(instanceParams.get(), true))
                    : Optional.<ImmutableList<Optional<ExprData>>>absent();
            normalParamsData = FullExpressionsAnalysis.this.analyzeSubexpressions(normalParams, true);
        }

        /**
         * Checks the correctness of the callee and extracts its return type and
         * expected types of parameters.
         */
        private void analyzeCallee() {
            if (!identifier.isPresent()) {
                problem = Optional.of(InvalidNescCallError.invalidCallee(callExpr.getFunction()));
                return;
            }

            final Identifier ident = identifier.get();

            final Optional<? extends ObjectDeclaration> optDeclaration =
                    environment.getObjects().get(ident.getName());
            if (!optDeclaration.isPresent()) {
                anotherProblem = true;
                errorHelper.error(ident.getLocation(), ident.getEndLocation(),
                        new UndeclaredIdentifierError(ident.getName()));
                return;
            }

            final ObjectDeclaration declaration = optDeclaration.get();
            ident.setDeclaration(declaration);

            if (declaration.getKind() == ObjectKind.INTERFACE) {
                analyzeInterfaceCallee((InterfaceRefDeclaration) declaration);
            } else if (declaration.getKind() == ObjectKind.FUNCTION) {
                analyzeBareCallee((FunctionDeclaration) declaration);
            } else {
                problem = Optional.of(InvalidNescCallError.invalidCallee(callExpr.getFunction()));
            }
        }

        private void analyzeInterfaceCallee(InterfaceRefDeclaration ifaceRef) {
            final InterfaceRefFacade ifaceFacade = ifaceRef.getFacade();

            if (!methodName.isPresent()) {
                problem = Optional.of(InvalidNescCallError.invalidCallee(callExpr.getFunction()));
                return;
            } else if (!ifaceFacade.goodInterfaceRef()) {
                anotherProblem = true;
                if (LOG.isDebugEnabled()) {
                    LOG.debug(format("Finish analysis of NesC call '%s' because the interface reference is not good",
                            ASTWriter.writeToString(callExpr)));
                }
                return;
            }

            final Optional<InterfaceEntity> ifaceEntity = ifaceFacade.get(methodName.get());

            if (!ifaceEntity.isPresent()) {
                problem = Optional.of(InvalidNescCallError.nonexistentInterfaceEntity(methodName.get(),
                        ifaceFacade.getInstanceName(), ifaceFacade.getInterfaceName()));
                return;
            } else if (!ifaceEntity.get().getType().isPresent()) {
                anotherProblem = true;
                return;
            }

            this.isEvent = ifaceEntity.get().getKind() == InterfaceEntity.Kind.EVENT;
            this.returnType = ifaceEntity.get().getType().get().getReturnType();
            this.normalParamsExpectedTypes = ifaceEntity.get().getType().get().getArgumentsTypes();
            this.instanceParamsExpectedTypes = ifaceFacade.getInstanceParameters();
        }

        private void analyzeBareCallee(FunctionDeclaration funDecl) {
            final boolean isEvent;

            switch (funDecl.getFunctionType()) {
                case COMMAND:
                    isEvent = false;
                    break;
                case EVENT:
                    isEvent = true;
                    break;
                default:
                    problem = Optional.of(InvalidNescCallError.invalidCallee(callExpr.getFunction()));
                    return;
            }

            if (methodName.isPresent()) {
                problem = Optional.of(InvalidNescCallError.invalidCallee(callExpr.getFunction()));
                return;
            }

            if (!funDecl.getType().isPresent()) {
                anotherProblem = true;
                if (LOG.isDebugEnabled()) {
                    LOG.debug(format("Finish analysis of NesC call '%s' because the type is absent in the function declaration object",
                            ASTWriter.writeToString(callExpr)));
                }
                return;
            }

            final FunctionType funType = (FunctionType) funDecl.getType().get();

            this.isEvent = isEvent;
            this.returnType = funType.getReturnType();
            this.normalParamsExpectedTypes = funType.getArgumentsTypes();
            this.instanceParamsExpectedTypes = funDecl.getInstanceParameters();
        }

        private void analyzeParametersPresence() {
            if (!canContinue()) {
                return;
            }

            // Presence and count of instance parameters
            if (instanceParamsExpectedTypes.isPresent() && !instanceParams.isPresent()) {
                problem = Optional.of(InvalidNescCallError.missingInstanceParameters(isEvent,
                        callExpr.getFunction()));
                return;
            } else if (!instanceParamsExpectedTypes.isPresent() && instanceParams.isPresent()) {
                problem = Optional.of(InvalidNescCallError.unexpectedInstanceParameters(identifier.get().getName(),
                        methodName));
                return;
            } else if (instanceParamsExpectedTypes.isPresent()
                    && instanceParamsExpectedTypes.get().size() != instanceParams.get().size()) {

                final String parameterisedEntity = methodName.isPresent()
                        ? identifier.get().getName() + "." + methodName.get()
                        : identifier.get().getName();

                problem = Optional.of(InvalidNescCallError.invalidInstanceParametersCount(isEvent,
                        parameterisedEntity, instanceParamsExpectedTypes.get().size(),
                        instanceParams.get().size()));
                return;
            }

            // Count of normal parameters
            if (normalParamsExpectedTypes.size() != normalParams.size()) {
                problem = Optional.of(InvalidNescCallError.invalidNormalParametersCount(isEvent,
                        callExpr.getFunction(), normalParamsExpectedTypes.size(),
                        normalParams.size()));
            }
        }

        private void analyzeParametersTypes() {
            if (!canContinue()) {
                return;
            }

            // Types of instance parameters

            final InvalidParameterTypeError.FunctionKind funKind = isEvent
                    ? InvalidParameterTypeError.FunctionKind.EVENT
                    : InvalidParameterTypeError.FunctionKind.COMMAND;

            if (instanceParams.isPresent()) {
                FullExpressionsAnalysis.this.checkParametersTypes(
                        callExpr.getFunction(),
                        instanceParamsExpectedTypes.get().iterator(),
                        instanceParamsData.get().iterator(),
                        instanceParams.get().iterator(),
                        funKind,
                        InvalidParameterTypeError.ParameterKind.INSTANCE_PARAMETER
                );
            }

            // Types of normal parameters

            FullExpressionsAnalysis.this.checkParametersTypes(
                    callExpr.getFunction(),
                    normalParamsExpectedTypes.iterator(),
                    normalParamsData.iterator(),
                    normalParams.iterator(),
                    funKind,
                    InvalidParameterTypeError.ParameterKind.NORMAL_PARAMETER
            );
        }

        private void analyzeCallKind() {
            if (!canContinue()) {
                return;
            }

            if (isEvent && !isSignal || !isEvent && isSignal) {
                problem = Optional.of(InvalidNescCallError.invalidCallKind(isEvent, callExpr.getFunction()));
            }
        }

        /**
         * Report the detected error (if any) and prepare the result of the
         * whole analysis.
         *
         * @return Object that represents the result of the whole analysis.
         */
        private Optional<ExprData> finish() {
            // Report the detected error
            if (problem.isPresent()) {

                FullExpressionsAnalysis.this.errorHelper.error(callExpr.getLocation(),
                        callExpr.getEndLocation(), problem.get());
                return Optional.absent();

            } else if (!canContinue()) {
                return Optional.absent();
            }

            // Prepare and return the final result

            final ExprData result = ExprData.builder()
                    .type(returnType)
                    .isLvalue(false)
                    .isBitField(false)
                    .isNullPointerConstant(false)
                    .build()
                    .spread(callExpr);

            return Optional.of(result);
        }

        private boolean canContinue() {
            return !anotherProblem && !problem.isPresent();
        }
    }

    /**
     * <p>Visitor for analysis and retrieving data about used identifiers.
     * A visitor contains proper data about an object after visiting its
     * declaration object.</p>
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class ObjectReferenceVisitor implements ObjectDeclaration.Visitor<Void, Void> {
        private boolean entityCorrect;
        private boolean isLvalue;
        private Optional<Type> type;
        private boolean isGenericReference;
        private Optional<String> uniqueName;

        @Override
        public Void visit(VariableDeclaration declaration, Void arg) {
            this.entityCorrect = true;
            this.isLvalue = !declaration.isGenericParameter();
            this.type = declaration.getType();
            this.isGenericReference = declaration.isGenericParameter();
            this.uniqueName = Optional.of(declaration.getUniqueName());

            return null;
        }

        @Override
        public Void visit(FunctionDeclaration declaration, Void arg) {
            final FunctionDeclaration.FunctionType funKind = declaration.getFunctionType();

            this.entityCorrect = funKind != FunctionDeclaration.FunctionType.COMMAND
                    && funKind != FunctionDeclaration.FunctionType.EVENT
                    && funKind != FunctionDeclaration.FunctionType.TASK;
            this.isLvalue = false;
            this.type = declaration.getType();
            this.isGenericReference = false;
            this.uniqueName = this.entityCorrect
                    ? Optional.of(declaration.getUniqueName())
                    : null;

            return null;
        }

        @Override
        public Void visit(ConstantDeclaration declaration, Void arg) {
            this.entityCorrect = true;
            this.isLvalue = false;
            this.type = declaration.getType();
            this.isGenericReference = false;
            this.uniqueName = Optional.of(declaration.getUniqueName());

            return null;
        }

        @Override
        public Void visit(InterfaceRefDeclaration declaration, Void arg) {
            setIllegalReferenceValues();
            return null;
        }

        @Override
        public Void visit(ComponentRefDeclaration declaration, Void arg) {
            setIllegalReferenceValues();
            return null;
        }

        @Override
        public Void visit(TypenameDeclaration declaration, Void arg) {
            setIllegalReferenceValues();
            return null;
        }

        private void setIllegalReferenceValues() {
            this.entityCorrect = false;
            this.isLvalue = false;
            this.type = Optional.absent();
            this.isGenericReference = false;
            this.uniqueName = null;
        }
    }
}
