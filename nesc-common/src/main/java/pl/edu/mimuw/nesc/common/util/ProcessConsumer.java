package pl.edu.mimuw.nesc.common.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * <p>An object that consumes a process, i.e. it reads
 * {@link Process#getInputStream the input stream} associated with a process
 * until it is exhausted and then waits for the process.</p>
 *
 * <p>An object of this class should be used by only one thread.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class ProcessConsumer {
    /**
     * Process consumed by this object.
     */
    private final Process processToConsume;

    /**
     * Stream that takes the output of the process consumed by this object.
     */
    private final ByteArrayOutputStream storeStream;

    /**
     * Value indicating if the process has already been consumed.
     */
    private boolean consumed;

    /**
     * Initializes this object to consume the given process.
     *
     * @param processToConsume Process that will be consumed. It could have
     *                         already terminated.
     */
    public ProcessConsumer(Process processToConsume) {
        checkNotNull(processToConsume, "process to consume cannot be null");
        this.processToConsume = processToConsume;
        this.storeStream = new ByteArrayOutputStream();
        this.consumed = false;
    }

    /**
     * Consume the process associated with this object.
     *
     * @return Exit value of the consumed process.
     * @throws IllegalArgumentException The process associated with this object
     *                                  has been already consumed.
     */
    public int consume() throws IOException, InterruptedException {
        checkState(!consumed, "the process has been already consumed");

        final InputStream input = processToConsume.getInputStream();
        storeStream.reset();

        // Consume the output of the process
        for (int b = input.read(); b != -1; b = input.read()) {
            storeStream.write(b);
        }

        // Wait for the process
        processToConsume.waitFor();

        consumed = true;
        return processToConsume.exitValue();
    }

    /**
     * Get an array with the output of the consumed process from its
     * {@link Process#getInputStream input stream}.
     *
     * @return Array with the output of the consumed process.
     * @throws IllegalStateException The process associated with this object
     *                               has not been consumed yet.
     */
    public byte[] getProcessOutput() {
        checkState(consumed, "the process has not been consumed yet");
        return storeStream.toByteArray();
    }
}
