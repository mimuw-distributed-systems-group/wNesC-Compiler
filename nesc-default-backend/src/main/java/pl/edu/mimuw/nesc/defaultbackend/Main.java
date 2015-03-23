package pl.edu.mimuw.nesc.defaultbackend;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.SetMultimap;
import java.io.IOException;
import org.apache.log4j.Level;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.astwriting.ASTWriter;
import pl.edu.mimuw.nesc.astwriting.WriteSettings;
import pl.edu.mimuw.nesc.common.util.VariousUtils;
import pl.edu.mimuw.nesc.compilation.CompilationExecutor;
import pl.edu.mimuw.nesc.compilation.CompilationResult;
import pl.edu.mimuw.nesc.compilation.DefaultCompilationListener;
import pl.edu.mimuw.nesc.compilation.ErroneousIssueException;
import pl.edu.mimuw.nesc.exception.InvalidOptionsException;
import pl.edu.mimuw.nesc.externalvar.ExternalVariablesWriter;

/**
 * <p>Class with <code>main</code> method that allows usage of the compiler. It
 * performs all steps of the compilation of a NesC program in the default
 * backend. A single file with C source code is created as the result.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class Main {
    /**
     * Error code returned if the compilation succeeds.
     */
    private static final int STATUS_SUCCESS = 0;

    /**
     * Error code returned if the compilation fails.
     */
    private static final int STATUS_ERROR = 1;

    public static void main(String[] args) {
        try {
            VariousUtils.setLoggingLevel(Level.OFF);
            new Main().compile(args);
            System.exit(STATUS_SUCCESS);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("An error occurred: " + e.getMessage());
            System.exit(STATUS_ERROR);
        }
    }

    private Main() {
    }

    /**
     * Performs the whole compilation process. This method should be called
     * exactly once per instance of this class.
     */
    private void compile(String[] args) throws InvalidOptionsException {
        try {
            final CompilationExecutor executor = new CompilationExecutor();
            executor.setListener(new DefaultCompilationListener());
            final CompilationResult result = executor.compile(args);
            writeCode(result.getDeclarations(), result.getOutputFileName());
            writeExternalVariables(result.getDeclarations(), result.getExternalVariables(),
                    result.getExternalVariablesFileName());
        } catch (ErroneousIssueException e) {
            System.exit(STATUS_ERROR);
        }
    }

    /**
     * Write the generated code to the file.
     *
     * @param finalCode All declarations to write in proper order.
     */
    private void writeCode(ImmutableList<Declaration> finalCode, String outputFile) {
        final WriteSettings writeSettings = WriteSettings.builder()
                .charset("UTF-8")
                .indentWithSpaces(3)
                .nameMode(WriteSettings.NameMode.USE_UNIQUE_NAMES)
                .uniqueMode(WriteSettings.UniqueMode.OUTPUT_VALUES)
                .build();

        try (ASTWriter writer = new ASTWriter(outputFile, writeSettings)) {
            writer.write(finalCode);
        } catch(IOException e) {
            System.err.println("Cannot write the code to the file: " + e.getMessage());
            System.exit(STATUS_ERROR);
        }
    }

    private void writeExternalVariables(ImmutableList<Declaration> finalCode,
            SetMultimap<Optional<String>, String> externalVariables,
            Optional<String> externalVariablesFile) {
        if (!externalVariablesFile.isPresent()) {
            return;
        }

        try {
            new ExternalVariablesWriter(
                    finalCode,
                    externalVariablesFile.get(),
                    "UTF-8",
                    externalVariables
            ).write();
        } catch (IOException e) {
            System.err.println("Cannot write the external variables file: " + e.getMessage());
            System.exit(STATUS_ERROR);
        }
    }
}
