package pl.edu.mimuw.nesc.externalvar;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.SetMultimap;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;
import pl.edu.mimuw.nesc.ast.gen.DataDecl;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.ExceptionVisitor;
import pl.edu.mimuw.nesc.ast.gen.ExtensionDecl;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;
import pl.edu.mimuw.nesc.ast.gen.VariableDecl;
import pl.edu.mimuw.nesc.astutil.DeclaratorUtils;
import pl.edu.mimuw.nesc.declaration.object.ObjectKind;
import pl.edu.mimuw.nesc.declaration.object.VariableDeclaration;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * <p>Class that is responsible for collecting unique names of external
 * variables and writing them to a file.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class ExternalVariablesWriter {
    /**
     * Name of the output file to create.
     */
    private final String outputFileName;

    /**
     * Name of the charset to use while writing the file.
     */
    private final String charsetName;

    /**
     * List with top-level declarations that potentially contain external
     * variables.
     */
    private final ImmutableList<Declaration> declarations;

    /**
     * Multimap that defines the order of writing information about
     * unique names of external variables.
     */
    private final SetMultimap<Optional<String>, String> externalVariables;

    public ExternalVariablesWriter(ImmutableList<Declaration> declarations, String outputFileName,
            String charsetName, SetMultimap<Optional<String>, String> externalVariables) {
        checkNotNull(declarations, "declaration cannot be null");
        checkNotNull(outputFileName, "output file name cannot be null");
        checkNotNull(charsetName, "charset name cannot be null");
        checkNotNull(externalVariables, "external variables cannot be null");
        checkArgument(!outputFileName.isEmpty(), "name of the output file cannot be an empty string");
        checkArgument(!charsetName.isEmpty(), "charset name cannot be an empty string");
        this.outputFileName = outputFileName;
        this.charsetName = charsetName;
        this.declarations = declarations;
        this.externalVariables = externalVariables;
    }

    /**
     * Creates a file with name given at construction that contains unique names
     * of external variables that appear in given declarations in the order
     * defined by the iterator of the multimap given to constructor.
     */
    public void write() throws IOException {
        final ImmutableListMultimap<String, String> uniqueNames = collectUniqueNames();
        writeUniqueNames(uniqueNames);
    }

    private ImmutableListMultimap<String, String> collectUniqueNames() {
        final CollectingVisitor collectingVisitor = new CollectingVisitor();

        for (Declaration declaration : declarations) {
            declaration.accept(collectingVisitor, null);
        }

        return collectingVisitor.getUniqueNames();
    }

    private void writeUniqueNames(ListMultimap<String, String> uniqueNames) throws IOException {
        try (final FileOutputStream outStream = new FileOutputStream(outputFileName)) {
            // Truncate the file
            outStream.getChannel().truncate(0L);
            int writtenCount = 0;

            // Create the writer
            try (final Writer writer = new OutputStreamWriter(outStream, charsetName)) {
                final Iterator<Map.Entry<Optional<String>, String>> entriesIt
                        = externalVariables.entries().iterator();

                // Write the unique names in proper order
                if (entriesIt.hasNext()) {
                    final Map.Entry<Optional<String>, String> firstEntry = entriesIt.next();
                    writtenCount += writeForSingleExternalVariable(writer,
                            firstEntry.getKey(), firstEntry.getValue(), uniqueNames);

                    while (entriesIt.hasNext()) {
                        final Map.Entry<Optional<String>, String> nextEntry = entriesIt.next();

                        writer.write(';');
                        writtenCount += writeForSingleExternalVariable(writer,
                                nextEntry.getKey(), nextEntry.getValue(), uniqueNames);
                    }
                }

                // Write the line terminator at the end
                writer.write(System.lineSeparator());
            }

            if (writtenCount != uniqueNames.size()) {
                throw new IllegalStateException("not all entries have been recognized, "
                        + writtenCount + " recognized, " + uniqueNames.size() + " all");
            }
        }
    }

    private int writeForSingleExternalVariable(Writer writer, Optional<String> componentName, String variableName,
                    ListMultimap<String, String> uniqueNames) throws IOException {
        final String originalExternalName = componentName.isPresent()
                ? format("%s.%s", componentName.get(), variableName)
                : variableName;

        final Iterator<String> uniqueNamesIt = uniqueNames.get(originalExternalName).iterator();

        if (uniqueNamesIt.hasNext()) {
            writer.write(uniqueNamesIt.next());

            while (uniqueNamesIt.hasNext()) {
                writer.write(',');
                writer.write(uniqueNamesIt.next());
            }
        }

        return uniqueNames.get(originalExternalName).size();
    }

    /**
     * Visitor that collects unique names of external variables from visited
     * declarations.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class CollectingVisitor extends ExceptionVisitor<Void, Void> {
        private final ImmutableListMultimap.Builder<String, String> uniqueNamesBuilder = ImmutableListMultimap.builder();

        private ImmutableListMultimap<String, String> getUniqueNames() {
            return uniqueNamesBuilder.build();
        }

        @Override
        public Void visitFunctionDecl(FunctionDecl declaration, Void arg) {
            return null;
        }

        @Override
        public Void visitExtensionDecl(ExtensionDecl declaration, Void arg) {
            declaration.getDeclaration().accept(this, null);
            return null;
        }

        @Override
        public Void visitDataDecl(DataDecl declaration, Void arg) {
            for (Declaration innerDeclaration : declaration.getDeclarations()) {
                innerDeclaration.accept(this, null);
            }
            return null;
        }

        @Override
        public Void visitVariableDecl(VariableDecl declaration, Void arg) {
            if (declaration.getDeclaration() != null
                    && declaration.getDeclaration().getKind() == ObjectKind.VARIABLE) {

                final VariableDeclaration variableDeclaration =
                        (VariableDeclaration) declaration.getDeclaration();

                if (variableDeclaration.isExternalVariable()) {
                    uniqueNamesBuilder.put(variableDeclaration.getOriginalExternalName(),
                            DeclaratorUtils.getUniqueName(declaration.getDeclarator().get()).get());
                }
            }
            return null;
        }
    }
}
