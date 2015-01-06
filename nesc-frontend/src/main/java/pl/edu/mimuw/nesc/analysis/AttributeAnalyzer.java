package pl.edu.mimuw.nesc.analysis;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.util.List;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.StructKind;
import pl.edu.mimuw.nesc.ast.gen.Attribute;
import pl.edu.mimuw.nesc.ast.gen.ErrorExpr;
import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.gen.InitList;
import pl.edu.mimuw.nesc.ast.gen.InitSpecific;
import pl.edu.mimuw.nesc.ast.gen.NescAttribute;
import pl.edu.mimuw.nesc.ast.gen.StringAst;
import pl.edu.mimuw.nesc.astutil.AstUtils;
import pl.edu.mimuw.nesc.astutil.CAttributePredicate;
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
import pl.edu.mimuw.nesc.problem.issue.InvalidCombineAttributeUsageError;

import static com.google.common.base.Preconditions.checkNotNull;
import static pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration.FunctionType.*;

/**
 * <p>Class of objects responsible for analyzing attributes and performing all
 * necessary actions associated with them.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class AttributeAnalyzer {
    /**
     * Analyzers that will perform the analysis.
     */
    private final ImmutableList<AttributeSmallAnalyzer> analyzersChain;

    /**
     * Object used to generate occurred semantic events.
     */
    private final SemanticListener semanticListener;

    /**
     * Object that will be notified about detected errors.
     */
    private final ErrorHelper errorHelper;

    public AttributeAnalyzer(SemanticListener semanticListener, ErrorHelper errorHelper) {
        checkNotNull(semanticListener, "semantic listener cannot be null");
        checkNotNull(errorHelper, "error helper cannot be null");

        this.analyzersChain = ImmutableList.of(
                new CombineAttributeAnalyzer(),
                new CAttributeAnalyzer()
        );
        this.semanticListener = semanticListener;
        this.errorHelper = errorHelper;
    }

    public void analyzeAttributes(List<Attribute> attributes, Declaration declaration,
            Environment environment) {
        checkNotNull(attributes, "attributes list cannot be null");
        checkNotNull(declaration, "declaration cannot be null");
        checkNotNull(environment, "environment cannot be null");

        for (AttributeSmallAnalyzer smallAnalyzer : analyzersChain) {
            smallAnalyzer.analyzeAttribute(attributes, declaration, environment);
        }
    }

    /**
     * Interface for a small analyzer. It it intended to analyze a single
     * attribute, e.g. '@C()'.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private interface AttributeSmallAnalyzer {
        /**
         * Performs all necessary actions for analysis of a single attribute,
         * e.g. generates semantic events or notifies the error helper about
         * detected errors.
         *
         * @param attributes All attributes applied to an entity.
         * @param declaration Entity that the attributes are applied to.
         * @param environment Environment of the declaration.
         */
        void analyzeAttribute(List<Attribute> attributes, Declaration declaration, Environment environment);
    }

    /**
     * Class of objects responsible for analysis of '@C()' attribute.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class CAttributeAnalyzer implements AttributeSmallAnalyzer {
        /**
         * Unique name of the declaration with @C() attribute.
         */
        private String uniqueName;

        /**
         * Original name of the declaration with @C() attribute.
         */
        private String name;

        @Override
        public void analyzeAttribute(List<Attribute> attributes, Declaration declaration, Environment environment) {
            final CAttributePredicate predicate = new CAttributePredicate();
            if (!predicate.apply(attributes)) {
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
                AttributeAnalyzer.this.errorHelper.error(predicate.getStartLocation().get(),
                        predicate.getEndLocation().get(), error.get());
            } else {
                AttributeAnalyzer.this.semanticListener.globalName(this.uniqueName, this.name);
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
                    emitEnumeratorsGlobalNameEvents(((EnumDeclaration) declaration).getEnumerators().get());
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
                AttributeAnalyzer.this.semanticListener.globalName(constantDeclaration.getUniqueName(),
                        constantDeclaration.getName());
            }
        }
    }

    /**
     * Object responsible for analysis of '@combine' attribute in a single type
     * definition declaration.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class CombineAttributeAnalyzer implements AttributeSmallAnalyzer {
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
         * Private constructor to limit its accessibility.
         */
        private CombineAttributeAnalyzer() {
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
                    AttributeAnalyzer.this.semanticListener.combiningFunction(typenameDeclaration.getUniqueName(),
                            combiningFunctionName.get());
                } else if (error.isPresent()) {
                    AttributeAnalyzer.this.errorHelper.error(declaration.getLocation(),
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
                AttributeAnalyzer.this.errorHelper.error(nescAttribute.getLocation(),
                        nescAttribute.getEndLocation(), error.get());
            }
        }
    }
}
