package pl.edu.mimuw.nesc.basicreduce;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import pl.edu.mimuw.nesc.ast.CallDirection;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.NescCallKind;
import pl.edu.mimuw.nesc.ast.RID;
import pl.edu.mimuw.nesc.ast.TagRefSemantics;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.ast.util.AstUtils;
import pl.edu.mimuw.nesc.ast.util.DeclaratorUtils;
import pl.edu.mimuw.nesc.common.AtomicSpecification;
import pl.edu.mimuw.nesc.common.SchedulerSpecification;
import pl.edu.mimuw.nesc.common.util.VariousUtils;
import pl.edu.mimuw.nesc.common.util.list.Lists;
import pl.edu.mimuw.nesc.declaration.nesc.ComponentDeclaration;
import pl.edu.mimuw.nesc.declaration.nesc.ModuleDeclaration;
import pl.edu.mimuw.nesc.declaration.object.unique.UniqueDeclaration;
import pl.edu.mimuw.nesc.facade.component.specification.ModuleTable;
import pl.edu.mimuw.nesc.facade.component.specification.TaskElement;
import pl.edu.mimuw.nesc.names.collecting.ObjectEnvironmentNameCollector;
import pl.edu.mimuw.nesc.names.mangling.CountingNameMangler;
import pl.edu.mimuw.nesc.names.mangling.NameMangler;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * <p>A visitor for traversing AST nodes that is responsible for performing
 * basic reduce actions.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 * @see BasicReduceExecutor
 */
final class BasicReduceVisitor extends IdentityVisitor<BlockData> {
    /**
     * Name that will be mangled to create unique names for variables that
     * will be used to store the result of the atomic start function.
     */
    private static final String ATOMIC_VARIABLE_BASE_NAME = "atomic_data";

    /**
     * Name used for variables that are assigned the returned value before
     * ending the atomic block.
     */
    private static final String ATOMIC_RETURN_VARIABLE_BASE_NAME = "atomic_ret";

    /**
     * Set with all not mangled names that are to be located in the global scope
     * at the end of compiling.
     */
    private final ImmutableSet<String> globalNames;

    /**
     * Keys in the map are unique names and values are their final names in the
     * end of the compiling process. Initially this is mapping of unique names
     * of global entities to their original names.
     */
    private final Map<String, String> uniqueNamesMap;

    /**
     * Name mangler used for generating new names if a global name (before
     * mangling) is the same as an unique name. It shall be the same object used
     * for performing the mangling in the traversed nodes.
     */
    private final NameMangler mangler;

    /**
     * The specification of the scheduler that will be used for tasks.
     */
    private final SchedulerSpecification schedulerSpecification;

    /**
     * The created configuration that wires task interface references from
     * non-generic modules with the scheduler.
     */
    private final Configuration taskWiringConfiguration;

    /**
     * Information necessary to transform 'atomic' statements.
     */
    private final AtomicSpecification atomicSpecification;

    /**
     * Unique names used for transformation of 'atomic' statements.
     */
    private final String atomicTypeUniqueName;
    private final String atomicStartFunUniqueName;
    private final String atomicEndFunUniqueName;

    /**
     * Visitor that modifies visited type elements of the return type of
     * a function for new variable that has assigned a returned expression
     * in an atomic block.
     */
    private final AtomicReturnTypeVisitor atomicReturnTypeVisitor = new AtomicReturnTypeVisitor();

    /**
     * Visitor that is used for detecting statements that may need a change
     * because of <code>atomic</code> usage.
     */
    private final NullVisitor<LinkedList<Statement>, BlockData> atomicTransformGateway = new NullVisitor<LinkedList<Statement>, BlockData>() {
        @Override
        public LinkedList<Statement> visitAtomicStmt(AtomicStmt atomicStmt, BlockData atomicStmtData) {
            return atomizeAtomicStmt(atomicStmt, atomicStmtData);
        }

        @Override
        public LinkedList<Statement> visitReturnStmt(ReturnStmt retStmt, BlockData retStmtData) {
            return atomizeReturnStmt(retStmt, retStmtData);
        }

        @Override
        public LinkedList<Statement> visitContinueStmt(ContinueStmt contStmt, BlockData contStmtData) {
            return atomizeContinueStmt(contStmt, contStmtData);
        }

        @Override
        public LinkedList<Statement> visitBreakStmt(BreakStmt breakStmt, BlockData breakStmtData) {
            return atomizeBreakStmt(breakStmt, breakStmtData);
        }
    };

    /**
     * Get a new builder that will build a basic reduce visitor.
     *
     * @return Newly created builder that will build a basic reduce visitor.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Initializes this visitor with information provided by the builder.
     *
     * @param builder Builder with information necessary to initialize this
     *                visitor.
     */
    private BasicReduceVisitor(Builder builder, String atomicTypeUniqueName,
                String atomicStartFunUniqueName, String atomicEndFunUniqueName) {

        this.globalNames = builder.buildGlobalNames();
        this.uniqueNamesMap = builder.buildUniqueNamesMap(atomicTypeUniqueName,
                atomicStartFunUniqueName, atomicEndFunUniqueName);
        this.mangler = builder.mangler;
        this.schedulerSpecification = builder.schedulerSpecification;
        this.taskWiringConfiguration = builder.buildInitialTaskWiringConfiguration();
        this.atomicSpecification = builder.atomicSpecification;
        this.atomicTypeUniqueName = atomicTypeUniqueName;
        this.atomicStartFunUniqueName = atomicStartFunUniqueName;
        this.atomicEndFunUniqueName = atomicEndFunUniqueName;
    }

