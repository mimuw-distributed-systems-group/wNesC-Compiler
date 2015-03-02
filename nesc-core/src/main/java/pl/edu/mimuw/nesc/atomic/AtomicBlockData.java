package pl.edu.mimuw.nesc.atomic;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.gen.AstType;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Class of immutable objects that contain block information necessary for
 * performing the atomic transformation.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class AtomicBlockData {
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
     * Get an atomic block data object that contains initial information that
     * is suitable for traversing a top-level declaration, e.g. a function
     * definition or global declarations.
     *
     * @return Newly created atomic block data object with initial state
     *         suitable for traversing a top-level declaration.
     */
    public static AtomicBlockData newInitialData() {
        return builder().build();
    }

    /**
     * Get a new builder that will build a block data object.
     *
     * @return Newly created builder that will build a block data object.
     */
    static Builder builder() {
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
    static Builder builder(AtomicBlockData specimen) {
        checkNotNull(specimen, "the speciman block data cannot be null");
        return new Builder(specimen);
    }

    private AtomicBlockData(Builder builder) {
        this.atomicVariableUniqueName = builder.atomicVariableUniqueName;
        this.insideLoopOrSwitch = builder.insideLoopOrSwitch;
        this.insideBreakableAtomic = builder.insideBreakableAtomic;
        this.functionReturnType = builder.functionReturnType;
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
     * Builder for an atomic block data object.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    static final class Builder {
        /**
         * Data needed to build a block data object.
         */
        private Optional<String> atomicVariableUniqueName = Optional.absent();
        private Optional<AstType> functionReturnType = Optional.absent();
        private boolean insideLoopOrSwitch = false;
        private boolean insideBreakableAtomic = false;

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
        private Builder(AtomicBlockData specimen) {
            this.atomicVariableUniqueName = specimen.atomicVariableUniqueName;
            this.insideLoopOrSwitch = specimen.insideLoopOrSwitch;
            this.insideBreakableAtomic = specimen.insideBreakableAtomic;
            this.functionReturnType = specimen.functionReturnType;
        }

        /**
         * Set the unique name of the atomic variable if the node is located
         * inside a block executed atomically. The argument may be
         * <code>null</code> if the node is not located in an atomic block.
         *
         * @param atomicVariableUniqueName Value to set.
         * @return <code>this</code>
         * @see AtomicBlockData#getAtomicVariableUniqueName
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
         * @see AtomicBlockData#isInsideLoopOrSwitch
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
         * @see AtomicBlockData#isInsideBreakableAtomic
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

        private void validate() {
            checkNotNull(atomicVariableUniqueName, "atomic variable unique name cannot be null");
            checkNotNull(functionReturnType, "function return type cannot be null");
        }

        public AtomicBlockData build() {
            validate();
            return new AtomicBlockData(this);
        }
    }
}
