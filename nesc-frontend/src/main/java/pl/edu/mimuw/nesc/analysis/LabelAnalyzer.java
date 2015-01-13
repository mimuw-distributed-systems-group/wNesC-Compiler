package pl.edu.mimuw.nesc.analysis;

import com.google.common.base.Optional;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import pl.edu.mimuw.nesc.ast.gen.AtomicStmt;
import pl.edu.mimuw.nesc.ast.gen.CompoundStmt;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;
import pl.edu.mimuw.nesc.ast.gen.GotoStmt;
import pl.edu.mimuw.nesc.ast.gen.IdLabel;
import pl.edu.mimuw.nesc.ast.gen.IdentityVisitor;
import pl.edu.mimuw.nesc.declaration.label.LabelDeclaration;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.problem.ErrorHelper;
import pl.edu.mimuw.nesc.problem.issue.InvalidGotoStmtError;
import pl.edu.mimuw.nesc.problem.issue.UnplacedLabelError;
import pl.edu.mimuw.nesc.symboltable.LabelSymbolTable;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * <p>Class responsible for analysis of <code>goto</code> statements and labels
 * declarations.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class LabelAnalyzer {
    /**
     * Function definition that will be analyzed.
     */
    private final FunctionDecl toAnalyze;

    /**
     * Object that will be notified about detected errors.
     */
    private final ErrorHelper errorHelper;

    /**
     * Prepares the analysis of the given function definition. Definitions of
     * nested functions will also be analyzed.
     *
     * @param toAnalyze Function definition to analyze.
     * @param errorHelper Object that will be notified about all detected
     *                    errors.
     * @throws NullPointerException One of the arguments is <code>null</code>.
     */
    public LabelAnalyzer(FunctionDecl toAnalyze, ErrorHelper errorHelper) {
        checkNotNull(toAnalyze, "function definition to analyze cannot be null");
        checkNotNull(errorHelper, "error helper cannot be null");

        this.toAnalyze = toAnalyze;
        this.errorHelper = errorHelper;
    }

    /**
     * Performs the analysis on the function definition given at
     * construction-time and notifies the error helper about errors.
     */
    public void analyze() {
        final CompoundStmt funBody = (CompoundStmt) toAnalyze.getBody();
        final PrivateVisitor visitor = new PrivateVisitor();
        funBody.traverse(visitor, new Oracle(funBody.getEnvironment(), Optional.<Set<String>>absent()));
    }

    /**
     * Visitor that performs the actual analysis. The argument is the set of
     * labels names defined in a not nested atomic block.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class PrivateVisitor extends IdentityVisitor<Oracle> {
        @Override
        public Oracle visitAtomicStmt(AtomicStmt stmt, Oracle oracle) {
            checkState(oracle.atomicLabelsNames.isPresent() != stmt.getDeclaredLabelsNames().isPresent(),
                    "inconsistent state of labels names in atomic statements");
            return !oracle.atomicLabelsNames.isPresent()
                    ? oracle.modifyLabelsNames(stmt.getDeclaredLabelsNames())
                    : oracle;
        }

        @Override
        public Oracle visitFunctionDecl(FunctionDecl declaration, Oracle oracle) {
            return oracle.modifyLabelsNames(Optional.<Set<String>>absent());
        }

        @Override
        public Oracle visitCompoundStmt(CompoundStmt stmt, Oracle oracle) {
            checkLabelsDeclarations(stmt.getIdLabels());
            return oracle.modifyEnvironment(stmt.getEnvironment());
        }

        @Override
        public Oracle visitGotoStmt(GotoStmt stmt, Oracle oracle) {
            final String targetLabelName = stmt.getIdLabel().getId();
            final LabelSymbolTable<LabelDeclaration> labelsTable =
                    oracle.environment.getLabels().get();
            final Optional<? extends LabelDeclaration> labelDecl =
                    labelsTable.getLabel(targetLabelName);

            if (!labelDecl.isPresent()) {
                LabelAnalyzer.this.errorHelper.error(
                        stmt.getIdLabel().getLocation(),
                        stmt.getIdLabel().getEndLocation(),
                        UnplacedLabelError.undeclaredLabel(targetLabelName)
                );
            } else if (oracle.atomicLabelsNames.isPresent()
                    && !oracle.atomicLabelsNames.get().contains(targetLabelName)
                    && labelDecl.get().isPlacedInsideAtomicArea()) {
                LabelAnalyzer.this.errorHelper.error(
                        stmt.getLocation(),
                        stmt.getEndLocation(),
                        InvalidGotoStmtError.jumpInsideAnotherAtomicStmt()
                );
            } else if (!oracle.atomicLabelsNames.isPresent()
                    && labelDecl.get().isPlacedInsideAtomicArea()) {
                LabelAnalyzer.this.errorHelper.error(
                        stmt.getLocation(),
                        stmt.getEndLocation(),
                        InvalidGotoStmtError.jumpToAtomicStmtFromNonatomicArea()
                );
            }

            return oracle;
        }

        private void checkLabelsDeclarations(List<IdLabel> labels) {
            /* Collect names of labels not to report multiple errors for the
               same label. */
            final Set<String> labelsNames = new HashSet<>();

            for (IdLabel label : labels) {
                if (!labelsNames.add(label.getId())) {
                    continue;
                }

                if (!label.getDeclaration().isDefined()) {
                    LabelAnalyzer.this.errorHelper.error(
                            label.getLocation(),
                            label.getEndLocation(),
                            UnplacedLabelError.undefinedLocalLabel(label.getId())
                    );
                }
            }
        }
    }

    /**
     * <p>Class that represents important data that aren't known when a node is
     * visited by a {@link PrivateVisitor}, e.g. data about its ancestors.
     * Oracle objects are immutable.</p>
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class Oracle {
        private final Optional<Set<String>> atomicLabelsNames;
        private final Environment environment;

        private Oracle(Environment environment, Optional<Set<String>> atomicLabelsNames) {
            checkNotNull(environment, "environment cannot be null");
            checkNotNull(atomicLabelsNames, "atomic labels names cannot be null");

            this.atomicLabelsNames = atomicLabelsNames;
            this.environment = environment;
        }

        private Oracle modifyEnvironment(Environment environment) {
            return new Oracle(environment, this.atomicLabelsNames);
        }

        private Oracle modifyLabelsNames(Optional<Set<String>> atomicLabelsNames) {
            return new Oracle(this.environment, atomicLabelsNames);
        }
    }
}
