package pl.edu.mimuw.nesc.backend8051.option;

import com.google.common.collect.FluentIterable;
import java.io.IOException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import pl.edu.mimuw.nesc.option.OptionsLoader;

import static pl.edu.mimuw.nesc.backend8051.option.Options8051.*;

/**
 * <p>Factory of options for the 8051 version of the compiler.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class Options8051Factory {
    /**
     * Get Options object that depict all options for the 8051 version of the
     * compiler.
     *
     * @return Newly created Options object with all options for the compiler.
     */
    public Options newOptions() throws IOException {
        final Option[] options8051 = {
            Option.builder(OPTION_SHORT_BANKS)
                .longOpt(OPTION_LONG_BANKS)
                .hasArg()
                .desc("description of available areas of banks in form of a comma-separated list; the first element of the list should be the name of the common bank and all remaining elements should consist of the name of the bank, '=' and the amount of space available in the bank in bytes; if this option is not specified, the following bank schema is used: HOME,HOME=32768,BANK1=32768,BANK2=32768,BANK3=32768,BANK4=32768,BANK5=32768,BANK6=32768,BANK7=32768; this schema specifies that the name of the common bank is 'HOME' and there are 8 banks of 32 kB each")
                .argName("bank-schema")
                .build(),
            Option.builder(OPTION_SHORT_THREADS_COUNT)
                .longOpt(OPTION_LONG_THREADS_COUNT)
                .hasArg()
                .desc("count of threads used for estimating sizes of functions")
                .argName("number")
                .build(),
            Option.builder()
                .longOpt(OPTION_LONG_SDCC_EXEC)
                .hasArg()
                .desc("path to SDCC executable to use for functions size estimation")
                .argName("sdcc-exec")
                .build(),
            Option.builder()
                .longOpt(OPTION_LONG_DUMP_CALL_GRAPH)
                .hasArg()
                .desc("save the call graph of functions in the output C program to file; inline functions are eliminated from the graph")
                .argName("file")
                .build(),
            Option.builder()
                .longOpt(OPTION_LONG_INTERRUPTS)
                .hasArg()
                .desc("comma-separated list of assignments of functions to interrupts numbers; each element of the list consists of the name of the function, '=' and the interrupt number; declarations of functions in the program will be annotated by SDCC attribute '__interrupt' according to value for this option; example interrupts map: sig_timer=1,serial_interrupt=0,external_interrupt=2")
                .argName("interrupts-map")
                .build(),
            Option.builder(OPTION_SHORT_RELAX_BANKED)
                .longOpt(OPTION_LONG_RELAX_BANKED)
                .desc("allow changing a function declared as banked that is defined in the NesC program and not annotated as spontaneous not to be banked for optimization")
                .build(),
            Option.builder(OPTION_SHORT_SDCC_PARAMS)
                .longOpt(OPTION_LONG_SDCC_PARAMS)
                .hasArg()
                .desc("comma-separated list of parameters to pass to SDCC during estimation of functions sizes; every character can be escaped by preceding it with backslash, the comma looses its special meaning when escaped; parameters list and the name of the option can be separated by equality sign; example usage of this option: -"
                        + OPTION_SHORT_SDCC_PARAMS + "=--stack-auto,--opt-code-size,--std-c99")
                .argName("params-list")
                .valueSeparator(PARAMETER_SEPARATOR_SDCC_PARAMS)
                .build(),
            Option.builder()
                .longOpt(OPTION_LONG_SDAS_EXEC)
                .hasArg()
                .desc("path to 8051 assembler executable to use for functions size estimation")
                .argName("sdas-exec")
                .build(),
            Option.builder(OPTION_SHORT_MAXIMUM_INLINE_SIZE)
                .longOpt(OPTION_LONG_MAXIMUM_INLINE_SIZE)
                .hasArg()
                .desc("maximum size (in bytes) of a function that will become inline during compilation; all functions whose maximum size is less than or equal to the given value are made inline; if this option is not specified, then the size of 20 bytes is used")
                .argName("function-size")
                .build(),
            Option.builder()
                .longOpt(OPTION_LONG_RELAX_INLINE)
                .desc("allow the compiler to decide if a function declared as inline will remain such; if this option is not specified, then every inline function will remain inline unless it is considered unsafe by the compiler")
                .build(),
            Option.builder()
                .longOpt(OPTION_LONG_DUMP_INLINE_FUNCTIONS)
                .hasArg()
                .desc("save names of inline functions in the output C program to file; each name constitutes a single line in the written file")
                .argName("file")
                .build(),
            Option.builder()
                .longOpt(OPTION_LONG_PRINT_BANKING_STATS)
                .desc("print statistics of functions defined in output C files: time of functions sizes estimation, time of functions partition, count of inline functions, count of banked functions, count of functions that are not banked and sum of all these counts which is the total number of functions")
                .build(),
            Option.builder(OPTION_SHORT_PARTITION_HEURISTIC)
                .longOpt(OPTION_LONG_PARTITION_HEURISTIC)
                .hasArg()
                .desc("heuristic to use for partitioning of functions into banks; available heuristics: simple, bcomponents, greedy-n (where n is an arbitrary positive natural number), tmsearch-n-m (where n and m are arbitrary natural numbers, n is the maximum count of iterations and m is the maximum count of consecutive fruitless iterations); if this option is not specified, then heuristic 'bcomponents' is used")
                .argName("heuristic-kind")
                .build(),
            Option.builder()
                .longOpt(OPTION_LONG_SPANNING_FOREST)
                .hasArg()
                .desc("kind of spanning forest that will be used by the biconnected components heuristic; available kinds: original, minimum, maximum; original implies that the spanning forest constructed during the computation of biconnected components will be used; if this option is not specified, then original spanning forest is used; this option has no effect if the biconnected components heuristic is not used for the partitioning")
                .argName("forest-kind")
                .build(),
            Option.builder()
                .longOpt(OPTION_LONG_PREFER_HIGHER_ESTIMATE_ALLOCATIONS)
                .desc("prefer allocations with higher frequency estimates in the biconnected components heuristic; if the biconnected components heuristic is not used for the partitioning, this option has no effect")
                .build(),
            Option.builder()
                .longOpt(OPTION_LONG_ARBITRARY_SUBTREE_PARTITIONING)
                .hasArg()
                .desc("select an arbitrary subtree partitioning mode in the biconnected components heuristic; available modes: always, emergency, never; if this option is not specified, mode 'never' is used; if the biconnected components heuristic is not used for the partitioning, this option has no effect")
                .argName("mode")
                .build(),
            Option.builder()
                .longOpt(OPTION_LONG_LOOP_FACTOR)
                .hasArg()
                .desc("use the given number as the loop factor in the biconnected components heuristic; the number must be greater than or equal to 1; if the biconnected components heuristic is not selected for the partitioning, this option has no effect")
                .argName("number")
                .build(),
            Option.builder()
                .longOpt(OPTION_LONG_CONDITIONAL_FACTOR)
                .hasArg()
                .desc("use the specified number as the conditional factor in the biconnected components heuristic; the number must be a floating-point number from range [0, 1]; if the biconnected components heuristic is not selected for the partitioning, this option has no effect")
                .argName("number")
                .build(),
            Option.builder()
                .longOpt(OPTION_LONG_COMMON_BANK_ALLOCATION_ALGORITHM)
                .hasArg()
                .desc("use the specified algorithm for allocation to the common bank in the first stage of the biconnected components heuristic; available algorithms: greedy-estimations, 2approx, nop; if this option is not specified, then algorithm 'greedy-estimations' will be used; if the biconnected components heuristic is not selected for the partitioning, this option has no effect")
                .argName("algorithm")
                .build(),
            Option.builder()
                .longOpt(OPTION_LONG_BANK_CHOICE_METHOD_CUT_VERTICES)
                .hasArg()
                .desc("use the specified method for choosing the target bank for a candidate allocation in a cut vertex in the biconnected components heuristic; available methods: floor, ceiling; if this option is not specified, then method 'floor' is used; if the biconnected components heuristic is not selected for the partitioning, this option has no effect")
                .argName("method")
                .build(),
            Option.builder()
                .longOpt(OPTION_LONG_BANK_CHOICE_METHOD_DFS)
                .hasArg()
                .desc("use the specified method for choosing the target bank for the DFS allocation in the biconnected components heuristic; available methods: floor, ceiling; if this option is not specified, then method 'ceiling' is used; if the biconnected components heuristic is not selected for the partitioning, this option has no effect")
                .argName("method")
                .build(),
            Option.builder()
                .longOpt(OPTION_LONG_BANK_CHOICE_METHOD_ASP)
                .hasArg()
                .desc("use the specified method for choosing the target bank for the arbitrary subtree partitioning in the biconnected components heuristic; available methods: floor, ceiling; if this option is not specified, then all banks will be considered for an allocation; if the biconnected components heuristic is not selected for the partitioning, this option has no effect")
                .argName("method")
                .build(),
        };

        final Iterable<Option> optionsSource = FluentIterable.from(new OptionsLoader().load())
                .append(options8051);
        Options allOptions = new Options();

        for (Option option : optionsSource) {
            allOptions = allOptions.addOption(option);
        }
        allOptions.addOptionGroup(newMemoryModelGroup());

        return allOptions;
    }

    /**
     * Create a new group of options that correspond to the memory model.
     *
     * @return Newly created option group with options related to the memory
     *         model.
     */
    private OptionGroup newMemoryModelGroup() {
        final Option[] memoryModelOptions = {
            Option.builder().longOpt(OPTION_LONG_MODEL_SMALL)
                    .desc("assume small SDCC memory model during compilation")
                    .build(),
            Option.builder().longOpt(OPTION_LONG_MODEL_MEDIUM)
                    .desc("assume medium SDCC memory model during compilation")
                    .build(),
            Option.builder().longOpt(OPTION_LONG_MODEL_LARGE)
                    .desc("assume large SDCC memory model during compilation")
                    .build(),
            Option.builder().longOpt(OPTION_LONG_MODEL_HUGE)
                    .desc("assume huge SDCC memory model during compilation")
                    .build()
        };

        final OptionGroup memoryModelGroup = new OptionGroup();
        for (Option memoryModelOption : memoryModelOptions) {
            memoryModelGroup.addOption(memoryModelOption);
        }

        return memoryModelGroup;
    }
}
