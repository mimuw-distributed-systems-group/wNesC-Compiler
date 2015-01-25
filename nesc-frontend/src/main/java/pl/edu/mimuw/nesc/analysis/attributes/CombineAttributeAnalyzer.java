package pl.edu.mimuw.nesc.analysis.attributes;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import java.util.List;
import pl.edu.mimuw.nesc.analysis.SemanticListener;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.Attribute;
import pl.edu.mimuw.nesc.ast.gen.ErrorExpr;
import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.gen.InitList;
import pl.edu.mimuw.nesc.ast.gen.InitSpecific;
import pl.edu.mimuw.nesc.ast.gen.NescAttribute;
import pl.edu.mimuw.nesc.ast.gen.StringAst;
import pl.edu.mimuw.nesc.astutil.AstUtils;
import pl.edu.mimuw.nesc.declaration.Declaration;
import pl.edu.mimuw.nesc.declaration.object.TypenameDeclaration;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.problem.ErrorHelper;
import pl.edu.mimuw.nesc.problem.issue.ErroneousIssue;
import pl.edu.mimuw.nesc.problem.issue.InvalidCombineAttributeUsageError;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Object responsible for analysis of '@combine' attribute in a single type
 * definition declaration.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class CombineAttributeAnalyzer implements AttributeSmallAnalyzer {
    /**
     * Name of the '@combine' attribute
     */
    private static final String COMBINE_ATTRIBUTE_NAME = "combine";

    /**
     * Count of arguments expected for '@combine' attribute.
     */
    private static final int COMBINE_EXPECTED_ARGUMENTS_COUNT = 1;

    /**
     * Counter of '@combine' attributes that occurred on the list.
     */
    private int combineAttributesCount;

    /**
     * Variable for the name of the detected combining function name.
     */
    private Optional<String> combiningFunctionName = Optional.absent();

    /**
     * Object that will be notified about detected errors.
     */
    private final ErrorHelper errorHelper;

    /**
     * Object that will be used to generate events.
     */
    private final SemanticListener semanticListener;

    /**
     * Private constructor to limit its accessibility.
     */
    CombineAttributeAnalyzer(ErrorHelper errorHelper, SemanticListener semanticListener) {
        checkNotNull(errorHelper, "error helper cannot be null");
        checkNotNull(semanticListener, "semantic listener cannot be null");
        this.errorHelper = errorHelper;
        this.semanticListener = semanticListener;
    }

    @Override
    public void analyzeAttribute(List<Attribute> attributes, Declaration declaration, Environment environment) {
        final FluentIterable<NescAttribute> nescAttributes = FluentIterable.from(attributes)
                .filter(NescAttribute.class);
        this.combineAttributesCount = 0;
        this.combiningFunctionName = Optional.absent();

        for (NescAttribute nescAttribute : nescAttributes) {
            analyzeAttribute(nescAttribute);
        }

        if (this.combineAttributesCount > 0) {
            final Optional<? extends ErroneousIssue> error;

            if (!(declaration instanceof TypenameDeclaration)) {
                error = Optional.of(InvalidCombineAttributeUsageError.appliedNotToTypedef());
            } else if (this.combineAttributesCount > 1) {
                error = Optional.of(InvalidCombineAttributeUsageError.usedMoreThanOnce());
            } else {
                error = Optional.absent();
            }

            if (!error.isPresent() && this.combiningFunctionName.isPresent()) {
                final TypenameDeclaration typenameDeclaration = (TypenameDeclaration) declaration;
                this.semanticListener.combiningFunction(typenameDeclaration.getUniqueName(),
                        combiningFunctionName.get());
            } else if (error.isPresent()) {
                this.errorHelper.error(declaration.getLocation(),
                        Optional.<Location>absent(), error.get());
            }
        }
    }

    private void analyzeAttribute(NescAttribute nescAttribute) {
        if (!COMBINE_ATTRIBUTE_NAME.equals(nescAttribute.getName().getName())) {
            return;
        }

        ++this.combineAttributesCount;
        final InitList initializer = (InitList) nescAttribute.getValue();
        final Optional<? extends ErroneousIssue> error;
        boolean designatorPresent = false;

        for (Expression param : initializer.getArguments()) {
            if (param instanceof InitSpecific) {
                designatorPresent = true;
            }
        }

        if (designatorPresent) {
            error = Optional.of(InvalidCombineAttributeUsageError.designatorsUsed());
        } else if (initializer.getArguments().size() != COMBINE_EXPECTED_ARGUMENTS_COUNT) {
            error = Optional.of(InvalidCombineAttributeUsageError.invalidParamsCount(
                    initializer.getArguments().size(), COMBINE_EXPECTED_ARGUMENTS_COUNT));
        } else if (!(initializer.getArguments().getFirst() instanceof StringAst)
                && !(initializer.getArguments().getFirst() instanceof ErrorExpr)) {
            error = Optional.of(InvalidCombineAttributeUsageError.stringLiteralExpected(
                    initializer.getArguments().getFirst()));
        } else if (initializer.getArguments().getFirst() instanceof StringAst) {
            final StringAst funNameAst = (StringAst) initializer.getArguments().getFirst();
            final String funName = AstUtils.concatenateStrings(funNameAst);

            if (funName.isEmpty()) {
                error = Optional.of(InvalidCombineAttributeUsageError.emptyStringLiteralUsed());
            } else {
                error = Optional.absent();
                this.combiningFunctionName = Optional.of(funName);
            }
        } else {
            error = Optional.absent();
        }

        if (error.isPresent()) {
            this.errorHelper.error(nescAttribute.getLocation(),
                    nescAttribute.getEndLocation(), error.get());
        }
    }
}
