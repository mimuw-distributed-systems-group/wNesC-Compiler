package pl.edu.mimuw.nesc.astbuilding;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableListMultimap;
import pl.edu.mimuw.nesc.analysis.AttributeAnalyzer;
import pl.edu.mimuw.nesc.analysis.SemanticListener;
import pl.edu.mimuw.nesc.ast.gen.IdLabel;
import pl.edu.mimuw.nesc.declaration.label.LabelDeclaration;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.environment.NescEntityEnvironment;
import pl.edu.mimuw.nesc.problem.NescIssue;
import pl.edu.mimuw.nesc.problem.issue.InvalidLabelDeclarationError;
import pl.edu.mimuw.nesc.problem.issue.RedeclarationError;
import pl.edu.mimuw.nesc.problem.issue.RedefinitionError;
import pl.edu.mimuw.nesc.symboltable.LabelSymbolTable;
import pl.edu.mimuw.nesc.token.Token;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class Labels extends AstBuildingBase {

    public Labels(NescEntityEnvironment nescEnvironment,
                  ImmutableListMultimap.Builder<Integer, NescIssue> issuesMultimapBuilder,
                  ImmutableListMultimap.Builder<Integer, Token> tokensMultimapBuilder,
                  SemanticListener semanticListener,
                  AttributeAnalyzer attributeAnalyzer) {
        super(nescEnvironment, issuesMultimapBuilder, tokensMultimapBuilder,
                semanticListener, attributeAnalyzer);
    }

    public void defineLabel(Environment environment, IdLabel label) {
        if (!environment.getLabels().isPresent()) {
            errorHelper.error(label.getLocation(), label.getEndLocation(),
                    InvalidLabelDeclarationError.invalidLocation());
            return;
        }

        final LabelSymbolTable<LabelDeclaration> labelsTable = environment.getLabels().get();
        final Optional<? extends LabelDeclaration> optLabelDeclaration =
                labelsTable.getFromCurrentFunction(label.getId());

        // Check if the table has been already declared
        if (optLabelDeclaration.isPresent()) {
            final LabelDeclaration declaration = optLabelDeclaration.get();

            if (declaration.isDefined()) {
                errorHelper.error(label.getLocation(), label.getEndLocation(),
                        new RedefinitionError(label.getId(), RedefinitionError.RedefinitionKind.LABEL));
            }

            // Update state
            declaration.defined();
            label.setDeclaration(declaration);
        } else {
            final LabelDeclaration declaration = LabelDeclaration.builder()
                    .name(label.getId())
                    .nonlocal()
                    .startLocation(label.getLocation())
                    .build();
            labelsTable.addFunctionScopeLabel(label.getId(), declaration);
            label.setDeclaration(declaration);
        }
    }

    public void declareLocalLabel(Environment environment, IdLabel label) {
        if (!environment.getLabels().isPresent()) {
            errorHelper.error(label.getLocation(), label.getEndLocation(),
                    InvalidLabelDeclarationError.invalidLocation());
            return;
        }

        final LabelSymbolTable<LabelDeclaration> labelsTable = environment.getLabels().get();
        final Optional<? extends LabelDeclaration> optLabelDeclaration =
                labelsTable.get(label.getId(), true);

        if (!optLabelDeclaration.isPresent()) {
            final LabelDeclaration declaration = LabelDeclaration.builder()
                    .name(label.getId())
                    .local()
                    .startLocation(label.getLocation())
                    .build();
            labelsTable.add(label.getId(), declaration);
            label.setDeclaration(declaration);
        } else {
            errorHelper.error(label.getLocation(), label.getEndLocation(),
                    new RedeclarationError(label.getId(), RedeclarationError.RedeclarationKind.LABEL));
        }
    }
}
