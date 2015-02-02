package pl.edu.mimuw.nesc.constexpr.value.decode;

import java.math.BigInteger;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Skeletal implementation of a decoder.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public abstract class AbstractDecoder implements Decoder {
    /**
     * Count of bits of the target representation.
     */
    private final int bitsCount;

    /**
     * The smallest number of bytes necessary to represent the value in the
     * target representation. It is equal to ceil(bitsCount / 8).
     */
    private final int bytesCount;

    protected AbstractDecoder(int bitsCount) {
        checkArgument(bitsCount > 0, "count of bits must be positive");
        this.bitsCount = bitsCount;
        this.bytesCount = (bitsCount + 7) / 8;
    }

    protected int getBytesCount() {
        return bytesCount;
    }

    @Override
    public int bitsCount() {
        return bitsCount;
    }

    @Override
    public BigInteger decode(BigInteger n) {
        checkNotNull(n, "the number cannot be null");

        final byte[] sourceBits = n.toByteArray();
        final byte[] finalBits;

        // Do the sign extension if it is necessary
        if (sourceBits.length < bytesCount) {
            // First index that contains copied byte
            final int middleIndex = bytesCount - sourceBits.length;
            finalBits = new byte[bytesCount];
            System.arraycopy(sourceBits, 0, finalBits, middleIndex, sourceBits.length);
            final byte sign = n.signum() == -1
                    ? (byte) -1
                    : 0;
            Arrays.fill(finalBits, 0, middleIndex, sign);
        } else {
            finalBits = sourceBits;
        }

        return decode(finalBits);
    }
}
