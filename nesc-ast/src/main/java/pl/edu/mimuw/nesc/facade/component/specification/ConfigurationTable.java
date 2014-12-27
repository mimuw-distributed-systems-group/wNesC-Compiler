package pl.edu.mimuw.nesc.facade.component.specification;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration;
import pl.edu.mimuw.nesc.declaration.object.InterfaceRefDeclaration;
import pl.edu.mimuw.nesc.facade.iface.InterfaceEntity;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Class that represents a kind of a symbol table for configurations. It is
 * responsible for storing information about implementation of elements from
 * specifications of configurations.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class ConfigurationTable extends ComponentTable<WiringElement> {
    /**
     * Get a builder that will build a configuration table.
     *
     * @return Newly created builder to create a configuration table.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Initialize this table with information from given builder.
     *
     * @param builder Builder with necessary data.
     */
    private ConfigurationTable(Builder builder) {
        super(builder.getComponentTablePrivateBuilder());
    }

    /**
     * Mark element with given name as wired. An exception is not thrown if the
     * element has been already wired because it is allowed to wire a single
     * element multiple times.
     *
     * @param name Name of the element to mark as wired.
     * @throws NullPointerException Given name is null.
     * @throws IllegalArgumentException Given name is an empty string or there
     *                                  is no element with given name.
     */
    @Override
    public void markFulfilled(String name) {
        checkNotNull(name, "name cannot be null");
        checkArgument(!name.isEmpty(), "name cannot be an empty string");

        final Optional<WiringElement> optElement = get(name);
        checkArgument(optElement.isPresent(), "there is no wiring element with name '%s'", name);

        optElement.get().wired();
    }

    /**
     * Builder for a configuration table.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static final class Builder extends ComponentTable.FromSpecificationBuilder<WiringElement, ConfigurationTable> {
        /**
         * Private constructor to limit its accessibility.
         */
        private Builder() {
        }

        @Override
        protected ConfigurationTable create() {
            return new ConfigurationTable(this);
        }

        @Override
        protected void addInterfaceElements(InterfaceRefDeclaration declaration,
                ImmutableMap.Builder<String, WiringElement> builder) {

            final WiringElement element = new WiringElement(WiringElement.Kind.INTERFACE);
            builder.put(declaration.getName(), element);
        }

        @Override
        protected void addBareElements(FunctionDeclaration declaration, InterfaceEntity.Kind bareKind,
                ImmutableMap.Builder<String, WiringElement> builder) {

            final WiringElement.Kind kind;

            switch (bareKind) {
                case COMMAND:
                    kind = WiringElement.Kind.BARE_COMMAND;
                    break;
                case EVENT:
                    kind = WiringElement.Kind.BARE_EVENT;
                    break;
                default:
                    throw new RuntimeException("unexpected interface entity kind '" + bareKind + "'");
            }

            final WiringElement element = new WiringElement(kind);
            builder.put(declaration.getName(), element);
        }
    }
}
