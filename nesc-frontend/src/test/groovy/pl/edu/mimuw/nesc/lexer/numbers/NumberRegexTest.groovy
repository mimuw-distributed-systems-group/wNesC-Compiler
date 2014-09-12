package pl.edu.mimuw.nesc.lexer.numbers

import pl.edu.mimuw.nesc.lexer.Lexer
import pl.edu.mimuw.nesc.lexer.NescLexer
import pl.edu.mimuw.nesc.parser.Symbol
import spock.lang.Specification

import static pl.edu.mimuw.nesc.parser.Parser.Lexer.*

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
class NumberRegexTest extends Specification {

    def "matches integer regex"() {
        expect:
        (value + suffix).matches(NescLexer.INTEGER_REGEX)

        where:
        [value, suffix] << [
                [
                        // decimal
                        "1", "123",
                        // octal
                        "0", "0475",
                        // hexadecimal
                        "0x34fa43", "0Xc432fea", "0xFa43fb", "0XA021c"
                ],
                // possible suffixes
                [
                        "",
                        "u", "U", "ul", "uL", "Ul", "UL",
                        "ull", "uLL", "Ull", "ULL",
                        "l", "L", "lu", "lU", "Lu", "LU",
                        "ll", "LL", "llu", "llU", "LLu", "LLU"
                ]
        ].combinations()
    }

    def "does not match integer regex"() {
        expect:
        !value.matches(NescLexer.INTEGER_REGEX)

        where:
        value << [
                // decimal
                "123uU", "54lll",
                // octal
                "053lll", "0123999", "0123456789", "01234afc", "0153FDA",
                // hexadecimal
                "x4F43", "x364CaDlll", "x76uU", "0x", "0X", "0xrtfha343", "0xRtc40",
                // floats
                "0.1", "1.2e-7", ".213", "3.9E+7", "0.1F", "4.1f", "5.3435l", "9.32L"
        ]
    }

    def "matches float regex decimal with dot"() {
        expect:
        (value + exponent + suffix).matches(NescLexer.FLOAT_REGEX)

        where:
        [value, exponent, suffix] << [
                ["12.", ".2", "1.232", "12.2", "3.4", "5.6", "7.8543"],
                ["", "e7", "E12", "e+2", "E+4", "e-5", "E-32"],
                ["", "l", "L", "f", "F"]
        ].combinations()
    }

    def "matches float regex decimal without dot"() {
        expect:
        (value + exponent + suffix).matches(NescLexer.FLOAT_REGEX)

        where:
        [value, exponent, suffix] << [
                ["12", "343254"],
                ["e7", "E12", "e+2", "E+4", "e-5", "E-32"],
                ["", "l", "L", "f", "F"]
        ].combinations()
    }

    def "matches float regex hexadecimal"() {
        expect:
        (prefix + value + exponent + suffix).matches(NescLexer.FLOAT_REGEX)

        where:
        [prefix, value, exponent, suffix] << [
                ["0x", "0X"],
                ["d.", ".2", "1f.2c2", "12.2aaf3", "c3.4c", "5.6", "7.85fd43a", "12", "44681"],
                ["p7", "P12", "p+2", "P+4", "p-5", "P-32"],
                ["", "l", "L", "f", "F"]
        ].combinations()
    }

    def "does not match float regex hexadecimal"() {
        expect:
        !value.matches(NescLexer.FLOAT_REGEX)

        where:
        value << [
                "12.43e3FL", ".343fl", "123", "123F",
                "x23p12", "0xfff1", "0xddf2p+f"
        ]
    }

    def "lexer integration test"() {
        given:
        String filePath = Thread.currentThread()
                .getContextClassLoader()
                .getResource("lexer/numbers/numbers.nc")
                .getFile();
        Lexer lexer = NescLexer.builder()
                .mainFile(filePath)
                .build()

        def tokens = []

        Symbol symbol = lexer.nextToken()
        while (symbol.getSymbolCode() != EOF) {
            // filter out whitespaces
            if (symbol.getSymbolCode() in [INTEGER_LITERAL, FLOATING_POINT_LITERAL, INVALID_NUMBER_LITERAL]) {
                tokens.add(symbol.getSymbolCode())
            }
            symbol = lexer.nextToken()
        }

        expect:
        tokens == [INTEGER_LITERAL, INTEGER_LITERAL, INTEGER_LITERAL, FLOATING_POINT_LITERAL, FLOATING_POINT_LITERAL,
                FLOATING_POINT_LITERAL, INVALID_NUMBER_LITERAL, INVALID_NUMBER_LITERAL, INVALID_NUMBER_LITERAL]
    }
}