    /**
     * Get the configuration created for wiring task interface references in
     * non-generic modules.
     *
     * @return Configuration with connections that wire task interface
     *         references from non-generic modules with the scheduler
     *         component. The object is absent if the configuration is not
     *         needed, i.e. no non-generic modules with tasks have been
     *         visited.
     */
    public Optional<Configuration> getTaskWiringConfiguration() {
        final ConfigurationImpl confImpl = (ConfigurationImpl) taskWiringConfiguration.getImplementation();

        return confImpl.getDeclarations().size() > 1
                ? Optional.of(taskWiringConfiguration)
                : Optional.<Configuration>absent();
    }

    @Override
    public BlockData visitEnumerator(Enumerator node, BlockData arg) {
        node.setUniqueName(getFinalUniqueName(node.getUniqueName()));
        return arg;
    }

    @Override
    public BlockData visitTypename(Typename node, BlockData arg) {
        node.setUniqueName(getFinalUniqueName(node.getUniqueName()));
        return arg;
    }

    @Override
    public BlockData visitComponentTyperef(ComponentTyperef node, BlockData arg) {
        node.setUniqueName(getFinalUniqueName(node.getUniqueName()));
        return arg;
    }

    @Override
    public BlockData visitAttributeRef(AttributeRef node, BlockData arg) {
        node.setUniqueName(getFinalUniqueName(node.getUniqueName()));
        return arg;
    }

    @Override
    public BlockData visitStructRef(StructRef node, BlockData arg) {
        node.setUniqueName(getFinalUniqueName(node.getUniqueName()));
        return arg;
    }

    @Override
    public BlockData visitNxStructRef(NxStructRef node, BlockData arg) {
        node.setUniqueName(getFinalUniqueName(node.getUniqueName()));
        return arg;
    }

    @Override
    public BlockData visitUnionRef(UnionRef node, BlockData arg) {
        node.setUniqueName(getFinalUniqueName(node.getUniqueName()));
        return arg;
    }

    @Override
    public BlockData visitNxUnionRef(NxUnionRef node, BlockData arg) {
        node.setUniqueName(getFinalUniqueName(node.getUniqueName()));
        return arg;
    }

    @Override
    public BlockData visitEnumRef(EnumRef node, BlockData arg) {
        node.setUniqueName(getFinalUniqueName(node.getUniqueName()));
        return arg;
    }

    @Override
    public BlockData visitIdentifierDeclarator(IdentifierDeclarator node, BlockData arg) {
        node.setUniqueName(getFinalUniqueName(node.getUniqueName()));
        return arg;
    }

    @Override
    public BlockData visitIdentifier(Identifier node, BlockData arg) {
        node.setUniqueName(getFinalUniqueName(node.getUniqueName()));
        return arg;
    }

    @Override
    public BlockData visitTypeParmDecl(TypeParmDecl node, BlockData arg) {
        node.setUniqueName(getFinalUniqueName(node.getUniqueName()));
        return arg;
    }

    private String getFinalUniqueName(String currentUniqueName) {
        final Optional<String> optFinalUniqueName = Optional.fromNullable(uniqueNamesMap.get(currentUniqueName));

        if (optFinalUniqueName.isPresent()) {
            /* Remove the mangling of the name that will be global or update
               a remangled name. */
            return optFinalUniqueName.get();
        } else if (globalNames.contains(currentUniqueName)) {
            // Change the unique name that has been mangled to a global name
            final String finalUniqueName = mangler.remangle(currentUniqueName);
            uniqueNamesMap.put(currentUniqueName, finalUniqueName);
            return finalUniqueName;
        }

        // The unique name stays the same
        return currentUniqueName;
    }

    private Optional<String> getFinalUniqueName(Optional<String> currentUniqueName) {
        return currentUniqueName.isPresent()
                ? Optional.of(getFinalUniqueName(currentUniqueName.get()))
                : currentUniqueName;
    }

    @Override
    public BlockData visitModule(Module node, BlockData arg) {
        createTaskInterfaceRefs(node);
        wireTaskInterfaceRefs(node);

        return BlockData.builder(arg)
                .moduleTable(node.getDeclaration().getModuleTable())
                .build();
    }

    private void createTaskInterfaceRefs(Module module) {
        /* Collect all names from the module specification and implementation
           to avoid name conflicts and create the name mangler. */

        final ObjectEnvironmentNameCollector nameCollector = new ObjectEnvironmentNameCollector();
        nameCollector.collect(module.getParameterEnvironment());
        nameCollector.collect(module.getSpecificationEnvironment());
        nameCollector.collect(module.getImplementation().getEnvironment());
        final CountingNameMangler nameMangler = new CountingNameMangler(nameCollector.get());

        // Create the task interface references

        final LinkedList<Declaration> specificationDeclarations = module.getDeclarations();
        final Map<String, TaskElement> tasks = module.getDeclaration().getModuleTable().getTasks();

        for (TaskElement taskElement : tasks.values()) {
            taskElement.setInterfaceRefName(nameMangler.mangle(schedulerSpecification.getTaskInterfaceName()));
            specificationDeclarations.add(createTaskInterfaceRef(taskElement.getInterfaceRefName().get()));
        }
    }

    private RequiresInterface createTaskInterfaceRef(String interfaceRefName) {
        final InterfaceRef interfaceRef = new InterfaceRef(
                Location.getDummyLocation(),
                new Word(Location.getDummyLocation(), schedulerSpecification.getTaskInterfaceName()),
                Optional.<LinkedList<Expression>>absent()
        );

        interfaceRef.setAlias(Optional.of(new Word(Location.getDummyLocation(), interfaceRefName)));
        interfaceRef.setGenericParameters(Optional.<LinkedList<Declaration>>absent());
        interfaceRef.setAttributes(new LinkedList<Attribute>());

        return new RequiresInterface(
                Location.getDummyLocation(),
                Lists.<Declaration>newList(interfaceRef)
        );
    }

