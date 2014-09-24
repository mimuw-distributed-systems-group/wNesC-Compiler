package pl.edu.mimuw.nesc.astbuilding

import com.google.common.base.Optional
import pl.edu.mimuw.nesc.ast.IntegerCstKind
import pl.edu.mimuw.nesc.ast.IntegerCstSuffix
import pl.edu.mimuw.nesc.ast.Location
import pl.edu.mimuw.nesc.ast.gen.IntegerCst
import spock.lang.Specification

/**
 * Tests of creating AST nodes that represent integer constants. Method
 * {@link Expressions#makeIntegerCst makeIntegerCst} is responsible for it.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
class IntegerCstBuildingTest extends Specification {
    /**
     * All possible suffixes defined for use in all tests.
     */
    static final SUFFIXES = [
            ""    : IntegerCstSuffix.NO_SUFFIX,
            "u"   : IntegerCstSuffix.SUFFIX_U,
            "U"   : IntegerCstSuffix.SUFFIX_U,
            "ul"  : IntegerCstSuffix.SUFFIX_UL,
            "uL"  : IntegerCstSuffix.SUFFIX_UL,
            "Ul"  : IntegerCstSuffix.SUFFIX_UL,
            "UL"  : IntegerCstSuffix.SUFFIX_UL,
            "uLL" : IntegerCstSuffix.SUFFIX_ULL,
            "ull" : IntegerCstSuffix.SUFFIX_ULL,
            "ULL" : IntegerCstSuffix.SUFFIX_ULL,
            "Ull" : IntegerCstSuffix.SUFFIX_ULL,
            "l"   : IntegerCstSuffix.SUFFIX_L,
            "L"   : IntegerCstSuffix.SUFFIX_L,
            "lU"  : IntegerCstSuffix.SUFFIX_UL,
            "LU"  : IntegerCstSuffix.SUFFIX_UL,
            "lu"  : IntegerCstSuffix.SUFFIX_UL,
            "Lu"  : IntegerCstSuffix.SUFFIX_UL,
            "ll"  : IntegerCstSuffix.SUFFIX_LL,
            "LL"  : IntegerCstSuffix.SUFFIX_LL,
            "llu" : IntegerCstSuffix.SUFFIX_ULL,
            "LLu" : IntegerCstSuffix.SUFFIX_ULL,
            "llU" : IntegerCstSuffix.SUFFIX_ULL,
            "LLU" : IntegerCstSuffix.SUFFIX_ULL
    ]

    def "too big value is correctly handled"() {
        given:
        String literal = literalCore + suffix
        IntegerCst cst = Expressions.makeIntegerCst(literal, Location.getDummyLocation(),
                Location.getDummyLocation())

        expect:
        cst.getValue() == Optional.absent()

        where:
        [literalCore, suffix] << [[
                "02000000000000000000000",
                "0x10000000000000000",
                "18446744073709551616",
                "0xfffffffffffffffff",
                "07777777777777777777777",
                "99999999999999999999",
                "0X785965b875867585c7685f87576585687",
                "04235234524532345346245245232543",
                "987089780465876054327680546278306580"
            ],
            SUFFIXES.keySet()
        ].combinations()
    }

    def "value that fits is correctly handled"() {
        given:
        String literal = literalCore + suffix
        IntegerCst cst = Expressions.makeIntegerCst(literal, Location.getDummyLocation(),
                Location.getDummyLocation())
        Optional<BigInteger> actualValue = cst.getValue()

        expect:
        actualValue == Optional.of(expectedValue)

        where:
        [literalCore, expectedValue, suffix] << [[
                ["0xffffFfffffffFfff", 0xffffffffffffffffg],
                ["01777777777777777777777", 01777777777777777777777g],
                ["0X00000000000000000ffffffFfffffffff", 0xffffffffffffffffg],
                ["0x00000000000000000ffffffffffffffff", 0xffffffffffffffffg],
                ["0000000000000000000001777777777777777777777", 01777777777777777777777g],
                ["0", 0g],
                ["0x0", 0g],
                ["00", 0g],
                ["18446744073709551615", 18446744073709551615g],
                ["999", 999g],
                ["0xff", 0xffg],
                ["0777", 0777g]
            ],
            SUFFIXES.keySet()
        ].combinations().collect{ it.flatten() }
    }

    def "kind of a decimal constant is correctly set"() {
        given:
        String literal = firstDigit + rest + suffix;
        IntegerCst cst = Expressions.makeIntegerCst(literal, Location.getDummyLocation(),
                Location.getDummyLocation())

        expect:
        cst.getKind() == IntegerCstKind.DECIMAL

        where:
        [firstDigit, rest, suffix] << [
                ["1", "2", "3", "4", "5", "6", "7", "8", "9"], [
                        "00000000000000000000000000000000000000",
                        "123456789",
                        "77789778967895759785978579587585659",
                        "0"
                ],
                SUFFIXES.keySet()
        ].combinations()
    }

    def "kind of a hexadecimal constant is correctly set"() {
        given:
        String literal = prefix + hexLiteralCore + suffix
        IntegerCst cst = Expressions.makeIntegerCst(literal, Location.getDummyLocation(),
                Location.getDummyLocation())

        expect:
        cst.getKind() == IntegerCstKind.HEXADECIMAL

        where:
        [prefix, hexLiteralCore, suffix] << [
                ["0x", "0X"], [
                    "00000000000000000000000000000",
                    "0123456789abcdef",
                    "123fabc"
                ],
                SUFFIXES.keySet()
        ].combinations()
    }

    def "kind of an octal constant is correctly set"() {
        given:
        String literal = literalCore + suffix
        IntegerCst cst = Expressions.makeIntegerCst(literal, Location.getDummyLocation(),
                Location.getDummyLocation())

        expect:
        cst.getKind() == IntegerCstKind.OCTAL

        where:
        [literalCore, suffix] << [[
                "0",
                "01231234",
                "000000000000000000000000000000",
                "00000000000000000000000000000000012312",
                "0123456701234567012345670123456701234567",
                "000000012345667",
                "0000000000000000000000001"
            ],
            SUFFIXES.keySet()
        ].combinations()
    }

    def "suffix of the constant is correctly set"() {
        given:
        String literal = literalCore + suffix
        IntegerCst cst = Expressions.makeIntegerCst(literal, Location.getDummyLocation(),
                Location.getDummyLocation())
        IntegerCstSuffix expectedSuffix = SUFFIXES[suffix]

        expect:
        cst.getSuffix() == expectedSuffix

        where:
        [literalCore, suffix] << [[
                    "0",
                    "0x0",
                    "0X0",
                    "01223",
                    "0xff",
                    "0XFFFF",
                    "123",
                    "0x000000000000000000000000000000000",
                    "0X000000000000000000000000000000000000000123",
                    "0xffffffffFfFfFffffFFFFfffFFFfffFFFfffFFFFFFfffFF",
                    "9999999999999999999999999999999999999",
                    "00000000777777777777777777777777777777777777",
                    "000000000000000000000000000000000000000001",
                    "78887"
                ],
                SUFFIXES.keySet()
        ].combinations()
    }
}
