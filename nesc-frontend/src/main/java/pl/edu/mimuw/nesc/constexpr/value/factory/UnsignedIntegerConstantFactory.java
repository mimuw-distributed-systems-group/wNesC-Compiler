package pl.edu.mimuw.nesc.constexpr.value.factory;

import java.math.BigInteger;
import pl.edu.mimuw.nesc.constexpr.value.UnsignedIntegerConstantValue;
import pl.edu.mimuw.nesc.constexpr.value.type.UnsignedIntegerConstantType;

/**
 * <p>Implementation of integer constant factory for unsigned integer values.
 * It follows the singleton pattern.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class UnsignedIntegerConstantFactory implements IntegerConstantFactory {
    /**
     * The only instance of this class.
     */
    private static final UnsignedIntegerConstantFactory INSTANCE = new UnsignedIntegerConstantFactory();

    /**
     * Get the only instance of this class.
     *
     * @return The only instance of this class.
     */
    public static UnsignedIntegerConstantFactory getInstance() {
        return INSTANCE;
    }

    /**
     * Private constructor for the singleton pattern.
     */
    private UnsignedIntegerConstantFactory() {
    }

    @Override
    public UnsignedIntegerConstantType newType(int bitsCount) {
        return new UnsignedIntegerConstantType(bitsCount);
    }

    @Override
    public UnsignedIntegerConstantValue newValue(String value, int bitsCount) {
        return new UnsignedIntegerConstantValue(new BigInteger(value), newType(bitsCount));
    }
}
