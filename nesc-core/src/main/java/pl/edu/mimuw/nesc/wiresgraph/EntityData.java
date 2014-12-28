package pl.edu.mimuw.nesc.wiresgraph;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;

/**
 * <p>Information about the command or event that corresponds to a node in the
 * wires graph.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public abstract class EntityData {
    /**
     * Check if the command or event is a provided command or event by a module.
     *
     * @return <code>true</code> if and only if the command or event is provided
     *         by a module. If so it can be safely casted to
     *         {@link SinkFunctionData}. Otherwise, it will be
     *         {@link IntermediateFunctionData}.
     */
    public abstract boolean isImplemented();
}