    private void wireTaskInterfaceRefs(Module module) {
        final Map<String, TaskElement> tasks = module.getDeclaration().getModuleTable().getTasks();

        if (module.getIsAbstract() || tasks.isEmpty()) {
            return;
        }

        final LinkedList<Declaration> configurationImplDecls =
                ((ConfigurationImpl) taskWiringConfiguration.getImplementation()).getDeclarations();
        final String moduleName = module.getName().getName();

        // Add the usage of the component if it is not the scheduler itself
        if (!moduleName.equals(schedulerSpecification.getComponentName())) {
            final ComponentRef moduleRef = new ComponentRef(
                    Location.getDummyLocation(),
                    new Word(Location.getDummyLocation(), moduleName),
                    false,
                    new LinkedList<Expression>()
            );
            moduleRef.setAlias(Optional.<Word>absent());

            final ComponentsUses newModuleUsage = new ComponentsUses(
                    Location.getDummyLocation(),
                    Lists.newList(moduleRef)
            );

            configurationImplDecls.add(newModuleUsage);
        }

        // Add connections
        for (TaskElement taskElement : tasks.values()) {
            configurationImplDecls.add(createTaskConnection(taskElement, moduleName,
                    schedulerSpecification.getComponentName()));
        }
    }

    private RpConnection createTaskConnection(TaskElement taskElement, String moduleRefName,
            String schedulerComponentRefName) {
        // Create module endpoint

        final LinkedList<ParameterisedIdentifier> moduleEndpointIds = new LinkedList<>();
        moduleEndpointIds.add(new ParameterisedIdentifier(
                Location.getDummyLocation(),
                new Word(Location.getDummyLocation(), moduleRefName),
                new LinkedList<Expression>()
        ));
        moduleEndpointIds.add(new ParameterisedIdentifier(
                Location.getDummyLocation(),
                new Word(Location.getDummyLocation(), taskElement.getInterfaceRefName().get()),
                new LinkedList<Expression>()
        ));
        final EndPoint moduleEndpoint = new EndPoint(Location.getDummyLocation(), moduleEndpointIds);
        moduleEndpoint.setImplicitIdentifier(Optional.<String>absent());

        // Create scheduler endpoint

        final Identifier uniqueIdentifier = new Identifier(Location.getDummyLocation(),
                UniqueDeclaration.getInstance().getFunctionName());
        uniqueIdentifier.setIsGenericReference(false);
        uniqueIdentifier.setUniqueName(Optional.of(UniqueDeclaration.getInstance().getUniqueName()));
        uniqueIdentifier.setRefsDeclInThisNescEntity(false);
        uniqueIdentifier.setDeclaration(UniqueDeclaration.getInstance());

        final LinkedList<Expression> uniqueArgs = new LinkedList<>();
        uniqueArgs.add(new StringAst(
                Location.getDummyLocation(),
                Lists.newList(new StringCst(Location.getDummyLocation(), schedulerSpecification.getUniqueIdentifier()))
        ));
        final UniqueCall ifaceUniqueCall = new UniqueCall(
                Location.getDummyLocation(),
                uniqueArgs,
                NescCallKind.NORMAL_CALL,
                uniqueIdentifier
        );
        final LinkedList<ParameterisedIdentifier> schedulerEndpointIds = new LinkedList<>();
        schedulerEndpointIds.add(new ParameterisedIdentifier(
                Location.getDummyLocation(),
                new Word(Location.getDummyLocation(), schedulerComponentRefName),
                new LinkedList<Expression>()
        ));
        schedulerEndpointIds.add(new ParameterisedIdentifier(
                Location.getDummyLocation(),
                new Word(Location.getDummyLocation(), schedulerSpecification.getInterfaceNameInScheduler()),
                Lists.<Expression>newList(ifaceUniqueCall)
        ));
        final EndPoint schedulerEndpoint = new EndPoint(Location.getDummyLocation(), schedulerEndpointIds);

        // Make the connection

        final RpConnection connection = new RpConnection(
                Location.getDummyLocation(),
                moduleEndpoint,
                schedulerEndpoint
        );
        connection.setCallDirection(CallDirection.FROM_1_TO_2);

        return connection;
    }

    @Override
    public BlockData visitFunctionCall(FunctionCall funCall, BlockData arg) {
        if (funCall.getCallKind() != NescCallKind.POST_TASK) {
            return arg;
        }

        final Identifier taskIdentifier = (Identifier) funCall.getFunction();
        final TaskElement taskElement = arg.getModuleTable().get().getTasks().get(taskIdentifier.getName());

        funCall.setCallKind(NescCallKind.COMMAND_CALL);
        taskIdentifier.setName(taskElement.getInterfaceRefName().get());
        taskIdentifier.setRefsDeclInThisNescEntity(false);
        taskIdentifier.setIsGenericReference(false);
        taskIdentifier.setUniqueName(Optional.<String>absent());

        final InterfaceDeref ifaceDeref = new InterfaceDeref(
                Location.getDummyLocation(),
                taskIdentifier,
                new Word(Location.getDummyLocation(), schedulerSpecification.getTaskPostCommandName())
        );
        funCall.setFunction(ifaceDeref);

        return arg;
    }

    @Override
    public BlockData visitFunctionDecl(FunctionDecl funDecl, BlockData arg) {
        final BlockData returnData = BlockData.builder(arg)
                .functionReturnType(AstUtils.extractReturnType(funDecl))
                .build();

        if (!taskKeywordToEventKeyword(funDecl.getModifiers())) {
            return returnData;
        }

        // Change the declarator

        final FunctionDeclarator funDeclarator = DeclaratorUtils.getFunctionDeclarator(funDecl.getDeclarator());
        final IdentifierDeclarator identDeclarator = (IdentifierDeclarator) funDeclarator.getDeclarator().get();
        final TaskElement taskElement = arg.getModuleTable().get().getTasks().get(identDeclarator.getName());

        final InterfaceRefDeclarator ifaceRefDeclarator = new InterfaceRefDeclarator(
                Location.getDummyLocation(),
                Optional.<Declarator>of(identDeclarator),
                new Word(Location.getDummyLocation(), taskElement.getInterfaceRefName().get())
        );

        identDeclarator.setName(schedulerSpecification.getTaskRunEventName());
        funDeclarator.setDeclarator(Optional.<Declarator>of(ifaceRefDeclarator));

        return returnData;
    }

