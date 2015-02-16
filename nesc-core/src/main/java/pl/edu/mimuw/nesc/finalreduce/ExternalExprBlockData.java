package pl.edu.mimuw.nesc.finalreduce;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.gen.CompoundStmt;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Class that contains block information necessary during the transformation
 * of expressions of an external base type. Its objects are immutable.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class ExternalExprBlockData {
    private final Optional<CompoundStmt> enclosingBlock;

    /**
     * Initialize the data for traversal that starts outside any function.
     */
    public ExternalExprBlockData() {
        this(Optional.<CompoundStmt>absent());
    }

    private ExternalExprBlockData(Optional<CompoundStmt> enclosingBlock) {
        this.enclosingBlock = enclosingBlock;
    }

    /**
     * Get the most nested non-atomic block that the expression is located
     * in.
     *
     * @return The most nested non-atomic block. The object is absent if the
     *         expression is located outside any compound statements.
     */
    Optional<CompoundStmt> getEnclosingBlock() {
        return enclosingBlock;
    }

    /**
     * Create a copy of this data object that contains the given enclosing
     * block instead of the block in this instance. All other data is unchanged.
     *
     * @param stmt New enclosing block.
     * @return New instance of an external block data that has the given block
     *         as the enclosing block as the only change.
     */
    ExternalExprBlockData modifyEnclosingBlock(CompoundStmt stmt) {
        return new ExternalExprBlockData(Optional.of(stmt));
    }
}
