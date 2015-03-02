package pl.edu.mimuw.nesc.basicreduce;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.facade.component.specification.ModuleTable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Class that contains data for a block construct in the NesC language.
 * Objects of this class are immutable.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class BlockData {
    /**
     * Module table of currently traversed module.
     */
    private final Optional<ModuleTable> moduleTable;

    /**
     * Get a new builder that will build a block data object.
     *
     * @return Newly created builder that will build a block data object.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Get a new builder initialized to build a clone of the given block data
     * object. However, all needed data can be modified.
     *
     * @param specimen Pattern block data object.
     * @return Newly created builder initialized to build a clone of the given
     *         block data object.
     */
    public static Builder builder(BlockData specimen) {
        checkNotNull(specimen, "the speciman block data cannot be null");
        return new Builder(specimen);
    }

    /**
     * Initialize this block data object with information provided by the
     * builder.
     *
     * @param builder Builder with information necessary to initialize this
     *                block data.
     */
    private BlockData(Builder builder) {
        this.moduleTable = builder.moduleTable;
    }

    /**
     * Get the module table contained in this block data object. The object
     * shall be present if and only if this block data object corresponds to
     * NesC language block nested in a module implementation.
     *
     * @return Module table contained in this block data object.
     */
    public Optional<ModuleTable> getModuleTable() {
        return moduleTable;
    }

    /**
     * Builder for a block data object.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    static final class Builder {
        /**
         * Data needed to build a block data object.
         */
        private Optional<ModuleTable> moduleTable = Optional.absent();

        /**
         * Private constructor to limit its accessibility.
         */
        private Builder() {
        }

        /**
         * Initializes all data to build a copy of the given object.
         *
         * @param specimen Pattern object.
         */
        private Builder(BlockData specimen) {
            this.moduleTable = specimen.moduleTable;
        }

        /**
         * Set the module table for this block data object. The argument can be
         * <code>null</code> and then no module table will be contained in the
         * built block data object.
         *
         * @param table Module table to set or <code>null</code>.
         * @return <code>this</code>
         */
        public Builder moduleTable(ModuleTable table) {
            this.moduleTable = Optional.fromNullable(table);
            return this;
        }

        private void validate() {
            checkNotNull(moduleTable, "module table cannot be null");
        }

        public BlockData build() {
            validate();
            return new BlockData(this);
        }
    }
}
