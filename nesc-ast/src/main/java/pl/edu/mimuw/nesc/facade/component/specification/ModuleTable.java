package pl.edu.mimuw.nesc.facade.component.specification;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import pl.edu.mimuw.nesc.ast.gen.DataDecl;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.InterfaceRef;
import pl.edu.mimuw.nesc.ast.gen.RpInterface;
import pl.edu.mimuw.nesc.ast.gen.VariableDecl;
import pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration;
import pl.edu.mimuw.nesc.declaration.object.InterfaceRefDeclaration;
import pl.edu.mimuw.nesc.declaration.object.ObjectDeclaration;
import pl.edu.mimuw.nesc.declaration.object.ObjectKind;
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
    private final Map<String, TaskElement> tasks = new HashMap<>();

    /**
     * Unmodifiable view of tasks added to this table.
     */
    private final Map<String, TaskElement> unmodifiableTasks = Collections.unmodifiableMap(tasks);

    /**
     * Get the object that will create a module implementation analyzer.
     *
     * @return Newly created builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Initialize this instance using the builder.
     *
     * @param builder Builder with necessary information.
     */
    private ModuleTable(Builder builder) {
        super(builder);
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
     * Builder for a module table. It shall be built after the specification of
     * a module is fully parsed and analyzed and interfaces references facades
     * are set.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static final class Builder extends ComponentTable.Builder<InterfaceEntityElement, ModuleTable> {
        /**
         * Private constructor to prevent this class from an unauthorized
         * instantiation.
         */
        private Builder() {
        }

        @Override
        protected ModuleTable create() {
            return new ModuleTable(this);
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
}