    /**
     * Replace the <code>task</code> keyword in the given list of type elements
     * to <code>event</code>.
     *
     * @param typeElements List with type elements to change.
     * @return <code>true</code> if and only if the given list had contained the
     *         <code>task</code> keyword (and it has been changed to
     *         <code>event</code> keyword).
     */
    private boolean taskKeywordToEventKeyword(LinkedList<TypeElement> typeElements) {
        final Iterator<TypeElement> typeElemIt = typeElements.iterator();
        boolean isTask = false;

        while (typeElemIt.hasNext()) {
            final TypeElement typeElem = typeElemIt.next();
            if (!(typeElem instanceof Rid)) {
                continue;
            }

            final Rid rid = (Rid) typeElem;
            if (rid.getId() == RID.TASK) {
                if (isTask) {
                    typeElemIt.remove();
                } else {
                    isTask = true;
                    rid.setId(RID.EVENT);
                }
            }
        }

        return isTask;
    }

    @Override
    public BlockData visitConfigurationImpl(ConfigurationImpl confImpl, BlockData arg) {
        // Skip this configuration if it does not instantiate modules with tasks
        final LinkedList<ComponentRef> componentRefs = collectRefsToGenericModulesWithTasks(confImpl.getDeclarations());
        if (componentRefs.isEmpty()) {
            return arg;
        }

        // Add the usage of the scheduler to the configuration implementation
        final ComponentsUses schedulerRef = createSchedulerComponentRef(confImpl);
        final String schedulerRefName = schedulerRef.getComponents().getFirst().getAlias().get().getName();
        confImpl.getDeclarations().add(createSchedulerComponentRef(confImpl));

        /* Loop through the declarations and collect all necessary connections.
           It cannot be done while traversing the declarations because a collection
           cannot be modified while iterating over it. */

        final LinkedList<RpConnection> taskConnections = new LinkedList<>();

        for (ComponentRef componentRef : componentRefs) {
            wireTaskInterfaceRefs(componentRef, taskConnections, schedulerRefName);
        }

        confImpl.getDeclarations().addAll(taskConnections);

        return arg;
    }

    private LinkedList<ComponentRef> collectRefsToGenericModulesWithTasks(LinkedList<Declaration> declarations) {
        final LinkedList<ComponentRef> result = new LinkedList<>();

        for (Declaration declaration : declarations) {
            if (!(declaration instanceof ComponentsUses)) {
                continue;
            }

            final ComponentsUses componentsUses = (ComponentsUses) declaration;

            for (ComponentRef componentRef : componentsUses.getComponents()) {
                if (!(componentRef.getDeclaration().getComponentDeclaration().get() instanceof ModuleDeclaration)
                        || !componentRef.getIsAbstract()) {
                    continue;
                }

                final ModuleDeclaration moduleDeclaration = (ModuleDeclaration) componentRef.getDeclaration()
                        .getComponentDeclaration().get();

                if (!moduleDeclaration.getModuleTable().getTasks().isEmpty()) {
                    result.add(componentRef);
                }
            }
        }

        return result;
    }

    private ComponentsUses createSchedulerComponentRef(ConfigurationImpl confImpl) {
        final ObjectEnvironmentNameCollector nameCollector = new ObjectEnvironmentNameCollector();
        nameCollector.collect(confImpl.getEnvironment());
        final String schedulerComponentRefName = new CountingNameMangler(nameCollector.get())
                .mangle(schedulerSpecification.getComponentName());

        final ComponentRef schedulerRef = new ComponentRef(
                Location.getDummyLocation(),
                new Word(Location.getDummyLocation(), schedulerSpecification.getComponentName()),
                false,
                new LinkedList<Expression>()
        );
        schedulerRef.setAlias(Optional.of(new Word(Location.getDummyLocation(), schedulerComponentRefName)));

        return new ComponentsUses(
                Location.getDummyLocation(),
                Lists.newList(schedulerRef)
        );
    }

    private void wireTaskInterfaceRefs(ComponentRef componentRef, List<RpConnection> connections,
            String schedulerComponentRefName) {
        if (!componentRef.getIsAbstract()) {
            return;
        }

        final ComponentDeclaration declaration = componentRef.getDeclaration().getComponentDeclaration().get();
        if (!(declaration instanceof ModuleDeclaration)) {
            return;
        }
        final ModuleDeclaration moduleDeclaration = (ModuleDeclaration) declaration;
        final ModuleTable moduleTable = moduleDeclaration.getModuleTable();
        final String moduleRefName = componentRef.getDeclaration().getName();

        for (TaskElement taskElement : moduleTable.getTasks().values()) {
            connections.add(createTaskConnection(taskElement, moduleRefName, schedulerComponentRefName));
        }
    }

    @Override
    public BlockData visitCompoundStmt(CompoundStmt stmt, BlockData arg) {
        if (stmt.getAtomicVariableUniqueName() == null) {
            stmt.setAtomicVariableUniqueName(Optional.<String>absent());
        }

        checkState(!stmt.getAtomicVariableUniqueName().isPresent() || !arg.getAtomicVariableUniqueName().isPresent(),
                "block executed atomically inside such block encountered");

        final BlockData result = BlockData.builder(arg)
                .atomicVariableUniqueName(arg.getAtomicVariableUniqueName().or(stmt.getAtomicVariableUniqueName()).orNull())
                .insideBreakableAtomic(!arg.isInsideAtomicBlock() && arg.isInsideLoopOrSwitch()
                        && stmt.getAtomicVariableUniqueName().isPresent()
                        || arg.isInsideBreakableAtomic())
                .build();
        replaceTransformedStatements(stmt.getStatements(), result);

        return result;
    }

