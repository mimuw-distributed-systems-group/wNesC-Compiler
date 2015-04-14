package pl.edu.mimuw.nesc.backend8051;

import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>Enum type that represents storage-class specifiers that are added to
 * C language by SDCC.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
enum StorageClassExtension {
    /**
     * Storage-class specifier for variables that are to be located in the
     * directly addressable portion of the internal RAM of a 8051
     * microcontroller.
     */
    DATA(false, "__data", "__near"),
    /**
     * Specifies that a variable is to be located in the external RAM.
     */
    XDATA(false, "__xdata", "__far"),
    /**
     * Specifies that a variable is to be located in the indirectly addressable
     * portion of the internal RAM of a 8051 microcontroller.
     */
    IDATA(false, "__idata"),
    /**
     * Specifies a paged XDATA access.
     */
    PDATA(false, "__pdata"),
    /**
     * Specifies that a variable is to be located in the code memory.
     */
    CODE(false, "__code"),
    /**
     * Specifies that a variable is to be located in the bit addressable memory
     * of a 8051 microcontroller. It is both a data type and a storage-class
     * specifier.
     */
    BIT(true, "__bit"),
    /**
     * Specifies a variable that will be associated with a special function
     * register. It is both a data type and a storage-class specifier.
     */
    SFR(true, "__sfr"),
    /**
     * Specifies a 16-bit special function register. It is both a data type and
     * a storage-class specifier.
     */
    SFR16(true, "__sfr16"),
    /**
     * Specifies a 32-bit special function register. It is both a data type and
     * a storage-class specifier.
     */
    SFR32(true, "__sfr32"),
    /**
     * Specifies a specific bit inside a special function register that is
     * located on an address dividable by 8 and is bit-addressable. It is both
     * a data type and a storage-class specifier.
     */
    SBIT(true, "__sbit"),
    ;

    /**
     * Set with keywords of storage-class extension that are also types.
     */
    private static final ImmutableSet<String> SET_KEYWORDS_TYPE;
    static {
        final ImmutableSet.Builder<String> keywordsTypeBuilder = ImmutableSet.builder();
        for (StorageClassExtension extension : StorageClassExtension.values()) {
            if (extension.isType()) {
                keywordsTypeBuilder.addAll(extension.getKeywords());
            }
        }
        SET_KEYWORDS_TYPE = keywordsTypeBuilder.build();
    }

    /**
     * Set with keywords of all storage-class extensions.
     */
    private static final ImmutableSet<String> SET_KEYWORDS;
    static {
        final ImmutableSet.Builder<String> keywordsBuilder = ImmutableSet.builder();
        for (StorageClassExtension extension : StorageClassExtension.values()) {
            keywordsBuilder.addAll(extension.getKeywords());
        }
        SET_KEYWORDS = keywordsBuilder.build();
    }

    /**
     * Set with keywords of storage-class extensions that are not types.
     */
    private static final ImmutableSet<String> SET_KEYWORDS_PURE;
    static {
        final Set<String> pureKeywordsSpecimen = new HashSet<>(SET_KEYWORDS);
        pureKeywordsSpecimen.removeAll(SET_KEYWORDS_TYPE);
        SET_KEYWORDS_PURE = ImmutableSet.copyOf(pureKeywordsSpecimen);
    }

    /**
     * Keywords that indicate usage of this storage-class specifier.
     */
    private final ImmutableSet<String> keywords;

    /**
     * Value indicating if this storage-class extension is also a type.
     */
    private final boolean isType;

    /**
     * Get a set with keywords of storage-class extensions that are also types.
     *
     * @return Set with specifiers keywords.
     */
    public static ImmutableSet<String> getTypeKeywords() {
        return SET_KEYWORDS_TYPE;
    }

    /**
     * Get a set with keywords of all storage-class extensions.
     *
     * @return Set with keywords.
     */
    public static ImmutableSet<String> getAllKeywords() {
        return SET_KEYWORDS;
    }

    /**
     * Get a set with keywords of storage-class extensions that are not types.
     *
     * @return Set with keywords.
     */
    public static ImmutableSet<String> getPureKeywords() {
        return SET_KEYWORDS_PURE;
    }

    private StorageClassExtension(boolean isType, String... specifierKeywords) {
        this.isType = isType;
        this.keywords = ImmutableSet.copyOf(specifierKeywords);
    }

    /**
     * Get set of keywords that can be used to specify this storage-class
     * specifier in a program.
     *
     * @return Set with keywords for this specifier.
     */
    public ImmutableSet<String> getKeywords() {
        return keywords;
    }

    /**
     * Check if this storage-class extension is also a type.
     *
     * @return <code>true</code> if and only if this storage-class extension is
     *         simultaneously a type.
     */
    public boolean isType() {
        return isType;
    }
}
