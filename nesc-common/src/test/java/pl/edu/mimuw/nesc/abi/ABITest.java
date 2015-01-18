package pl.edu.mimuw.nesc.abi;

import java.io.IOException;
import java.net.URL;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.junit.Test;
import org.xml.sax.SAXException;

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
        checkABI(simpleAbi, false, 2, 2, 4, 4, 8, 8, 8, 8, 4, 4, 8, 8, 16, 16, 8, 8);
    }

    @Test
    public void testABIWithWhitespace() throws SAXException, ParserConfigurationException, IOException, XPathExpressionException {
        final ABI abiWithWhitespace = loadABI("abi/ABIWithWhitespace.xml");
        checkABI(abiWithWhitespace, true, 2, 4, 4, 8, 8, 16, 8, 16, 4, 8, 8, 16, 16, 32, 8, 16);
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

    private void checkABI(ABI abi, boolean isCharSigned, int sizeShort, int alignShort, int sizeInt,
                int alignInt, int sizeLong, int alignLong, int sizeLongLong, int alignLongLong,
                int sizeFloat, int alignFloat, int sizeDouble, int alignDouble, int sizeLongDouble,
                int alignLongDouble, int sizePointer, int alignPointer) {

        assertEquals(isCharSigned, abi.isCharSigned());
        assertEquals(sizeShort, abi.getShort().getSize());
        assertEquals(alignShort, abi.getShort().getAlignment());
        assertEquals(sizeInt, abi.getInt().getSize());
        assertEquals(alignInt, abi.getInt().getAlignment());
        assertEquals(sizeLong, abi.getLong().getSize());
        assertEquals(alignLong, abi.getLong().getAlignment());
        assertEquals(sizeLongLong, abi.getLongLong().getSize());
        assertEquals(alignLongLong, abi.getLongLong().getAlignment());
        assertEquals(sizeFloat, abi.getFloat().getSize());
        assertEquals(alignFloat, abi.getFloat().getAlignment());
        assertEquals(sizeDouble, abi.getDouble().getSize());
        assertEquals(alignDouble, abi.getDouble().getAlignment());
        assertEquals(sizeLongDouble, abi.getLongDouble().getSize());
        assertEquals(alignLongDouble, abi.getLongDouble().getAlignment());
        assertEquals(sizePointer, abi.getPointerType().getSize());
        assertEquals(alignPointer, abi.getPointerType().getAlignment());
    }
}
