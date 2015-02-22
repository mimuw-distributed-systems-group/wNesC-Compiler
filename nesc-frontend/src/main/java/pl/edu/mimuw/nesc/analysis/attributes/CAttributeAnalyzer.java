package pl.edu.mimuw.nesc.analysis.attributes;

import com.google.common.base.Optional;
import java.util.List;
import pl.edu.mimuw.nesc.analysis.SemanticListener;
import pl.edu.mimuw.nesc.ast.StructKind;
import pl.edu.mimuw.nesc.ast.gen.Attribute;
import pl.edu.mimuw.nesc.astutil.PredicateTester;
import pl.edu.mimuw.nesc.astutil.predicates.AttributePredicates;
import pl.edu.mimuw.nesc.declaration.Declaration;
import pl.edu.mimuw.nesc.declaration.object.ConstantDeclaration;
import pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration;
import pl.edu.mimuw.nesc.declaration.object.TypenameDeclaration;
import pl.edu.mimuw.nesc.declaration.object.VariableDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.EnumDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.TagDeclaration;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.environment.ScopeType;
import pl.edu.mimuw.nesc.problem.ErrorHelper;
import pl.edu.mimuw.nesc.problem.issue.ErroneousIssue;
import pl.edu.mimuw.nesc.problem.issue.InvalidCAttributeUsageError;

import static com.google.common.base.Preconditions.checkNotNull;
import static pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration.FunctionType.COMMAND;
import static pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration.FunctionType.EVENT;
import static pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration.FunctionType.STATIC;
import static pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration.FunctionType.TASK;

/**
 * Class of objects responsible for analysis of '@C()' attribute.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class CAttributeAnalyzer implements AttributeSmallAnalyzer {
    /**
     * Unique name of the declaration with @C() attribute.
     */
    private String uniqueName;

    /**
     * Original name of the declaration with @C() attribute.
     */
    private String name;

    /**
     * Object that will be notified about detected errors.
     */
    private final ErrorHelper errorHelper;

    /**
     * Object used to generate semantic events.
     */
    private final SemanticListener semanticListener;

    CAttributeAnalyzer(ErrorHelper errorHelper, SemanticListener semanticListener) {
        checkNotNull(errorHelper, "error helper cannot be null");
        checkNotNull(semanticListener, "semantic listener cannot be null");
        this.errorHelper = errorHelper;
        this.semanticListener = semanticListener;
    }

    @Override
    public void analyzeAttribute(List<Attribute> attributes, Declaration declaration, Environment environment) {
        final PredicateTester<Attribute> tester = new PredicateTester<>(AttributePredicates.getCPredicate());
        if (!tester.test(attributes)) {
            return;
        }

        final Optional<? extends ErroneousIssue> error;
        if (environment.getScopeType() != ScopeType.MODULE_IMPLEMENTATION) {
            error = Optional.of(InvalidCAttributeUsageError.invalidScope());
        } else if (environment.isEnclosedInGenericNescEntity()) {
            error = Optional.of(InvalidCAttributeUsageError.usageInGenericComponent());
        } else if (declaration instanceof FunctionDeclaration) {
            error = analyzeFunctionAttribute((FunctionDeclaration) declaration);
        } else if (declaration instanceof TagDeclaration) {
            error = analyzeTagAttribute((TagDeclaration) declaration);
        } else if (declaration instanceof TypenameDeclaration) {
            error = analyzeTypenameDeclaration((TypenameDeclaration) declaration);
        } else if (declaration instanceof VariableDeclaration) {
            error = analyzeVariableDeclaration((VariableDeclaration) declaration);
        } else {
            error = Optional.of(InvalidCAttributeUsageError.appliedToInvalidEntity());
        }

        if (error.isPresent()) {
            this.errorHelper.error(tester.getStartLocation().get(),
                    tester.getEndLocation().get(), error.get());
        } else {
            this.semanticListener.globalName(this.uniqueName, this.name);
        }
    }

    private Optional<? extends ErroneousIssue> analyzeFunctionAttribute(FunctionDeclaration declaration) {
        final FunctionDeclaration.FunctionType funKind = declaration.getFunctionType();

        if (funKind == COMMAND || funKind == EVENT || funKind == TASK) {
            return Optional.of(InvalidCAttributeUsageError.usageForNescObject());
        } else if (funKind == STATIC) {
            return Optional.of(InvalidCAttributeUsageError.internalLinkage());
        } else {
            this.uniqueName = declaration.getUniqueName();
            this.name = declaration.getName();
            return Optional.absent();
        }
    }

    private Optional<? extends ErroneousIssue> analyzeTagAttribute(TagDeclaration declaration) {
        if (!declaration.getName().isPresent()) {
            return Optional.of(InvalidCAttributeUsageError.appliedToUnnamedTag());
        } else if (declaration.getKind() == StructKind.ATTRIBUTE) {
            return Optional.of(InvalidCAttributeUsageError.appliedToAttribute());
        } else {
            this.uniqueName = declaration.getUniqueName().get();
            this.name = declaration.getName().get();

            /* Handle names of enumeration constants from enumerations
               with @C() attribute. Global enumeration constants are handled
               in 'Declarations.makeEnumerator' method. Currently, @C()
               attribute is forbidden for anonymous tags so constants from
               anonymous enumerations have unmangled names only if they are
               declared in the global scope. */
            if (declaration.getKind() == StructKind.ENUM && declaration.isDefined()) {
                emitEnumeratorsGlobalNameEvents(((EnumDeclaration) declaration).getConstants().get());
            }

            return Optional.absent();
        }
    }

    private Optional<? extends ErroneousIssue> analyzeVariableDeclaration(VariableDeclaration declaration) {
        this.uniqueName = declaration.getUniqueName();
        this.name = declaration.getName();
        return Optional.absent();
    }

    private Optional<? extends ErroneousIssue> analyzeTypenameDeclaration(TypenameDeclaration declaration) {
        this.uniqueName = declaration.getUniqueName();
        this.name = declaration.getName();
        return Optional.absent();
    }

    /**
     * Unconditionally emits global name events for given enumeration
     * constants.
     *
     * @param constants Constants of an enumerated type.
     */
    private void emitEnumeratorsGlobalNameEvents(List<ConstantDeclaration> constants) {
        for (ConstantDeclaration constantDeclaration : constants) {
            this.semanticListener.globalName(constantDeclaration.getUniqueName(),
                    constantDeclaration.getName());
        }
    }
}
