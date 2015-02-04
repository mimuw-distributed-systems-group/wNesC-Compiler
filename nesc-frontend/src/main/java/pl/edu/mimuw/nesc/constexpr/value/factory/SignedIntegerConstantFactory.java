package pl.edu.mimuw.nesc.constexpr.value.factory;

import java.math.BigInteger;
import pl.edu.mimuw.nesc.constexpr.value.SignedIntegerConstantValue;
import pl.edu.mimuw.nesc.constexpr.value.type.SignedIntegerConstantType;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * <p>Implementation of integer constant factory for signed integer values.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class SignedIntegerConstantFactory implements IntegerConstantFactory {
    /**
     * Count of bits for the created types and value.
     */
    private final int bitsCount;

    public SignedIntegerConstantFactory(int bitsCount) {
        checkArgument(bitsCount > 0, "count of bits must be positive");
        this.bitsCount = bitsCount;
    }

    @Override
    public SignedIntegerConstantType newType() {
        return new SignedIntegerConstantType(this.bitsCount);
    }

    @Override
    public SignedIntegerConstantValue newValue(BigInteger value) {
        return new SignedIntegerConstantValue(value, newType());
    }

    @Override
    public SignedIntegerConstantValue newValue(String value) {
        return newValue(new BigInteger(value));
    }

    @Override
    public SignedIntegerConstantValue newValue(long value) {
        return newValue(BigInteger.valueOf(value));
    }
}
