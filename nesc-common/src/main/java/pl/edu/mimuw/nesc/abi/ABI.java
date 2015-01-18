package pl.edu.mimuw.nesc.abi;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.XMLConstants;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * <p>Class that contains important information about ABI (Application Binary
 * Interface).</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class ABI {
    /**
     * Path to the file that contains the XML schema for specifying ABI.
     */
    private static final String ABI_SCHEMA_FILENAME = "schemas/abi.xsd";

    /**
     * Data about the ABI.
     */
    private final boolean isCharSigned;
    private final TypeData typeShort;
    private final TypeData typeInt;
    private final TypeData typeLong;
    private final TypeData typeLongLong;
    private final TypeData typeFloat;
    private final TypeData typeDouble;
    private final TypeData typeLongDouble;
    private final TypeData typePointer;

    /**
     * Load information about the ABI from an XML file with given path. The file
     * should validate against the ABI XML Schema.
     *
     * @param xmlFileName Name of the file with data about the ABI.
     */
    public ABI(String xmlFileName) throws SAXException, ParserConfigurationException,
            IOException, XPathExpressionException {
        checkNotNull(xmlFileName, "name of the XML file cannot be null");
        checkArgument(!xmlFileName.isEmpty(), "name of the XML file cannot be an empty string");

        final Builder builder = new Builder(xmlFileName);
        builder.parse();

        this.isCharSigned = builder.buildIsCharSigned();
        this.typeShort = builder.buildShortTypeData();
        this.typeInt = builder.buildIntTypeData();
        this.typeLong = builder.buildLongTypeData();
        this.typeLongLong = builder.buildLongLongTypeData();
        this.typeFloat = builder.buildFloatTypeData();
        this.typeDouble = builder.buildDoubleTypeData();
        this.typeLongDouble = builder.buildLongDoubleTypeData();
        this.typePointer = builder.buildPointerTypeData();
    }

    public boolean isCharSigned() {
        return isCharSigned;
    }

    public TypeData getShort() {
        return typeShort;
    }

    public TypeData getInt() {
        return typeInt;
    }

    public TypeData getLong() {
        return typeLong;
    }

    public TypeData getLongLong() {
        return typeLongLong;
    }

    public TypeData getFloat() {
        return typeFloat;
    }

    public TypeData getDouble() {
        return typeDouble;
    }

    public TypeData getLongDouble() {
        return typeLongDouble;
    }

    public TypeData getPointerType() {
        return typePointer;
    }

    /**
     * Structure with information about a type other than a character type.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static final class TypeData {
        private final int size;
        private final int alignment;

        private TypeData(int size, int alignment) {
            checkArgument(size >= 1, "size must be positive");
            checkArgument(alignment >= 1, "alignment must be positive");

            this.size = size;
            this.alignment = alignment;
        }

        /**
         * Get the size of the type in bytes.
         *
         * @return Size of the type in bytes.
         */
        public int getSize() {
            return size;
        }

        /**
         * Get the alignment of the type in bytes.
         *
         * @return Alignment of the type in bytes.
         */
        public int getAlignment() {
            return alignment;
        }
    }

    /**
     * Object that will load all necessary data from an XML file. The builder
     * implements {@link ErrorHandler} interface to handle errors of the syntax
     * and structure of XML file.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class Builder implements ErrorHandler, NamespaceContext {
        /**
         * Map with prefixes to handle namespaces in XPath queries.
         */
        private static final ImmutableMap<String, String> PREFIXES = ImmutableMap.of(
                "abi", "http://mimuw.edu.pl/nesc/abi",
                XMLConstants.XML_NS_PREFIX, XMLConstants.XML_NS_URI,
                XMLConstants.XMLNS_ATTRIBUTE, XMLConstants.XMLNS_ATTRIBUTE_NS_URI
        );

        /**
         * Name of the XML file that will be loaded.
         */
        private final String xmlFileName;

        /**
         * Object for making XPath queries on the loaded document.
         */
        private final XPath xpath;

        /**
         * The root node of the loaded XML file with ABI.
         */
        private Document root;

        private Builder(String xmlFileName) {
            this.xmlFileName = xmlFileName;

            this.xpath = XPathFactory.newInstance().newXPath();
            this.xpath.setNamespaceContext(this);
        }

        private void parse() throws SAXException, ParserConfigurationException, IOException {
            // Load the XML schema
            final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            final InputStream schemaInput = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(ABI_SCHEMA_FILENAME);
            checkNotNull(schemaInput, "Cannot find XML schema: %s", ABI_SCHEMA_FILENAME);
            final Schema abiSchema = schemaFactory.newSchema(new StreamSource(schemaInput));

            // Create the parser for the XML file with ABI data
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setSchema(abiSchema);
            final DocumentBuilder parser = factory.newDocumentBuilder();
            parser.setErrorHandler(this);

            // Parse the XML file
            this.root = parser.parse(new FileInputStream(xmlFileName));
        }

        private boolean buildIsCharSigned() throws XPathExpressionException {
            final Element isSignedElement = (Element) xpath.evaluate("/abi:abi/abi:types/abi:char/abi:is-signed",
                    root, XPathConstants.NODE);
            checkNotNull(isSignedElement, "cannot find the node that specifies if char is signed");
            final String text = extractText(isSignedElement);

            if (text.equals("true") || text.equals("1")) {
                return true;
            } else if (text.equals("false") || text.equals("0")) {
                return false;
            } else {
                throw new RuntimeException("value '" + text + "' in an element of type 'boolean'");
            }
        }

        private TypeData buildShortTypeData() throws XPathExpressionException {
            return retrieveTypeData("short");
        }

        private TypeData buildIntTypeData() throws XPathExpressionException {
            return retrieveTypeData("int");
        }

        private TypeData buildLongTypeData() throws XPathExpressionException {
            return retrieveTypeData("long");
        }

        private TypeData buildLongLongTypeData() throws XPathExpressionException {
            return retrieveTypeData("long-long");
        }

        private TypeData buildFloatTypeData() throws XPathExpressionException {
            return retrieveTypeData("float");
        }

        private TypeData buildDoubleTypeData() throws XPathExpressionException {
            return retrieveTypeData("double");
        }

        private TypeData buildLongDoubleTypeData() throws XPathExpressionException {
            return retrieveTypeData("long-double");
        }

        private TypeData buildPointerTypeData() throws XPathExpressionException {
            return retrieveTypeData("pointer-type");
        }

        private TypeData retrieveTypeData(String typeElementName) throws XPathExpressionException {
            final Element sizeNode = (Element) xpath.evaluate(format("/abi:abi/abi:types/abi:%s/abi:size", typeElementName),
                    root, XPathConstants.NODE);
            final Element alignmentNode = (Element) xpath.evaluate(format("/abi:abi/abi:types/abi:%s/abi:alignment", typeElementName),
                    root, XPathConstants.NODE);
            checkNotNull(sizeNode, "cannot find the size element of a type");
            checkNotNull(alignmentNode, "cannot find the alignment element of a type");

            return new TypeData(Integer.parseInt(extractText(sizeNode)),
                    Integer.parseInt(extractText(alignmentNode)));
        }

        private String extractText(Element parent) {
            final NodeList children = parent.getChildNodes();
            final StringBuilder builder = new StringBuilder();

            for (int i = 0; i < children.getLength(); ++i) {
                if (children.item(i).getNodeType() != Node.TEXT_NODE) {
                    continue;
                }

                final Text textNode = (Text) children.item(i);
                builder.append(textNode.getWholeText());
            }

            return builder.toString();
        }

        @Override
        public void warning(SAXParseException e) {
            // warnings are ignored
        }

        @Override
        public void error(SAXParseException e) throws SAXException {
            throw e;
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            throw e;
        }

        @Override
        public String getNamespaceURI(String prefix) {
            checkArgument(prefix != null, "prefix cannot be null");
            return Optional.fromNullable(PREFIXES.get(prefix)).or(XMLConstants.NULL_NS_URI);
        }

        @Override
        public String getPrefix(String namespaceURI) {
            checkArgument(namespaceURI != null, "namespace URI cannot be null");

            for (Map.Entry<String, String> entry : PREFIXES.entrySet()) {
                if (entry.getValue().equals(namespaceURI)) {
                    return entry.getKey();
                }
            }

            return null;
        }

        @Override
        public Iterator<String> getPrefixes(String namespaceURI) {
            checkArgument(namespaceURI != null, "namespace URI cannot be null");
            final Collection<String> prefixes = new HashSet<>();

            for (Map.Entry<String, String> entry : PREFIXES.entrySet()) {
                if (entry.getValue().equals(namespaceURI)) {
                    prefixes.add(entry.getKey());
                }
            }

            return prefixes.iterator();
        }
    }
}
