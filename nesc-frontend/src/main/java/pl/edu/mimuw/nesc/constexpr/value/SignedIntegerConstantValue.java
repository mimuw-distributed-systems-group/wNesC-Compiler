package pl.edu.mimuw.nesc.constexpr.value;

import java.math.BigInteger;
import pl.edu.mimuw.nesc.constexpr.value.decode.TwosComplementDecoder;
import pl.edu.mimuw.nesc.constexpr.value.type.SignedIntegerConstantType;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class SignedIntegerConstantValue extends IntegerConstantValue<SignedIntegerConstantValue> {
    /**
     * Type of this signed integer constant.
     */
    private final SignedIntegerConstantType type;

    /**
     * Create a constant of given value and type.
     *
     * @param value Value of the constant.
     * @param type Type of this constant.
     * @throws NullPointerException One of the arguments is <code>null</code>.
     * @throws IllegalArgumentException The given value is out of range of the
     *                                  given type.
     */
    public SignedIntegerConstantValue(BigInteger value, SignedIntegerConstantType type) {
        super(value, new TwosComplementDecoder(type.getBitsCount()));
        checkNotNull(type, "type of the constant cannot be null");
        checkArgument(type.getRange().contains(value), "the given value is out of range of the given type");
        this.type = type;
    }

    @Override
    public SignedIntegerConstantType getType() {
        return type;
    }

    @Override
    public SignedIntegerConstantValue newValue(BigInteger value) {
        return new SignedIntegerConstantValue(value, getType());
    }
}
