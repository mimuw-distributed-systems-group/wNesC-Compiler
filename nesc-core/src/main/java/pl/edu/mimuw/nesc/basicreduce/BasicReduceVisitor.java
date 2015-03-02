package pl.edu.mimuw.nesc.basicreduce;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import pl.edu.mimuw.nesc.ast.CallDirection;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.NescCallKind;
import pl.edu.mimuw.nesc.ast.RID;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.astutil.AstUtils;
import pl.edu.mimuw.nesc.astutil.DeclaratorUtils;
import pl.edu.mimuw.nesc.common.SchedulerSpecification;
import pl.edu.mimuw.nesc.common.util.list.Lists;
import pl.edu.mimuw.nesc.declaration.nesc.ComponentDeclaration;
import pl.edu.mimuw.nesc.declaration.nesc.ModuleDeclaration;
import pl.edu.mimuw.nesc.declaration.object.unique.UniqueDeclaration;
import pl.edu.mimuw.nesc.facade.component.specification.ModuleTable;
import pl.edu.mimuw.nesc.facade.component.specification.TaskElement;
import pl.edu.mimuw.nesc.names.collecting.ObjectEnvironmentNameCollector;
import pl.edu.mimuw.nesc.names.mangling.CountingNameMangler;
import pl.edu.mimuw.nesc.names.mangling.NameMangler;
import pl.edu.mimuw.nesc.type.ArrayType;
import pl.edu.mimuw.nesc.type.CharType;
import pl.edu.mimuw.nesc.type.Type;
import pl.edu.mimuw.nesc.type.UnsignedIntType;

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
    private BasicReduceVisitor(Builder builder) {

        this.globalNames = builder.buildGlobalNames();
        this.uniqueNamesMap = builder.buildUniqueNamesMap();
        this.mangler = builder.mangler;
        this.schedulerSpecification = builder.schedulerSpecification;
        this.taskWiringConfiguration = builder.buildInitialTaskWiringConfiguration();
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
                .moduleTable(node.getModuleTable())
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
        final ModuleTable moduleTable = module.getModuleTable();
        final Map<String, TaskElement> tasks = moduleTable.getTasks();

        for (Map.Entry<String, TaskElement> taskEntry : tasks.entrySet()) {
            final TaskElement taskElement = taskEntry.getValue();
            final String taskInterfaceRefName = nameMangler.mangle(schedulerSpecification.getTaskInterfaceName());
            moduleTable.associateTaskWithInterfaceRef(taskEntry.getKey(), taskInterfaceRefName);
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
        final Map<String, TaskElement> tasks = module.getModuleTable().getTasks();

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
        uniqueIdentifier.setType(UniqueDeclaration.getInstance().getType());

        final StringCst uniqueArgInner = new StringCst(Location.getDummyLocation(),
                schedulerSpecification.getUniqueIdentifier());
        final Type uniqueArgType = new ArrayType(new CharType(), Optional.of(
                AstUtils.newIntegerConstant(schedulerSpecification.getUniqueIdentifier().length() + 1)));
        uniqueArgInner.setType(Optional.of(uniqueArgType));
        final StringAst uniqueArgOuter = new StringAst(Location.getDummyLocation(), Lists.newList(uniqueArgInner));
        uniqueArgOuter.setType(Optional.of(uniqueArgType));

        final UniqueCall ifaceUniqueCall = new UniqueCall(
                Location.getDummyLocation(),
                Lists.<Expression>newList(uniqueArgOuter),
                NescCallKind.NORMAL_CALL,
                uniqueIdentifier
        );
        ifaceUniqueCall.setType(Optional.<Type>of(new UnsignedIntType()));
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
        taskIdentifier.setType(Optional.<Type>absent());

        final InterfaceDeref ifaceDeref = new InterfaceDeref(
                Location.getDummyLocation(),
                taskIdentifier,
                new Word(Location.getDummyLocation(), schedulerSpecification.getTaskPostCommandName())
        );
        ifaceDeref.setType(Optional.<Type>absent());
        funCall.setFunction(ifaceDeref);

        return arg;
    }

    @Override
    public BlockData visitFunctionDecl(FunctionDecl funDecl, BlockData arg) {
        if (!taskKeywordToEventKeyword(funDecl.getModifiers())) {
            return arg;
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

        return arg;
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

                if (!moduleDeclaration.getAstComponent().getModuleTable().getTasks().isEmpty()) {
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
        final ModuleTable moduleTable = moduleDeclaration.getAstComponent().getModuleTable();
        final String moduleRefName = componentRef.getDeclaration().getName();

        for (TaskElement taskElement : moduleTable.getTasks().values()) {
            connections.add(createTaskConnection(taskElement, moduleRefName, schedulerComponentRefName));
        }
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

        private void validate() {
            checkState(mangler != null, "mangler has not been set or is set to null");
            checkState(schedulerSpecification != null, "the scheduler specification has not been set or is set to null");
            checkState(taskWiringConfigurationName != null, "name of the task wiring configuration has not been set or is set to null");
            checkState(!globalNames.containsKey(null), "a mapping from null has been added");
            checkState(!globalNames.containsValue(null), "a mapping to a null value has been added");
            checkState(!globalNames.containsKey(""), "a mapping from an empty string has been added");
            checkState(!globalNames.containsValue(""), "a mapping to an empty string has been added");
        }

        public BasicReduceVisitor build() {
            validate();
            return new BasicReduceVisitor(this);
        }

        private ImmutableSet<String> buildGlobalNames() {
            return ImmutableSet.copyOf(globalNames.values());
        }

        private Map<String, String> buildUniqueNamesMap() {
            return new HashMap<>(globalNames);
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
}
