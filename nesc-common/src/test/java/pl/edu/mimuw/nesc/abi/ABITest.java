package pl.edu.mimuw.nesc.abi;

import com.google.common.collect.Range;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.junit.Test;
import org.xml.sax.SAXException;
import pl.edu.mimuw.nesc.abi.typedata.SignedIntegerType;
import pl.edu.mimuw.nesc.abi.typedata.UnsignedIntegerType;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.Assert.*;

/**
 * Test for {@link ABI} class.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class ABITest {
    @Test
    public void testSimpleLoad() throws SAXException, ParserConfigurationException, IOException, XPathExpressionException {
        final ABI simpleAbi = loadABI("abi/simpleABI.xml");
        checkEndianness(simpleAbi, Endianness.LITTLE_ENDIAN);

        checkChar(
                simpleAbi,
                false,
                Range.closed(BigInteger.valueOf(-128L), BigInteger.valueOf(127)),
                Range.closed(BigInteger.ZERO, BigInteger.valueOf(255L))
        );

        checkShort(
                simpleAbi,
                2, 2,
                Range.closed(BigInteger.valueOf(-32768L), BigInteger.valueOf(32767L)),
                Range.closed(BigInteger.ZERO, BigInteger.valueOf(65535L))
        );

        checkInt(
                simpleAbi,
                4, 4,
                Range.closed(BigInteger.valueOf(-2147483648L), BigInteger.valueOf(2147483647)),
                Range.closed(BigInteger.ZERO, BigInteger.valueOf(4294967295L))
        );

        checkLong(
                simpleAbi,
                8, 8,
                Range.closed(BigInteger.valueOf(-9223372036854775808L), BigInteger.valueOf(9223372036854775807L)),
                Range.closed(BigInteger.ZERO, BigInteger.valueOf(2L).pow(64).subtract(BigInteger.ONE))
        );

        checkLongLong(
                simpleAbi,
                8, 8,
                Range.closed(BigInteger.valueOf(-9223372036854775808L), BigInteger.valueOf(9223372036854775807L)),
                Range.closed(BigInteger.ZERO, BigInteger.valueOf(2L).pow(64).subtract(BigInteger.ONE))
        );

        checkFloatingTypes(simpleAbi, 4, 4, 8, 8, 16, 16);
        checkPointerType(simpleAbi, 8, 8);
        checkSpecialTypes(simpleAbi, UnsignedIntegerType.UNSIGNED_LONG, SignedIntegerType.LONG);
        checkFieldTagType(simpleAbi, 1, true, 8);
    }

    @Test
    public void testABIWithWhitespace() throws SAXException, ParserConfigurationException, IOException, XPathExpressionException {
        final ABI abiWithWhitespace = loadABI("abi/ABIWithWhitespace.xml");
        checkEndianness(abiWithWhitespace, Endianness.BIG_ENDIAN);

        checkChar(
                abiWithWhitespace,
                true,
                Range.closed(BigInteger.valueOf(-128L), BigInteger.valueOf(127)),
                Range.closed(BigInteger.ZERO, BigInteger.valueOf(255L))
        );

        checkShort(
                abiWithWhitespace,
                2, 4,
                Range.closed(BigInteger.valueOf(-32768L), BigInteger.valueOf(32767L)),
                Range.closed(BigInteger.ZERO, BigInteger.valueOf(65535L))
        );

        checkInt(
                abiWithWhitespace,
                4, 8,
                Range.closed(BigInteger.valueOf(-2147483648L), BigInteger.valueOf(2147483647)),
                Range.closed(BigInteger.ZERO, BigInteger.valueOf(4294967295L))
        );

        checkLong(
                abiWithWhitespace,
                8, 16,
                Range.closed(BigInteger.valueOf(-9223372036854775808L), BigInteger.valueOf(9223372036854775807L)),
                Range.closed(BigInteger.ZERO, BigInteger.valueOf(2L).pow(64).subtract(BigInteger.ONE))
        );

        checkLongLong(
                abiWithWhitespace,
                8, 16,
                Range.closed(BigInteger.valueOf(-9223372036854775808L), BigInteger.valueOf(9223372036854775807L)),
                Range.closed(BigInteger.ZERO, BigInteger.valueOf(2L).pow(64).subtract(BigInteger.ONE))
        );

        checkFloatingTypes(abiWithWhitespace, 4, 8, 8, 16, 16, 32);
        checkPointerType(abiWithWhitespace, 8, 16);
        checkSpecialTypes(abiWithWhitespace, UnsignedIntegerType.UNSIGNED_LONG_LONG, SignedIntegerType.SIGNED_CHAR);
        checkFieldTagType(abiWithWhitespace, 2, false, 16);
    }

    @Test(expected=org.xml.sax.SAXException.class)
    public void testInvalidIsSigned() throws SAXException, ParserConfigurationException, IOException, XPathExpressionException{
        loadABI("abi/invalidIsSigned.xml");
    }

    @Test(expected=org.xml.sax.SAXException.class)
    public void testInvalidZeroSize() throws SAXException, ParserConfigurationException, IOException, XPathExpressionException {
        loadABI("abi/zeroSize.xml");
    }

    @Test(expected=org.xml.sax.SAXException.class)
    public void testInvalidNegativeAlignment() throws SAXException, ParserConfigurationException, IOException, XPathExpressionException {
        loadABI("abi/negativeAlignment.xml");
    }

    @Test(expected=org.xml.sax.SAXException.class)
    public void testInvalidInteger() throws SAXException, ParserConfigurationException, IOException, XPathExpressionException {
        loadABI("abi/invalidInteger.xml");
    }

    private ABI loadABI(String resourceFileName) throws SAXException, ParserConfigurationException,
            IOException, XPathExpressionException {
        final URL resourceURL = Thread.currentThread().getContextClassLoader()
                .getResource(resourceFileName);
        checkNotNull(resourceURL, "cannot find file: %s", resourceFileName);
        return new ABI(resourceURL.getFile());
    }

    private void checkEndianness(ABI abi, Endianness endianness) {
        assertEquals(endianness, abi.getEndianness());
    }

    private void checkChar(ABI abi, boolean isCharSigned,
            Range<BigInteger> rangeSignedChar, Range<BigInteger> rangeUnsignedChar) {
        assertEquals(isCharSigned, abi.getChar().isSigned());
        assertEquals(rangeSignedChar, abi.getChar().getSignedRange());
        assertEquals(rangeUnsignedChar, abi.getChar().getUnsignedRange());
    }

    private void checkShort(ABI abi, int sizeShort, int alignShort,
            Range<BigInteger> rangeSignedShort,
            Range<BigInteger> rangeUnsignedShort) {
        assertEquals(sizeShort, abi.getShort().getSize());
        assertEquals(alignShort, abi.getShort().getAlignment());
        assertEquals(rangeSignedShort, abi.getShort().getSignedRange());
        assertEquals(rangeUnsignedShort, abi.getShort().getUnsignedRange());
    }

    private void checkInt(ABI abi, int sizeInt, int alignInt,
            Range<BigInteger> rangeSignedInt,
            Range<BigInteger> rangeUnsignedInt) {
        assertEquals(sizeInt, abi.getInt().getSize());
        assertEquals(alignInt, abi.getInt().getAlignment());
        assertEquals(rangeSignedInt, abi.getInt().getSignedRange());
        assertEquals(rangeUnsignedInt, abi.getInt().getUnsignedRange());
    }

    private void checkLong(ABI abi, int sizeLong, int alignLong,
            Range<BigInteger> rangeSignedLong, Range<BigInteger> rangeUnsignedLong) {
        assertEquals(sizeLong, abi.getLong().getSize());
        assertEquals(alignLong, abi.getLong().getAlignment());
        assertEquals(rangeSignedLong, abi.getLong().getSignedRange());
        assertEquals(rangeUnsignedLong, abi.getLong().getUnsignedRange());
    }

    private void checkLongLong(ABI abi, int sizeLongLong, int alignLongLong,
            Range<BigInteger> rangeSignedLongLong, Range<BigInteger> rangeUnsignedLongLong) {
        assertEquals(sizeLongLong, abi.getLongLong().getSize());
        assertEquals(alignLongLong, abi.getLongLong().getAlignment());
        assertEquals(rangeSignedLongLong, abi.getLongLong().getSignedRange());
        assertEquals(rangeUnsignedLongLong, abi.getLongLong().getUnsignedRange());
    }

    private void checkFloatingTypes(ABI abi, int sizeFloat, int alignFloat,
            int sizeDouble, int alignDouble, int sizeLongDouble,
            int alignLongDouble) {
        assertEquals(sizeFloat, abi.getFloat().getSize());
        assertEquals(alignFloat, abi.getFloat().getAlignment());
        assertEquals(sizeDouble, abi.getDouble().getSize());
        assertEquals(alignDouble, abi.getDouble().getAlignment());
        assertEquals(sizeLongDouble, abi.getLongDouble().getSize());
        assertEquals(alignLongDouble, abi.getLongDouble().getAlignment());
    }

    private void checkPointerType(ABI abi, int sizePointer, int alignPointer) {
        assertEquals(sizePointer, abi.getPointerType().getSize());
        assertEquals(alignPointer, abi.getPointerType().getAlignment());
    }

    private void checkSpecialTypes(ABI abi, UnsignedIntegerType sizeT, SignedIntegerType ptrdiffT) {
        assertEquals(sizeT, abi.getSizeT());
        assertEquals(ptrdiffT, abi.getPtrdiffT());
    }

    private void checkFieldTagType(ABI abi, int minimumAlignment, boolean bitFieldTypeMatters,
                int emptyBitFieldAlignment) {
        assertEquals(bitFieldTypeMatters, abi.getFieldTagType().bitFieldTypeMatters());
        assertEquals(emptyBitFieldAlignment, abi.getFieldTagType().getEmptyBitFieldAlignmentInBits());
        assertEquals(minimumAlignment, abi.getFieldTagType().getMinimumAlignment());
    }
}
