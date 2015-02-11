package pl.edu.mimuw.nesc.finalanalysis;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableListMultimap;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import pl.edu.mimuw.nesc.abi.ABI;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.constexpr.ConstExprInterpreter;
import pl.edu.mimuw.nesc.constexpr.Interpreter;
import pl.edu.mimuw.nesc.constexpr.value.ConstantValue;
import pl.edu.mimuw.nesc.constexpr.value.IntegerConstantValue;
import pl.edu.mimuw.nesc.constexpr.value.type.ConstantType;
import pl.edu.mimuw.nesc.declaration.object.ConstantDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.*;
import pl.edu.mimuw.nesc.declaration.tag.fieldtree.BlockElement;
import pl.edu.mimuw.nesc.declaration.tag.fieldtree.FieldElement;
import pl.edu.mimuw.nesc.declaration.tag.fieldtree.TreeElement;
import pl.edu.mimuw.nesc.problem.ErrorHelper;
import pl.edu.mimuw.nesc.problem.NescIssue;
import pl.edu.mimuw.nesc.problem.issue.ErroneousIssue;
import pl.edu.mimuw.nesc.problem.issue.InvalidBitFieldDeclarationError;
import pl.edu.mimuw.nesc.problem.issue.InvalidExternalTagFieldError;
import pl.edu.mimuw.nesc.type.EnumeratedType;
import pl.edu.mimuw.nesc.type.FieldTagType;
import pl.edu.mimuw.nesc.type.Type;
import pl.edu.mimuw.nesc.typelayout.UniversalTypeLayoutCalculator;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * <p>Class responsible for performing the final part of analysis after
 * instantiation of components and evaluating compile-time constant functions.
 * </p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class FinalAnalyzer {
    /**
     * Visitor that will analyze for this analyzer.
     */
    private final FinalAnalysisVisitor analysisVisitor = new FinalAnalysisVisitor();

    /**
     * ABI to use during the analysis.
     */
    private final ABI abi;

    /**
     * Interpreter for evaluation of constant expressions.
     */
    private final Interpreter interpreter;

    /**
     * Issues that come from the analysis.
     */
    private final ImmutableListMultimap.Builder<Integer, NescIssue> issuesBuilder;

    /**
     * Error helper used to update issues list.
     */
    private final ErrorHelper errorHelper;

    public FinalAnalyzer(ABI abi) {
        checkNotNull(abi, "ABI cannot be null");

        this.abi = abi;
        this.issuesBuilder = ImmutableListMultimap.builder();
        this.errorHelper = new ErrorHelper(this.issuesBuilder);
        this.interpreter = new ConstExprInterpreter(abi);
    }

    /**
     * Get the map with issues that has come from the analysis.
     *
     * @return Multimap with issues that arose from the analysis.
     */
    public ImmutableListMultimap<Integer, NescIssue> getIssues() {
        return issuesBuilder.build();
    }

    /**
     * Perform the analysis of given node.
     *
     * @param node Node to be analysed.
     */
    public void analyze(Node node) {
        checkNotNull(node, "node cannot be null");
        node.traverse(analysisVisitor, null);
    }

    /**
     * Visitor that actually performs the final analysis.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class FinalAnalysisVisitor extends IdentityVisitor<Void>
                implements TreeElement.Visitor<Boolean, FieldTagDeclaration<?>> {
        /**
         * Visitor that facilities analysis of tags.
         */
        private final TagDeclaration.Visitor<Void, Void> tagDeclarationSwitch =
                    new TagDeclaration.Visitor<Void, Void>() {

            @Override
            public Void visit(AttributeDeclaration attribute, Void arg) {
                analyzeTag(attribute);
                return null;
            }

            @Override
            public Void visit(EnumDeclaration enumDeclaration, Void arg) {
                analyzeTag(enumDeclaration);
                return null;
            }

            @Override
            public Void visit(StructDeclaration struct, Void arg) {
                analyzeTag(struct);
                return null;
            }

            @Override
            public Void visit(UnionDeclaration union, Void arg) {
                analyzeTag(union);
                return null;
            }
        };

        @Override
        public Void visitStructRef(StructRef tagRef, Void arg) {
            analyzeTag(tagRef.getDeclaration());
            return null;
        }

        @Override
        public Void visitUnionRef(UnionRef tagRef, Void arg) {
            analyzeTag(tagRef.getDeclaration());
            return null;
        }

        @Override
        public Void visitNxStructRef(NxStructRef tagRef, Void arg) {
            analyzeTag(tagRef.getDeclaration());
            return null;
        }

        @Override
        public Void visitNxUnionRef(NxUnionRef tagRef, Void arg) {
            analyzeTag(tagRef.getDeclaration());
            return null;
        }

        @Override
        public Void visitEnumRef(EnumRef tagRef, Void arg) {
            analyzeTag(tagRef.getDeclaration());
            return null;
        }

        private void analyzeTag(FieldTagDeclaration<?> declaration) {
            if (!declaration.isDefined() || declaration.isCorrect().isPresent()) {
                return;
            }

            boolean isCorrect = true;

            for (TreeElement element : declaration.getStructure().get()) {
                isCorrect = element.accept(this, declaration) && isCorrect;
            }

            declaration.setIsCorrect(isCorrect);
        }

        private void analyzeTag(EnumDeclaration declaration) {
            // TODO check if values of constants are not too large or too small

            if (!declaration.isDefined() || declaration.isCorrect().isPresent()) {
                return;
            }

            final TagCheckingVisitor tagCheckingVisitor = new TagCheckingVisitor();

            for (ConstantDeclaration constant : declaration.getConstants().get()) {
                if (constant.getEnumerator().getValue().isPresent()) {
                    constant.getEnumerator().getValue().get().accept(tagCheckingVisitor, null);
                }
            }

            declaration.setIsCorrect(!tagCheckingVisitor.errorFlag);
        }

        private void analyzeTag(TagDeclaration tagDeclaration) {
            tagDeclaration.accept(tagDeclarationSwitch, null);
        }

        @Override
        public Boolean visit(FieldElement fieldElement, FieldTagDeclaration<?> tagDeclaration) {
            final FieldDeclaration fieldDeclaration = fieldElement.getFieldDeclaration();
            final boolean isExternalTag = tagDeclaration.getKind().isExternal();
            final List<ErroneousIssue> errors = new LinkedList<>();
            final boolean bitFieldError;

            // Check type of the field

            if (isExternalTag && !fieldDeclaration.getType().get().isExternal()) {
                errors.add(InvalidExternalTagFieldError.fieldOfNonExternalType(
                        fieldDeclaration.getName(), fieldDeclaration.getType().get(),
                        tagDeclaration.getKind()));
            } else if (!isExternalTag && fieldDeclaration.getType().get().isExternal()
                    && fieldDeclaration.isBitField()) {
                errors.add(InvalidBitFieldDeclarationError.externalBitFieldInNonexternalTag(
                        fieldDeclaration.getName(), tagDeclaration.getKind()));
            }

            // Check the width if the field is a bit-field

            if (fieldDeclaration.isBitField()) {
                bitFieldError = checkBitField(fieldDeclaration, errors);
            } else {
                bitFieldError = false;
            }

            // Report all errors

            for (ErroneousIssue error : errors) {
                FinalAnalyzer.this.errorHelper.error(fieldDeclaration.getAstField().getLocation(),
                        fieldDeclaration.getAstField().getEndLocation(), error);
            }

            return errors.isEmpty() && !bitFieldError;
        }

        private boolean checkBitField(FieldDeclaration fieldDeclaration, List<ErroneousIssue> errors) {
            final TagCheckingVisitor tagCheckingVisitor = new TagCheckingVisitor();
            final Expression widthExpr = fieldDeclaration.getAstField().getBitfield().get();
            widthExpr.accept(tagCheckingVisitor, null);

            if (tagCheckingVisitor.errorFlag) {
                return true;
            }

            final ConstantValue widthValue = FinalAnalyzer.this.interpreter.evaluate(widthExpr);
            checkState(widthValue.getType().getType() == ConstantType.Type.SIGNED_INTEGER
                     || widthValue.getType().getType() == ConstantType.Type.UNSIGNED_INTEGER,
                    "width of a bit-field has not evaluated to an integer constant");
            final BigInteger width = ((IntegerConstantValue<?>) widthValue).getValue();
            final int typeSizeInBits = 8 * new UniversalTypeLayoutCalculator(abi, fieldDeclaration.getType().get())
                    .calculate()
                    .getSize();

            if (width.signum() < 0) {
                errors.add(InvalidBitFieldDeclarationError.negativeWidth(fieldDeclaration.getName(),
                        width));
                return true;
            } else if (width.compareTo(BigInteger.valueOf(typeSizeInBits)) > 0) {
                errors.add(InvalidBitFieldDeclarationError.tooLargeWidth(fieldDeclaration.getName(),
                        width, fieldDeclaration.getType().get()));
                return true;
            } else {
                return false;
            }
        }

        @Override
        public Boolean visit(BlockElement blockElement, FieldTagDeclaration<?> tagDeclaration) {
            final FieldTagDeclaration<?> innerTagDeclaration = blockElement.getDeclaration();
            analyzeTag(innerTagDeclaration);
            return innerTagDeclaration.isCorrect().get();
        }
    }

    /**
     * <p>Visitor that checks tags that are arguments for <code>sizeof</code>,
     * <code>_Alignof</code> and <code>offsetof</code> expressions.</p>
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class TagCheckingVisitor extends IdentityVisitor<Void> {
        /**
         * Flag that is raised when there is an error and the expression cannot
         * be evaluated.
         */
        private boolean errorFlag = false;

        @Override
        public Void visitSizeofType(SizeofType expr, Void arg) {
            checkType(expr.getAsttype().getType().get());
            return null;
        }

        @Override
        public Void visitSizeofExpr(SizeofExpr expr, Void arg) {
            checkType(expr.getArgument().getType().get());
            return null;
        }

        @Override
        public Void visitAlignofType(AlignofType expr, Void arg) {
            checkType(expr.getAsttype().getType().get());
            return null;
        }

        @Override
        public Void visitAlignofExpr(AlignofExpr expr, Void arg) {
            checkType(expr.getArgument().getType().get());
            return null;
        }

        @Override
        public Void visitOffsetof(Offsetof expr, Void arg) {
            checkType(expr.getTypename().getType().get());

            for (FieldIdentifier fieldIdentifier : expr.getFieldlist()) {
                checkType(fieldIdentifier.getDeclaration().getType().get());
            }

            return null;
        }

        private void checkType(Type type) {
            final Optional<? extends TagDeclaration> declaration;

            if (type.isFieldTagType()) {
                declaration = Optional.of(((FieldTagType<?>) type).getDeclaration());
            } else if (type instanceof EnumeratedType) {
                declaration = Optional.of(((EnumeratedType) type).getEnumDeclaration());
            } else {
                declaration = Optional.absent();
            }

            if (declaration.isPresent()) {
                FinalAnalyzer.this.analysisVisitor.analyzeTag(declaration.get());
                errorFlag = errorFlag || !declaration.get().isCorrect().get();
            }
        }
    }
}
