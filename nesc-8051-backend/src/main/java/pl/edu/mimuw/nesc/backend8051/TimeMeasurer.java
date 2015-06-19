package pl.edu.mimuw.nesc.backend8051;

import com.google.common.base.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 * Simple class for measuring time needed for various operations during the
 * compilation.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class TimeMeasurer {
    /**
     * Values returned by {@link System#nanoTime} when starting an operation.
     */
    private Optional<Long> startCodeSizeEstimation = Optional.absent();
    private Optional<Long> startCodePartition = Optional.absent();

    /**
     * Values returned by {@link System#nanoTime} after an operation is
     * finished.
     */
    private Optional<Long> endCodeSizeEstimation = Optional.absent();
    private Optional<Long> endCodePartition = Optional.absent();

    public void codeSizeEstimationStarted() {
        final long currentNanos = System.nanoTime();
        checkState(!startCodeSizeEstimation.isPresent(),
                "code size estimation has already started");
        this.startCodeSizeEstimation = Optional.of(currentNanos);
    }

    public void codeSizeEstimationEnded() {
        final long currentNanos = System.nanoTime();
        checkState(startCodeSizeEstimation.isPresent() && !endCodeSizeEstimation.isPresent(),
                "code size estimation has not yet started or it has already ended");
        this.endCodeSizeEstimation = Optional.of(currentNanos);
    }

    public void codePartitionStarted() {
        final long currentNanos = System.nanoTime();
        checkState(!startCodePartition.isPresent(), "code partition has already started");
        this.startCodePartition = Optional.of(currentNanos);
    }

    public void codePartitionEnded() {
        final long currentNanos = System.nanoTime();
        checkState(startCodePartition.isPresent() && !endCodePartition.isPresent(),
                "code partition has not yet started or it has already ended");
        this.endCodePartition = Optional.of(currentNanos);
    }

    public double getCodeSizeEstimationSeconds() {
        checkState(startCodeSizeEstimation.isPresent() && endCodeSizeEstimation.isPresent(),
                "code size estimation has not been yet fully performed");
        return computeElapsedSeconds(startCodeSizeEstimation.get(), endCodeSizeEstimation.get());
    }

    public double getCodePartitionSeconds() {
        checkState(startCodePartition.isPresent() && endCodePartition.isPresent(),
                "code partition has not yet been fully performed");
        return computeElapsedSeconds(startCodePartition.get(), endCodePartition.get());
    }

    private double computeElapsedSeconds(long startTimeNanos, long endTimeNanos) {
        checkArgument(endTimeNanos - startTimeNanos >= 0, "end time is less than the start time");
        return (double) (endTimeNanos - startTimeNanos) / 1_000_000_000.0;
    }
}
