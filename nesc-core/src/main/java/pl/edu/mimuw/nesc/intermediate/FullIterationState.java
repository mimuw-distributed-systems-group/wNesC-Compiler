package pl.edu.mimuw.nesc.intermediate;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.Map;
import pl.edu.mimuw.nesc.wiresgraph.IndexedNode;

/**
 * <p>Class that represents an iteration over all successors of a node.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class FullIterationState extends SuccessorIterationState<Map.Entry<Optional<ImmutableList<BigInteger>>, IndexedNode>> {
    FullIterationState(NodeWrapper nodeWrapper, Iterator<Map.Entry<Optional<ImmutableList<BigInteger>>, IndexedNode>> iterator) {
        super(nodeWrapper, iterator);
    }

    @Override
    <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
