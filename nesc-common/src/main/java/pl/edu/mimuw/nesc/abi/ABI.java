package pl.edu.mimuw.nesc.abi;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
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
import pl.edu.mimuw.nesc.abi.typedata.CharData;
import pl.edu.mimuw.nesc.abi.typedata.FieldTagTypeData;
import pl.edu.mimuw.nesc.abi.typedata.SignedIntegerType;
import pl.edu.mimuw.nesc.abi.typedata.StandardIntegerTypeData;
import pl.edu.mimuw.nesc.abi.typedata.TypeData;
import pl.edu.mimuw.nesc.abi.typedata.UnsignedIntegerType;

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
     * Map for retrieving signed standard integer types.
     */
    private static final ImmutableMap<String, SignedIntegerType> SIGNED_TYPES_MAP = ImmutableMap.of(
        "signed char", SignedIntegerType.SIGNED_CHAR,
        "short", SignedIntegerType.SHORT,
        "int", SignedIntegerType.INT,
        "long", SignedIntegerType.LONG,
        "long long", SignedIntegerType.LONG_LONG
    );

    /**
     * Map for retrieving unsigned standard integer types.
     */
    private static final ImmutableMap<String, UnsignedIntegerType> UNSIGNED_TYPES_MAP = ImmutableMap.of(
        "unsigned char", UnsignedIntegerType.UNSIGNED_CHAR,
        "unsigned short", UnsignedIntegerType.UNSIGNED_SHORT,
        "unsigned int", UnsignedIntegerType.UNSIGNED_INT,
        "unsigned long", UnsignedIntegerType.UNSIGNED_LONG,
        "unsigned long long", UnsignedIntegerType.UNSIGNED_LONG_LONG
    );

    /**
     * Map for retrieving interrupt semantics indicated by GCC attributes
     * 'atomic_hwevent' and 'hwevent'.
     */
    private static final ImmutableMap<String, AttributesAssumptions.InterruptSemantics> INTERRUPT_SEMANTICS_MAP = ImmutableMap.of(
        "atomic-hwevent", AttributesAssumptions.InterruptSemantics.ATOMIC,
        "hwevent", AttributesAssumptions.InterruptSemantics.NORMAL
    );

    /**
     * Map for retrieving the preferential GCC attribute.
     */
    private static final ImmutableMap<String, AttributesAssumptions.PreferentialAttribute> PREFERENTIAL_ATTRIBUTES_MAP = ImmutableMap.of(
        "signal", AttributesAssumptions.PreferentialAttribute.SIGNAL,
        "interrupt", AttributesAssumptions.PreferentialAttribute.INTERRUPT
    );

    /**
     * Endiannes of the target architecture.
     */
    private final Endianness endianness;

    /**
     * Data about types.
     */
    private final CharData typeChar;
    private final StandardIntegerTypeData typeShort;
    private final StandardIntegerTypeData typeInt;
    private final StandardIntegerTypeData typeLong;
    private final StandardIntegerTypeData typeLongLong;
    private final TypeData typeFloat;
    private final TypeData typeDouble;
    private final TypeData typeLongDouble;
    private final TypeData typePointer;
    private final UnsignedIntegerType typeSizeT;
    private final SignedIntegerType typePtrdiffT;
    private final FieldTagTypeData typeFieldTag;

    /**
     * Assumptions about interrupt handlers indicated by GCC attributes.
     */
    private final AttributesAssumptions attributesAssumptions;

    /**
     * Load the ABI from the given stream. It should contain XML data that
     * validates against the ABI XML schema.
     *
     * @param xmlInput Input stream to read the ABI from.
     */
    public ABI(InputStream xmlInput) throws SAXException, ParserConfigurationException,
            IOException, XPathExpressionException {
        checkNotNull(xmlInput, "the input stream with XML contents cannot be null");

        final Builder builder = new Builder(xmlInput);
        builder.parse();

        this.endianness = builder.buildEndianness();
        this.typeChar = builder.buildCharData();
        this.typeShort = builder.buildShortTypeData();
        this.typeInt = builder.buildIntTypeData();
        this.typeLong = builder.buildLongTypeData();
        this.typeLongLong = builder.buildLongLongTypeData();
        this.typeFloat = builder.buildFloatTypeData();
        this.typeDouble = builder.buildDoubleTypeData();
        this.typeLongDouble = builder.buildLongDoubleTypeData();
        this.typePointer = builder.buildPointerTypeData();
        this.typeSizeT = builder.buildSizeT();
        this.typePtrdiffT = builder.buildPtrdiffT();
        this.typeFieldTag = builder.buildFieldTagTypeData();
        this.attributesAssumptions = builder.buildAttributesAssumptions();
    }

    /**
     * Load information about the ABI from an XML file with given path. The file
     * should validate against the ABI XML Schema.
     *
     * @param xmlFileName Name of the file with data about the ABI.
     */
    public ABI(String xmlFileName) throws SAXException, ParserConfigurationException,
            IOException, XPathExpressionException {
        this(new FileInputStream(xmlFileName));
    }

    public Endianness getEndianness() {
        return endianness;
    }

    public CharData getChar() {
        return typeChar;
    }

    public StandardIntegerTypeData getShort() {
        return typeShort;
    }

    public StandardIntegerTypeData getInt() {
        return typeInt;
    }

    public StandardIntegerTypeData getLong() {
        return typeLong;
    }

    public StandardIntegerTypeData getLongLong() {
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

    public UnsignedIntegerType getSizeT() {
        return typeSizeT;
    }

    public SignedIntegerType getPtrdiffT() {
        return typePtrdiffT;
    }

    public FieldTagTypeData getFieldTagType() {
        return typeFieldTag;
    }

    public AttributesAssumptions getAttributesAssumptions() {
        return attributesAssumptions;
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
         * Stream with the XML contents.
         */
        private final InputStream xmlInput;

        /**
         * Object for making XPath queries on the loaded document.
         */
        private final XPath xpath;

        /**
         * The root node of the loaded XML file with ABI.
         */
        private Document root;

        private Builder(InputStream xmlInput) {
            this.xmlInput = xmlInput;

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
            this.root = parser.parse(xmlInput);
        }

        private Endianness buildEndianness() throws XPathExpressionException {
            final String endiannessText = retrieveText("/abi:abi/abi:endianness");

            switch (endiannessText) {
                case "little-endian":
                    return Endianness.LITTLE_ENDIAN;
                case "big-endian":
                    return Endianness.BIG_ENDIAN;
                default:
                    throw new RuntimeException("value '" + endiannessText + "' validated as endianness");
            }
        }

        private CharData buildCharData() throws XPathExpressionException {
            return new CharData(
                    isCharSigned(),
                    retrieveUnboundedInteger("/abi:abi/abi:types/abi:char/abi:signed/abi:minimum-value"),
                    retrieveUnboundedInteger("/abi:abi/abi:types/abi:char/abi:signed/abi:maximum-value"),
                    retrieveUnboundedInteger("/abi:abi/abi:types/abi:char/abi:unsigned/abi:maximum-value")
            );
        }

        private boolean isCharSigned() throws XPathExpressionException {
            return retrieveBoolean("/abi:abi/abi:types/abi:char/abi:is-signed");
        }

        private StandardIntegerTypeData buildShortTypeData() throws XPathExpressionException {
            return retrieveStandardIntegerTypeData("short");
        }

        private StandardIntegerTypeData buildIntTypeData() throws XPathExpressionException {
            return retrieveStandardIntegerTypeData("int");
        }

        private StandardIntegerTypeData buildLongTypeData() throws XPathExpressionException {
            return retrieveStandardIntegerTypeData("long");
        }

        private StandardIntegerTypeData buildLongLongTypeData() throws XPathExpressionException {
            return retrieveStandardIntegerTypeData("long-long");
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

        private UnsignedIntegerType buildSizeT() throws XPathExpressionException {
            return retrieveUnsignedIntegerType("/abi:abi/abi:types/abi:size_t/abi:alias-of");
        }

        private SignedIntegerType buildPtrdiffT() throws XPathExpressionException {
            return retrieveSignedIntegerType("/abi:abi/abi:types/abi:ptrdiff_t/abi:alias-of");
        }

        private FieldTagTypeData buildFieldTagTypeData() throws XPathExpressionException {
            return new FieldTagTypeData(
                    retrieveBoolean("/abi:abi/abi:types/abi:struct-or-union/abi:bitfield-type-matters"),
                    retrieveInt("/abi:abi/abi:types/abi:struct-or-union/abi:empty-bitfield-alignment-in-bits"),
                    retrieveInt("/abi:abi/abi:types/abi:struct-or-union/abi:minimum-alignment")
            );
        }

        private AttributesAssumptions buildAttributesAssumptions() throws XPathExpressionException {
            return new AttributesAssumptions(
                    retrieveInterruptSemantics("/abi:abi/abi:call-assumptions/abi:for-interrupt-attribute"),
                    retrieveInterruptSemantics("/abi:abi/abi:call-assumptions/abi:for-signal-attribute"),
                    retrievePreferentialAttribute("/abi:abi/abi:call-assumptions/abi:preferential-attribute")
            );
        }

        private TypeData retrieveTypeData(String typeElementName) throws XPathExpressionException {
            return new TypeData(
                    retrieveInt(format("/abi:abi/abi:types/abi:%s/abi:size", typeElementName)),
                    retrieveInt(format("/abi:abi/abi:types/abi:%s/abi:alignment", typeElementName))
            );
        }

        private AttributesAssumptions.InterruptSemantics retrieveInterruptSemantics(String path) throws XPathExpressionException {
            return retrieveMappedValue(path, INTERRUPT_SEMANTICS_MAP);
        }

        private AttributesAssumptions.PreferentialAttribute retrievePreferentialAttribute(String path) throws XPathExpressionException {
            return retrieveMappedValue(path, PREFERENTIAL_ATTRIBUTES_MAP);
        }

        private SignedIntegerType retrieveSignedIntegerType(String path) throws XPathExpressionException {
            return retrieveMappedValue(path, SIGNED_TYPES_MAP);
        }

        private UnsignedIntegerType retrieveUnsignedIntegerType(String path) throws XPathExpressionException {
            return retrieveMappedValue(path, UNSIGNED_TYPES_MAP);
        }

        private StandardIntegerTypeData retrieveStandardIntegerTypeData(String typeElementName) throws XPathExpressionException {
            return new StandardIntegerTypeData(
                    retrieveInt(format("/abi:abi/abi:types/abi:%s/abi:size", typeElementName)),
                    retrieveInt(format("/abi:abi/abi:types/abi:%s/abi:alignment", typeElementName)),
                    retrieveUnboundedInteger(format("/abi:abi/abi:types/abi:%s/abi:signed/abi:minimum-value", typeElementName)),
                    retrieveUnboundedInteger(format("/abi:abi/abi:types/abi:%s/abi:signed/abi:maximum-value", typeElementName)),
                    retrieveUnboundedInteger(format("/abi:abi/abi:types/abi:%s/abi:unsigned/abi:maximum-value", typeElementName))
            );
        }

        private boolean retrieveBoolean(String xpathExpr) throws XPathExpressionException {
            final String text = retrieveText(xpathExpr);

            switch (text) {
                case "true":
                case "1":
                    return true;
                case "false":
                case "0":
                    return false;
                default:
                    throw new RuntimeException("value '" + text + "' in an element of type 'boolean'");
            }
        }

        private <V> V retrieveMappedValue(String path, Map<String, V> values) throws XPathExpressionException {
            final String text = retrieveText(path);
            final Optional<V> value = Optional.fromNullable(values.get(text));
            if (!value.isPresent()) {
                throw new RuntimeException("value '" + text + "' not present in the map");
            }
            return value.get();
        }

        private int retrieveInt(String xpathExpr) throws XPathExpressionException {
            return Integer.parseInt(retrieveText(xpathExpr));
        }

        private BigInteger retrieveUnboundedInteger(String xpathExpr) throws XPathExpressionException {
            return new BigInteger(retrieveText(xpathExpr));
        }

        private String retrieveText(String xpathExpr) throws XPathExpressionException {
            final Element element = (Element) xpath.evaluate(xpathExpr, root, XPathConstants.NODE);
            checkNotNull(element, "cannot find an element in th XML ABI file");
            return extractText(element);
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
