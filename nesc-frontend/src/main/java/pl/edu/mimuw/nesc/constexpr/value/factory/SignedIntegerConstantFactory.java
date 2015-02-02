package pl.edu.mimuw.nesc.constexpr.value.factory;

import java.math.BigInteger;
import pl.edu.mimuw.nesc.constexpr.value.SignedIntegerConstantValue;
import pl.edu.mimuw.nesc.constexpr.value.type.SignedIntegerConstantType;

/**
 * <p>Implementation of integer constant factory for signed integer values.
 * It follows the singleton pattern.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class SignedIntegerConstantFactory implements IntegerConstantFactory {
    /**
     * The only instance of this class.
     */
    private static final SignedIntegerConstantFactory INSTANCE = new SignedIntegerConstantFactory();

    /**
     * Get the only instance of this class.
     *
     * @return The only instance of this class.
     */
    public static SignedIntegerConstantFactory getInstance() {
        return INSTANCE;
    }

    /**
     * Private constructor for the singleton pattern.
     */
    private SignedIntegerConstantFactory() {
    }

    @Override
    public SignedIntegerConstantType newType(int bitsCount) {
        return new SignedIntegerConstantType(bitsCount);
    }

    @Override
    public SignedIntegerConstantValue newValue(String value, int bitsCount) {
        return new SignedIntegerConstantValue(new BigInteger(value), newType(bitsCount));
    }
}
