package pl.edu.mimuw.nesc.backend8051.option;

import com.google.common.collect.ImmutableList;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Parser for parameters for SDCC specified by '--sdcc-params' option. It
 * handles escaping of characters.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class SDCCParametersParser {
    /**
     * Parse parameters for SDCC using backslash as the escape character.
     *
     * @param params String with parameters for SDCC to parse.
     * @return Immutable list with parameters for SDCC after parsing.
     * @throws InvalidParametersException Given specification of SDCC parameters
     *                                    has invalid syntax.
     */
    public ImmutableList<String> parse(String params) throws InvalidParametersException {
        checkNotNull(params, "parameters cannot be null");

        final ImmutableList.Builder<String> sdccParamsBuilder = ImmutableList.builder();
        final StringBuilder currentParam = new StringBuilder();
        boolean escapeNextChar = false;

        for (int i = 0; i < params.length(); ++i) {
            if (escapeNextChar) {
                currentParam.append(params.charAt(i));
                escapeNextChar = false;
            } else {
                switch (params.charAt(i)) {
                    case ',':
                        sdccParamsBuilder.add(currentParam.toString());
                        currentParam.delete(0, currentParam.length());
                        break;
                    case '\\':
                        escapeNextChar = true;
                        break;
                    default:
                        currentParam.append(params.charAt(i));
                        break;
                }
            }
        }

        if (escapeNextChar) {
            throw new InvalidParametersException("no character to escape by the backslash at position "
                    + params.length());
        }

        sdccParamsBuilder.add(currentParam.toString());

        return sdccParamsBuilder.build();
    }

    /**
     * Exception thrown when the string specifying parameters for SDCC is
     * invalid.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    static final class InvalidParametersException extends Exception {
        private InvalidParametersException(String msg) {
            super(msg);
        }

        private InvalidParametersException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }
}