    @Override
    public BlockData visitIfStmt(IfStmt stmt, BlockData arg) {
        stmt.setTrueStatement(performAtomicSmallTransformationCompact(stmt.getTrueStatement(), arg));
        stmt.setFalseStatement(stmt.getFalseStatement().transform(new AtomicTransformFunction(arg)));
        return arg;
    }

    @Override
    public BlockData visitLabeledStmt(LabeledStmt stmt, BlockData arg) {
        stmt.setStatement(stmt.getStatement().transform(new AtomicTransformFunction(arg)));
        return arg;
    }

    @Override
    public BlockData visitWhileStmt(WhileStmt stmt, BlockData arg) {
        return conditionalStmtAtomicTransformation(stmt, arg);
    }

    @Override
    public BlockData visitDoWhileStmt(DoWhileStmt stmt, BlockData arg) {
        return conditionalStmtAtomicTransformation(stmt, arg);
    }

    @Override
    public BlockData visitSwitchStmt(SwitchStmt stmt, BlockData arg) {
        return conditionalStmtAtomicTransformation(stmt, arg);
    }

    @Override
    public BlockData visitForStmt(ForStmt stmt, BlockData arg) {
        final BlockData newBlockData = BlockData.builder(arg)
                .insideLoopOrSwitch(true)
                .insideBreakableAtomic(false)
                .build();
        stmt.setStatement(performAtomicSmallTransformationCompact(stmt.getStatement(), newBlockData));
        return newBlockData;
    }

    @Override
    public BlockData visitCompoundExpr(CompoundExpr expr, BlockData arg) {
        expr.setStatement(performAtomicSmallTransformationCompact(expr.getStatement(), arg));
        return arg;
    }

    @Override
    public BlockData visitAtomicStmt(AtomicStmt stmt, BlockData arg) {
        throw new IllegalStateException("entered atomic statement - it implies it hasn't been processed earlier");
    }

    private BlockData conditionalStmtAtomicTransformation(ConditionalStmt stmt, BlockData arg) {
        final BlockData newData = BlockData.builder(arg)
                .insideLoopOrSwitch(true)
                .insideBreakableAtomic(false)
                .build();
        stmt.setStatement(performAtomicSmallTransformationCompact(stmt.getStatement(), newData));
        return newData;
    }

    /**
     * Loop over statements in the given list and perform atomic small
     * transformation for each of them. Transformed statements are replaced
     * by the new ones.
     *
     * @param stmts Statements to iterate over.
     * @param stmtsArg Block data for each of the statements in given list.
     */
    private void replaceTransformedStatements(List<Statement> stmts, BlockData stmtsArg) {
        final ListIterator<Statement> stmtIt = stmts.listIterator();

        while (stmtIt.hasNext()) {
            final Statement stmt = stmtIt.next();
            final LinkedList<Statement> replacement = performAtomicSmallTransformationList(stmt, stmtsArg);

            if (replacement.isEmpty()) {
                stmtIt.remove();
            } else {
                final Iterator<Statement> newStmtIt = replacement.iterator();
                final Statement first = newStmtIt.next();

                if (first != stmt) {
                    stmtIt.set(first);
                }

                while (newStmtIt.hasNext()) {
                    stmtIt.add(newStmtIt.next());
                }
            }
        }
    }

    /**
     * Get statements that are the result of atomic transformation for given
     * statement.
     *
     * @param stmt A statement from an AST node.
     * @param stmtArg Block data depicting the given statement.
     * @return List of statements that is to replace the given one in the
     *         containing node. Never null.
     */
    private LinkedList<Statement> performAtomicSmallTransformationList(Statement stmt, BlockData stmtArg) {
        return Optional.fromNullable(stmt.accept(atomicTransformGateway, stmtArg)).or(Lists.newList(stmt));
    }

    /**
     * Get statement that is the result of atomic transformation for given
     * statement.
     *
     * @param stmt A statement from an AST node.
     * @param stmtArg Block data depicting the given statement.
     * @return Statement that is to replace the given one in the containing
     *         node. Never <code>null</code>.
     */
    private Statement performAtomicSmallTransformationCompact(Statement stmt, BlockData stmtArg) {
        final Optional<LinkedList<Statement>> optResult = Optional.fromNullable(stmt.accept(atomicTransformGateway, stmtArg));

        if (!optResult.isPresent()) {
            return stmt;
        }

        final LinkedList<Statement> result = optResult.get();

        if (result.isEmpty()) {
            return new EmptyStmt(Location.getDummyLocation());
        } else if (result.size() == 1) {
            return result.getFirst();
        } else {
            return new CompoundStmt(
                    Location.getDummyLocation(),
                    Lists.<IdLabel>newList(),
                    Lists.<Declaration>newList(),
                    result
            );
        }
    }

    /**
     * Perform the small transformation on a single atomic statement.
     *
     * @param stmt Atomic statement to transform.
     * @param stmtData Data about the atomic statement.
     * @return List with statements that will replace the given atomic
     *         statement. If it is empty, the atomic statement will be removed
     *         and no other statement takes its place.
     */
    private LinkedList<Statement> atomizeAtomicStmt(AtomicStmt stmt, BlockData stmtData) {
        return stmtData.isInsideAtomicBlock()
                ? atomizeAtomicInsideAtomic(stmt, stmtData)
                : atomizeRealAtomic(stmt);
    }

