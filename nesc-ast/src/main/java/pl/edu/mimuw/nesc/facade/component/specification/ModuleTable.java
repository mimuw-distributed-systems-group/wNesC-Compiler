package pl.edu.mimuw.nesc.facade.component.specification;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import pl.edu.mimuw.nesc.ast.gen.ArrayDeclarator;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.Declarator;
import pl.edu.mimuw.nesc.ast.gen.ExceptionVisitor;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;
import pl.edu.mimuw.nesc.ast.gen.FunctionDeclarator;
import pl.edu.mimuw.nesc.ast.gen.IdentifierDeclarator;
import pl.edu.mimuw.nesc.ast.gen.InterfaceRefDeclarator;
import pl.edu.mimuw.nesc.ast.gen.ModuleImpl;
import pl.edu.mimuw.nesc.ast.gen.PointerDeclarator;
import pl.edu.mimuw.nesc.ast.gen.QualifiedDeclarator;
import pl.edu.mimuw.nesc.astutil.TypeElementUtils;
import pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration;
import pl.edu.mimuw.nesc.declaration.object.InterfaceRefDeclaration;
import pl.edu.mimuw.nesc.facade.iface.InterfaceEntity;
import pl.edu.mimuw.nesc.facade.iface.InterfaceRefFacade;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

