package pl.edu.mimuw.nesc.fold;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;
import pl.edu.mimuw.nesc.ast.gen.CharacterCst;
import pl.edu.mimuw.nesc.ast.gen.ConstantFunctionCall;
import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.gen.IntegerCst;
import pl.edu.mimuw.nesc.ast.gen.NullVisitor;
import pl.edu.mimuw.nesc.ast.gen.StringAst;
import pl.edu.mimuw.nesc.ast.gen.StringCst;
import pl.edu.mimuw.nesc.ast.gen.UniqueCall;
import pl.edu.mimuw.nesc.ast.gen.UniqueCountCall;
import pl.edu.mimuw.nesc.ast.gen.UniqueNCall;
import pl.edu.mimuw.nesc.ast.util.AstUtils;
import pl.edu.mimuw.nesc.ast.util.PrettyPrint;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

/**
 * <p>A class responsible for evaluating the following NesC constant functions:
 * </p>
 *
 * <ul>
 *     <li><code>unique</code></li>
 *     <li><code>uniqueN</code></li>
 * </ul>
 *
 * <p>It also sets {@link pl.edu.mimuw.nesc.ast.gen.ConstantFunctionCall#identifier}
 * for calls to all constant functions. Values of these functions are written to
 * fields {@link pl.edu.mimuw.nesc.ast.gen.ConstantFunctionCall#value}. AST
 * nodes with this field set to a non-null value are ignored.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class ValuesProcessor extends NullVisitor<Void, Void> {
    /**
     * Logger for unique value processors.
     */
    private static final Logger LOG = Logger.getLogger(ValuesProcessor.class);

    /**
     * Map with the next unique value for every identifier.
     */
    private final Map<String, Long> counters = new HashMap<>();

    @Override
    public Void visitUniqueCall(UniqueCall uniqueCall, Void arg) {
        // Return if this 'unique' has been already evaluated
        if (uniqueCall.getValue() != null) {
            return null;
        }

        retrieveIdentifier(uniqueCall);
        evaluate(uniqueCall, 1);

        return null;
    }

    @Override
    public Void visitUniqueNCall(UniqueNCall uniqueNCall, Void arg) {
        // FIXME evaluating all kinds of constant expression as the 2nd argument

        // Return if this 'uniqueN' usage has been already evaluated
        if (uniqueNCall.getValue() != null) {
            return null;
        }

        // Get the numbers count
        final Expression sndArg = uniqueNCall.getArguments().getLast();
        final long numbersCount;
        if (sndArg instanceof IntegerCst) {
            final IntegerCst integerCst = (IntegerCst) sndArg;
            numbersCount = integerCst.getValue().get().longValue();
        } else if (sndArg instanceof  CharacterCst) {
            final CharacterCst charCst = (CharacterCst) sndArg;
            numbersCount = (int) charCst.getValue().get();
        } else {
            throw new IllegalStateException("unexpected class of the 2nd parameter for 'uniqueN': '"
                    + sndArg.getClass() + "'");
        }

        retrieveIdentifier(uniqueNCall);
        evaluate(uniqueNCall, numbersCount);

        return null;
    }

    @Override
    public Void visitUniqueCountCall(UniqueCountCall uniqueCountCall, Void arg) {
        if (uniqueCountCall.getIdentifier() != null) {
            return null;
        }

        retrieveIdentifier(uniqueCountCall);

        // this unique processor does not evaluate 'uniqueCount'

        return null;
    }

    /**
     * Sets {@link pl.edu.mimuw.nesc.ast.gen.ConstantFunctionCall#identifier}
     * for the given parameter using the first argument in the given constant
     * function call AST.
     *
     * @param constantFunCall Constant function call to retrieve the identifier
     *                        from.
     */
    private void retrieveIdentifier(ConstantFunctionCall constantFunCall) {
        final Expression firstParam = constantFunCall.getArguments().getFirst();
        checkState(firstParam instanceof StringAst, "identifier for a constant function has unexpected class '%s'",
                firstParam.getClass());

        constantFunCall.setIdentifier(AstUtils.concatenateStrings((StringAst) firstParam));
    }

    private void evaluate(ConstantFunctionCall constantFunCall, long numbersCount) {
        // Set the value of the call
        final String identifier = constantFunCall.getIdentifier();
        final long value = Optional.fromNullable(counters.get(identifier))
                .or(0L);
        constantFunCall.setValue(value);

        // Update counters
        counters.put(identifier, value + numbersCount);

        // Log the evaluation
        if (LOG.isDebugEnabled()) {
            LOG.debug(format("Evaluate '%s' to %d allocating %d numbers",
                    PrettyPrint.expression(constantFunCall), value,
                    numbersCount));
        }
    }

    /**
     * <p>Get the counts of numbers allocated for every identifier.</p>
     *
     * @return Map with identifiers mapped to counts of numbers allocated for
     *         them.
     */
    public ImmutableMap<String, Long> getCounters() {
        return ImmutableMap.copyOf(counters);
    }
}
