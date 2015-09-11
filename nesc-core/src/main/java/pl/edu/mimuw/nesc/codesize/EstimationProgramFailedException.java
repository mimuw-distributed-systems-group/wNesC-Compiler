package pl.edu.mimuw.nesc.codesize;

import java.io.IOException;
import java.io.OutputStream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Exception thrown when an external program used for estimation like SDCC or
 * SDAS has failed.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class EstimationProgramFailedException extends Exception {
    /**
     * Byte that signifies the newline character.
     */
    private static final byte BYTE_NEWLINE = 10;

    /**
     * Name of the program that has failed.
     */
    private final String programName;

    /**
     * Output of the program that has failed.
     */
    private final byte[] programOutput;

    /**
     * Create a new instance of the exception with a message with information
     * from passed parameters.
     *
     * @param programName Name of the program that has failed that can be
     *                    understood by the compiler user.
     * @param returnCode Exit value of the failed program.
     * @param programOutput Array with output of the program (this can be, for
     *                      example, its standard output).
     * @return Newly created instance of the exception.
     */
    static EstimationProgramFailedException newInstance(String programName, int returnCode,
            byte[] programOutput) {

        // Build the message for the exception
        final String programSymbol = programName != null && !programName.isEmpty()
                ? programName
                : "a program used for the estimation";
        final String message = programSymbol + " failed with exit value "
                + returnCode;

        return new EstimationProgramFailedException(message, programName, programOutput);
    }

    private EstimationProgramFailedException(String message, String programName,
                byte[] programOutput) {
        super(message);
        this.programName = programName;
        this.programOutput = programOutput;
    }

    /**
     * Writes the output of the program that has failed to the given output
     * stream. Each line written to the stream is prefixed with the name of
     * the program that has failed and a colon, e.g. "sdcc:".
     *
     * @param output Stream to which the output of the program that has failed
     *               will be written.
     * @throws IOException An error is thrown by the given output stream when
     *                     it is used.
     */
    public void writeProgramOutput(OutputStream output) throws IOException {
        checkNotNull(output, "output cannot be null");

        final String linePrefix = programName != null && !programName.isEmpty()
                ? programName.toLowerCase() + ":"
                : "";
        final byte[] bytesLinePrefix = linePrefix.getBytes();
        boolean newLine = true; /* true if the last character written was
                                    a new line character. */

        // Write the output to the given stream
        for (byte b : programOutput) {
            if (newLine) {
                output.write(bytesLinePrefix);
            }
            output.write(b);
            newLine = b == BYTE_NEWLINE;
        }
    }
}