/**
 * <p>A class that is responsible for storing information about a module
 * implementation. It handles a list of all commands and events that can be
 * implemented in a module and provides information about them.</p>
 *
 * <p>It is a kind of a high level symbol table.</p>
 *
 * <p>The module table maintains a map whose set of entries is the least set
 * that fulfills the following conditions:</p>
 *
 * <ul>
 *     <li>the map contains an entry for each command or event of each
 *     interface the component provides or uses</li>
 *     <li>the map contains an entry for each bare command or event
 *     the component provides</li>
 * </ul>
 *
 * <p>In other words, the content of the map depicts all commands and events
 * that a module can implement (including default implementations).</p>
 *
 * <p>The keys of the map are constructed in the following way. For
 * a command or an event from an interface the key is the name of the
 * interface reference and the name of the command or event separated by
 * a dot. If it is a bare command or event, the key is simply its name.</p>
 *
 * <p>For the following declarations:</p>
 * <pre>
 *     interface I { command void c(); event void e(); }
 *
 *     module M
 *     {
 *         provides interface I as I1;
 *         provides interface I as I2;
 *         provides command void c();
 *         provides event void e();
 *     }
 *     implementation { &hellip; }
 * </pre>
 * <p>the set of keys shall be equal to:</p>
 * <pre>{ I1.c, I1.e, I2.c, I2.e, c, e }</pre>
 *
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class ModuleTable extends ComponentTable<InterfaceEntityElement> {
    /**
     * Map that contains information about declared tasks.
     */
    private final Map<String, TaskElement> tasks;

    /**
     * Unmodifiable view of tasks added to this table.
     */
    private final Map<String, TaskElement> unmodifiableTasks;

    /**
     * Map with keys that are names of interface references created for tasks
     * and values are corresponding task elements. The map is created
     * incrementally.
     */
    private final Map<String, TaskElement> tasksInterfaceRefs;

    /**
     * Unmodifiable view of interface references associated with tasks.
     */
    private final Map<String, TaskElement> unmodifiableTasksInterfaceRefs;

    /**
     * Visitor that collects unique names.
     */
    private final UniqueNamesCollector UNIQUE_NAMES_COLLECTOR = new UniqueNamesCollector();

    /**
     * Get the object that will create a module implementation analyzer.
     *
     * @return Newly created builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Get a builder that will create a copy of the given module table.
     *
     * @param specimen Object that will be copied.
     * @return Newly created builder that will build a copy of the given module
     *         table.
     */
    public static CopyingBuilder builder(ModuleTable specimen) {
        checkNotNull(specimen, "the table to copy cannot be null");
        return new CopyingBuilder(specimen);
    }

    /**
     * Initialize this instance using the builder.
     *
     * @param builder Builder with necessary information.
     */
    private ModuleTable(PrivateBuilder builder) {
        super(builder.getSuperclassBuilder().getComponentTablePrivateBuilder());

        this.tasks = builder.buildTasks();
        this.unmodifiableTasks = Collections.unmodifiableMap(this.tasks);
        this.tasksInterfaceRefs = new HashMap<>();
        this.unmodifiableTasksInterfaceRefs = Collections.unmodifiableMap(this.tasksInterfaceRefs);

        for (TaskElement taskElement : this.tasks.values()) {
            if (taskElement.getInterfaceRefName().isPresent()) {
                this.tasksInterfaceRefs.put(taskElement.getInterfaceRefName().get(), taskElement);
            }
        }
    }

    /**
     * Get a newly created module table that contains the same elements of this.
     *
     * @return Newly created deep copy of this module table.
     */
    public ModuleTable deepCopy() {
        return builder(this).build();
    }

    /**
     * Mark the implementation element with given name as implemented.
     *
     * @param name Name of a command or event (with dot if from an interface).
     * @throws NullPointerException Name is null.
     * @throws IllegalArgumentException Name is an empty string.
     * @throws IllegalStateException Element with given name has been already
     *                               marked as implemented.
     */
    @Override
    public void markFulfilled(String name) {
        checkNotNull(name, "name cannot be null");
        checkArgument(!name.isEmpty(), "name cannot be an empty string");

        final Optional<InterfaceEntityElement> optElement = get(name);
        checkArgument(optElement.isPresent(), "'%s' is not a valid name of a command or event", name);

        final InterfaceEntityElement element = optElement.get();

        checkState(!element.isImplemented(), "element '%s' is already marked as implemented");
        element.implemented();
    }

    /**
     * Adds information about a task with the given name to the module table. If
     * the table contains a task with the given name, then calling this method
     * has no effect. Otherwise, adds a task element with given name and marks
     * it as not implemented.
     *
     * @param taskName Name of the task to declare in the module table.
     * @throws NullPointerException Task name is null.
     * @throws IllegalArgumentException Task name is an empty string.
     */
    public void addTask(String taskName) {
        checkNotNull(taskName, "name of the task cannot be null");
        checkArgument(!taskName.isEmpty(), "name of the task cannot be an empty string");

        if (!tasks.containsKey(taskName)) {
            tasks.put(taskName, new TaskElement());
        }
    }

    /**
     * Marks the task with given name as implemented.
     *
     * @param taskName Name of the task that has been implemented.
     * @throws NullPointerException Name of the task is null.
     * @throws IllegalArgumentException Name of the task is an empty string.
     * @throws IllegalStateException This table does not contain a task with
     *                               given name or it has been already marked
     *                               as implemented.
     */
    public void taskImplemented(String taskName) {
        checkNotNull(taskName, "name of the task cannot be null");
        checkArgument(!taskName.isEmpty(), "name of the task cannot be an empty string");

        final Optional<TaskElement> optTaskElement = Optional.fromNullable(tasks.get(taskName));
        checkState(optTaskElement.isPresent(), "task '%s' has not been declared", taskName);

        final TaskElement taskElement = optTaskElement.get();
        checkState(!taskElement.isImplemented(), "task '%s' is already marked as implemented", taskName);
        taskElement.implemented();
    }

    /**
     * Get an unmodifiable view of the tasks contained in this table.
     *
     * @return Unmodifiable view of tasks from this table.
     */
    public Map<String, TaskElement> getTasks() {
        return unmodifiableTasks;
    }

    /**
     * Get an unmodifiable view of interface references associated with tasks
     * in this table.
     *
     * @return Unmodifiable view of interface references associated with tasks
     *         in this table.
     */
    public Map<String, TaskElement> getTasksInterfaceRefs() {
        return unmodifiableTasksInterfaceRefs;
    }

    /**
     * Associate the task with given name with an interface reference of given
     * name.
     *
     * @param taskName Name of the task.
     * @param interfaceRefName Name of the interface reference.
     * @throws NullPointerException One of the arguments is <code>null</code>.
     * @throws IllegalArgumentException One of the argument is an empty string.
     * @throws IllegalStateException There is a task associated with given
     *                               interface reference name. The task with
     *                               given name does not exist or is already
     *                               associated with an interface reference.
     */
    public void associateTaskWithInterfaceRef(String taskName, String interfaceRefName) {
        checkNotNull(taskName, "task name cannot be null");
        checkNotNull(interfaceRefName, "interface reference name cannot be null");
        checkArgument(!taskName.isEmpty(), "name of tha task cannot be an empty string");
        checkArgument(!interfaceRefName.isEmpty(), "name of the interface reference cannot be an empty string");
        checkState(!tasksInterfaceRefs.containsKey(interfaceRefName),
                "a task has been already associated with given interface reference name");

        final Optional<TaskElement> optTaskElement = Optional.fromNullable(tasks.get(taskName));
        checkState(optTaskElement.isPresent(), "task '%s' does not exist in this module table", taskName);

        final TaskElement taskElement = optTaskElement.get();
        taskElement.setInterfaceRefName(interfaceRefName);
        tasksInterfaceRefs.put(interfaceRefName, taskElement);
    }

    /**
     * Collects unique names from given module implementation and saves them in
     * the table.
     *
     * @param moduleImpl Module implementation with the unique names of the
     *                   entities.
     * @throws NullPointerException Module implementation is <code>null</code>.
     * @throws IllegalArgumentException Given module implementation does not
     *                                  contain exactly the implementation
     *                                  elements contained in this module table.
     * @throws IllegalStateException Names for this table has been already
     *                               collected.
     */
    public void collectUniqueNames(ModuleImpl moduleImpl) {
        checkNotNull(moduleImpl, "module implementation cannot be null");

        int uniqueNamesCount = 0;

        for (Declaration declaration : moduleImpl.getDeclarations()) {
            if (!(declaration instanceof FunctionDecl)) {
                continue;
            }

            if (declaration.accept(UNIQUE_NAMES_COLLECTOR, null)) {
                ++uniqueNamesCount;
            }
        }

        final Iterable<ImplementationElement> allElements =
                Iterables.concat(elements.values(), tasks.values());

        // Compute the number of unique names not set
        for (ImplementationElement ifaceElement : allElements) {
            if (ifaceElement.isImplemented()) {
                --uniqueNamesCount;
            }
        }

        checkArgument(uniqueNamesCount == 0, "missing %s unique name(s) in given module implementation",
                -uniqueNamesCount);
    }

    /**
     * Builder for particular elements of a module table.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private interface PrivateBuilder {
        Map<String, TaskElement> buildTasks();
        ComponentTable.Builder<InterfaceEntityElement, ModuleTable> getSuperclassBuilder();
    }

    /**
     * Builder for a module table. It shall be built after the specification of
     * a module is fully parsed and analyzed and interfaces references facades
     * are set.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static final class Builder extends ComponentTable.FromSpecificationBuilder<InterfaceEntityElement, ModuleTable> {
        /**
         * Builder for elements of the module table.
         */
        private final PrivateBuilder privateBuilder = new PrivateBuilder() {
            @Override
            public Map<String, TaskElement> buildTasks() {
                return new HashMap<>();
            }

            @Override
            public ComponentTable.Builder<InterfaceEntityElement, ModuleTable> getSuperclassBuilder() {
                return Builder.this;
            }
        };

        /**
         * Private constructor to prevent this class from an unauthorized
         * instantiation.
         */
        private Builder() {
        }

        @Override
        protected ModuleTable create() {
            return new ModuleTable(privateBuilder);
        }

        @Override
        protected void addInterfaceElements(InterfaceRefDeclaration declaration,
                ImmutableMap.Builder<String, InterfaceEntityElement> builder) {

            final InterfaceRefFacade facade = declaration.getFacade();

            for (Map.Entry<String, InterfaceEntity> ifaceEntry : facade.getAll()) {
                final String ifaceEntityName = ifaceEntry.getKey();
                final InterfaceEntity ifaceEntity = ifaceEntry.getValue();

                final String name = format("%s.%s", facade.getInstanceName(), ifaceEntityName);
                final boolean isProvided = facade.isProvided() && ifaceEntity.getKind() == InterfaceEntity.Kind.COMMAND
                        || !facade.isProvided() && ifaceEntity.getKind() == InterfaceEntity.Kind.EVENT;

                final InterfaceEntityElement value = new InterfaceEntityElement(isProvided,
                        ifaceEntity.getKind(), Optional.of(facade.getInterfaceName()));

                builder.put(name, value);
            }
        }

        @Override
        protected void addBareElements(FunctionDeclaration funDecl, InterfaceEntity.Kind kind,
                ImmutableMap.Builder<String, InterfaceEntityElement> builder) {

            final InterfaceEntityElement value = new InterfaceEntityElement(funDecl.isProvided().get(),
                    kind, Optional.<String>absent());

            builder.put(funDecl.getName(), value);
        }
    }

    /**
     * Builder that copies a module table.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static final class CopyingBuilder extends ComponentTable.CopyingBuilder<InterfaceEntityElement, ModuleTable> {
        /**
         * Map with tasks to be copied.
         */
        private final Map<String, TaskElement> tasksSpecimen;

        /**
         * Builder that will create copies of particular elements of a module
         * table.
         */
        private final PrivateBuilder privateBuilder = new PrivateBuilder() {
            @Override
            public Map<String, TaskElement> buildTasks() {
                final Map<String, TaskElement> result = new HashMap<>(tasksSpecimen);
                for (Map.Entry<String, TaskElement> taskEntry : result.entrySet()) {
                    taskEntry.setValue(taskEntry.getValue().deepCopy());
                }
                return result;
            }

            @Override
            public ComponentTable.Builder<InterfaceEntityElement, ModuleTable> getSuperclassBuilder() {
                return CopyingBuilder.this;
            }
        };

        /**
         * Constructor that stores elements to be copied.
         *
         * @param specimen Module table to be copied.
         */
        private CopyingBuilder(ModuleTable specimen) {
            super(specimen);
            this.tasksSpecimen = specimen.tasks;
        }

        @Override
        protected InterfaceEntityElement copyElement(InterfaceEntityElement specimen) {
            return specimen.deepCopy();
        }

        @Override
        protected ModuleTable create() {
            return new ModuleTable(privateBuilder);
        }
    }

    /**
     * Visitor responsible for saving unique names encountered in visited
     * nodes.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class UniqueNamesCollector extends ExceptionVisitor<Boolean, Void> {
        @Override
        public Boolean visitFunctionDecl(FunctionDecl functionDecl, Void arg) {
            final FunctionDeclaration.FunctionType funType =
                    TypeElementUtils.getFunctionType(functionDecl.getModifiers());

            switch (funType) {
                case COMMAND:
                case EVENT:
                    return functionDecl.getDeclarator().accept(this, null);
                default:
                    return false;
            }
        }

        @Override
        public Boolean visitIdentifierDeclarator(IdentifierDeclarator declarator, Void arg) {
            final Optional<InterfaceEntityElement> optIfaceEntityElement =
                    ModuleTable.this.get(declarator.getName());
            checkState(optIfaceEntityElement.isPresent(), "bare command or event '%s' does not exist in this module table",
                    declarator.getName());
            final InterfaceEntityElement ifaceEntityElement = optIfaceEntityElement.get();
            checkState(ifaceEntityElement.isImplemented(), "implementation of an unimplemented bare command or event encoutnered");

            // Save the unique name
            ifaceEntityElement.setUniqueName(declarator.getUniqueName().get());
            return true;
        }

        @Override
        public Boolean visitInterfaceRefDeclarator(InterfaceRefDeclarator declarator, Void arg) {
            final IdentifierDeclarator idDeclarator = (IdentifierDeclarator) declarator.getDeclarator().get();
            final String uniqueName = idDeclarator.getUniqueName().get();
            final String ifaceRefName = declarator.getName().getName();
            final String methodName = idDeclarator.getName();
            final String key = format("%s.%s", ifaceRefName, methodName);

            // Look for element to store the name and check correctness
            final Optional<InterfaceEntityElement> optIfaceElement = ModuleTable.this.get(key);
            final Optional<TaskElement> optTaskElement = Optional.fromNullable(
                    ModuleTable.this.tasksInterfaceRefs.get(ifaceRefName));
            checkState(optIfaceElement.isPresent() || optTaskElement.isPresent(),
                    "cannot find an element with name '%s' in this module table", key);
            checkState(!optIfaceElement.isPresent() || !optTaskElement.isPresent(),
                    "a task and an interface entity element exist for the same function");
            final ImplementationElement implElement = optIfaceElement.isPresent()
                    ? optIfaceElement.get()
                    : optTaskElement.get();
            checkState(implElement.isImplemented(), "implementation of an unimplemented element encountered");

            // Save the unique name
            implElement.setUniqueName(uniqueName);
            return true;
        }

        @Override
        public Boolean visitArrayDeclarator(ArrayDeclarator declarator, Void arg) {
            return jump(declarator.getDeclarator());
        }

        @Override
        public Boolean visitQualifiedDeclarator(QualifiedDeclarator declarator, Void arg) {
            return jump(declarator.getDeclarator());
        }

        @Override
        public Boolean visitFunctionDeclarator(FunctionDeclarator declarator, Void arg) {
            return jump(declarator.getDeclarator());
        }

        @Override
        public Boolean visitPointerDeclarator(PointerDeclarator declarator, Void arg) {
            return jump(declarator.getDeclarator());
        }

        private Boolean jump(Optional<Declarator> nextDeclarator) {
            return nextDeclarator.isPresent()
                    ? nextDeclarator.get().accept(this, null)
                    : false;
        }
    }
}