    private LinkedList<Statement> atomizeAtomicInsideAtomic(AtomicStmt stmt, BlockData stmtData) {
        /* 'atomic' is removed from an atomic statement nested in an atomic
           block. We need to continue transforming the statement inside atomic
           because it wouldn't happen if the statement was returned. */
        return performAtomicSmallTransformationList(stmt.getStatement(), stmtData);
    }

    private LinkedList<Statement> atomizeRealAtomic(AtomicStmt stmt) {
        final CompoundStmt result = stmt.getStatement() instanceof CompoundStmt
                ? (CompoundStmt) stmt.getStatement()
                : new CompoundStmt(
                    Location.getDummyLocation(),
                    Lists.<IdLabel>newList(),
                    Lists.<Declaration>newList(),
                    Lists.newList(stmt.getStatement())
                );

        final String atomicVariableUniqueName = mangler.mangle(ATOMIC_VARIABLE_BASE_NAME);

        result.setAtomicVariableUniqueName(Optional.of(atomicVariableUniqueName));
        result.getDeclarations().addFirst(createAtomicInitialCall(atomicVariableUniqueName));
        result.getStatements().addLast(createAtomicFinalCall(atomicVariableUniqueName));

        return Lists.<Statement>newList(result);
    }

    private DataDecl createAtomicInitialCall(String atomicVariableUniqueName) {
        // Type name

        final Typename atomicVarTypename = new Typename(
                Location.getDummyLocation(),
                atomicSpecification.getTypename()
        );

        atomicVarTypename.setIsGenericReference(false);
        atomicVarTypename.setUniqueName(atomicTypeUniqueName);
        atomicVarTypename.setIsDeclaredInThisNescEntity(false);

        // Call to the atomic start function

        final Identifier funIdentifier = new Identifier(
                Location.getDummyLocation(), atomicSpecification.getStartFunctionName()
        );
        funIdentifier.setUniqueName(Optional.of(atomicStartFunUniqueName));
        funIdentifier.setIsGenericReference(false);
        funIdentifier.setRefsDeclInThisNescEntity(false);

        final FunctionCall atomicStartCall = new FunctionCall(
                Location.getDummyLocation(),
                funIdentifier,
                Lists.<Expression>newList(),
                NescCallKind.NORMAL_CALL
        );

        // Variable declaration

        final IdentifierDeclarator declarator = new IdentifierDeclarator(
                Location.getDummyLocation(),
                ATOMIC_VARIABLE_BASE_NAME
        );

        declarator.setIsNestedInNescEntity(true);
        declarator.setUniqueName(Optional.of(atomicVariableUniqueName));

        final VariableDecl atomicVariableDecl = new VariableDecl(
                Location.getDummyLocation(),
                Optional.<Declarator>of(declarator),
                Lists.<Attribute>newList(),
                Optional.<AsmStmt>absent()
        );

        atomicVariableDecl.setInitializer(Optional.<Expression>of(atomicStartCall));

        // Final declaration

        return new DataDecl(
                Location.getDummyLocation(),
                Lists.<TypeElement>newList(atomicVarTypename),
                Lists.<Declaration>newList(atomicVariableDecl)
        );
    }

    private ExpressionStmt createAtomicFinalCall(String atomicVariableUniqueName) {
        // Prepare identifiers

        final Identifier funIdentifier = new Identifier(
                Location.getDummyLocation(), atomicSpecification.getEndFunctionName()
        );
        funIdentifier.setRefsDeclInThisNescEntity(false);
        funIdentifier.setUniqueName(Optional.of(atomicEndFunUniqueName));
        funIdentifier.setIsGenericReference(false);

        final Identifier argIdentifier = new Identifier(
                Location.getDummyLocation(), ATOMIC_VARIABLE_BASE_NAME
        );
        argIdentifier.setRefsDeclInThisNescEntity(true);
        argIdentifier.setUniqueName(Optional.of(atomicVariableUniqueName));
        argIdentifier.setIsGenericReference(false);

        // Prepare the call to the atomic end function

        final FunctionCall finalCall = new FunctionCall(
                Location.getDummyLocation(),
                funIdentifier,
                Lists.<Expression>newList(argIdentifier),
                NescCallKind.NORMAL_CALL
        );

        return new ExpressionStmt(
                Location.getDummyLocation(),
                finalCall
        );
    }

    private LinkedList<Statement> atomizeReturnStmt(ReturnStmt stmt, BlockData stmtData) {
        if (!stmtData.isInsideAtomicBlock() || VariousUtils.getBooleanValue(stmt.getIsAtomicSafe())) {
            return Lists.<Statement>newList(stmt);
        }

        stmt.setIsAtomicSafe(true);

        return stmt.getValue().isPresent()
                ? atomizeExprReturnStmt(stmt.getValue().get(), stmtData)
                : atomizeVoidReturnStmt(stmt, stmtData);
    }

    private LinkedList<Statement> atomizeExprReturnStmt(Expression retExpr, BlockData stmtData) {
        final String retVariableUniqueName = mangler.mangle(ATOMIC_RETURN_VARIABLE_BASE_NAME);

        // Prepare the identifier of the returned variable

        final Identifier retIdentifier = new Identifier(
                Location.getDummyLocation(),
                ATOMIC_RETURN_VARIABLE_BASE_NAME
        );
        retIdentifier.setIsGenericReference(false);
        retIdentifier.setRefsDeclInThisNescEntity(true);
        retIdentifier.setUniqueName(Optional.of(retVariableUniqueName));

        // Prepare the new 'return' statement

        final ReturnStmt newRetStmt = new ReturnStmt(
                Location.getDummyLocation(),
                Optional.<Expression>of(retIdentifier)
        );
        newRetStmt.setIsAtomicSafe(true);

        // Prepare statements in the created block

        final LinkedList<Statement> stmts = new LinkedList<>();
        stmts.add(createAtomicFinalCall(stmtData.getAtomicVariableUniqueName().get()));
        stmts.add(newRetStmt);

        // Prepare the block

        final CompoundStmt resultStmt = new CompoundStmt(
                Location.getDummyLocation(),
                Lists.<IdLabel>newList(),
                Lists.<Declaration>newList(createReturnExprEvalVariable(stmtData.getFunctionReturnType().get(),
                        retVariableUniqueName, retExpr)),
                stmts
        );

        return Lists.<Statement>newList(resultStmt);
    }

