package pl.edu.mimuw.nesc.backend8051;

import com.google.common.base.Optional;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Class that is responsible for checking the SDCC version that will be used
 * by the 8051 backend.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class SDCCChecker {
    /**
     * Checks the SDCC executable given as parameter. The returned iterable
     * contains all detected issues.
     *
     * @param sdccExecutablePath SDCC to check.
     * @return Iterable with detected issues.
     */
    public Iterable<String> check(String sdccExecutablePath) {
        checkNotNull(sdccExecutablePath, "SDCC executable path cannot be null");
        checkArgument(!sdccExecutablePath.isEmpty(), "SDCC executable path cannot be an empty string");

        final Optional<String> sdccHelp = getSDCCHelp(sdccExecutablePath);

        if (!sdccHelp.isPresent()) {
            return Collections.singletonList("cannot successfully obtain SDCC help information");
        }

        /* Singleton list is here only to suppress a warning. The intended
           method here is Arrays.asList. */
        final List<Optional<String>> issues = Collections.singletonList(
                checkC99Standard(sdccHelp.get())
        );

        return Optional.presentInstances(issues);
    }

    private Optional<String> getSDCCHelp(String sdccExecutablePath) {
        try {
            final String[] sdccHelpCmd = { sdccExecutablePath, "--help" };
            final Process sdccProcess = Runtime.getRuntime().exec(sdccHelpCmd);

            if (sdccProcess.waitFor() != 0) {
                return Optional.absent();
            }

            final InputStream inStream = sdccProcess.getErrorStream();
            final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            final byte[] buffer = new byte[524288];

            // Get the output
            for (int bytesRead = inStream.read(buffer); bytesRead != -1;
                    bytesRead = inStream.read(buffer)) {
                outStream.write(buffer, 0, bytesRead);
            }

            return Optional.of(new String(outStream.toByteArray()));
        } catch (InterruptedException | IOException e) {
            return Optional.absent();
        }
    }

    private Optional<String> checkC99Standard(String sdccHelp) {
        return sdccHelp.contains("--std-c99")
                ? Optional.<String>absent()
                : Optional.of("cannot verify SDCC option '--std-c99'");
    }
}
