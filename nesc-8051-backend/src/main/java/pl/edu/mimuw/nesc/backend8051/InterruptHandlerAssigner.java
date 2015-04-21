package pl.edu.mimuw.nesc.backend8051;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import pl.edu.mimuw.nesc.abi.ABI;
import pl.edu.mimuw.nesc.ast.gen.Attribute;
import pl.edu.mimuw.nesc.ast.gen.DataDecl;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.ExceptionVisitor;
import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.gen.ExtensionDecl;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;
import pl.edu.mimuw.nesc.ast.gen.FunctionDeclarator;
import pl.edu.mimuw.nesc.ast.gen.NestedDeclarator;
import pl.edu.mimuw.nesc.ast.gen.TargetAttribute;
import pl.edu.mimuw.nesc.ast.gen.VariableDecl;
import pl.edu.mimuw.nesc.astutil.AstUtils;
import pl.edu.mimuw.nesc.astutil.DeclaratorUtils;
import pl.edu.mimuw.nesc.constexpr.ConstExprInterpreter;
import pl.edu.mimuw.nesc.constexpr.Interpreter;
import pl.edu.mimuw.nesc.constexpr.value.ConstantValue;
import pl.edu.mimuw.nesc.constexpr.value.IntegerConstantValue;
import pl.edu.mimuw.nesc.constexpr.value.type.ConstantType;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Assignment of functions to interrupts by addition of SDCC attribute
 * '__interrupt'.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class InterruptHandlerAssigner {
    /**
     * Name of the attribute that specifies the interrupt handler assignment.
     */
    private static final String ATTRIBUTE_INTERRUPT = "__interrupt";

    /**
     * Multimap that specifies interrupts that the functions are assigned to
     * (keys are unique names of functions and values are numbers of assigned
     * interrupts).
     */
    private final SetMultimap<String, Integer> interrupts;

    /**
     * Interpreter used for computation of numbers of already assigned
     * interrupts.
     */
    private final Interpreter interpreter;

    InterruptHandlerAssigner(SetMultimap<String, Integer> interrupts, ABI abi) {
        checkNotNull(abi, "ABI cannot be null");
        checkNotNull(interrupts, "interrupts cannot be null");
        this.interrupts = interrupts;
        this.interpreter = new ConstExprInterpreter(abi);
    }

    /**
     * Assign interrupt handlers to interrupts by adding SDCC attribute
     * '__interrupt' to declarations according to the multimap specified at
     * construction.
     *
     * @param declarations Declarations to modify.
     */
    public void assign(ImmutableList<Declaration> declarations) {
        checkNotNull(declarations, "declarations cannot be null");
        final AssigningVisitor assigningVisitor = new AssigningVisitor();
        for (Declaration declaration : declarations) {
            declaration.accept(assigningVisitor, null);
        }
    }

    private ImmutableSet<Integer> collectAssignedInterrupts(Iterable<? extends Attribute> attributes) {
        final ImmutableSet.Builder<Integer> assignedInterruptsBuilder = ImmutableSet.builder();
        final Iterable<TargetAttribute> targetAttributes = FluentIterable.from(attributes)
                .filter(TargetAttribute.class);
        final BigInteger maxInt = BigInteger.valueOf(Integer.MAX_VALUE);

        for (TargetAttribute attribute : targetAttributes) {
            if (!(attribute.getName().getName().equals(ATTRIBUTE_INTERRUPT))
                    || !attribute.getArguments().isPresent()
                    || attribute.getArguments().get().isEmpty()) {
                continue;
            } else if (attribute.getArguments().get().size() != 1) {
                throw new IllegalStateException("'__interrupt' attribute that has more than one parameter");
            }

            final Expression parameter = attribute.getArguments().get().getFirst();
            final ConstantValue value = interpreter.evaluate(parameter);

            if (value.getType().getType() != ConstantType.Type.SIGNED_INTEGER
                    && value.getType().getType() != ConstantType.Type.UNSIGNED_INTEGER) {
                throw new IllegalStateException("expression for '__interrupt' attribute does not evaluate to an integer at "
                        + attribute.getLocation());
            } else if (((IntegerConstantValue<?>) value).getValue().compareTo(maxInt) > 0) {
                throw new IllegalStateException("value of expression for '__interrupt' attribute exceeds "
                        + maxInt + " at " + attribute.getLocation());
            }

            assignedInterruptsBuilder.add(((IntegerConstantValue<?>) value).getValue().intValue());
        }

        return assignedInterruptsBuilder.build();
    }

    private void assignInterrupts(String funUniqueName, List<Attribute> funAttributes) {
        final ImmutableSet<Integer> assignedInterrupts = collectAssignedInterrupts(funAttributes);
        final Set<Integer> missingInterrupts = new HashSet<>(interrupts.get(funUniqueName));
        missingInterrupts.removeAll(assignedInterrupts);

        for (int missingInterrupt : missingInterrupts) {
            funAttributes.add(AstUtils.newTargetAttribute1(ATTRIBUTE_INTERRUPT,
                    AstUtils.newIntegerConstant(missingInterrupt)));
        }
    }

    /**
     * Visitor that actually performs the assignment.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class AssigningVisitor extends ExceptionVisitor<Void, Void> {
        @Override
        public Void visitFunctionDecl(FunctionDecl declaration, Void arg) {
            assignInterrupts(DeclaratorUtils.getUniqueName(declaration.getDeclarator()).get(),
                    declaration.getAttributes());
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
        public Void visitExtensionDecl(ExtensionDecl declaration, Void arg) {
            declaration.getDeclaration().accept(this, null);
            return null;
        }

        @Override
        public Void visitVariableDecl(VariableDecl declaration, Void arg) {
            final Optional<NestedDeclarator> deepestDeclarator =
                    DeclaratorUtils.getDeepestNestedDeclarator(declaration.getDeclarator().get());
            if (deepestDeclarator.isPresent() &&
                    deepestDeclarator.get() instanceof FunctionDeclarator) {
                assignInterrupts(DeclaratorUtils.getUniqueName(declaration.getDeclarator().get()).get(),
                        declaration.getAttributes());
            }
            return null;
        }
    }
}
