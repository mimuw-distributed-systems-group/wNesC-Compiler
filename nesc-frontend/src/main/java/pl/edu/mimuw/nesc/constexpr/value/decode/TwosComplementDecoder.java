package pl.edu.mimuw.nesc.constexpr.value.decode;

import java.math.BigInteger;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Decoder that interprets bits as a value encoded in two's complement
 * representation.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class TwosComplementDecoder extends AbstractDecoder {
    /**
     * Masks for extending the sign bit in the middle byte of the
     * representation.
     */
    private static final byte[] POSITIVE_MASK = { -1, 1, 3, 7, 15, 31, 63, 127 };
    private static final byte[] NEGATIVE_MASK = { 0, -2, -4, -8, -16, -32, -64, -128 };
    private static final byte[] SIGN_BIT_SHIFT = { 7, 0, 1, 2, 3, 4, 5, 6 };

    public TwosComplementDecoder(int bitsCount) {
        super(bitsCount);
    }

    @Override
    public BigInteger decode(byte[] bits) {
        checkNotNull(bits, "array of bits cannot be null");
        checkArgument(bits.length >=  getBytesCount(), "the given array contains too less elements (expected at least %s element(s))",
                getBytesCount());

        final int middleByteIndex = bits.length - getBytesCount();
        final int valueBitsCount = bitsCount() % 8;
        final boolean signBitCleared =
                (bits[middleByteIndex] >>> SIGN_BIT_SHIFT[valueBitsCount] & 1) == 0;

        // Make sign extension

        if (signBitCleared) {
            bits[middleByteIndex] &= POSITIVE_MASK[valueBitsCount];
        } else {
            bits[middleByteIndex] |= NEGATIVE_MASK[valueBitsCount];
        }

        final byte signByte = signBitCleared
                ? 0
                : (byte) -128;
        Arrays.fill(bits, 0, middleByteIndex, signByte);

        return new BigInteger(bits);
    }
}
