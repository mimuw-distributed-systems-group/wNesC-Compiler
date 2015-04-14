package pl.edu.mimuw.nesc.codesize;

import com.google.common.collect.ImmutableSet;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Enum type that represents an SDCC memory model. It </p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public enum SDCCMemoryModel {
    /**
     * Small memory model dedicated to 8051 microcontrollers.
     */
    SMALL("--model-small"),
    /**
     * Medium memory model dedicated to 8051 microcontrollers.
     */
    MEDIUM("--model-medium"),
    /**
     * Large memory model dedicated to 8051 microcontrollers.
     */
    LARGE("--model-large"),
    /**
     * Huge memory model dedicated to 8051 microcontrollers.
     */
    HUGE("--model-huge"),
    /**
     * Flat 24 memory model dedicated to DS390 microcontrollers.
     */
    FLAT_24("--model-flat24"),
    ;

    /**
     * Set that contains options that specify a SDCC memory model.
     */
    private static final ImmutableSet<String> SET_OPTIONS;
    static {
        final ImmutableSet.Builder<String> optionsSetBuilder = ImmutableSet.builder();
        for (SDCCMemoryModel memoryModel : SDCCMemoryModel.values()) {
            optionsSetBuilder.add(memoryModel.getOption());
        }
        SET_OPTIONS = optionsSetBuilder.build();
    }

    /**
     * Option for the SDCC compiler that activates the memory model.
     */
    private final String option;

    /**
     * Get a set that contains all SDCC options that specify a memory model.
     *
     * @return Set with SDCC options.
     */
    public static ImmutableSet<String> getAllOptions() {
        return SET_OPTIONS;
    }

    private SDCCMemoryModel(String option) {
        checkNotNull(option, "option cannot be null");
        checkArgument(option.startsWith("--model-"), "option must start with '--model-'");
        this.option = option;
    }

    /**
     * Get the option for SDCC compiler that activates this memory model.
     *
     * @return String with option for this model for SDCC.
     */
    public String getOption() {
        return option;
    }
}
