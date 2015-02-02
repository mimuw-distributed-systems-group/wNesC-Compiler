package pl.edu.mimuw.nesc.constexpr.value.decode;

import java.math.BigInteger;

/**
 * <p>Interface with operations of reading values in different
 * representations. Each class implementing this interface shall specify
 * a target representation that all values are interpreted in.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public interface Decoder {
    /**
     * Determine the count of all bits in the target representation. This method
     * shall always return the same number when invoked on the same instance of
     * an implementing class.
     *
     * @return Count of bits of the target representation.
     */
    int bitsCount();

    /**
     * <p>Let <code>N</code> be the number returned by
     * {@link Decoder#bitsCount}. This method interprets <code>N</code> least
     * significant bits from the given array in the target representation and
     * returns the result of the interpretation as a big integer.</p>
     *
     * <p>The bytes in the given array are assumed to be in big-endian order.
     * The least significant bit is the bit that is the least significant bit
     * of bits[bits.length - 1]. The most significant bit is the bit that is the
     * most significant bit of bits[0].</p>
     *
     * <p>The given array of bits may be changed after the call.</p>
     *
     * @param bits Bits that will be interpreted.
     * @return Interpreted value of the given bits.
     * @throws NullPointerException <code>bits</code> is <code>null</code>.
     * @throws IllegalArgumentException <code>bits * 8</code> is less than the
     *                                 number returned by <code>bitsCount</code>
     *                                 method.
     */
    BigInteger decode(byte[] bits);

    /**
     * Uses the two's complement representation of the given number and
     * interprets it in the target representation (using
     * {@link Decoder#decode(byte[]) convert} method).
     *
     * @param n Number that will be encoded and then decoded.
     * @return Result of the conversion.
     * @throws NullPointerException <code>n</code> is <code>null</code>.
     */
    BigInteger decode(BigInteger n);
}
