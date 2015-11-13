package pl.edu.mimuw.nesc.backend8051.option;

import com.google.common.collect.ImmutableMap;
import pl.edu.mimuw.nesc.codepartition.BComponentsCodePartitioner;

/**
 * <p>Class with some data about the options for 8051 version of the
 * compiler.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class Options8051 {
    /**
     * Short names of options for the 8051 backend.
     */
    public static final String OPTION_SHORT_BANKS = "n";
    public static final String OPTION_SHORT_THREADS_COUNT = "z";
    public static final String OPTION_SHORT_RELAX_BANKED = "r";
    public static final String OPTION_SHORT_SDCC_PARAMS = "w";
    public static final String OPTION_SHORT_MAXIMUM_INLINE_SIZE = "l";
    public static final String OPTION_SHORT_PARTITION_HEURISTIC = "h";

    /**
     * Long names of options for the 8051 backend.
     */
    public static final String OPTION_LONG_BANKS = "banks";
    public static final String OPTION_LONG_THREADS_COUNT = "estimate-threads";
    public static final String OPTION_LONG_SDCC_EXEC = "sdcc-exec";
    public static final String OPTION_LONG_SDAS_EXEC = "sdas-exec";
    public static final String OPTION_LONG_MODEL_SMALL = "model-small";
    public static final String OPTION_LONG_MODEL_MEDIUM = "model-medium";
    public static final String OPTION_LONG_MODEL_LARGE = "model-large";
    public static final String OPTION_LONG_MODEL_HUGE = "model-huge";
    public static final String OPTION_LONG_DUMP_CALL_GRAPH = "dump-call-graph";
    public static final String OPTION_LONG_INTERRUPTS = "interrupts";
    public static final String OPTION_LONG_RELAX_BANKED = "relax-banked";
    public static final String OPTION_LONG_SDCC_PARAMS = "sdcc-parameters";
    public static final String OPTION_LONG_MAXIMUM_INLINE_SIZE = "maximum-inline-size";
    public static final String OPTION_LONG_RELAX_INLINE = "relax-inline";
    public static final String OPTION_LONG_DUMP_INLINE_FUNCTIONS = "dump-inline-functions";
    public static final String OPTION_LONG_PRINT_BANKING_STATS = "print-banking-stats";
    public static final String OPTION_LONG_PARTITION_HEURISTIC = "partition-heuristic";
    public static final String OPTION_LONG_SPANNING_FOREST = "spanning-forest";
    public static final String OPTION_LONG_PREFER_HIGHER_ESTIMATE_ALLOCATIONS = "prefer-higher-estimate-allocations";
    public static final String OPTION_LONG_ARBITRARY_SUBTREE_PARTITIONING = "arbitrary-subtree-partitioning";
    public static final String OPTION_LONG_LOOP_FACTOR = "loop-factor";
    public static final String OPTION_LONG_CONDITIONAL_FACTOR = "conditional-factor";
    public static final String OPTION_LONG_COMMON_BANK_ALLOCATION_ALGORITHM = "common-bank-allocation-algorithm";

    /**
     * Separators used for options values.
     */
    public static final String SEPARATOR_INTERRUPT_ASSIGNMENT_OUTER = ",";
    public static final String SEPARATOR_INTERRUPT_ASSIGNMENT_INNER = "=";
    public static final String SEPARATOR_BANKS_SCHEMA_OUTER = ",";
    public static final String SEPARATOR_BANKS_SCHEMA_INNER = "=";

    /**
     * Separators of parameter names and their values.
     */
    public static final char PARAMETER_SEPARATOR_SDCC_PARAMS = '=';

    /**
     * Map for parsing the spanning forest kind.
     */
    public static final ImmutableMap<String, BComponentsCodePartitioner.SpanningForestKind> MAP_SPANNING_FOREST =
            ImmutableMap.of(
                    "original", BComponentsCodePartitioner.SpanningForestKind.BCOMPONENTS,
                    "minimum", BComponentsCodePartitioner.SpanningForestKind.MINIMUM,
                    "maximum", BComponentsCodePartitioner.SpanningForestKind.MAXIMUM
            );

    /**
     * Map for parsing the arbitrary subtree partitioning mode.
     */
    public static final ImmutableMap<String, BComponentsCodePartitioner.ArbitrarySubtreePartitioningMode> MAP_ARBITRARY_SUBTREE_PARTITIONING =
            ImmutableMap.of(
                    "always", BComponentsCodePartitioner.ArbitrarySubtreePartitioningMode.ALWAYS,
                    "emergency", BComponentsCodePartitioner.ArbitrarySubtreePartitioningMode.ON_EMERGENCY,
                    "never", BComponentsCodePartitioner.ArbitrarySubtreePartitioningMode.NEVER
            );

    /**
     * Map for parsing the common bank allocation algorithm.
     */
    public static final ImmutableMap<String, BComponentsCodePartitioner.CommonBankAllocationAlgorithm> MAP_COMMON_BANK_ALLOCATION_ALGORITHM =
            ImmutableMap.of(
                    "greedy-estimations", BComponentsCodePartitioner.CommonBankAllocationAlgorithm.GREEDY_DESCENDING_ESTIMATIONS,
                    "2approx", BComponentsCodePartitioner.CommonBankAllocationAlgorithm.TWO_APPROXIMATION,
                    "nop", BComponentsCodePartitioner.CommonBankAllocationAlgorithm.NO_OPERATION
            );

    /**
     * Private constructor to prevent instantiating this class.
     */
    private Options8051() {
    }
}
