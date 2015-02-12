package pl.edu.mimuw.nesc.constexpr.value;

import java.math.BigInteger;
import pl.edu.mimuw.nesc.constexpr.value.factory.IntegerConstantFactory;
import pl.edu.mimuw.nesc.constexpr.value.factory.SignedIntegerConstantFactory;
import pl.edu.mimuw.nesc.constexpr.value.factory.UnsignedIntegerConstantFactory;
import pl.edu.mimuw.nesc.constexpr.value.type.IntegerConstantType;

import static org.junit.Assert.assertEquals;

/**
 * <p>Class with assertion methods for testing constant values.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class ConstantValueAssert {
    public static void assertSignedIntAdd(String lhs, String rhs, String result, int bitsCount) {
        assertIntAdd(lhs, rhs, result, new SignedIntegerConstantFactory(bitsCount));
    }

    public static void assertUnsignedIntAdd(String lhs, String rhs, String result, int bitsCount) {
        assertIntAdd(lhs, rhs, result, new UnsignedIntegerConstantFactory(bitsCount));
    }

    public static void assertSignedIntSubtract(String lhs, String rhs, String result, int bitsCount) {
        assertIntSubtract(lhs, rhs, result, new SignedIntegerConstantFactory(bitsCount));
    }

    public static void assertUnsignedIntSubtract(String lhs, String rhs, String result, int bitsCount) {
        assertIntSubtract(lhs, rhs, result, new UnsignedIntegerConstantFactory(bitsCount));
    }

    private static void assertIntAdd(String lhs, String rhs, String result,
            IntegerConstantFactory factory) {
        final IntegerConstantType type = factory.newType();
        final IntegerConstantValue<?> valueLhs = factory.newValue(lhs);
        final IntegerConstantValue<?> valueRhs = factory.newValue(rhs);
        final IntegerConstantValue<?> valueResult = valueLhs.add(valueRhs);

        assertEquals(type, valueResult.getType());
        assertEquals(new BigInteger(result), valueResult.getValue());
    }

    private static void assertIntSubtract(String lhs, String rhs, String result,
            IntegerConstantFactory factory) {
        final IntegerConstantType type = factory.newType();
        final IntegerConstantValue<?> valueLhs = factory.newValue(lhs);
        final IntegerConstantValue<?> valueRhs = factory.newValue(rhs);
        final IntegerConstantValue<?> valueResult = valueLhs.subtract(valueRhs);

        assertEquals(type, valueResult.getType());
        assertEquals(new BigInteger(result), valueResult.getValue());
    }

    /**
     * Private constructor to limit its accessibility.
     */
    private ConstantValueAssert() {
    }
}
