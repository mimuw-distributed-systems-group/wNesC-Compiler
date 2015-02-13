package pl.edu.mimuw.nesc.finalreduce;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.abi.Endianness;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Class that facilitates transformation of external structures.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class BitFieldSequenceData {
    private Optional<Endianness> endianness = Optional.absent();
    private int totalSizeInBits = 0;

    public int getTotalSizeInBits() {
        return totalSizeInBits;
    }

    public Optional<Endianness> getEndianness() {
        return endianness;
    }

    public boolean isNonEmpty() {
        return totalSizeInBits > 0;
    }

    public boolean hasSameEndianness(Endianness endianness) {
        checkNotNull(endianness, "endianness cannot be null");
        return this.endianness.isPresent() && this.endianness.get() == endianness;
    }

    public void reset() {
        endianness = Optional.absent();
        totalSizeInBits = 0;
    }

    public void update(int sizeIncrease, Endianness endianness) {
        checkNotNull(endianness, "endianness cannot be null");
        checkArgument(sizeIncrease >= 0, "size increase cannot be negative");

        this.totalSizeInBits += sizeIncrease;
        this.endianness = Optional.of(endianness);
    }
}
