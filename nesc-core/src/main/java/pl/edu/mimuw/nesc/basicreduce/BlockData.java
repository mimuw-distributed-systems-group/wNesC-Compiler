package pl.edu.mimuw.nesc.basicreduce;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.gen.AstType;
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
     * Unique name of the variable created for the atomic block to store the
     * result of the atomic start function call. The value is absent if and
     * only if the node is not located inside a block executed atomically.
     */
    private final Optional<String> atomicVariableUniqueName;

    /**
     * Value indicating if a node is located inside a 'while', 'do', 'for' or
     * 'switch' statement.
     */
    private final boolean insideLoopOrSwitch;

    /**
     * Value indicating if a node is located inside a block executed atomically
     * and simultaneously inside a 'while', 'do', 'for' or 'switch' statement
     * that is not as a whole executed atomically (it is not nested in an atomic
     * block).
     */
    private final boolean insideBreakableAtomic;

    /**
     * Return type of the function the node is nested in. Object is absent if
     * the node is not nested in a function.
     */
    private final Optional<AstType> functionReturnType;

    /**
     * Value indicating if the current node is located inside a node excluded
     * from mangling names.
     */
    private final boolean insideNotMangledArea;

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
        this.atomicVariableUniqueName = builder.atomicVariableUniqueName;
        this.insideLoopOrSwitch = builder.insideLoopOrSwitch;
        this.insideBreakableAtomic = builder.insideBreakableAtomic;
        this.functionReturnType = builder.functionReturnType;
        this.insideNotMangledArea = builder.insideNotMangledArea;
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
     * Check if the node is located inside a block executed atomically.
     * Equivalent to <code>getAtomicVariableUniqueName().isPresent()</code>.
     *
     * @return <code>true</code> if and only if the node is located inside
     *         a block executed atomically.
     */
    public boolean isInsideAtomicBlock() {
        return atomicVariableUniqueName.isPresent();
    }

    /**
     * Get the unique name of the variable created to store the result of the
     * atomic start function call at the beginning of the block executed
     * atomically that the node is nested in. If the node is not located inside
     * an atomic block, the object is absent.
     *
     * @return Unique name of the atomic variable.
     */
    public Optional<String> getAtomicVariableUniqueName() {
        return atomicVariableUniqueName;
    }

    /**
     * Check if the node is located inside a loop or switch.
     *
     * @return <code>true</code> if and only if the node is located inside
     *         a 'while', 'for', 'do' or 'switch' statement.
     */
    public boolean isInsideLoopOrSwitch() {
        return insideLoopOrSwitch;
    }

    /**
     * Check if this node is located inside an atomic statement in a loop
     * or switch not executed atomically.
     *
     * @return <code>true</code> if and only if the node is located inside
     *         a block executed atomically and simultaneously inside a 'while',
     *         'for', 'do' or 'switch' statement not nested inside an atomic
     *         block.
     */
    public boolean isInsideBreakableAtomic() {
        return insideBreakableAtomic;
    }

    /**
     * Get the return type of the function the node is nested in.
     *
     * @return AST node that represents the returned type of the function the
     *         node is nested in. Object is absent if the node is not nested
     *         in a function.
     */
    public Optional<AstType> getFunctionReturnType() {
        return functionReturnType;
    }

    /**
     * Check if this node is located inside an area of the program (i.e. a node)
     * that is excluded from name mangling.
     *
     * @return <code>true</code> if and only if the node is located inside an
     *         area excluded from names mangling.
     */
    public boolean isInsideNotMangledArea() {
        return insideNotMangledArea;
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
        private Optional<String> atomicVariableUniqueName = Optional.absent();
        private Optional<AstType> functionReturnType = Optional.absent();
        private boolean insideLoopOrSwitch = false;
        private boolean insideBreakableAtomic = false;
        private boolean insideNotMangledArea = false;

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
            this.atomicVariableUniqueName = specimen.atomicVariableUniqueName;
            this.insideLoopOrSwitch = specimen.insideLoopOrSwitch;
            this.insideBreakableAtomic = specimen.insideBreakableAtomic;
            this.functionReturnType = specimen.functionReturnType;
            this.insideNotMangledArea = specimen.insideNotMangledArea;
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

        /**
         * Set the unique name of the atomic variable if the node is located
         * inside a block executed atomically. The argument may be
         * <code>null</code> if the node is not located in an atomic block.
         *
         * @param atomicVariableUniqueName Value to set.
         * @return <code>this</code>
         * @see BlockData#getAtomicVariableUniqueName
         */
        public Builder atomicVariableUniqueName(String atomicVariableUniqueName) {
            this.atomicVariableUniqueName = Optional.fromNullable(atomicVariableUniqueName);
            return this;
        }

        /**
         * Set the value indicating location of a node inside a loop or switch.
         *
         * @param insideLoopOrSwitch Value to set.
         * @return <code>this</code>
         * @see BlockData#isInsideLoopOrSwitch
         */
        public Builder insideLoopOrSwitch(boolean insideLoopOrSwitch) {
            this.insideLoopOrSwitch = insideLoopOrSwitch;
            return this;
        }

        /**
         * Set the value indicating location of a node inside a breakable atomic
         * block.
         *
         * @param insideBreakableAtomic Value to set.
         * @return <code>this</code>
         * @see BlockData#isInsideBreakableAtomic
         */
        public Builder insideBreakableAtomic(boolean insideBreakableAtomic) {
            this.insideBreakableAtomic = insideBreakableAtomic;
            return this;
        }

        /**
         * Set the return type of the function the node is nested in. The
         * argument may be <code>null</code> if the node is not nested in any
         * function.
         *
         * @param functionReturnType Return type of the function to set.
         * @return <code>this</code>
         */
        public Builder functionReturnType(AstType functionReturnType) {
            this.functionReturnType = Optional.fromNullable(functionReturnType);
            return this;
        }

        /**
         * Set if the node is located inside an area that is not mangled.
         *
         * @param insideNotMangledArea Value to set.
         * @return <code>this</code>
         */
        public Builder insideNotMangledArea(boolean insideNotMangledArea) {
            this.insideNotMangledArea = insideNotMangledArea;
            return this;
        }

        private void validate() {
            checkNotNull(moduleTable, "module table cannot be null");
            checkNotNull(atomicVariableUniqueName, "atomic variable unique name cannot be null");
            checkNotNull(functionReturnType, "function return type cannot be null");
        }

        public BlockData build() {
            validate();
            return new BlockData(this);
        }
    }
}
