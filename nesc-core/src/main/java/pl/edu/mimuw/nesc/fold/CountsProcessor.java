package pl.edu.mimuw.nesc.fold;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import org.apache.log4j.Logger;
import pl.edu.mimuw.nesc.ast.gen.NullVisitor;
import pl.edu.mimuw.nesc.ast.gen.UniqueCountCall;
import pl.edu.mimuw.nesc.astwriting.ASTWriter;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class CountsProcessor extends NullVisitor<Void, Void> {
    /**
     * Logger for counts processors.
     */
    private static final Logger LOG = Logger.getLogger(CountsProcessor.class);

    /**
     * Map with counts of numbers allocated for every identifier.
     */
    private final ImmutableMap<String, Long> counters;

    /**
     * <p>Initialize a counts processor by storing given map in a member field.
     * </p>
     *
     * @param counters Map with counts of used numbers for every identifier.
     * @throws NullPointerException 'counters' parameter is null.
     */
    public CountsProcessor(ImmutableMap<String, Long> counters) {
        checkNotNull(counters, "counter cannot be null");
        this.counters = counters;
    }

    @Override
    public Void visitUniqueCountCall(UniqueCountCall uniqueCountCall, Void arg) {
        if (uniqueCountCall.getValue() != null) {
            return null;
        }

        final String identifier = uniqueCountCall.getIdentifier();
        final long value = Optional.fromNullable(counters.get(identifier))
                .or(0L);
        uniqueCountCall.setValue(value);

        if (LOG.isDebugEnabled()) {
            LOG.debug(format("Evaluate '%s' to %d", ASTWriter.writeToString(uniqueCountCall), value));
        }

        return null;
    }
}
