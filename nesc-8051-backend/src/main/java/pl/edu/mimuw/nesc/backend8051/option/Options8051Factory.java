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
            Option.builder()
                .longOpt(OPTION_LONG_BANK_SIZE)
                .hasArg()
                .desc("size of a single bank (in bytes)")
                .argName("size")
                .build(),
            Option.builder(OPTION_SHORT_BANKS_COUNT)
                .longOpt(OPTION_LONG_BANKS_COUNT)
                .hasArg()
                .desc("count of banks on the target 8051 microcontroller; if not specified, it is assumed that 8 banks are available (including the common area bank)")
                .argName("number")
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
                .desc("save the call graph of functions in the output C program to file")
                .argName("file")
                .build(),
            Option.builder()
                .longOpt(OPTION_LONG_INTERRUPTS)
                .hasArg()
                .desc("comma-separated list of assignments of functions to interrupts numbers; each element of the list consists of the name of the function, '=' and the interrupt number; declarations of functions in the program will be annotated by SDCC attribute '__interrupt' according to value for this option; example interrupts map: sig_timer=1,serial_interrupt=0,external_interrupt=2")
                .argName("interrupts-map")
                .build()
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