    private DataDecl createReturnExprEvalVariable(AstType retType, String retVarUniqueName, Expression retExpr) {
        // Prepare the type

        final AstType usedType = retType.deepCopy(true);
        for (TypeElement typeElement : usedType.getQualifiers()) {
            typeElement.accept(atomicReturnTypeVisitor, null);
        }

        // Create and assign the identifier declarator

        final IdentifierDeclarator identifierDeclarator = new IdentifierDeclarator(
                Location.getDummyLocation(),
                ATOMIC_RETURN_VARIABLE_BASE_NAME
        );
        identifierDeclarator.setUniqueName(Optional.of(retVarUniqueName));

        /* Set to 'true' even if we aren't in a NesC entity to remangle the name
           if it appears in a generic component. If no, it has no effect. */
        identifierDeclarator.setIsNestedInNescEntity(true);

        if (!usedType.getDeclarator().isPresent()) {
            usedType.setDeclarator(Optional.<Declarator>of(identifierDeclarator));
        } else {
            final NestedDeclarator deepestDeclarator = DeclaratorUtils.getDeepestNestedDeclarator(
                    usedType.getDeclarator().get()).get();
            deepestDeclarator.setDeclarator(Optional.<Declarator>of(identifierDeclarator));
        }

        // Create the inner declaration

        final VariableDecl variableDecl = new VariableDecl(
                Location.getDummyLocation(),
                usedType.getDeclarator(),
                Lists.<Attribute>newList(),
                Optional.<AsmStmt>absent()
        );
        variableDecl.setInitializer(Optional.of(retExpr));

        // Return the final declaration

        return new DataDecl(
                Location.getDummyLocation(),
                usedType.getQualifiers(),
                Lists.<Declaration>newList(variableDecl)
        );
    }

    private LinkedList<Statement> atomizeVoidReturnStmt(ReturnStmt stmt, BlockData stmtData) {
        final LinkedList<Statement> result = new LinkedList<>();
        result.add(createAtomicFinalCall(stmtData.getAtomicVariableUniqueName().get()));
        result.add(stmt);
        return result;
    }

    private LinkedList<Statement> atomizeBreakStmt(BreakStmt stmt, BlockData stmtData) {
        final boolean isAtomicSafe = VariousUtils.getBooleanValue(stmt.getIsAtomicSafe());
        stmt.setIsAtomicSafe(true);
        return atomizeBreakingStmt(stmt, stmtData, isAtomicSafe);
    }

    private LinkedList<Statement> atomizeContinueStmt(ContinueStmt stmt, BlockData stmtData) {
        final boolean isAtomicSafe = VariousUtils.getBooleanValue(stmt.getIsAtomicSafe());
        stmt.setIsAtomicSafe(true);
        return atomizeBreakingStmt(stmt, stmtData, isAtomicSafe);
    }

    private LinkedList<Statement> atomizeBreakingStmt(Statement breakingStmt, BlockData stmtData,
            boolean isAtomicSafe) {
        if (!stmtData.isInsideBreakableAtomic() || isAtomicSafe) {
            return Lists.newList(breakingStmt);
        }

        final LinkedList<Statement> result = new LinkedList<>();
        result.add(createAtomicFinalCall(stmtData.getAtomicVariableUniqueName().get()));
        result.add(breakingStmt);
        return result;
    }

    /**
     * Builder for the basic reduce visitor.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    static final class Builder {
        /**
         * Objects needed to build a basic reduce visitor.
         */
        private final Map<String, String> globalNames = new HashMap<>();
        private NameMangler mangler;
        private SchedulerSpecification schedulerSpecification;
        private String taskWiringConfigurationName;
        private AtomicSpecification atomicSpecification;

        /**
         * Private constructor to limit its accessibility.
         */
        private Builder() {
        }

        /**
         * Set the name mangler that will be used for generating needed unique
         * names. It shall guarantee that no global name added to this builder
         * will be repeated. In other words, the global names should be added as
         * forbidden to the mangler.
         *
         * @param nameMangler Name mangler to set.
         * @return <code>this</code>
         */
        public Builder nameMangler(NameMangler nameMangler) {
            this.mangler = nameMangler;
            return this;
        }

        /**
         * Adds mapping from unique names to corresponding global names.
         * Previously added mappings with the same keys as in the parameter are
         * overwritten.
         *
         * @param globalNames Map from unique names to global names.
         * @return <code>this</code>
         */
        public Builder putGlobalNames(Map<String, String> globalNames) {
            this.globalNames.putAll(globalNames);
            return this;
        }

        /**
         * Set the scheduler specification with information necessary to strip
         * the tasks by the visitor.
         *
         * @param schedulerSpec Scheduler specification to set.
         * @return <code>this</code>
         */
        public Builder schedulerSpecification(SchedulerSpecification schedulerSpec) {
            this.schedulerSpecification = schedulerSpec;
            return this;
        }

        /**
         * Set the name of the configuration that will be created by the
         * visitor to wire task interface references (created by it) from
         * non-generic modules to the scheduler.
         *
         * @param name Name of the configuration to set.
         * @return <code>this</code>
         */
        public Builder taskWiringConfigurationName(String name) {
            this.taskWiringConfigurationName = name;
            return this;
        }

