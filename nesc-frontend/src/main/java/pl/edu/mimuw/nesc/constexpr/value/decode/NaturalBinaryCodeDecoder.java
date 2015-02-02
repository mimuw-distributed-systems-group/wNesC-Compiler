package pl.edu.mimuw.nesc.constexpr.value.decode;

import java.math.BigInteger;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Decoder that interprets bits as a value in the natural binary code (and
 * it is the target representation).</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class NaturalBinaryCodeDecoder extends AbstractDecoder {
    /**
     * Masks for clearing bits.
     */
    private static final byte[] MASK = { -1, 1, 3, 7, 15, 31, 63, 127 };

    public NaturalBinaryCodeDecoder(int bitsCount) {
        super(bitsCount);
    }

    @Override
    public BigInteger decode(byte[] bits) {
        checkNotNull(bits, "bits array cannot be null");
        checkArgument(bits.length >= getBytesCount(), "the array contains too less elements (at least %s element(s) expected)",
                getBytesCount());

        final int middleByteIndex = bits.length - getBytesCount();
        final int valueBitsCount = bitsCount() % 8;

        bits[middleByteIndex] &= MASK[valueBitsCount];
        Arrays.fill(bits, 0, middleByteIndex, (byte) 0);

        final byte[] preparedBits;

        if (bits[0] >>> 7 == 1) {
            preparedBits = new byte[bits.length + 1];
            System.arraycopy(bits, 0, preparedBits, 1, bits.length);
            preparedBits[0] = 0;
        } else {
            preparedBits = bits;
        }

        return new BigInteger(preparedBits);
    }
}
