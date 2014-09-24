package pl.edu.mimuw.nesc.ast;

/**
 * Enumeration type that represents a suffix of an integer literal.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public enum IntegerCstSuffix {
    /**
     * No suffix, e.g. 0xff, 0700, 97.
     */
    NO_SUFFIX,

    /**
     * Only "u" or "U" suffix, e.g. 0xffU, 0700u, 97u.
     */
    SUFFIX_U,

    /**
     * Only "l" or "L" suffix, e.g. 0xffl, 0700L, 78L.
     */
    SUFFIX_L,

    /**
     * Only "LL" or "ll" suffix, e.g. 0xffLL, 78ll.
     */
    SUFFIX_LL,

    /**
     * Only unsigned and long suffix, e.g. 0xffLu, 0700uL.
     */
    SUFFIX_UL,

    /**
     * Only unsigned and long long suffix, e.g. 0xaaULL, 070llU.
     */
    SUFFIX_ULL
}