        /**
         * Set the atomic specification with information necessary to strip the
         * atomic statements.
         *
         * @param atomicSpec Atomic specification to set.
         * @return <code>this</code>
         */
        public Builder atomicSpecification(AtomicSpecification atomicSpec) {
            this.atomicSpecification = atomicSpec;
            return this;
        }

        private void validate() {
            checkState(mangler != null, "mangler has not been set or is set to null");
            checkState(schedulerSpecification != null, "the scheduler specification has not been set or is set to null");
            checkState(taskWiringConfigurationName != null, "name of the task wiring configuration has not been set or is set to null");
            checkState(atomicSpecification != null, "the atomic specification has not been set or set to null");
            checkState(!globalNames.containsKey(null), "a mapping from null has been added");
            checkState(!globalNames.containsValue(null), "a mapping to a null value has been added");
            checkState(!globalNames.containsKey(""), "a mapping from an empty string has been added");
            checkState(!globalNames.containsValue(""), "a mapping to an empty string has been added");
        }

        public BasicReduceVisitor build() {
            validate();
            return new BasicReduceVisitor(this, mangler.mangle(atomicSpecification.getTypename()),
                    mangler.mangle(atomicSpecification.getStartFunctionName()),
                    mangler.mangle(atomicSpecification.getEndFunctionName()));
        }

        private ImmutableSet<String> buildGlobalNames() {
            return ImmutableSet.copyOf(globalNames.values());
        }

        private Map<String, String> buildUniqueNamesMap(String atomicTypeUniqueName,
                String atomicStartFunUniqueName, String atomicEndFunUniqueName) {
            final Map<String, String> result = new HashMap<>(globalNames);

            result.put(atomicTypeUniqueName, atomicSpecification.getTypename());
            result.put(atomicStartFunUniqueName, atomicSpecification.getStartFunctionName());
            result.put(atomicEndFunUniqueName, atomicSpecification.getEndFunctionName());

            return result;
        }

        private Configuration buildInitialTaskWiringConfiguration() {
            final ComponentRef schedulerRef = new ComponentRef(
                    Location.getDummyLocation(),
                    new Word(Location.getDummyLocation(), schedulerSpecification.getComponentName()),
                    false,
                    new LinkedList<Expression>()
            );
            schedulerRef.setAlias(Optional.<Word>absent());

            final ConfigurationImpl impl = new ConfigurationImpl(
                    Location.getDummyLocation(),
                    Lists.<Declaration>newList(new ComponentsUses(
                            Location.getDummyLocation(),
                            Lists.newList(schedulerRef)
                    ))
            );

            return new Configuration(
                    Location.getDummyLocation(),
                    new LinkedList<Attribute>(),
                    new Word(Location.getDummyLocation(), this.taskWiringConfigurationName),
                    new LinkedList<Declaration>(),
                    impl,
                    false,
                    Optional.<LinkedList<Declaration>>absent()
            );
        }
    }

    /**
     * Class to simplify atomic transformation of statements that are wrapped by
     * <code>Optional</code>.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class AtomicTransformFunction implements Function<Statement, Statement> {
        private final BlockData blockData;

        /**
         * Initializes this function by storing the argument in a member field.
         *
         * @param blockData Block data to use for transformations.
         * @throws NullPointerException The block data is <code>null</code>.
         */
        private AtomicTransformFunction(BlockData blockData) {
            checkNotNull(blockData, "block data object cannot be null");
            this.blockData = blockData;
        }

        @Override
        public Statement apply(Statement stmt) {
            checkNotNull(stmt, "statement cannot be null");
            return BasicReduceVisitor.this.performAtomicSmallTransformationCompact(stmt, blockData);
        }
    }

    /**
     * Visitor that modifies visited type elements and declarators for usage
     * as the type for the variable that holds the returned expression before
     * ending an atomic block.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class AtomicReturnTypeVisitor extends ExceptionVisitor<Void, Void> {
        @Override
        public Void visitTypeofType(TypeofType typeElement, Void arg) {
            return null;
        }

        @Override
        public Void visitTypeofExpr(TypeofExpr typeElement, Void arg) {
            return null;
        }

        @Override
        public Void visitTypename(Typename typeElement, Void arg) {
            return null;
        }

        @Override
        public Void visitComponentTyperef(ComponentTyperef typeElement, Void arg) {
            return null;
        }

        @Override
        public Void visitStructRef(StructRef typeElement, Void arg) {
            modifyFieldTagRef(typeElement);
            return null;
        }

        @Override
        public Void visitNxStructRef(NxStructRef typeElement, Void arg) {
            modifyFieldTagRef(typeElement);
            return null;
        }

        @Override
        public Void visitUnionRef(UnionRef typeElement, Void arg) {
            modifyFieldTagRef(typeElement);
            return null;
        }

        @Override
        public Void visitNxUnionRef(NxUnionRef typeElement, Void arg) {
            modifyFieldTagRef(typeElement);
            return null;
        }

        @Override
        public Void visitEnumRef(EnumRef typeElement, Void arg) {
            for (Declaration enumerator : typeElement.getFields()) {
                enumerator.accept(this, null);
            }

            return null;
        }

        @Override
        public Void visitRid(Rid typeElement, Void arg) {
            return null;
        }

        @Override
        public Void visitQualifier(Qualifier typeElement, Void arg) {
            return null;
        }

        @Override
        public Void visitEnumerator(Enumerator enumerator, Void arg) {
            enumerator.setUniqueName(BasicReduceVisitor.this.mangler.remangle(enumerator.getUniqueName()));
            return null;
        }

        private void modifyFieldTagRef(TagRef fieldTagRef) {
            // Transform the tag reference not to be the definition
            fieldTagRef.setFields(Lists.<Declaration>newList());
            fieldTagRef.setSemantics(TagRefSemantics.OTHER);
        }
    }
}
