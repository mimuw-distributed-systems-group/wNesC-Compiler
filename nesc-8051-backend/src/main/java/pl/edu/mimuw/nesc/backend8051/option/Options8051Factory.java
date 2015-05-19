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
                .desc("save the call graph of functions in the output C program to file")
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
